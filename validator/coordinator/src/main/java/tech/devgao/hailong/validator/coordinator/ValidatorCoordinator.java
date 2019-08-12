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

package tech.devgao.hailong.validator.coordinator;

import static tech.devgao.hailong.datastructures.util.AttestationUtil.getGenericAttestationData;
import static tech.devgao.hailong.datastructures.util.BeaconStateUtil.compute_epoch_at_slot;
import static tech.devgao.hailong.datastructures.util.BeaconStateUtil.get_domain;
import static tech.devgao.hailong.util.alogger.ALogger.STDOUT;
import static tech.devgao.hailong.util.async.SafeFuture.reportExceptions;
import static tech.devgao.hailong.util.config.Constants.DOMAIN_BEACON_ATTESTER;
import static tech.devgao.hailong.util.config.Constants.GENESIS_EPOCH;
import static tech.devgao.hailong.util.config.Constants.MAX_DEPOSITS;
import static tech.devgao.hailong.validator.coordinator.ValidatorCoordinatorUtil.getSignature;
import static tech.devgao.hailong.validator.coordinator.ValidatorCoordinatorUtil.isEpochStart;
import static tech.devgao.hailong.validator.coordinator.ValidatorCoordinatorUtil.isGenesis;
import static tech.devgao.hailong.validator.coordinator.ValidatorLoader.initializeValidators;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.UnsignedLong;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;
import org.apache.logging.log4j.Level;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import tech.devgao.hailong.datastructures.blocks.BeaconBlock;
import tech.devgao.hailong.datastructures.blocks.BeaconBlockBodyLists;
import tech.devgao.hailong.datastructures.blocks.Eth1Data;
import tech.devgao.hailong.datastructures.blocks.SignedBeaconBlock;
import tech.devgao.hailong.datastructures.operations.AggregateAndProof;
import tech.devgao.hailong.datastructures.operations.Attestation;
import tech.devgao.hailong.datastructures.operations.AttestationData;
import tech.devgao.hailong.datastructures.operations.Deposit;
import tech.devgao.hailong.datastructures.operations.ProposerSlashing;
import tech.devgao.hailong.datastructures.state.BeaconState;
import tech.devgao.hailong.datastructures.state.BeaconStateWithCache;
import tech.devgao.hailong.datastructures.state.Committee;
import tech.devgao.hailong.datastructures.state.Validator;
import tech.devgao.hailong.datastructures.util.AttestationUtil;
import tech.devgao.hailong.datastructures.util.DepositUtil;
import tech.devgao.hailong.datastructures.validator.AttesterInformation;
import tech.devgao.hailong.datastructures.validator.Signer;
import tech.devgao.hailong.statetransition.AttestationAggregator;
import tech.devgao.hailong.statetransition.BlockAttestationsPool;
import tech.devgao.hailong.statetransition.BlockProposalUtil;
import tech.devgao.hailong.statetransition.StateTransition;
import tech.devgao.hailong.statetransition.StateTransitionException;
import tech.devgao.hailong.statetransition.events.BlockImportedEvent;
import tech.devgao.hailong.statetransition.events.BlockProposedEvent;
import tech.devgao.hailong.statetransition.events.BroadcastAggregatesEvent;
import tech.devgao.hailong.statetransition.events.BroadcastAttestationEvent;
import tech.devgao.hailong.statetransition.events.ProcessedAggregateEvent;
import tech.devgao.hailong.statetransition.events.ProcessedAttestationEvent;
import tech.devgao.hailong.statetransition.util.EpochProcessingException;
import tech.devgao.hailong.statetransition.util.SlotProcessingException;
import tech.devgao.hailong.storage.ChainStorageClient;
import tech.devgao.hailong.storage.Store;
import tech.devgao.hailong.storage.events.SlotEvent;
import tech.devgao.hailong.storage.events.StoreInitializedEvent;
import tech.devgao.hailong.util.SSZTypes.Bitlist;
import tech.devgao.hailong.util.SSZTypes.SSZList;
import tech.devgao.hailong.util.bls.BLSPublicKey;
import tech.devgao.hailong.util.bls.BLSSignature;
import tech.devgao.hailong.util.config.HailongConfiguration;
import tech.devgao.hailong.util.config.Constants;
import tech.devgao.hailong.util.time.TimeProvider;

