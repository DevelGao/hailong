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

package tech.devgao.hailong.statetransition;

import static tech.devgao.hailong.datastructures.Constants.FAR_FUTURE_EPOCH;
import static tech.devgao.hailong.datastructures.util.BeaconStateUtil.get_current_epoch;

import com.google.common.primitives.UnsignedLong;
import tech.devgao.hailong.datastructures.state.BeaconState;
import tech.devgao.hailong.metrics.HailongMetricCategory;
import tech.devgao.hailong.metrics.SettableGauge;
import tech.devgao.pantheon.metrics.MetricsSystem;

public class BeaconChainStateMetrics {

  private final SettableGauge currentSlotGauge;
  private final SettableGauge currentJustifiedEpoch;
  private final SettableGauge currentFinalizedEpoch;
  private final SettableGauge previousJustifiedEpoch;
  private final SettableGauge currentEpochLiveValidators;
  private final SettableGauge previousEpochLiveValidators;
  private final SettableGauge pendingExits;

  public BeaconChainStateMetrics(final MetricsSystem metricsSystem) {
    currentSlotGauge =
        SettableGauge.create(
            metricsSystem,
            HailongMetricCategory.BEACONCHAIN,
            "current_slot",
            "Latest slot recorded by the beacon chain");

    currentJustifiedEpoch =
        SettableGauge.create(
            metricsSystem,
            HailongMetricCategory.BEACONCHAIN,
            "current_justified_epoch",
            "Current justified epoch");
    currentFinalizedEpoch =
        SettableGauge.create(
            metricsSystem,
            HailongMetricCategory.BEACONCHAIN,
            "current_finalized_epoch",
            "Current finalized epoch");
    previousJustifiedEpoch =
        SettableGauge.create(
            metricsSystem,
            HailongMetricCategory.BEACONCHAIN,
            "current_prev_justified_epoch",
            "Current previously justified epoch");

    currentEpochLiveValidators =
        SettableGauge.create(
            metricsSystem,
            HailongMetricCategory.BEACONCHAIN,
            "current_epoch_live_validators",
            "Number of active validators who reported for the current epoch");
    previousEpochLiveValidators =
        SettableGauge.create(
            metricsSystem,
            HailongMetricCategory.BEACONCHAIN,
            "previous_epoch_live_validators",
            "Number of active validators who reported for the previous epoch");

    pendingExits =
        SettableGauge.create(
            metricsSystem,
            HailongMetricCategory.BEACONCHAIN,
            "pending_exits",
            "Number of pending exits");
  }

  public void onSlotStarted(UnsignedLong slotNumber) {
    this.currentSlotGauge.set(slotNumber.doubleValue());
  }

  public void onEpoch(final BeaconState headState) {
    previousJustifiedEpoch.set(headState.getPrevious_justified_epoch().doubleValue());
    currentJustifiedEpoch.set(headState.getCurrent_justified_epoch().longValue());
    currentFinalizedEpoch.set(headState.getFinalized_epoch().longValue());
    currentEpochLiveValidators.set(headState.getCurrent_epoch_attestations().size());
    previousEpochLiveValidators.set(headState.getPrevious_epoch_attestations().size());

    final UnsignedLong currentEpoch = get_current_epoch(headState);
    pendingExits.set(
        headState.getValidator_registry().stream()
            .filter(
                v ->
                    !v.getExit_epoch().equals(FAR_FUTURE_EPOCH)
                        && currentEpoch.compareTo(v.getExit_epoch()) < 0)
            .count());
  }
}
