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

import com.google.common.primitives.UnsignedLong;
import java.util.List;
import tech.devgao.artemis.datastructures.state.Validator;
import tech.devgao.artemis.statetransition.BeaconState;

public class CrosslinkCommitteeUtil {
  // Return the number of committees in the previous epoch of the given ``state`
  public static int get_previous_epoch_committee_count(BeaconState state) {
    List<Validator> previous_active_validators =
        ValidatorsUtil.get_active_validators(
            state.getValidator_registry(), state.getPrevious_calculation_epoch());
    return previous_active_validators.size();
  }
  // Return the number of committees in the previous epoch of the given ``state`
  public static int get_current_epoch_committee_count(BeaconState state) {
    List<Validator> previous_active_validators =
        ValidatorsUtil.get_active_validators(
            state.getValidator_registry(), state.getCurrent_calculation_epoch());
    return previous_active_validators.size();
  }

  public static int get_next_epoch_committee_count(BeaconState state) {
    List<Validator> previous_active_validators =
        ValidatorsUtil.get_active_validators(
            state.getValidator_registry(),
            state.getCurrent_calculation_epoch().plus(UnsignedLong.ONE));
    return previous_active_validators.size();
  }
}
