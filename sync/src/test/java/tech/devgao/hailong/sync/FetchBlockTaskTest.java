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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.devgao.hailong.datastructures.blocks.SignedBeaconBlock;
import tech.devgao.hailong.datastructures.util.DataStructureUtil;
import tech.devgao.hailong.networking.eth2.Eth2Network;
import tech.devgao.hailong.networking.eth2.peers.Eth2Peer;
import tech.devgao.hailong.networking.p2p.mock.MockNodeId;
import tech.devgao.hailong.sync.FetchBlockTask.FetchBlockResult;
import tech.devgao.hailong.sync.FetchBlockTask.FetchBlockResult.Status;
import tech.devgao.hailong.util.async.SafeFuture;

public class FetchBlockTaskTest {

  final Eth2Network eth2Network = mock(Eth2Network.class);
  final List<Eth2Peer> peers = new ArrayList<>();

  @BeforeEach
  public void setup() {
    when(eth2Network.streamPeers()).thenAnswer((invocation) -> peers.stream());
  }

  @Test
  public void run_successful() {
    final SignedBeaconBlock block = DataStructureUtil.randomSignedBeaconBlock(10, 1);
    final Bytes32 blockRoot = block.getMessage().hash_tree_root();
    FetchBlockTask task = FetchBlockTask.create(eth2Network, blockRoot);
    assertThat(task.getBlockRoot()).isEqualTo(blockRoot);

    final Eth2Peer peer = registerNewPeer();
    when(peer.requestBlockByRoot(blockRoot)).thenReturn(SafeFuture.completedFuture(block));

    final SafeFuture<FetchBlockResult> result = task.run();
    assertThat(result).isDone();
    final FetchBlockResult fetchBlockResult = result.getNow(null);
    assertThat(fetchBlockResult.isSuccessful()).isTrue();
    assertThat(fetchBlockResult.getBlock()).isEqualTo(block);
  }

  @Test
  public void run_noPeers() {
    final SignedBeaconBlock block = DataStructureUtil.randomSignedBeaconBlock(10, 1);
    final Bytes32 blockRoot = block.getMessage().hash_tree_root();
    FetchBlockTask task = FetchBlockTask.create(eth2Network, blockRoot);

    final SafeFuture<FetchBlockResult> result = task.run();
    assertThat(result).isDone();
    final FetchBlockResult fetchBlockResult = result.getNow(null);
    assertThat(fetchBlockResult.isSuccessful()).isFalse();
    assertThat(fetchBlockResult.getStatus()).isEqualTo(Status.NO_AVAILABLE_PEERS);
  }

  @Test
  public void run_failAndRetryWithNoNewPeers() {
    final SignedBeaconBlock block = DataStructureUtil.randomSignedBeaconBlock(10, 1);
    final Bytes32 blockRoot = block.getMessage().hash_tree_root();
    FetchBlockTask task = FetchBlockTask.create(eth2Network, blockRoot);

    final Eth2Peer peer = registerNewPeer();
    when(peer.requestBlockByRoot(blockRoot))
        .thenReturn(SafeFuture.failedFuture(new RuntimeException("whoops")));

    final SafeFuture<FetchBlockResult> result = task.run();
    assertThat(result).isDone();
    final FetchBlockResult fetchBlockResult = result.getNow(null);
    assertThat(fetchBlockResult.isSuccessful()).isFalse();
    assertThat(fetchBlockResult.getStatus()).isEqualTo(Status.FETCH_FAILED);
    assertThat(task.getNumberOfRetries()).isEqualTo(0);

    // Retry
    final SafeFuture<FetchBlockResult> result2 = task.run();
    assertThat(result).isDone();
    final FetchBlockResult fetchBlockResult2 = result2.getNow(null);
    assertThat(fetchBlockResult2.isSuccessful()).isFalse();
    assertThat(fetchBlockResult2.getStatus()).isEqualTo(Status.NO_AVAILABLE_PEERS);
    assertThat(task.getNumberOfRetries()).isEqualTo(0);
  }