/** This class coordinates the activity between the validator clients and the the beacon chain */
public class ValidatorCoordinator {
  private final EventBus eventBus;
  private final Map<BLSPublicKey, ValidatorInfo> validators;
  private final StateTransition stateTransition;
  private final BlockProposalUtil blockCreator;
  private final SSZList<Deposit> newDeposits = new SSZList<>(Deposit.class, MAX_DEPOSITS);
  private final ChainStorageClient chainStorageClient;
  private final AttestationAggregator attestationAggregator;
  private final BlockAttestationsPool blockAttestationsPool;
  private Eth1DataCache eth1DataCache;
  private CommitteeAssignmentManager committeeAssignmentManager;

  //  maps slots to Lists of attestation informations
  //  (which contain information for our validators to produce attestations)
  private Map<UnsignedLong, List<AttesterInformation>> committeeAssignments = new HashMap<>();

  private LinkedBlockingQueue<ProposerSlashing> slashings = new LinkedBlockingQueue<>();

  public ValidatorCoordinator(
      TimeProvider timeProvider,
      EventBus eventBus,
      ChainStorageClient chainStorageClient,
      AttestationAggregator attestationAggregator,
      BlockAttestationsPool blockAttestationsPool,
      HailongConfiguration config) {
    this.eventBus = eventBus;
    this.chainStorageClient = chainStorageClient;
    this.stateTransition = new StateTransition(false);
    this.blockCreator = new BlockProposalUtil(stateTransition);
    this.validators = initializeValidators(config);
    this.attestationAggregator = attestationAggregator;
    this.blockAttestationsPool = blockAttestationsPool;
    this.eth1DataCache = new Eth1DataCache(eventBus, timeProvider);
    this.eventBus.register(this);
  }

  /*
  @Subscribe
  public void checkIfIncomingBlockObeysSlashingConditions(BeaconBlock block) {

    int proposerIndex =
        BeaconStateUtil.get_beacon_proposer_index(headState);
    Validator proposer = headState.getValidator_registry().get(proposerIndex);

    checkArgument(
        bls_verify(
            proposer.getPubkey(),
            block.signing_root("signature"),
            block.getSignature(),
            get_domain(
                headState,
                Constants.DOMAIN_BEACON_PROPOSER,
                get_current_epoch(headState))),
        "Proposer signature is invalid");

    BeaconBlockHeader blockHeader =
        new BeaconBlockHeader(
            block.getSlot(),
            block.getParent_root(),
            block.getState_root(),
            block.getBody().hash_tree_root(),
            block.getSignature());
    UnsignedLong headerSlot = blockHeader.getSlot();
    if (store.getBeaconBlockHeaders(proposerIndex).isPresent()) {
      List<BeaconBlockHeader> headers = store.getBeaconBlockHeaders(proposerIndex).get();
      headers.forEach(
          (header) -> {
            if (header.getSlot().equals(headerSlot)
                && !header.hash_tree_root().equals(blockHeader.hash_tree_root())
                && !proposer.isSlashed()) {
              ProposerSlashing slashing =
                  new ProposerSlashing(UnsignedLong.valueOf(proposerIndex), blockHeader, header);
              slashings.add(slashing);
            }
          });
    }
    this.store.addUnprocessedBlockHeader(proposerIndex, blockHeader);
  }
  */

  @Subscribe
  public void onStoreInitializedEvent(final StoreInitializedEvent event) {
    // Any deposits pre-genesis can be ignored.
    newDeposits.clear();

    final Store store = chainStorageClient.getStore();
    final Bytes32 head = chainStorageClient.getBestBlockRoot();
    final BeaconState genesisState = store.getBlockState(head);

    // Get validator indices of our own validators
    getIndicesOfOurValidators(genesisState, validators);

    this.committeeAssignmentManager =
        new CommitteeAssignmentManager(validators, committeeAssignments);
    eth1DataCache.startBeaconChainMode(genesisState);

    // Update committee assignments and subscribe to required committee indices for the next 2
    // epochs
    UnsignedLong genesisEpoch = UnsignedLong.valueOf(GENESIS_EPOCH);
    committeeAssignmentManager.updateCommitteeAssignments(genesisState, genesisEpoch, eventBus);
    committeeAssignmentManager.updateCommitteeAssignments(
        genesisState, genesisEpoch.plus(UnsignedLong.ONE), eventBus);
  }

  @Subscribe
  // TODO: make sure blocks that are produced right even after new slot to be pushed.
  public void onNewSlot(SlotEvent slotEvent) {
    UnsignedLong slot = slotEvent.getSlot();
    BeaconState headState =
        chainStorageClient.getStore().getBlockState(chainStorageClient.getBestBlockRoot());
    BeaconBlock headBlock =
        chainStorageClient.getStore().getBlock(chainStorageClient.getBestBlockRoot());

    // Copy state so that state transition during block creation
    // does not manipulate headState in storage
    if (!isGenesis(slot)) {
      createBlockIfNecessary(BeaconStateWithCache.fromBeaconState(headState), headBlock, slot);
    }
  }

