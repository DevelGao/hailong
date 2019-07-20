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

package tech.devgao.hailong.datastructures.util;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.toIntExact;
import static tech.devgao.hailong.datastructures.Constants.DOMAIN_ATTESTATION;
import static tech.devgao.hailong.datastructures.Constants.EFFECTIVE_BALANCE_INCREMENT;
import static tech.devgao.hailong.datastructures.Constants.MAX_INDICES_PER_ATTESTATION;
import static tech.devgao.hailong.datastructures.Constants.MAX_VALIDATORS_PER_COMMITTEE;
import static tech.devgao.hailong.datastructures.Constants.SHARD_COUNT;
import static tech.devgao.hailong.datastructures.Constants.SLOTS_PER_EPOCH;
import static tech.devgao.hailong.datastructures.Constants.ZERO_HASH;
import static tech.devgao.hailong.datastructures.util.BeaconStateUtil.get_bitfield_bit;
import static tech.devgao.hailong.datastructures.util.BeaconStateUtil.get_current_epoch;
import static tech.devgao.hailong.datastructures.util.BeaconStateUtil.get_domain;
import static tech.devgao.hailong.datastructures.util.BeaconStateUtil.get_committee_count;
import static tech.devgao.hailong.datastructures.util.BeaconStateUtil.compute_start_slot_of_epoch;
import static tech.devgao.hailong.datastructures.util.BeaconStateUtil.min;
import static tech.devgao.hailong.datastructures.util.BeaconStateUtil.verify_bitfield;
import static tech.devgao.hailong.datastructures.util.CrosslinkCommitteeUtil.get_crosslink_committee;
import static tech.devgao.hailong.datastructures.util.CrosslinkCommitteeUtil.get_start_shard;
import static tech.devgao.hailong.util.bls.BLSAggregate.bls_aggregate_pubkeys;

import com.google.common.primitives.UnsignedLong;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import tech.devgao.hailong.datastructures.Constants;
import tech.devgao.hailong.datastructures.blocks.BeaconBlock;
import tech.devgao.hailong.datastructures.operations.Attestation;
import tech.devgao.hailong.datastructures.operations.AttestationData;
import tech.devgao.hailong.datastructures.operations.AttestationDataAndCustodyBit;
import tech.devgao.hailong.datastructures.operations.IndexedAttestation;
import tech.devgao.hailong.datastructures.state.BeaconState;
import tech.devgao.hailong.datastructures.state.CompactCommittee;
import tech.devgao.hailong.datastructures.state.Crosslink;
import tech.devgao.hailong.datastructures.state.CrosslinkCommittee;
import tech.devgao.hailong.datastructures.state.Validator;
import tech.devgao.hailong.util.bitwise.BitwiseOps;
import tech.devgao.hailong.util.bls.BLSPublicKey;
import tech.devgao.hailong.util.bls.BLSSignature;
import tech.devgao.hailong.util.bls.BLSVerify;
import tech.devgao.hailong.util.hashtree.HashTreeUtil;

public class AttestationUtil {

  /**
   * Returns true if the attestation is verified
   *
   * @param state
   * @param attestation
   * @return boolean
   */
  public static boolean verifyAttestation(BeaconState state, Attestation attestation) {
    return true;
  }

  public static List<Triple<BLSPublicKey, Integer, CrosslinkCommittee>> getAttesterInformation(
      BeaconState headState,
      HashMap<UnsignedLong, List<Triple<List<Integer>, UnsignedLong, Integer>>>
          committeeAssignments) {

    UnsignedLong slot = headState.getSlot();
    List<Triple<List<Integer>, UnsignedLong, Integer>> committeeAssignmentsForSlot =
        committeeAssignments.get(slot);
    List<Triple<BLSPublicKey, Integer, CrosslinkCommittee>> attesters = new ArrayList<>();
    if (committeeAssignmentsForSlot != null) {
      for (int i = 0; i < committeeAssignmentsForSlot.size(); i++) {
        int validatorIndex = committeeAssignmentsForSlot.get(i).getRight();
        List<Integer> committee = committeeAssignmentsForSlot.get(i).getLeft();
        UnsignedLong shard = committeeAssignmentsForSlot.get(i).getMiddle();
        int indexIntoCommittee = committee.indexOf(validatorIndex);

        CrosslinkCommittee crosslinkCommittee = new CrosslinkCommittee(shard, committee);
        attesters.add(
            new MutableTriple<>(
                headState.getValidator_registry().get(validatorIndex).getPubkey(),
                indexIntoCommittee,
                crosslinkCommittee));
      }
    }
    return attesters;
  }

