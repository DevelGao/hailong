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

import static tech.devgao.artemis.datastructures.Constants.BASE_REWARD_QUOTIENT;
import static tech.devgao.artemis.datastructures.Constants.INACTIVITY_PENALTY_QUOTIENT;
import static tech.devgao.artemis.datastructures.Constants.MAX_DEPOSIT_AMOUNT;
import static tech.devgao.artemis.datastructures.Constants.MIN_ATTESTATION_INCLUSION_DELAY;
import static tech.devgao.artemis.statetransition.util.BeaconStateUtil.get_effective_balance;
import static tech.devgao.artemis.statetransition.util.BeaconStateUtil.get_total_effective_balance;

import com.google.common.collect.Sets;
import com.google.common.primitives.UnsignedLong;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.develgao.cava.bytes.Bytes32;
import tech.devgao.artemis.datastructures.Constants;
import tech.devgao.artemis.datastructures.state.Crosslink;
import tech.devgao.artemis.datastructures.state.CrosslinkCommittee;
import tech.devgao.artemis.datastructures.state.Validator;
import tech.devgao.artemis.statetransition.BeaconState;
import tech.devgao.artemis.util.bitwise.BitwiseOps;

public class EpochProcessorUtil {

  /**
   * Update Justification state fields
   *
   * @param state
   * @throws Exception
   */
  public static void updateJustification(BeaconState state) throws Exception {
    // Get previous and current epoch
    UnsignedLong current_epoch = BeaconStateUtil.get_current_epoch(state);
    UnsignedLong previous_epoch = BeaconStateUtil.get_previous_epoch(state);

    // Get previous and current epoch total balances
    List<Integer> current_active_validators =
        ValidatorsUtil.get_active_validator_indices(state.getValidator_registry(), current_epoch);
    List<Integer> previous_active_validators =
        ValidatorsUtil.get_active_validator_indices(state.getValidator_registry(), previous_epoch);
    UnsignedLong current_total_balance =
        get_total_effective_balance(state, current_active_validators);
    UnsignedLong previous_total_balance =
        get_total_effective_balance(state, previous_active_validators);

    // Update justification bitfield
    UnsignedLong new_justified_epoch = state.getJustified_epoch();
    UnsignedLong justification_bitfield = state.getJustification_bitfield();
    justification_bitfield = BitwiseOps.leftShift(justification_bitfield, 1);

    if (AttestationUtil.get_previous_epoch_boundary_attesting_balance(state)
            .times(UnsignedLong.valueOf(3))
            .compareTo(previous_total_balance.times(UnsignedLong.valueOf(2)))
        >= 0) {
      justification_bitfield = BitwiseOps.or(justification_bitfield, UnsignedLong.valueOf(2));
      new_justified_epoch = previous_epoch;
    }
    if (AttestationUtil.get_current_epoch_boundary_attesting_balance(state)
            .times(UnsignedLong.valueOf(3))
            .compareTo(current_total_balance.times(UnsignedLong.valueOf(2)))
        >= 0) {
      justification_bitfield = BitwiseOps.or(justification_bitfield, UnsignedLong.ONE);
      new_justified_epoch = current_epoch;
    }

    state.setJustification_bitfield(justification_bitfield);

    // Update last finalized epoch if possible
    UnsignedLong decimal4 = UnsignedLong.valueOf(4);
    UnsignedLong decimal8 = UnsignedLong.valueOf(8);
    UnsignedLong binary11 = UnsignedLong.valueOf(3);
    UnsignedLong binary111 = UnsignedLong.valueOf(7);
    UnsignedLong previous_justified_epoch = state.getPrevious_justified_epoch();
    UnsignedLong justified_epoch = state.getJustified_epoch();

    if (BitwiseOps.rightShift(justification_bitfield, 1).mod(decimal8).equals(binary111)
        && previous_justified_epoch.equals(previous_epoch.minus(UnsignedLong.valueOf(2)))) {
      state.setFinalized_epoch(previous_justified_epoch);
    }
    if (BitwiseOps.rightShift(justification_bitfield, 1).mod(decimal4).equals(binary11)
        && previous_justified_epoch.equals(previous_epoch.minus(UnsignedLong.ONE))) {
      state.setFinalized_epoch(previous_justified_epoch);
    }
    if (justification_bitfield.mod(decimal8).equals(binary111)
        && justified_epoch.equals(previous_epoch.minus(UnsignedLong.ONE))) {
      state.setFinalized_epoch(justified_epoch);
    }
    if (justification_bitfield.mod(decimal4).equals(binary11)
        && justified_epoch.equals(previous_epoch)) {
      state.setFinalized_epoch(justified_epoch);
    }

    // Update state justification variables
    state.setPrevious_justified_epoch(state.getJustified_epoch());
    state.setJustified_epoch(new_justified_epoch);
  }

