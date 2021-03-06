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

package tech.devgao.hailong.networking.eth2;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.devgao.hailong.util.Waiter.waitFor;

import com.google.common.eventbus.EventBus;
import com.google.common.primitives.UnsignedLong;
import java.util.ArrayList;
import java.util.List;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.devgao.hailong.datastructures.blocks.SignedBeaconBlock;
import tech.devgao.hailong.networking.eth2.peers.Eth2Peer;
import tech.devgao.hailong.statetransition.BeaconChainUtil;
import tech.devgao.hailong.storage.ChainStorageClient;

public class BeaconBlocksByRangeIntegrationTest {

  private final Eth2NetworkFactory networkFactory = new Eth2NetworkFactory();
  private Eth2Peer peer1;
  private ChainStorageClient storageClient1;
  private BeaconChainUtil beaconChainUtil;

  @BeforeEach
  public void setUp() throws Exception {
    final EventBus eventBus1 = new EventBus();
    storageClient1 = ChainStorageClient.memoryOnlyClient(eventBus1);
    final Eth2Network network1 =
        networkFactory
            .builder()
            .eventBus(eventBus1)
            .chainStorageClient(storageClient1)
            .startNetwork();

    final Eth2Network network2 = networkFactory.builder().peer(network1).startNetwork();
    peer1 = network2.getPeer(network1.getNodeId()).orElseThrow();
    beaconChainUtil = BeaconChainUtil.create(1, storageClient1);
  }

  @AfterEach
  public void tearDown() {
    networkFactory.stopAll();
  }

  @Test
  public void shouldSendEmptyResponsePreGenesisEvent() throws Exception {
    final List<SignedBeaconBlock> response = requestBlocks(Bytes32.ZERO);
    assertThat(response).isEmpty();
  }

  @Test
  public void shouldSendEmptyResponseWhenNoBlocksAreAvailable() throws Exception {
    beaconChainUtil.initializeStorage();
    final List<SignedBeaconBlock> response = requestBlocks(storageClient1.getBestBlockRoot());
    assertThat(response).isEmpty();
  }

  @Test
  public void shouldSendEmptyResponseWhenHeadBlockRootDoesNotMatchAnyBlock() throws Exception {
    beaconChainUtil.initializeStorage();
    beaconChainUtil.createAndImportBlockAtSlot(1);
    final List<SignedBeaconBlock> response = requestBlocks(Bytes32.ZERO);
    assertThat(response).isEmpty();
  }

  @Test
  public void shouldRespondWithBlocksWhenHeadBlockRootIsNotOnCanonicalChain() throws Exception {
    beaconChainUtil.initializeStorage();
    final SignedBeaconBlock nonCanonicalBlock =
        beaconChainUtil.createAndImportBlockAtSlot(1).getBlock();
    storageClient1.updateBestBlock(nonCanonicalBlock.getParent_root(), UnsignedLong.ZERO);
    final List<SignedBeaconBlock> response =
        requestBlocks(nonCanonicalBlock.getMessage().hash_tree_root());
    assertThat(response).containsExactly(nonCanonicalBlock);
  }

  @Test
  public void shouldRespondWithBlocksFromCanonicalChain() throws Exception {
    beaconChainUtil.initializeStorage();

    final SignedBeaconBlock block1 = beaconChainUtil.createAndImportBlockAtSlot(1).getBlock();
    final Bytes32 block1Root = block1.getMessage().hash_tree_root();
    storageClient1.updateBestBlock(block1Root, block1.getSlot());

    final SignedBeaconBlock block2 = beaconChainUtil.createAndImportBlockAtSlot(2).getBlock();
    final Bytes32 block2Root = block2.getMessage().hash_tree_root();
    storageClient1.updateBestBlock(block2Root, block2.getSlot());

    final List<SignedBeaconBlock> response = requestBlocks(block2Root);
    assertThat(response).containsExactly(block1, block2);
  }

  private List<SignedBeaconBlock> requestBlocks(final Bytes32 headBlockRoot)
      throws InterruptedException, java.util.concurrent.ExecutionException,
          java.util.concurrent.TimeoutException {
    final List<SignedBeaconBlock> blocks = new ArrayList<>();
    waitFor(
        peer1.requestBlocksByRange(
            headBlockRoot,
            UnsignedLong.ONE,
            UnsignedLong.valueOf(10),
            UnsignedLong.ONE,
            blocks::add));
    return blocks;
  }
}