  @Subscribe
  public void onProcessedAttestationEvent(ProcessedAttestationEvent event) {
    attestationAggregator.processAttestation(event.getAttestation());
  }

  @Subscribe
  public void onProcessedAggregateEvent(ProcessedAggregateEvent event) {
    blockAttestationsPool.addUnprocessedAggregateAttestationToQueue(event.getAttestation());
  }

  @Subscribe
  public void onBlockImported(BlockImportedEvent event) {
    event
        .getBlock()
        .getMessage()
        .getBody()
        .getAttestations()
        .forEach(blockAttestationsPool::addAggregateAttestationProcessedInBlock);
  }

  @Subscribe
  public void onNewDeposit(tech.devgao.hailong.pow.event.Deposit event) {
    STDOUT.log(Level.DEBUG, "New deposit received by ValidatorCoordinator");
    Deposit deposit = DepositUtil.convertDepositEventToOperationDeposit(event);
    if (!newDeposits.contains(deposit)) newDeposits.add(deposit);
  }

  @Subscribe
  public void onAttestationEvent(BroadcastAttestationEvent event) throws IllegalArgumentException {
    try {
      Store store = chainStorageClient.getStore();
      BeaconBlock headBlock = store.getBlock(event.getHeadBlockRoot());
      BeaconState headState = store.getBlockState(event.getHeadBlockRoot());
      UnsignedLong slot = event.getNodeSlot();

      if (!isGenesis(slot) && isEpochStart(slot)) {
        UnsignedLong epoch = compute_epoch_at_slot(slot);
        // NOTE: we get commmittee assignments for NEXT epoch
        reportExceptions(
            CompletableFuture.runAsync(
                () ->
                    committeeAssignmentManager.updateCommitteeAssignments(
                        headState, epoch.plus(UnsignedLong.ONE), eventBus)));
      }

      // Get attester information to prepare AttestationAggregator for new slot's aggregation
      List<AttesterInformation> attesterInformations = committeeAssignments.get(slot);

      // If our beacon node does have any attestation responsibilities for this slot
      if (attesterInformations == null) {
        return;
      }

      // Pass attestationAggregator all the attester information necessary
      // for aggregation
      attestationAggregator.updateAggregatorInformations(attesterInformations);

      asyncProduceAttestations(
          attesterInformations, headState, getGenericAttestationData(headState, headBlock));

      // Save headState to check for slashings
      //      this.headState = headState;
    } catch (IllegalArgumentException e) {
      STDOUT.log(Level.WARN, "Can not produce attestations or create a block" + e.toString());
    }
  }

  @Subscribe
  public void onAggregationEvent(BroadcastAggregatesEvent event) {
    List<AggregateAndProof> aggregateAndProofs = attestationAggregator.getAggregateAndProofs();
    for (AggregateAndProof aggregateAndProof : aggregateAndProofs) {
      this.eventBus.post(aggregateAndProof);
    }
    attestationAggregator.reset();
  }

  private void produceAttestations(
      BeaconState state,
      BLSPublicKey attester,
      int indexIntoCommittee,
      Committee committee,
      AttestationData genericAttestationData) {
    int commmitteSize = committee.getCommitteeSize();
    Bitlist aggregationBitfield =
        AttestationUtil.getAggregationBits(commmitteSize, indexIntoCommittee);
    AttestationData attestationData = genericAttestationData.withIndex(committee.getIndex());
    Bytes32 attestationMessage = AttestationUtil.getAttestationMessageToSign(attestationData);
    Bytes domain =
        get_domain(state, DOMAIN_BEACON_ATTESTER, attestationData.getTarget().getEpoch());

    BLSSignature signature = getSignature(validators, attestationMessage, domain, attester);
    Attestation attestation = new Attestation(aggregationBitfield, attestationData, signature);
    attestationAggregator.addOwnValidatorAttestation(new Attestation(attestation));
    this.eventBus.post(attestation);
  }

