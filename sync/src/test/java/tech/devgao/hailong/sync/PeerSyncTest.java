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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.common.primitives.UnsignedLong;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tech.devgao.hailong.data.BlockProcessingRecord;
import tech.devgao.hailong.datastructures.blocks.SignedBeaconBlock;
import tech.devgao.hailong.datastructures.networking.libp2p.rpc.GoodbyeMessage;
import tech.devgao.hailong.datastructures.networking.libp2p.rpc.StatusMessage;
import tech.devgao.hailong.datastructures.state.Fork;
import tech.devgao.hailong.datastructures.util.DataStructureUtil;
import tech.devgao.hailong.networking.eth2.peers.Eth2Peer;
import tech.devgao.hailong.networking.eth2.peers.PeerStatus;
import tech.devgao.hailong.networking.eth2.rpc.core.ResponseStream;
import tech.devgao.hailong.statetransition.StateTransitionException;
import tech.devgao.hailong.statetransition.blockimport.BlockImportResult;
import tech.devgao.hailong.statetransition.blockimport.BlockImporter;
import tech.devgao.hailong.storage.ChainStorageClient;
import tech.devgao.hailong.util.async.SafeFuture;
import tech.devgao.hailong.util.config.Constants;

public class PeerSyncTest {

  private final Eth2Peer peer = mock(Eth2Peer.class);
  private BlockImporter blockImporter = mock(BlockImporter.class);
  private ChainStorageClient storageClient = mock(ChainStorageClient.class);

  private static final SignedBeaconBlock BLOCK = DataStructureUtil.randomSignedBeaconBlock(1, 100);
  private static final Bytes32 PEER_HEAD_BLOCK_ROOT = Bytes32.fromHexString("0x1234");
  private static final UnsignedLong PEER_HEAD_SLOT = UnsignedLong.valueOf(30);
  private static final UnsignedLong PEER_FINALIZED_EPOCH = UnsignedLong.valueOf(3);

  private static final PeerStatus PEER_STATUS =
      PeerStatus.fromStatusMessage(
          new StatusMessage(
              Fork.VERSION_ZERO,
              Bytes32.ZERO,
              PEER_FINALIZED_EPOCH,
              PEER_HEAD_BLOCK_ROOT,
              PEER_HEAD_SLOT));

  private PeerSync peerSync;

  @SuppressWarnings("unchecked")
  private final ArgumentCaptor<ResponseStream.ResponseListener<SignedBeaconBlock>>
      responseListenerArgumentCaptor =
          ArgumentCaptor.forClass(ResponseStream.ResponseListener.class);

  @BeforeEach
  public void setUp() {
    when(storageClient.getFinalizedEpoch()).thenReturn(UnsignedLong.ZERO);
    when(peer.getStatus()).thenReturn(PEER_STATUS);
    when(peer.sendGoodbye(any())).thenReturn(new SafeFuture<>());
    // By default set up block import to succeed
    final BlockProcessingRecord processingRecord = mock(BlockProcessingRecord.class);
    when(blockImporter.importBlock(any()))
        .thenReturn(BlockImportResult.successful(processingRecord));
    peerSync = new PeerSync(storageClient, blockImporter);
  }

  @Test
  void sync_failedImport_stateTransitionError() {
    final BlockImportResult importResult =
        BlockImportResult.failedStateTransition(new StateTransitionException(null));
    testFailedBlockImport(() -> importResult, true);
  }

  @Test
  void sync_failedImport_unknownParent() {
    testFailedBlockImport(() -> BlockImportResult.FAILED_UNKNOWN_PARENT, true);
  }

  @Test
  void sync_failedImport_unknownAncestry() {
    testFailedBlockImport(() -> BlockImportResult.FAILED_INVALID_ANCESTRY, false);
  }

  @Test
  void sync_failedImport_unknownBlockIsFromFuture() {
    testFailedBlockImport(() -> BlockImportResult.FAILED_BLOCK_IS_FROM_FUTURE, false);
  }