  // Get attestation data that does not include attester specific shard or crosslink information
  public static AttestationData getGenericAttestationData(BeaconState state, BeaconBlock block) {
    // Get variables necessary that can be shared among Attestations of all validators
    UnsignedLong slot = state.getSlot();
    Bytes32 headBlockRoot = block.signing_root("signature");
    Bytes32 crosslinkDataRoot = Bytes32.ZERO;
    UnsignedLong epochStartSlot = compute_start_slot_of_epoch(BeaconStateUtil.get_current_epoch(state));
    Bytes32 epochBoundaryRoot;
    if (epochStartSlot.compareTo(slot) == 0) {
      epochBoundaryRoot = block.signing_root("signature");
    } else {
      epochBoundaryRoot = BeaconStateUtil.get_block_root(state, get_current_epoch(state));
    }
    UnsignedLong sourceEpoch = state.getCurrent_justified_chekpoint();
    Bytes32 sourceRoot = state.getCurrent_justified_root();
    UnsignedLong targetEpoch = get_current_epoch(state);

    // Set attestation data
    return new AttestationData(
        headBlockRoot, sourceEpoch, sourceRoot, targetEpoch, epochBoundaryRoot, new Crosslink());
  }

  public static Bytes getAggregationBitfield(int indexIntoCommittee, int arrayLength) {
    // Create aggregation bitfield
    byte[] aggregationBitfield = new byte[arrayLength];
    aggregationBitfield[indexIntoCommittee / 8] =
        (byte)
            (aggregationBitfield[indexIntoCommittee / 8]
                | (byte) Math.pow(2, (indexIntoCommittee % 8)));
    return Bytes.wrap(aggregationBitfield);
  }

  public static Bytes getCustodyBitfield(int arrayLength) {
    return Bytes.wrap(new byte[arrayLength]);
  }

  public static AttestationData completeAttestationData(
      BeaconState state, AttestationData attestationData, CrosslinkCommittee committee) {
    UnsignedLong shard = committee.getShard();
    Crosslink parent_crosslink = state.getCurrent_crosslinks().get(shard.intValue());
    UnsignedLong end_epoch =
        min(
            attestationData.getTarget_epoch(),
            parent_crosslink
                .getEnd_epoch()
                .plus(UnsignedLong.valueOf(Constants.MAX_EPOCHS_PER_CROSSLINK)));
    Crosslink crosslink =
        new Crosslink(
            shard,
            parent_crosslink.getEnd_epoch(),
            end_epoch,
            state.getCurrent_crosslinks().get(shard.intValue()).hash_tree_root(),
            ZERO_HASH);
    attestationData.setCrosslink(crosslink);
    return attestationData;
  }

  public static Bytes32 getAttestationMessageToSign(AttestationData attestationData) {
    AttestationDataAndCustodyBit attestation_data_and_custody_bit =
        new AttestationDataAndCustodyBit(attestationData, false);
    return attestation_data_and_custody_bit.hash_tree_root();
  }

  public static int getDomain(BeaconState state, AttestationData attestationData) {
    return toIntExact(
        get_domain(state, Constants.DOMAIN_ATTESTATION, attestationData.getTarget_epoch()));
  }

  /**
   * Check if ``data_1`` and ``data_2`` are slashable according to Casper FFG rules.
   *
   * @param data_1
   * @param data_2
   * @return
   * @see
   *     <a>https://github.com/ethereum/eth2.0-specs/blob/v0.8.0/specs/core/0_beacon-chain.md#is_slashable_attestation_data</a>
   */
  public static boolean is_slashable_attestation_data(
      AttestationData data_1, AttestationData data_2) {
    return (
    // case 1: double vote || case 2: surround vote
    (!data_1.equals(data_2) && data_1.getTarget().getEpoch().equals(data_2.getTarget().getEpoch()))
        || (data_1.getSource().getEpoch().compareTo(data_2.getSource().getEpoch()) < 0
            && data_2.getTarget().getEpoch().compareTo(data_1.getTarget().getEpoch()) < 0));
  }

