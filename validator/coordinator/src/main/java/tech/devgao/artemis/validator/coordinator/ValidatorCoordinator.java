/*
 * Copyright 2019 Developer Gao.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package tech.devgao.artemis.validator.coordinator;

import static java.lang.Math.toIntExact;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import org.apache.logging.log4j.Level;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.SECP256K1;
import org.apache.tuweni.units.bigints.UInt256;
import tech.devgao.artemis.datastructures.Constants;
import tech.devgao.artemis.datastructures.blocks.BeaconBlock;
import tech.devgao.artemis.datastructures.operations.Attestation;
import tech.devgao.artemis.datastructures.operations.Deposit;
import tech.devgao.artemis.datastructures.state.BeaconState;
import tech.devgao.artemis.datastructures.state.BeaconStateWithCache;
import tech.devgao.artemis.datastructures.util.AttestationUtil;
import tech.devgao.artemis.datastructures.util.BeaconStateUtil;
import tech.devgao.artemis.datastructures.util.DataStructureUtil;
import tech.devgao.artemis.services.ServiceConfig;
import tech.devgao.artemis.statetransition.GenesisHeadStateEvent;
import tech.devgao.artemis.statetransition.HeadStateEvent;
import tech.devgao.artemis.statetransition.StateTransition;
import tech.devgao.artemis.statetransition.StateTransitionException;
import tech.devgao.artemis.util.alogger.ALogger;
import tech.devgao.artemis.util.bls.BLSKeyPair;
import tech.devgao.artemis.util.bls.BLSPublicKey;
import tech.devgao.artemis.util.bls.BLSSignature;
import tech.devgao.artemis.util.hashtree.HashTreeUtil;
import tech.devgao.artemis.util.hashtree.HashTreeUtil.SSZTypes;

/** This class coordinates the activity between the validator clients and the beacon chain */
public class ValidatorCoordinator {
  private static final ALogger LOG = new ALogger(ValidatorCoordinator.class.getName());
  private final EventBus eventBus;

  private StateTransition stateTransition;
  private final Boolean printEnabled = false;
  private SECP256K1.SecretKey nodeIdentity;
  private int numValidators;
  private int numNodes;
  private BeaconBlock validatorBlock;
  private ArrayList<Deposit> newDeposits = new ArrayList<>();
  private final HashMap<BLSPublicKey, BLSKeyPair> validatorSet = new HashMap<>();
  static final Integer UNPROCESSED_BLOCKS_LENGTH = 100;
  private final PriorityBlockingQueue<Attestation> attestationsQueue =
      new PriorityBlockingQueue<>(
          UNPROCESSED_BLOCKS_LENGTH, Comparator.comparing(Attestation::getSlot));

  public ValidatorCoordinator(ServiceConfig config) {
    this.eventBus = config.getEventBus();
    this.eventBus.register(this);
    this.nodeIdentity =
        SECP256K1.SecretKey.fromBytes(Bytes32.fromHexString(config.getConfig().getIdentity()));
    this.numValidators = config.getConfig().getNumValidators();
    this.numNodes = config.getConfig().getNumNodes();

    initializeValidators();

    stateTransition = new StateTransition(printEnabled);
  }

  @Subscribe
  public void onNewSlot(Date date) {
    if (validatorBlock != null) {
      this.eventBus.post(validatorBlock);
      validatorBlock = null;
    }
  }

  @Subscribe
  public void onGenesisHeadStateEvent(GenesisHeadStateEvent genesisHeadStateEvent) {
    onNewHeadStateEvent(
        new HeadStateEvent(
            genesisHeadStateEvent.getHeadState(), genesisHeadStateEvent.getHeadBlock()));
    this.eventBus.post(true);
  }