  void testFailedBlockImport(
      final Supplier<BlockImportResult> importResult, final boolean shouldDisconnect) {
    final SafeFuture<Void> requestFuture = new SafeFuture<>();
    when(peer.requestBlocksByRange(any(), any(), any(), any(), any())).thenReturn(requestFuture);

    final SafeFuture<PeerSyncResult> syncFuture = peerSync.sync(peer);
    assertThat(syncFuture).isNotDone();

    verify(peer)
        .requestBlocksByRange(
            eq(PEER_HEAD_BLOCK_ROOT),
            any(),
            any(),
            eq(UnsignedLong.ONE),
            responseListenerArgumentCaptor.capture());

    // Respond with blocks and check they're passed to the block importer.
    final ResponseStream.ResponseListener<SignedBeaconBlock> responseListener =
        responseListenerArgumentCaptor.getValue();

    // Importing the returned block fails
    when(blockImporter.importBlock(BLOCK)).thenReturn(importResult.get());
    // Probably want to have a specific exception type to indicate bad data.
    try {
      responseListener.onResponse(BLOCK);
      fail("Should have thrown an error to indicate the response was bad");
    } catch (final FailedBlockImportException e) {
      // RpcMessageHandler will consider the request complete if there's an error processing a
      // response
      requestFuture.completeExceptionally(e);
    }

    assertThat(syncFuture).isCompleted();
    PeerSyncResult result = syncFuture.join();
    if (shouldDisconnect) {
      verify(peer).sendGoodbye(GoodbyeMessage.REASON_FAULT_ERROR);
      assertThat(result).isEqualByComparingTo(PeerSyncResult.BAD_BLOCK);
    } else {
      verify(peer, never()).sendGoodbye(any());
      assertThat(result).isEqualByComparingTo(PeerSyncResult.IMPORT_FAILED);
    }
  }

  @Test
  void sync_stoppedBeforeBlockImport() {
    final SafeFuture<Void> requestFuture = new SafeFuture<>();
    when(peer.requestBlocksByRange(any(), any(), any(), any(), any())).thenReturn(requestFuture);

    final SafeFuture<PeerSyncResult> syncFuture = peerSync.sync(peer);
    assertThat(syncFuture).isNotDone();

    verify(peer)
        .requestBlocksByRange(
            eq(PEER_HEAD_BLOCK_ROOT),
            any(),
            any(),
            eq(UnsignedLong.ONE),
            responseListenerArgumentCaptor.capture());

    // Respond with blocks and check they're passed to the block importer.
    final ResponseStream.ResponseListener<SignedBeaconBlock> responseListener =
        responseListenerArgumentCaptor.getValue();

    // Stop the sync, no further blocks should be imported
    peerSync.stop();

    try {
      responseListener.onResponse(BLOCK);
      fail("Should have thrown an error to indicate the sync was stopped");
    } catch (final CancellationException e) {
      // RpcMessageHandler will consider the request complete if there's an error processing a
      // response
      requestFuture.completeExceptionally(e);
    }

    // Should not disconnect the peer as it wasn't their fault
    verify(peer, never()).sendGoodbye(any());
    verifyNoInteractions(blockImporter);
    assertThat(syncFuture).isCompleted();
    PeerSyncResult result = syncFuture.join();
    assertThat(result).isEqualByComparingTo(PeerSyncResult.CANCELLED);
  }

  @Test
  void sync_badAdvertisedFinalizedEpoch() {
    final SafeFuture<Void> requestFuture = new SafeFuture<>();
    when(peer.requestBlocksByRange(any(), any(), any(), any(), any())).thenReturn(requestFuture);

    final SafeFuture<PeerSyncResult> syncFuture = peerSync.sync(peer);
    assertThat(syncFuture).isNotDone();

    verify(peer)
        .requestBlocksByRange(
            eq(PEER_HEAD_BLOCK_ROOT),
            any(),
            any(),
            eq(UnsignedLong.ONE),
            responseListenerArgumentCaptor.capture());

    // Respond with blocks and check they're passed to the block importer.
    final ResponseStream.ResponseListener<SignedBeaconBlock> responseListener =
        responseListenerArgumentCaptor.getValue();
    final List<SignedBeaconBlock> blocks =
        respondWithBlocksAtSlots(responseListener, 1, PEER_HEAD_SLOT.intValue());
    for (SignedBeaconBlock block : blocks) {
      verify(blockImporter).importBlock(block);
    }
    assertThat(syncFuture).isNotDone();

    // Now that we've imported the block, our finalized epoch has updated but hasn't reached what
    // the peer claimed
    when(storageClient.getFinalizedEpoch())
        .thenReturn(PEER_FINALIZED_EPOCH.minus(UnsignedLong.ONE));

    // Signal the request for data from the peer is complete.
    requestFuture.complete(null);

    // Check that the sync is done and the peer was not disconnected.
    assertThat(syncFuture).isCompleted();
    verify(peer).sendGoodbye(GoodbyeMessage.REASON_FAULT_ERROR);
  }

