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

package tech.devgao.hailong.storage;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.UnsignedLong;
import java.util.Optional;
import tech.devgao.hailong.datastructures.blocks.SignedBeaconBlock;
import tech.devgao.hailong.storage.events.GetFinalizedBlockAtSlotRequest;
import tech.devgao.hailong.storage.events.GetFinalizedBlockAtSlotResponse;
import tech.devgao.hailong.util.async.AsyncEventTracker;
import tech.devgao.hailong.util.async.SafeFuture;

public class HistoricalChainData {
  private final AsyncEventTracker<UnsignedLong, Optional<SignedBeaconBlock>> eventTracker;

  public HistoricalChainData(final EventBus eventBus) {
    this.eventTracker = new AsyncEventTracker<>(eventBus);
    eventBus.register(this);
  }

  public SafeFuture<Optional<SignedBeaconBlock>> getFinalizedBlockAtSlot(final UnsignedLong slot) {
    return eventTracker.sendRequest(slot, new GetFinalizedBlockAtSlotRequest(slot));
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onResponse(final GetFinalizedBlockAtSlotResponse response) {
    eventTracker.onResponse(response.getSlot(), response.getBlock());
  }
}
