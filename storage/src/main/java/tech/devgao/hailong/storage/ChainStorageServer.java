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
import java.io.File;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.devgao.hailong.datastructures.blocks.SignedBeaconBlock;
import tech.devgao.hailong.storage.events.GetFinalizedBlockAtSlotRequest;
import tech.devgao.hailong.storage.events.GetFinalizedBlockAtSlotResponse;
import tech.devgao.hailong.storage.events.StoreDiskUpdateCompleteEvent;
import tech.devgao.hailong.storage.events.StoreDiskUpdateEvent;
import tech.devgao.hailong.storage.events.StoreGenesisDiskUpdateEvent;
import tech.devgao.hailong.util.config.HailongConfiguration;

public class ChainStorageServer {
  private static final Logger LOG = LogManager.getLogger();
  private final Database database;
  private final EventBus eventBus;

  public ChainStorageServer(EventBus eventBus, HailongConfiguration config) {
    this.eventBus = eventBus;
    this.database = MapDbDatabase.createOnDisk(new File("./"), config.startFromDisk());
    eventBus.register(this);
    if (config.startFromDisk()) {
      Store memoryStore = database.createMemoryStore();
      eventBus.post(memoryStore);
    }
  }

  @Subscribe
  public void onStoreDiskUpdate(final StoreDiskUpdateEvent event) {
    try {
      database.insert(event);

      eventBus.post(new StoreDiskUpdateCompleteEvent(event.getTransactionId(), Optional.empty()));
    } catch (final RuntimeException e) {
      LOG.debug("Transaction " + event.getTransactionId() + " failed", e);
      eventBus.post(new StoreDiskUpdateCompleteEvent(event.getTransactionId(), Optional.of(e)));
    }
  }

  @Subscribe
  public void onStoreGenesis(final StoreGenesisDiskUpdateEvent event) {
    database.storeGenesis(event.getStore());
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onGetBlockBySlotRequest(final GetFinalizedBlockAtSlotRequest request) {
    final Optional<SignedBeaconBlock> block =
        database.getFinalizedRootAtSlot(request.getSlot()).flatMap(database::getSignedBlock);
    eventBus.post(new GetFinalizedBlockAtSlotResponse(request.getSlot(), block));
  }
}