  /**
   * Return the indexed attestation corresponding to ``attestation``.
   *
   * @param state
   * @param attestation
   * @return
   * @see
   *     <a>https://github.com/ethereum/eth2.0-specs/blob/v0.8.0/specs/core/0_beacon-chain.md#get_indexed_attestation</a>
   */
  public static IndexedAttestation get_indexed_attestation(BeaconState state, Attestation attestation) {
    List<Integer> attesting_indices =
        get_attesting_indices(state, attestation.getData(), attestation.getAggregation_bitfield());
    List<Integer> custody_bit_1_indices =
        get_attesting_indices(state, attestation.getData(), attestation.getCustody_bitfield());

    checkArgument(attesting_indices.containsAll(custody_bit_1_indices),
            "get_indexed_attestation: custody_bit_1_indices is not a subset of attesting_indices");

    List<Integer> custody_bit_0_indices = new ArrayList<>();
    for (int i = 0; i < attesting_indices.size(); i++) {
      Integer attesting_index = attesting_indices.get(i);
      if (!custody_bit_1_indices.contains(attesting_index)) custody_bit_0_indices.add(attesting_index);
    }
    return new IndexedAttestation(
        custody_bit_0_indices,
        custody_bit_1_indices,
        attestation.getData(),
        attestation.getAggregate_signature());
  }

  /**
   * Return the sorted attesting indices corresponding to ``attestation_data`` and ``bits``.
   *
   * @param state
   * @param attestation_data
   * @param bits
   * @return
   * @throws IllegalArgumentException
   * @see
   *     <a>https://github.com/ethereum/eth2.0-specs/blob/v0.8.0/specs/core/0_beacon-chain.md#get_attesting_indices</a>
   */
  public static List<Integer> get_attesting_indices(
      BeaconState state, AttestationData attestation_data, Bytes bits){
    List<Integer> committee =
        get_crosslink_committee(
            state, attestation_data.getTarget().getEpoch(), attestation_data.getCrosslink().getShard());

    HashSet<Integer> attesting_indices = new HashSet<>();
    for (int i = 0; i < committee.size(); i++) {
      int index = committee.get(i);
      int bitfieldBit = get_bitfield_bit(bits, i);
      if ((bitfieldBit & 1) == 1) attesting_indices.add(index);
    }
    return new ArrayList<>(attesting_indices);
  }

  /**
   * Verify validity of ``indexed_attestation``.
   *
   * @param state
   * @param indexed_attestation
   * @see
   *     <a>https://github.com/ethereum/eth2.0-specs/blob/v0.8.0/specs/core/0_beacon-chain.md#is_valid_indexed_attestation</a>
   */
  public static Boolean is_valid_indexed_attestation(
      BeaconState state, IndexedAttestation indexed_attestation) {
    List<Integer> bit_0_indices = indexed_attestation.getCustody_bit_0_indices();
    List<Integer> bit_1_indices = indexed_attestation.getCustody_bit_1_indices();

    if (!(bit_1_indices.size() == 0)) {
      System.out.println("AttestationUtil.is_valid_indexed_attestation: Verify no index has custody bit equal to 1 [to be removed in phase 1]");
      return false;
    }
    if (!((bit_0_indices.size() + bit_1_indices.size()) <= MAX_VALIDATORS_PER_COMMITTEE)){
      System.out.println("AttestationUtil.is_valid_indexed_attestation: Verify max number of indices");
      return false;
    }
    if (!(intersection(bit_0_indices, bit_1_indices).size() == 0)) {
      System.out.println("AttestationUtil.is_valid_indexed_attestation: Verify index sets are disjoint");
      return false;
    }
    List<Integer> bit_0_indices_sorted = new ArrayList<Integer>(bit_0_indices);
    Collections.sort(bit_0_indices_sorted);
    List<Integer> bit_1_indices_sorted = new ArrayList<Integer>(bit_1_indices);
    Collections.sort(bit_1_indices_sorted);
    if (!(bit_0_indices.equals(bit_0_indices_sorted) && bit_1_indices.equals(bit_1_indices_sorted))) {
      System.out.println("AttestationUtil.is_valid_indexed_attestation: Verify indices are sorted");
      return false;
    }

    List<Validator> validators = state.getValidators();
    List<BLSPublicKey> pubkeys = new ArrayList<>();
    pubkeys.add(
        bls_aggregate_pubkeys(
            bit_0_indices.stream()
                .map(i -> validators.get(i).getPubkey())
                .collect(Collectors.toList())));
    pubkeys.add(
        bls_aggregate_pubkeys(
            bit_1_indices.stream()
                .map(i -> validators.get(i).getPubkey())
                .collect(Collectors.toList())));

    List<Bytes32> message_hashes = new ArrayList<>();
    message_hashes.add(
        new AttestationDataAndCustodyBit(indexed_attestation.getData(), false).hash_tree_root());
    message_hashes.add(
        new AttestationDataAndCustodyBit(indexed_attestation.getData(), true).hash_tree_root());

    BLSSignature signature = indexed_attestation.getSignature();
    int domain =
        get_domain(state, DOMAIN_ATTESTATION, indexed_attestation.getData().getTarget().getEpoch());
    if (!BLSVerify.bls_verify_multiple(pubkeys, message_hashes, signature, domain)) {
      System.out.println("AttestationUtil.is_valid_indexed_attestation: Verify aggregate signature");
      return false;
    }
    return true;
  }

