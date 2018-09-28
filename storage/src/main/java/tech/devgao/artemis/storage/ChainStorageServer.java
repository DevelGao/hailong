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

package tech.devgao.artemis.storage;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import net.develgao.cava.bytes.Bytes;
import tech.devgao.artemis.datastructures.blocks.BeaconBlock;
import tech.devgao.artemis.datastructures.operations.Attestation;

/** This class is the ChainStorage server-side logic */
public class ChainStorageServer extends ChainStorageClient implements ChainStorage {

  public ChainStorageServer() {}

  public ChainStorageServer(EventBus eventBus) {
    this.unprocessedBlocks = new LinkedBlockingQueue<BeaconBlock>();
    this.unprocessedAttestations = new LinkedBlockingQueue<Attestation>();
    this.processedBlocks = new HashMap<Bytes, Bytes>();
    this.eventBus = eventBus;
  }

  @Subscribe
  public void onNewProcessedBlock(Bytes blockHash, BeaconBlock block) {
    LOG.info("ChainStorage: new block processed");
    addProcessedBlock(blockHash, block);
  }
}