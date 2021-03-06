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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.devgao.hailong.util.Waiter.waitFor;

import com.google.common.eventbus.EventBus;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.devgao.hailong.datastructures.blocks.BeaconBlock;
import tech.devgao.hailong.datastructures.blocks.SignedBeaconBlock;
import tech.devgao.hailong.datastructures.util.DataStructureUtil;
import tech.devgao.hailong.networking.eth2.peers.Eth2Peer;
import tech.devgao.hailong.statetransition.BeaconChainUtil;
import tech.devgao.hailong.storage.ChainStorageClient;
import tech.devgao.hailong.storage.Store.Transaction;

public class BeaconBlocksByRootIntegrationTest {

  private final Eth2NetworkFactory networkFactory = new Eth2NetworkFactory();
  private int seed = 1000;
  private Eth2Peer peer1;
  private ChainStorageClient storageClient1;

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
  }

  @AfterEach
  public void tearDown() {
    networkFactory.stopAll();
  }

  @Test
  public void shouldSendEmptyResponsePreGenesisEvent() throws Exception {
    final List<Bytes32> blockRoots = singletonList(Bytes32.ZERO);
    final List<SignedBeaconBlock> response = requestBlocks(blockRoots);
    assertThat(response).isEmpty();
  }

  @Test
  public void shouldSendEmptyResponseWhenNoBlocksAreAvailable() throws Exception {
    BeaconChainUtil.create(0, storageClient1).initializeStorage();
    final List<SignedBeaconBlock> response = requestBlocks(singletonList(Bytes32.ZERO));
    assertThat(response).isEmpty();
  }

  @Test
  public void shouldReturnSingleBlockWhenOnlyOneMatches() throws Exception {
    BeaconChainUtil.create(0, storageClient1).initializeStorage();
    final SignedBeaconBlock block = addBlock();

    final List<SignedBeaconBlock> response =
        requestBlocks(singletonList(block.getMessage().hash_tree_root()));
    assertThat(response).containsExactly(block);
  }

  @Test
  public void shouldReturnMultipleBlocksWhenAllRequestsMatch() throws Exception {
    BeaconChainUtil.create(0, storageClient1).initializeStorage();
    final List<SignedBeaconBlock> blocks = asList(addBlock(), addBlock(), addBlock());
    final List<Bytes32> blockRoots =
        blocks.stream()
            .map(SignedBeaconBlock::getMessage)
            .map(BeaconBlock::hash_tree_root)
            .collect(toList());
    final List<SignedBeaconBlock> response = requestBlocks(blockRoots);
    assertThat(response).containsExactlyElementsOf(blocks);
  }

  @Test
  public void shouldReturnMatchingBlocksWhenSomeRequestsDoNotMatch() throws Exception {
    BeaconChainUtil.create(0, storageClient1).initializeStorage();
    final List<SignedBeaconBlock> blocks = asList(addBlock(), addBlock(), addBlock());

    // Real block roots interspersed with ones that don't match any blocks
    final List<Bytes32> blockRoots =
        blocks.stream()
            .map(SignedBeaconBlock::getMessage)
            .map(BeaconBlock::hash_tree_root)
            .flatMap(hash -> Stream.of(Bytes32.fromHexStringLenient("0x123456789"), hash))
            .collect(toList());

    final List<SignedBeaconBlock> response = requestBlocks(blockRoots);
    assertThat(response).containsExactlyElementsOf(blocks);
  }

  private SignedBeaconBlock addBlock() {
    final SignedBeaconBlock block = DataStructureUtil.randomSignedBeaconBlock(seed, seed++);
    final Bytes32 blockRoot = block.getMessage().hash_tree_root();
    final Transaction transaction = storageClient1.startStoreTransaction();
    transaction.putBlock(blockRoot, block);
    assertThat(transaction.commit()).isCompleted();
    return block;
  }

  private List<SignedBeaconBlock> requestBlocks(final List<Bytes32> blockRoots)
      throws InterruptedException, java.util.concurrent.ExecutionException,
          java.util.concurrent.TimeoutException {
    final List<SignedBeaconBlock> blocks = new ArrayList<>();
    waitFor(peer1.requestBlocksByRoot(blockRoots, blocks::add));
    return blocks;
  }
}