  /**
   *  Return the slot corresponding to the attestation ``data``.
   *
   * @param state
   * @param data
   * @return
   * @see
   *     <a>https://github.com/ethereum/eth2.0-specs/blob/v0.8.0/specs/core/0_beacon-chain.md#get_attestation_data_slot</a>
   */
  public static UnsignedLong get_attestation_data_slot(BeaconState state, AttestationData data) {
    UnsignedLong committee_count = get_committee_count(state, data.getTarget().getEpoch());
    UnsignedLong offset =
        data.getCrosslink()
            .getShard()
            .plus(UnsignedLong.valueOf(SHARD_COUNT))
            .minus(get_start_shard(state, data.getTarget().getEpoch()))
            .mod(UnsignedLong.valueOf(SHARD_COUNT));
    return compute_start_slot_of_epoch(data.getTarget().getEpoch())
        .plus(offset.dividedBy(committee_count.dividedBy(UnsignedLong.valueOf(SLOTS_PER_EPOCH))));
  }

  /**
   *  Return the compact committee root at ``epoch``.
   *
   * @param state
   * @param epoch
   * @return
   * @see
   *     <a>https://github.com/ethereum/eth2.0-specs/blob/v0.8.0/specs/core/0_beacon-chain.md#get_compact_committees_root</a>
   */
  public static Bytes32 get_compact_committees_root(BeaconState state, UnsignedLong epoch) {
    List<CompactCommittee> committees = new ArrayList<>();
    for (int i = 0; i < SHARD_COUNT; i++) {
      CompactCommittee newCommittee = new CompactCommittee();
      committees.add(newCommittee);
    }
    int start_shard = get_start_shard(state, epoch).intValue();
    int committee_count = get_committee_count(state, epoch).intValue();
    for (int committee_number = 0; committee_number < committee_count; committee_number++) {
      int shard = (start_shard + committee_number) % SHARD_COUNT;
      List<Integer> crosslink_committee = get_crosslink_committee(state, epoch, UnsignedLong.valueOf(shard));
      for (Integer index : crosslink_committee) {
        Validator validator = state.getValidators().get(index);
        committees.get(shard).getPubkeys().add(validator.getPubkey());
        UnsignedLong compact_balance = validator.getEffective_balance().dividedBy(UnsignedLong.valueOf(EFFECTIVE_BALANCE_INCREMENT));
        UnsignedLong index16leftShift = BitwiseOps.leftShift(UnsignedLong.valueOf(index), 16);
        UnsignedLong slashed15leftShift = UnsignedLong.ZERO;
        if (validator.isSlashed()) {
          slashed15leftShift = BitwiseOps.leftShift(UnsignedLong.ONE, 15);
        }
        UnsignedLong compact_validator = index16leftShift.plus(slashed15leftShift).plus(compact_balance);
        committees.get(shard).getCompact_validators().add(compact_validator);
      }
    }
    // TODO serialization might be an issue here
    return HashTreeUtil.hash_tree_root(HashTreeUtil.SSZTypes.LIST_OF_COMPOSITE, committees);
  }

  public static <T> List<T> intersection(List<T> list1, List<T> list2) {
    List<T> list = new ArrayList<T>();

    for (T t : list1) {
      if (list2.contains(t)) {
        list.add(t);
      }
    }

    return list;
  }
}