  /**
   * Update latest crosslinks per shard in the state Spec:
   * https://github.com/ethereum/eth2.0-specs/blob/v0.1/specs/core/0_beacon-chain.md#crosslinks
   *
   * @param state
   * @throws Exception
   */
  public static void updateCrosslinks(BeaconState state) throws Exception {
    UnsignedLong current_epoch = BeaconStateUtil.get_current_epoch(state);
    UnsignedLong slot = state.getSlot();
    UnsignedLong end = UnsignedLong.valueOf(2 * Constants.EPOCH_LENGTH);
    while (slot.compareTo(end) < 0) {
      List<CrosslinkCommittee> crosslink_committees_at_slot =
          BeaconStateUtil.get_crosslink_committees_at_slot(state, slot);
      for (CrosslinkCommittee crosslink_committee : crosslink_committees_at_slot) {
        UnsignedLong shard = crosslink_committee.getShard();
        // TODO: This doesn't seem right. How is the winning root calculated?
        Bytes32 shard_block_root =
            state
                .getLatest_crosslinks()
                .get(Math.toIntExact(shard.longValue()))
                .getShard_block_root();
        UnsignedLong total_attesting_balance =
            AttestationUtil.total_attesting_balance(state, crosslink_committee, shard_block_root);
        if (UnsignedLong.valueOf(3L)
                .times(total_attesting_balance)
                .compareTo(total_balance(crosslink_committee))
            >= 0) {
          state
              .getLatest_crosslinks()
              .set(
                  Math.toIntExact(shard.longValue()),
                  new Crosslink(current_epoch, shard_block_root));
        }
      }
      slot = slot.plus(UnsignedLong.ONE);
    }
  }

  /**
   * Rewards and penalties applied with respect to justification and finalization. Spec:
   * https://github.com/ethereum/eth2.0-specs/blob/v0.1/specs/core/0_beacon-chain.md#justification-and-finalization
   *
   * @param state
   */
  public static void justificationAndFinalization(BeaconState state) throws Exception {

    final UnsignedLong FOUR = UnsignedLong.valueOf(4L);
    UnsignedLong epochs_since_finality =
        BeaconStateUtil.get_next_epoch(state).minus(state.getFinalized_epoch());
    UnsignedLong previous_total_balance = previous_total_balance(state);
    List<UnsignedLong> balances = state.getValidator_balances();

    // Case 1: epochs_since_finality <= 4:
    if (epochs_since_finality.compareTo(FOUR) <= 0) {
      // Expected FFG source
      UnsignedLong previous_balance =
          AttestationUtil.get_previous_epoch_justified_attesting_balance(state);
      List<Integer> previous_indices =
          AttestationUtil.get_previous_epoch_justified_attester_indices(state);
      case_one_penalties_and_rewards(
          state, balances, previous_total_balance, previous_balance, previous_indices);

      // Expected FFG target
      previous_balance = AttestationUtil.get_previous_epoch_boundary_attesting_balance(state);
      previous_indices = AttestationUtil.get_previous_epoch_boundary_attester_indices(state);
      case_one_penalties_and_rewards(
          state, balances, previous_total_balance, previous_balance, previous_indices);

      // Expected beacon chain head
      previous_balance = AttestationUtil.get_previous_epoch_head_attesting_balance(state);
      previous_indices = AttestationUtil.get_previous_epoch_head_attester_indices(state);
      case_one_penalties_and_rewards(
          state, balances, previous_total_balance, previous_balance, previous_indices);

      // Inclusion distance
      UnsignedLong reward_delta = UnsignedLong.ZERO;
      previous_indices = AttestationUtil.get_previous_epoch_attester_indices(state);
      for (int index : previous_indices) {
        UnsignedLong inclusion_distance = AttestationUtil.inclusion_distance(state, index);
        reward_delta =
            base_reward(state, index, previous_total_balance)
                .times(UnsignedLong.valueOf(MIN_ATTESTATION_INCLUSION_DELAY))
                .dividedBy(inclusion_distance);
        apply_penalty_or_reward(balances, index, reward_delta, true);
      }

      // Case 2: epochs_since_finality > 4:
    } else if (epochs_since_finality.compareTo(FOUR) > 0) {

      Function<Integer, UnsignedLong> inactivity_penalty =
          new Function<Integer, UnsignedLong>() {
            @Override
            public UnsignedLong apply(Integer index) {
              return inactivity_penality(
                  state, index, epochs_since_finality, previous_total_balance);
            }
          };

      Function<Integer, UnsignedLong> base_penalty =
          new Function<Integer, UnsignedLong>() {
            @Override
            public UnsignedLong apply(Integer index) {
              return base_reward(state, index, previous_total_balance);
            }
          };

      // prev epoch justified attester
      List<Integer> validator_indices =
          AttestationUtil.get_previous_epoch_justified_attester_indices(state);
      case_two_penalties_and_rewards(state, balances, validator_indices, inactivity_penalty);
      // prev epoch boundary attester
      validator_indices = AttestationUtil.get_previous_epoch_boundary_attester_indices(state);
      case_two_penalties_and_rewards(state, balances, validator_indices, inactivity_penalty);
      // prev epoch head attester
      validator_indices = AttestationUtil.get_previous_epoch_head_attester_indices(state);
      case_two_penalties_and_rewards(state, balances, validator_indices, base_penalty);

      Function<Integer, UnsignedLong> composite_penalty =
          new Function<Integer, UnsignedLong>() {
            @Override
            public UnsignedLong apply(Integer index) {
              return UnsignedLong.valueOf(2L)
                  .times(
                      inactivity_penality(
                          state, index, epochs_since_finality, previous_total_balance))
                  .plus(base_reward(state, index, previous_total_balance));
            }
          };

      validator_indices =
          IntStream.range(0, state.getValidator_registry().size())
              .boxed()
              .collect(Collectors.toList());
      case_two_penalties_and_rewards(state, balances, validator_indices, composite_penalty);

      // TODO: implement this on the next rewards and penalties PR.  tracked in issue #296
      // Any validator index in previous_epoch_attester_indices loses base_reward(state, index) -
      // base_reward(state, index) * MIN_ATTESTATION_INCLUSION_DELAY // inclusion_distance(state,
      // index)

    }
  }

