/*
 * Copyright 2020 Developer Gao.
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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.primitives.UnsignedLong;
import java.util.ArrayList;
import java.util.List;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tech.devgao.hailong.data.BlockProcessingRecord;
import tech.devgao.hailong.datastructures.blocks.SignedBeaconBlock;
import tech.devgao.hailong.networking.eth2.Eth2NetworkFactory;
import tech.devgao.hailong.statetransition.events.BlockProposedEvent;
import tech.devgao.hailong.util.Waiter;
import tech.devgao.hailong.util.bls.BLSKeyGenerator;
import tech.devgao.hailong.util.bls.BLSKeyPair;
import tech.devgao.hailong.util.config.Constants;

public class BlockPropagationIntegrationTest {
  private final List<BLSKeyPair> validatorKeys = BLSKeyGenerator.generateKeyPairs(3);
  private final Eth2NetworkFactory networkFactory = new Eth2NetworkFactory();

  @AfterEach
  public void tearDown() {
    networkFactory.stopAll();
  }

  @Test
  public void shouldFetchUnknownAncestorsOfPropagatedBlock() throws Exception {
    UnsignedLong currentSlot = UnsignedLong.valueOf(Constants.GENESIS_SLOT);

    // Setup node 1
    SyncingNodeManager node1 = SyncingNodeManager.create(networkFactory, validatorKeys);
    node1.chainUtil().setSlot(currentSlot);

    // Add some blocks to node1, which node 2 will need to fetch
    final List<SignedBeaconBlock> blocksToFetch = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      currentSlot = currentSlot.plus(UnsignedLong.ONE);
      final BlockProcessingRecord record =
          node1.chainUtil().createAndImportBlockAtSlot(currentSlot);
      blocksToFetch.add(record.getBlock());
    }

    // Setup node 2
    SyncingNodeManager node2 = SyncingNodeManager.create(networkFactory, validatorKeys);

    // Connect networks
    Waiter.waitFor(node1.network().connect(node2.network().getNodeAddress()));
    // Wait for connections to get set up
    Waiter.waitFor(
        () -> {
          assertThat(node1.network().getPeerCount()).isEqualTo(1);
          assertThat(node2.network().getPeerCount()).isEqualTo(1);
        });
    // TODO: debug this - we shouldn't have to wait here
    Thread.sleep(2000);

    // Update slot so that blocks can be imported
    currentSlot = currentSlot.plus(UnsignedLong.ONE);
    node1.setSlot(currentSlot);
    node2.setSlot(currentSlot);

    // Propagate new block
    final SignedBeaconBlock newBlock = node1.chainUtil().createBlockAtSlot(currentSlot);
    node1.eventBus().post(new BlockProposedEvent(newBlock));

    // Verify that node2 fetches required blocks in response
    Waiter.waitFor(
        () -> {
          for (SignedBeaconBlock block : blocksToFetch) {
            final Bytes32 blockRoot = block.getMessage().hash_tree_root();
            assertThat(node2.storageClient().getBlockByRoot(blockRoot)).isPresent();
          }
          // Last block should be imported as well
          final Bytes32 newBlockRoot = newBlock.getMessage().hash_tree_root();
          assertThat(node2.storageClient().getBlockByRoot(newBlockRoot)).isPresent();
        });
  }
}
