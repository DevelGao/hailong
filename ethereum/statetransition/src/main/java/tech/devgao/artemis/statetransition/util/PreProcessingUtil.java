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
import tech.devgao.artemis.datastructures.state.BeaconStateWithCache;
import tech.devgao.artemis.datastructures.util.BeaconStateUtil;

public final class PreProcessingUtil {

  public static void cacheCurrentBeaconProposerIndex(BeaconStateWithCache state) {
    int beaconProposerIndex =
        BeaconStateUtil.get_beacon_proposer_index(state, state.getSlot().plus(UnsignedLong.ONE));
    state.setCurrentBeaconProposerIndex(beaconProposerIndex);
  }
}