  // Helper method for justificationAndFinalization()
  static void case_one_penalties_and_rewards(
      BeaconState state,
      List<UnsignedLong> balances,
      UnsignedLong previous_total_balance,
      UnsignedLong previous_balance,
      List<Integer> previous_indices) {
    UnsignedLong reward_delta = UnsignedLong.ZERO;
    // make a list of integers from 0 to numberOfValidators
    List<Integer> missing_indices =
        IntStream.range(0, previous_indices.size()).boxed().collect(Collectors.toList());
    // apply rewards to validator indices in the list
    for (int index : previous_indices) {
      reward_delta =
          base_reward(state, index, previous_total_balance)
              .times(previous_balance)
              .dividedBy(previous_total_balance);
      apply_penalty_or_reward(balances, index, reward_delta, true);
      missing_indices.remove(index);
    }
    // apply penalties to active validator indices not in the list
    for (int index : missing_indices) {
      if (ValidatorsUtil.is_active_validator_index(
          state, index, BeaconStateUtil.get_current_epoch(state))) {
        reward_delta =
            base_reward(state, index, previous_total_balance)
                .times(previous_balance)
                .dividedBy(previous_total_balance);
        apply_penalty_or_reward(balances, index, reward_delta, false);
      }
    }
  }

  // Helper method for justificationAndFinalization()
  static void case_two_penalties_and_rewards(
      BeaconState state,
      List<UnsignedLong> balances,
      List<Integer> validator_indices,
      Function<Integer, UnsignedLong> penalty) {
    // make a list of integers from 0 to numberOfValidators
    List<Integer> all_indices =
        IntStream.range(0, validator_indices.size()).boxed().collect(Collectors.toList());
    Set<Integer> set_of_indices = Sets.newHashSet(all_indices);
    Set<Integer> set_of_validator_indices = Sets.newHashSet(validator_indices);
    // remove all validator indices provided and we are left with missing validator indices
    set_of_indices.removeAll(set_of_validator_indices);
    List<Integer> missing_indices = new ArrayList<>(set_of_indices);

    UnsignedLong penalty_delta = UnsignedLong.ZERO;
    for (int index : missing_indices) {
      if (ValidatorsUtil.is_active_validator_index(
          state, index, BeaconStateUtil.get_current_epoch(state))) {
        penalty_delta = penalty.apply(index);
        apply_penalty_or_reward(balances, index, penalty_delta, false);
      }
    }
  }

  // Helper method for justificationAndFinalization()
  static void apply_penalty_or_reward(
      List<UnsignedLong> balances, int index, UnsignedLong delta_balance, Boolean reward) {
    UnsignedLong balance = balances.get(index);
    if (reward) {
      // TODO: add check for overflow
      balance = balance.plus(delta_balance);
    } else {
      // TODO: add check for underflow
      balance = balance.minus(delta_balance);
    }
    balances.set(index, balance);
  }