  @Subscribe
  public void onNewHeadStateEvent(HeadStateEvent headStateEvent) {
    // Retrieve headState and headBlock from event
    BeaconStateWithCache headState = headStateEvent.getHeadState();
    BeaconBlock headBlock = headStateEvent.getHeadBlock();

    List<Attestation> attestations =
        AttestationUtil.createAttestations(headState, headBlock, validatorSet);

    for (Attestation attestation : attestations) {
      this.eventBus.post(attestation);
    }

    List<Attestation> blockAttestations = headBlock.getBody().getAttestations();
    synchronized (this.attestationsQueue) {
      for (Attestation blockAttestation : blockAttestations) {
        attestationsQueue.removeIf(attestation -> attestation.equals(blockAttestation));
      }
    }
    // Copy state so that state transition during block creation does not manipulate headState in
    // storage
    BeaconStateWithCache newHeadState = BeaconStateWithCache.deepCopy(headState);
    createBlockIfNecessary(newHeadState, headBlock);
  }

  @Subscribe
  public void onNewAttestation(Attestation attestation) {
    synchronized (this.attestationsQueue) {
      // Store attestations in a priority queue
      if (!attestationsQueue.contains(attestation)) {
        attestationsQueue.add(attestation);
      }
    }
  }

  private void initializeValidators() {
    // Add all validators to validatorSet hashMap
    int nodeCounter = UInt256.fromBytes(nodeIdentity.bytes()).mod(numNodes).intValue();
    // LOG.log(Level.DEBUG, "nodeCounter: " + nodeCounter);
    // if (nodeCounter == 0) {

    int startIndex = nodeCounter * (numValidators / numNodes);
    int endIndex =
        startIndex
            + (numValidators / numNodes - 1)
            + toIntExact(Math.round((double) nodeCounter / Math.max(1, numNodes - 1)));
    endIndex = Math.min(endIndex, numValidators - 1);
    // int startIndex = 0;
    // int endIndex = numValidators-1;
    LOG.log(Level.DEBUG, "startIndex: " + startIndex + " endIndex: " + endIndex);
    for (int i = startIndex; i <= endIndex; i++) {
      BLSKeyPair keypair = BLSKeyPair.random(i);
      LOG.log(Level.DEBUG, "i = " + i + ": " + keypair.getPublicKey().toString());
      validatorSet.put(keypair.getPublicKey(), keypair);
    }
    // }
  }

  private void createBlockIfNecessary(BeaconStateWithCache headState, BeaconBlock headBlock) {
    // Calculate the block proposer index, and if we have the
    // block proposer in our set of validators, produce the block
    Integer proposerIndex;
    BLSPublicKey proposerPubkey;
    // Implements change from 6.1 for validator client, quoting from spec:
    // "To see if a validator is assigned to proposer during the slot,
    // the validator must run an empty slot transition from the previous
    // state to the current slot."
    // However, this is only required on epoch changes, because otherwise
    // validator registry doesn't change anyway.
    if (headState.getSlot()
            .plus(UnsignedLong.ONE)
            .mod(UnsignedLong.valueOf(Constants.SLOTS_PER_EPOCH))
            .equals(UnsignedLong.ZERO)) {
      BeaconStateWithCache newState = BeaconStateWithCache.deepCopy(headState);
      try {
        stateTransition.initiate(newState, null);
      } catch (StateTransitionException e) {
        LOG.log(Level.WARN, e.toString(), printEnabled);
      }
      proposerIndex = BeaconStateUtil.get_beacon_proposer_index(
              newState, newState.getSlot());
      proposerPubkey = newState.getValidator_registry().get(proposerIndex).getPubkey();
    } else {
      proposerIndex = BeaconStateUtil.get_beacon_proposer_index(
              headState, headState.getSlot().plus(UnsignedLong.ONE));
      proposerPubkey = headState.getValidator_registry().get(proposerIndex).getPubkey();
    }
    if (validatorSet.containsKey(proposerPubkey)) {
      Bytes32 blockRoot = headBlock.signed_root("signature");
      createNewBlock(headState, blockRoot, validatorSet.get(proposerPubkey));
    }
  }

