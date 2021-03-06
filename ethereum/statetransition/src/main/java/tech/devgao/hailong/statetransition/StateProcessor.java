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

import static tech.devgao.hailong.statetransition.util.ForkChoiceUtil.get_head;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes32;
import tech.devgao.hailong.datastructures.blocks.BeaconBlock;
import tech.devgao.hailong.statetransition.blockimport.BlockImportResult;
import tech.devgao.hailong.statetransition.blockimport.BlockImporter;
import tech.devgao.hailong.statetransition.events.BlockProposedEvent;
import tech.devgao.hailong.storage.ChainStorageClient;
import tech.devgao.hailong.storage.Store;

/** Class to manage the state tree and initiate state transitions */
public class StateProcessor {
  private static final Logger LOG = LogManager.getLogger();

  private final BlockImporter blockImporter;
  private final ChainStorageClient chainStorageClient;

  public StateProcessor(EventBus eventBus, ChainStorageClient chainStorageClient) {
    this.chainStorageClient = chainStorageClient;
    this.blockImporter = new BlockImporter(chainStorageClient, eventBus);
    eventBus.register(this);
  }

  public Bytes32 processHead() {
    Store store = chainStorageClient.getStore();
    Bytes32 headBlockRoot = get_head(store);
    BeaconBlock headBlock = store.getBlock(headBlockRoot);
    chainStorageClient.updateBestBlock(headBlockRoot, headBlock.getSlot());
    return headBlockRoot;
  }

  @Subscribe
  @SuppressWarnings("unused")
  private void onBlockProposed(final BlockProposedEvent blockProposedEvent) {
    LOG.trace("Preparing to import proposed block: {}", blockProposedEvent.getBlock());
    final BlockImportResult result = blockImporter.importBlock(blockProposedEvent.getBlock());
    if (result.isSuccessful()) {
      LOG.trace("Successfully imported proposed block: {}", blockProposedEvent.getBlock());
    } else {
      LOG.error(
          "Failed to import proposed block for reason + "
              + result.getFailureReason()
              + ": "
              + blockProposedEvent,
          result.getFailureCause().orElse(null));
    }
  }
}