  private static boolean isPrevJustifiedSlotFinalized(BeaconState state) {
    // TODO: change values to UnsignedLong
    // TODO: Method requires major changes following BeaconState refactor
    return true;
    //    return ((state.getPrevious_justified_slot() == ((state.getSlot() - 2) *
    // Constants.EPOCH_LENGTH)
    //            && (state.getJustification_bitfield() % 4) == 3)
    //        || (state.getPrevious_justified_slot() == ((state.getSlot() - 3) *
    // Constants.EPOCH_LENGTH)
    //            && (state.getJustification_bitfield() % 8) == 7)
    //        || (state.getPrevious_justified_slot() == ((state.getSlot() - 4) *
    // Constants.EPOCH_LENGTH)
    //            && ((state.getJustification_bitfield() % 16) == 14
    //                || (state.getJustification_bitfield() % 16) == 15)));
  }

  private static UnsignedLong total_balance(CrosslinkCommittee crosslink_committee) {
    // todo
    return UnsignedLong.ZERO;
  }

  public static void update_validator_registry(BeaconState state) {
    UnsignedLong currentEpoch = BeaconStateUtil.get_current_epoch(state);
    List<Integer> active_validators =
        ValidatorsUtil.get_active_validator_indices(state.getValidator_registry(), currentEpoch);
    UnsignedLong total_balance = get_total_effective_balance(state, active_validators);

    UnsignedLong max_balance_churn =
        BeaconStateUtil.max(
            UnsignedLong.valueOf(MAX_DEPOSIT_AMOUNT),
            total_balance.dividedBy(
                UnsignedLong.valueOf((2 * Constants.MAX_BALANCE_CHURN_QUOTIENT))));

    // Activate validators within the allowable balance churn
    UnsignedLong balance_churn = UnsignedLong.ZERO;
    int index = 0;
    for (Validator validator : state.getValidator_registry()) {
      if (validator
                  .getActivation_epoch()
                  .compareTo(BeaconStateUtil.get_entry_exit_effect_epoch(currentEpoch))
              > 0
          && state
                  .getValidator_balances()
                  .get(index)
                  .compareTo(UnsignedLong.valueOf(Constants.MAX_DEPOSIT_AMOUNT))
              >= 0) {
        balance_churn = balance_churn.plus(get_effective_balance(state, index));
        if (balance_churn.compareTo(max_balance_churn) > 0) break;
        BeaconStateUtil.activate_validator(state, validator, false);
      }
      index++;
    }

    // Exit validators within the allowable balance churn
    balance_churn = UnsignedLong.ZERO;
    index = 0;
    for (Validator validator : state.getValidator_registry()) {
      List<UnsignedLong> balances = state.getValidator_balances();
      if (validator
                  .getExit_epoch()
                  .compareTo(BeaconStateUtil.get_entry_exit_effect_epoch(currentEpoch))
              > 0
          && validator.getStatus_flags().compareTo(UnsignedLong.valueOf(Constants.INITIATED_EXIT))
              == 0) {
        balance_churn = balance_churn.plus(get_effective_balance(state, validator));
        if (balance_churn.compareTo(max_balance_churn) > 0) break;
        BeaconStateUtil.exit_validator(state, index);
      }
      index++;
    }
    state.setValidator_registry_update_epoch(currentEpoch);
  }

  public static void process_ejections(BeaconState state) {
    int index = 0;
    UnsignedLong currentEpoch = BeaconStateUtil.get_current_epoch(state);
    List<Validator> active_validators =
        ValidatorsUtil.get_active_validators(state.getValidator_registry(), currentEpoch);
    for (Validator validator : active_validators) {
      List<UnsignedLong> balances = state.getValidator_balances();
      if (balances.get(index).compareTo(UnsignedLong.valueOf(Constants.EJECTION_BALANCE)) < 0) {
        BeaconStateUtil.exit_validator(state, index);
      }
      index++;
    }
  }