  private void createBlockIfNecessary(
      BeaconStateWithCache previousState, BeaconBlock previousBlock, UnsignedLong newSlot) {
    try {

      BeaconStateWithCache newState = BeaconStateWithCache.deepCopy(previousState);
      // Process empty slots up to the new slot
      stateTransition.process_slots(newState, newSlot, false);

      // Check if we should be proposing
      final BLSPublicKey proposer = blockCreator.getProposerForSlot(newState, newSlot);
      if (!validators.containsKey(proposer)) {
        // We're not proposing now
        return;
      }

      SignedBeaconBlock newBlock;
      // Collect attestations to include
      SSZList<Attestation> attestations = getAttestationsForSlot(newSlot);
      // Collect slashing to include
      final SSZList<ProposerSlashing> slashingsInBlock = getSlashingsForBlock(newState);
      // Collect deposits
      final SSZList<Deposit> deposits = getDepositsForBlock();

      final Signer signer = getSigner(proposer);
      Eth1Data eth1Data = eth1DataCache.get_eth1_vote(newState);
      final Bytes32 parentRoot = previousBlock.hash_tree_root();
      newBlock =
          blockCreator.createNewBlock(
              signer,
              newSlot,
              newState,
              parentRoot,
              eth1Data,
              attestations,
              slashingsInBlock,
              deposits);

      this.eventBus.post(new BlockProposedEvent(newBlock));
      STDOUT.log(Level.DEBUG, "Local validator produced a new block");

      if (validators.get(proposer).isNaughty()) {
        final SignedBeaconBlock naughtyBlock =
            blockCreator.createEmptyBlock(signer, newSlot, newState, parentRoot);
        this.eventBus.post(naughtyBlock);
      }
    } catch (SlotProcessingException | EpochProcessingException | StateTransitionException e) {
      STDOUT.log(Level.ERROR, "Error during block creation " + e.toString());
    }
  }

  private SSZList<Attestation> getAttestationsForSlot(final UnsignedLong slot) {
    SSZList<Attestation> attestations = BeaconBlockBodyLists.createAttestations();
    if (slot.compareTo(
            UnsignedLong.valueOf(
                Constants.GENESIS_SLOT + Constants.MIN_ATTESTATION_INCLUSION_DELAY))
        >= 0) {

      UnsignedLong attestation_slot =
          slot.minus(UnsignedLong.valueOf(Constants.MIN_ATTESTATION_INCLUSION_DELAY));

      attestations =
          blockAttestationsPool.getAggregatedAttestationsForBlockAtSlot(attestation_slot);
    }
    return attestations;
  }

  private SSZList<ProposerSlashing> getSlashingsForBlock(final BeaconState state) {
    SSZList<ProposerSlashing> slashingsForBlock = BeaconBlockBodyLists.createProposerSlashings();
    ProposerSlashing slashing = slashings.poll();
    while (slashing != null) {
      if (!state.getValidators().get(slashing.getProposer_index().intValue()).isSlashed()) {
        slashingsForBlock.add(slashing);
      }
      if (slashingsForBlock.size() >= slashingsForBlock.getMaxSize()) {
        break;
      }
      slashing = slashings.poll();
    }
    return slashingsForBlock;
  }

  private SSZList<Deposit> getDepositsForBlock() {
    // TODO - Look into how deposits should be managed, this seems wrong
    final SSZList<Deposit> deposits = BeaconBlockBodyLists.createDeposits();
    deposits.addAll(newDeposits);
    return deposits;
  }

  private Signer getSigner(BLSPublicKey signer) {
    return (message, domain) -> getSignature(validators, message, domain, signer);
  }

  @VisibleForTesting
  void asyncProduceAttestations(
      List<AttesterInformation> attesterInformations,
      BeaconState state,
      AttestationData genericAttestationData) {
    reportExceptions(
        CompletableFuture.runAsync(
            () ->
                attesterInformations
                    .parallelStream()
                    .forEach(
                        attesterInfo ->
                            produceAttestations(
                                state,
                                attesterInfo.getPublicKey(),
                                attesterInfo.getIndexIntoCommitee(),
                                attesterInfo.getCommittee(),
                                genericAttestationData))));
  }

  @VisibleForTesting
  static void getIndicesOfOurValidators(
      BeaconState state, Map<BLSPublicKey, ValidatorInfo> validators) {
    List<Validator> validatorRegistry = state.getValidators();
    IntStream.range(0, validatorRegistry.size())
        .forEach(
            i -> {
              if (validators.containsKey(validatorRegistry.get(i).getPubkey())) {
                STDOUT.log(
                    Level.DEBUG,
                    "owned index = " + i + ": " + validatorRegistry.get(i).getPubkey());
                validators.get(validatorRegistry.get(i).getPubkey()).setValidatorIndex(i);
              }
            });
  }
}
