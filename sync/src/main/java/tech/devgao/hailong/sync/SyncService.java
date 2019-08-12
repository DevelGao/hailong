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

package tech.devgao.hailong.sync;

import com.google.common.eventbus.EventBus;
import tech.devgao.hailong.networking.eth2.Eth2Network;
import tech.devgao.hailong.service.serviceutils.Service;
import tech.devgao.hailong.statetransition.blockimport.BlockImporter;
import tech.devgao.hailong.storage.ChainStorageClient;
import tech.devgao.hailong.util.async.SafeFuture;

public class SyncService extends Service {

  private final SyncManager syncManager;
  private final BlockPropagationManager blockPropagationManager;

  public SyncService(
      final EventBus eventBus,
      final Eth2Network network,
      final ChainStorageClient storageClient,
      final BlockImporter blockImporter) {
    this.syncManager = SyncManager.create(network, storageClient, blockImporter);
    this.blockPropagationManager =
        BlockPropagationManager.create(eventBus, network, storageClient, blockImporter);
  }

  @Override
  protected SafeFuture<?> doStart() {
    return SafeFuture.allOf(syncManager.start(), blockPropagationManager.start());
  }

  @Override
  protected SafeFuture<?> doStop() {
    return SafeFuture.allOf(syncManager.stop(), blockPropagationManager.stop());
  }
}