  @Test
  void sync_unresponsivePeer() {
    final SafeFuture<Void> requestFuture = new SafeFuture<>();
    when(peer.requestBlocksByRange(any(), any(), any(), any(), any())).thenReturn(requestFuture);

    final SafeFuture<PeerSyncResult> syncFuture = peerSync.sync(peer);
    assertThat(syncFuture).isNotDone();

    verify(peer)
        .requestBlocksByRange(
            eq(PEER_HEAD_BLOCK_ROOT),
            any(),
            any(),
            eq(UnsignedLong.ONE),
            responseListenerArgumentCaptor.capture());

    assertThat(syncFuture).isNotDone();
    // Signal the request for data from the peer is complete.
    requestFuture.complete(null);

    // Check that the sync is done and the peer was not disconnected.
    assertThat(syncFuture).isCompletedWithValue(PeerSyncResult.IMPORT_STALLED);
    verify(peer, never()).sendGoodbye(any());
  }

  @Test
  void sync_longSyncWithTwoRequests() {
    UnsignedLong peerHeadSlot = Constants.MAX_BLOCK_BY_RANGE_REQUEST_SIZE.plus(UnsignedLong.ONE);

    final PeerStatus peer_status =
        PeerStatus.fromStatusMessage(
            new StatusMessage(
                Fork.VERSION_ZERO,
                Bytes32.ZERO,
                PEER_FINALIZED_EPOCH,
                PEER_HEAD_BLOCK_ROOT,
                peerHeadSlot));

    when(peer.getStatus()).thenReturn(peer_status);
    peerSync = new PeerSync(storageClient, blockImporter);

    final SafeFuture<Void> requestFuture1 = new SafeFuture<>();
    final SafeFuture<Void> requestFuture2 = new SafeFuture<>();
    when(peer.requestBlocksByRange(any(), any(), any(), any(), any()))
        .thenReturn(requestFuture1)
        .thenReturn(requestFuture2);

    final SafeFuture<PeerSyncResult> syncFuture = peerSync.sync(peer);
    assertThat(syncFuture).isNotDone();

    verify(peer)
        .requestBlocksByRange(
            eq(PEER_HEAD_BLOCK_ROOT),
            eq(UnsignedLong.ONE),
            eq(Constants.MAX_BLOCK_BY_RANGE_REQUEST_SIZE),
            eq(UnsignedLong.ONE),
            responseListenerArgumentCaptor.capture());

    final ResponseStream.ResponseListener<SignedBeaconBlock> responseListener1 =
        responseListenerArgumentCaptor.getValue();
    final int lastReceivedBlockSlot = peerHeadSlot.intValue() - 2;
    List<SignedBeaconBlock> blocks =
        respondWithBlocksAtSlots(responseListener1, 1, lastReceivedBlockSlot);
    for (SignedBeaconBlock block : blocks) {
      verify(blockImporter).importBlock(block);
    }

    // Signal the request for data from the peer is complete.
    requestFuture1.complete(null);

    verify(peer)
        .requestBlocksByRange(
            eq(PEER_HEAD_BLOCK_ROOT),
            eq(UnsignedLong.valueOf(lastReceivedBlockSlot + 1)),
            eq(peerHeadSlot.minus(UnsignedLong.valueOf(lastReceivedBlockSlot))),
            eq(UnsignedLong.ONE),
            responseListenerArgumentCaptor.capture());

    // Respond with blocks and check they're passed to the block importer.
    final ResponseStream.ResponseListener<SignedBeaconBlock> responseListener2 =
        responseListenerArgumentCaptor.getValue();
    blocks = respondWithBlocksAtSlots(responseListener2, peerHeadSlot.intValue());
    for (SignedBeaconBlock block : blocks) {
      verify(blockImporter).importBlock(block);
    }
    assertThat(syncFuture).isNotDone();

    // Signal that sync is complete
    when(storageClient.getFinalizedEpoch()).thenReturn(PEER_FINALIZED_EPOCH);
    requestFuture2.complete(null);

    // Check that the sync is done and the peer was not disconnected.
    assertThat(syncFuture).isCompleted();
    verify(peer, never()).sendGoodbye(any());
  }

  private List<SignedBeaconBlock> respondWithBlocksAtSlots(
      final ResponseStream.ResponseListener<SignedBeaconBlock> responseListener, int... slots) {
    List<SignedBeaconBlock> blocks = new ArrayList<>();
    for (int slot : slots) {
      final SignedBeaconBlock block = DataStructureUtil.randomSignedBeaconBlock(slot, slot);
      blocks.add(block);
      responseListener.onResponse(block);
    }
    return blocks;
  }
}