  public static void process_penalties_and_exits(BeaconState state) {
    UnsignedLong currentEpoch = BeaconStateUtil.get_current_epoch(state);
    List<Integer> active_validators =
        ValidatorsUtil.get_active_validator_indices(state.getValidator_registry(), currentEpoch);

    // total_balance = sum(get_effective_balance(state, i) for i in active_validator_indices)
    UnsignedLong total_balance = get_total_effective_balance(state, active_validators);

    ListIterator<Validator> itr = state.getValidator_registry().listIterator();
    while (itr.hasNext()) {
      int index = itr.nextIndex();
      Validator validator = itr.next();

      if (currentEpoch.equals(
          validator
              .getPenalized_epoch()
              .plus(UnsignedLong.valueOf(Constants.LATEST_PENALIZED_EXIT_LENGTH / 2)))) {
        int epoch_index = currentEpoch.intValue() % Constants.LATEST_PENALIZED_EXIT_LENGTH;

        UnsignedLong total_at_start =
            state
                .getLatest_penalized_balances()
                .get((epoch_index + 1) % Constants.LATEST_PENALIZED_EXIT_LENGTH);
        UnsignedLong total_at_end = state.getLatest_penalized_balances().get(epoch_index);
        UnsignedLong total_penalties = total_at_end.minus(total_at_start);
        UnsignedLong penalty =
            get_effective_balance(state, validator)
                .times(
                    BeaconStateUtil.min(
                        total_penalties.times(UnsignedLong.valueOf(3)), total_balance))
                .dividedBy(total_balance);
        state
            .getValidator_balances()
            .set(index, state.getValidator_balances().get(index).minus(penalty));
      }
    }

    ArrayList<Validator> eligible_validators = new ArrayList<>();
    for (Validator validator : state.getValidator_registry()) {
      if (eligible(state, validator)) eligible_validators.add(validator);
    }
    Collections.sort(
        eligible_validators,
        (a, b) -> {
          return a.getExit_epoch().compareTo(b.getExit_epoch());
        });

    int withdrawn_so_far = 0;
    for (Validator validator : eligible_validators) {
      validator.setStatus_flags(UnsignedLong.valueOf(Constants.WITHDRAWABLE));
      withdrawn_so_far += 1;
      if (withdrawn_so_far >= Constants.MAX_WITHDRAWALS_PER_EPOCH) break;
    }
  }

  static boolean eligible(BeaconState state, Validator validator) {
    UnsignedLong currentEpoch = BeaconStateUtil.get_current_epoch(state);
    if (validator.getPenalized_epoch().compareTo(currentEpoch) <= 0) {
      UnsignedLong penalized_withdrawal_epochs =
          UnsignedLong.valueOf(
              (long)
                  Math.floor(
                      Constants.LATEST_PENALIZED_EXIT_LENGTH * Constants.EPOCH_LENGTH / 2.0));
      return state
              .getSlot()
              .compareTo(validator.getPenalized_epoch().plus(penalized_withdrawal_epochs))
          >= 0;
    } else {
      return currentEpoch.compareTo(
              validator
                  .getExit_epoch()
                  .plus(UnsignedLong.valueOf(Constants.MIN_VALIDATOR_WITHDRAWAL_EPOCHS)))
          >= 0;
    }
  }

  /**
   * calculate the total balance from the previous epoch
   *
   * @param state
   * @return
   */
  static UnsignedLong previous_total_balance(BeaconState state) {
    UnsignedLong previous_epoch = BeaconStateUtil.get_previous_epoch(state);
    List<Integer> previous_active_validators =
        ValidatorsUtil.get_active_validator_indices(state.getValidator_registry(), previous_epoch);
    return get_total_effective_balance(state, previous_active_validators);
  }

  /**
   * calculates the base reward for the supplied validator index
   *
   * @param state
   * @param index
   * @param previous_total_balance
   * @return
   */
  static UnsignedLong base_reward(
      BeaconState state, int index, UnsignedLong previous_total_balance) {
    UnsignedLong base_reward_quotient =
        BeaconStateUtil.integer_squareroot(previous_total_balance)
            .dividedBy(UnsignedLong.valueOf(BASE_REWARD_QUOTIENT));
    return get_effective_balance(state, index)
        .dividedBy(base_reward_quotient)
        .dividedBy(UnsignedLong.valueOf(5L));
  }

  /**
   * calculates the inactivity penalty for the supplied validator index
   *
   * @param state
   * @param index
   * @param epochs_since_finality
   * @param previous_total_balance
   * @return
   */
  static UnsignedLong inactivity_penality(
      BeaconState state,
      int index,
      UnsignedLong epochs_since_finality,
      UnsignedLong previous_total_balance) {
    return base_reward(state, index, previous_total_balance)
        .plus(get_effective_balance(state, index))
        .times(epochs_since_finality)
        .dividedBy(UnsignedLong.valueOf(INACTIVITY_PENALTY_QUOTIENT))
        .dividedBy(UnsignedLong.valueOf(2L));
  }

  // Return the starting slot of the given ``epoch``.
  static UnsignedLong get_epoch_start_slot(UnsignedLong epoch) {
    return epoch.times(UnsignedLong.valueOf(Constants.EPOCH_LENGTH));
  }
}
