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

package tech.devgao.artemis.statetransition.util;

import java.util.ArrayList;
import net.develgao.cava.bytes.Bytes32;
import tech.devgao.artemis.datastructures.Constants;
import tech.devgao.artemis.datastructures.state.CrosslinkCommittee;
import tech.devgao.artemis.datastructures.state.PendingAttestationRecord;
import tech.devgao.artemis.statetransition.BeaconState;

public class AttestationUtil {

  public static ArrayList<PendingAttestationRecord> get_current_epoch_attestations(
      BeaconState state) {
    ArrayList<PendingAttestationRecord> latest_attestations = state.getLatest_attestations();
    ArrayList<PendingAttestationRecord> current_epoch_attestations = new ArrayList<>();
    if (latest_attestations != null) {
      for (PendingAttestationRecord record : latest_attestations) {
        if (isAttestationCurrentEpoch(state, record)) current_epoch_attestations.add(record);
      }
    }
    return current_epoch_attestations;
  }

  public static ArrayList<PendingAttestationRecord> get_previous_epoch_attestations(
      BeaconState state) {
    ArrayList<PendingAttestationRecord> previous_epoch_attestations = new ArrayList<>();
    ArrayList<PendingAttestationRecord> current_epoch_attestation =
        get_current_epoch_attestations(state);

    for (PendingAttestationRecord record : current_epoch_attestation) {
      if ((state.getSlot() - 2 * Constants.EPOCH_LENGTH) <= record.getData().getSlot()
          && record.getData().getSlot() < state.getSlot() - Constants.EPOCH_LENGTH)
        previous_epoch_attestations.add(record);
    }
    return previous_epoch_attestations;
  }

  private static boolean isAttestationCurrentEpoch(
      BeaconState state, PendingAttestationRecord record) {
    long epoch_lower_boundary = state.getSlot() - Constants.EPOCH_LENGTH;
    long epoch_upper_boundary = state.getSlot();
    return (record.getData().getSlot() <= epoch_lower_boundary
        && record.getData().getSlot() > epoch_upper_boundary);
  }

  public static ArrayList<PendingAttestationRecord> get_current_epoch_boundary_attestations(
      BeaconState state, ArrayList<PendingAttestationRecord> current_epoch_attestations)
      throws Exception {
    ArrayList<PendingAttestationRecord> current_epoch_boundary_attestations = new ArrayList<>();
    if (current_epoch_attestations != null) {
      for (PendingAttestationRecord record : current_epoch_boundary_attestations) {
        if (record
                .getData()
                .getEpoch_boundary_hash()
                .equals(
                    BeaconStateUtil.get_block_root(
                        state, record.getData().getSlot() - Constants.EPOCH_LENGTH))
            && record.getData().getJustified_slot().longValue() == state.getJustified_slot())
          current_epoch_attestations.add(record);
      }
    }
    return current_epoch_boundary_attestations;
  }

  public static double get_previous_epoch_boundary_attesting_balance(BeaconState state)
      throws Exception {
    // todo
    return 0.0d;
  }

  public static double get_current_epoch_boundary_attesting_balance(BeaconState state) {
    // todo
    return 0.0d;
  }

  public static int ceil_div8(int input) {
    return (int) Math.ceil(((double) input) / 8.0d);
  }

  public static double getTotal_attesting_balance(BeaconState state) {
    //    total_attesting_balance(crosslink_committee) = sum([get_effective_balance(state, i) for i
    // in attesting_validators(crosslink_committee)])
    return 0.0d;
  }

  public static ArrayList<Integer> attesting_validator_indices(
      BeaconState state, CrosslinkCommittee crosslink_committee, Bytes32 shard_block_root)
      throws BlockValidationException {
    ArrayList<PendingAttestationRecord> combined_attestations =
        get_current_epoch_attestations(state);
    combined_attestations.addAll(get_previous_epoch_attestations(state));

    for (PendingAttestationRecord record : combined_attestations) {
      if (record.getData().getShard().compareTo(crosslink_committee.getShard()) == 0
          && record.getData().getShard_block_hash() == shard_block_root) {
        return BeaconState.get_attestation_participants(
            state, record.getData(), record.getParticipation_bitfield().toArray());
      }
    }
    throw new BlockValidationException("attesting_validator_indicies appear to be empty");
  }
}