  private void createNewBlock(
      BeaconStateWithCache headState, Bytes32 blockRoot, BLSKeyPair keypair) {
    try {
      List<Attestation> current_attestations;
      final Bytes32 MockStateRoot = Bytes32.ZERO;
      BeaconBlock block;
      if (headState
              .getSlot()
              .compareTo(
                  UnsignedLong.valueOf(
                      Constants.GENESIS_SLOT + Constants.MIN_ATTESTATION_INCLUSION_DELAY))
          >= 0) {
        UnsignedLong attestation_slot =
            headState
                .getSlot()
                .minus(UnsignedLong.valueOf(Constants.MIN_ATTESTATION_INCLUSION_DELAY));

        current_attestations =
            AttestationUtil.getAttestationsUntilSlot(attestationsQueue, attestation_slot);

        block =
            DataStructureUtil.newBeaconBlock(
                headState.getSlot() + 1,
                blockRoot,
                MockStateRoot,
                newDeposits,
                current_attestations);
      } else {
        block =
            DataStructureUtil.newBeaconBlock(
                headState.getSlot() + 1, blockRoot, MockStateRoot, newDeposits, new ArrayList<>());
      }

      BLSSignature epoch_signature = setEpochSignature(headState, keypair);
      block.getBody().setRandao_reveal(epoch_signature);
      stateTransition.initiate(headState, block);
      Bytes32 stateRoot = headState.hash_tree_root();
      block.setState_root(stateRoot);
      BLSSignature signed_proposal = signProposalData(headState, block, keypair);
      block.setSignature(signed_proposal);
      validatorBlock = block;

      LOG.log(Level.INFO, "ValidatorCoordinator - NEWLY PRODUCED BLOCK", printEnabled);
      LOG.log(Level.INFO, "ValidatorCoordinator - block.slot: " + block.getSlot(), printEnabled);
      LOG.log(
          Level.INFO,
          "ValidatorCoordinator - block.parent_root: " + block.getPrevious_block_root(),
          printEnabled);
      LOG.log(
          Level.INFO,
          "ValidatorCoordinator - block.state_root: " + block.getState_root(),
          printEnabled);

      LOG.log(Level.INFO, "End ValidatorCoordinator", printEnabled);
    } catch (StateTransitionException e) {
      LOG.log(Level.WARN, e.toString(), printEnabled);
    }
  }

  private BLSSignature setEpochSignature(BeaconState state, BLSKeyPair keypair) {
    UnsignedLong slot = state.getSlot().plus(UnsignedLong.ONE);
    UnsignedLong epoch = BeaconStateUtil.slot_to_epoch(slot);

    Bytes32 messageHash =
        HashTreeUtil.hash_tree_root(SSZTypes.BASIC, SSZ.encodeUInt64(epoch.longValue()));
    UnsignedLong domain =
        BeaconStateUtil.get_domain(state.getFork(), epoch, Constants.DOMAIN_RANDAO);
    LOG.log(Level.INFO, "Sign Epoch", printEnabled);
    LOG.log(Level.INFO, "Proposer pubkey: " + keypair.getPublicKey(), printEnabled);
    LOG.log(Level.INFO, "state: " + state.hash_tree_root(), printEnabled);
    LOG.log(Level.INFO, "slot: " + slot, printEnabled);
    LOG.log(Level.INFO, "domain: " + domain, printEnabled);
    return BLSSignature.sign(keypair, messageHash, domain);
  }

  private BLSSignature signProposalData(BeaconState state, BeaconBlock block, BLSKeyPair keypair) {
    // Let proposal = Proposal(block.slot, BEACON_CHAIN_SHARD_NUMBER,
    //   signed_root(block, "signature"), block.signature).

    UnsignedLong domain =
        BeaconStateUtil.get_domain(
            state.getFork(),
            BeaconStateUtil.slot_to_epoch(UnsignedLong.valueOf(block.getSlot())),
            Constants.DOMAIN_BEACON_BLOCK);
    BLSSignature signature =
        BLSSignature.sign(keypair, block.signed_root("signature"), domain.longValue());
    LOG.log(Level.INFO, "Sign Proposal", printEnabled);
    LOG.log(Level.INFO, "Proposer pubkey: " + keypair.getPublicKey(), printEnabled);
    LOG.log(Level.INFO, "state: " + state.hash_tree_root(), printEnabled);
    LOG.log(Level.INFO, "block signature: " + signature.toString(), printEnabled);
    LOG.log(Level.INFO, "slot: " + state.getSlot(), printEnabled);
    LOG.log(Level.INFO, "domain: " + domain, printEnabled);
    return signature;
  }
}