  @Test
  public void run_failAndRetryWithNewPeer() {
    final SignedBeaconBlock block = DataStructureUtil.randomSignedBeaconBlock(10, 1);
    final Bytes32 blockRoot = block.getMessage().hash_tree_root();
    FetchBlockTask task = FetchBlockTask.create(eth2Network, blockRoot);

    final Eth2Peer peer = registerNewPeer();
    when(peer.requestBlockByRoot(blockRoot))
        .thenReturn(SafeFuture.failedFuture(new RuntimeException("whoops")));

    final SafeFuture<FetchBlockResult> result = task.run();
    assertThat(result).isDone();
    final FetchBlockResult fetchBlockResult = result.getNow(null);
    assertThat(fetchBlockResult.isSuccessful()).isFalse();
    assertThat(fetchBlockResult.getStatus()).isEqualTo(Status.FETCH_FAILED);
    assertThat(task.getNumberOfRetries()).isEqualTo(0);

    // Add another peer
    final Eth2Peer peer2 = registerNewPeer();
    when(peer2.requestBlockByRoot(blockRoot)).thenReturn(SafeFuture.completedFuture(block));

    // Retry
    final SafeFuture<FetchBlockResult> result2 = task.run();
    assertThat(result).isDone();
    final FetchBlockResult fetchBlockResult2 = result2.getNow(null);
    assertThat(fetchBlockResult2.isSuccessful()).isTrue();
    assertThat(fetchBlockResult2.getBlock()).isEqualTo(block);
    assertThat(task.getNumberOfRetries()).isEqualTo(1);
  }

  @Test
  public void run_withMultiplesPeersAvailable() {
    final SignedBeaconBlock block = DataStructureUtil.randomSignedBeaconBlock(10, 1);
    final Bytes32 blockRoot = block.getMessage().hash_tree_root();
    FetchBlockTask task = FetchBlockTask.create(eth2Network, blockRoot);

    final Eth2Peer peer = registerNewPeer();
    when(peer.requestBlockByRoot(blockRoot))
        .thenReturn(SafeFuture.failedFuture(new RuntimeException("whoops")));
    when(peer.getOutstandingRequests()).thenReturn(1);
    // Add another peer
    final Eth2Peer peer2 = registerNewPeer();
    when(peer2.requestBlockByRoot(blockRoot)).thenReturn(SafeFuture.completedFuture(block));
    when(peer2.getOutstandingRequests()).thenReturn(0);

    // We should choose the peer that is less busy, which successfully returns the block
    final SafeFuture<FetchBlockResult> result = task.run();
    assertThat(result).isDone();
    final FetchBlockResult fetchBlockResult = result.getNow(null);
    assertThat(fetchBlockResult.isSuccessful()).isTrue();
    assertThat(fetchBlockResult.getBlock()).isEqualTo(block);
  }

  @Test
  public void cancel() {
    final SignedBeaconBlock block = DataStructureUtil.randomSignedBeaconBlock(10, 1);
    final Bytes32 blockRoot = block.getMessage().hash_tree_root();
    FetchBlockTask task = FetchBlockTask.create(eth2Network, blockRoot);

    final Eth2Peer peer = registerNewPeer();
    when(peer.requestBlockByRoot(blockRoot)).thenReturn(SafeFuture.completedFuture(block));

    task.cancel();
    final SafeFuture<FetchBlockResult> result = task.run();
    assertThat(result).isDone();
    final FetchBlockResult fetchBlockResult = result.getNow(null);
    assertThat(fetchBlockResult.isSuccessful()).isFalse();
    assertThat(fetchBlockResult.getStatus()).isEqualTo(Status.CANCELLED);
  }

  private Eth2Peer registerNewPeer() {
    final Eth2Peer peer = mock(Eth2Peer.class);
    when(peer.getOutstandingRequests()).thenReturn(0);
    when(peer.getId()).thenReturn(new MockNodeId());

    peers.add(peer);
    return peer;
  }
}
