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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.List;
import tech.devgao.hailong.datastructures.blocks.SignedBeaconBlock;
import tech.devgao.hailong.statetransition.events.BlockImportedEvent;

public class ImportedBlocks implements AutoCloseable {

  private final EventBus eventBus;
  private List<SignedBeaconBlock> importedBlocks = new ArrayList<>();

  public ImportedBlocks(final EventBus eventBus) {
    this.eventBus = eventBus;
    eventBus.register(this);
  }

  @Subscribe
  public void onImported(BlockImportedEvent blockImportedEvent) {
    importedBlocks.add(blockImportedEvent.getBlock());
  }

  public List<SignedBeaconBlock> get() {
    return importedBlocks;
  }

  @Override
  public void close() throws Exception {
    eventBus.unregister(this);
  }
}
