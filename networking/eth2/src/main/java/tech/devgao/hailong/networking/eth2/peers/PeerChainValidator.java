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

package tech.devgao.hailong.networking.eth2.peers;

import static tech.devgao.hailong.datastructures.util.BeaconStateUtil.compute_epoch_at_slot;
import static tech.devgao.hailong.datastructures.util.BeaconStateUtil.compute_start_slot_at_epoch;
import static tech.devgao.hailong.statetransition.util.ForkChoiceUtil.get_current_slot;

import com.google.common.primitives.UnsignedLong;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes32;
import tech.devgao.hailong.datastructures.blocks.SignedBeaconBlock;
import tech.devgao.hailong.datastructures.networking.libp2p.rpc.GoodbyeMessage;
import tech.devgao.hailong.datastructures.state.Checkpoint;
import tech.devgao.hailong.storage.ChainStorageClient;
import tech.devgao.hailong.storage.HistoricalChainData;
import tech.devgao.hailong.util.SSZTypes.Bytes4;
import tech.devgao.hailong.util.async.SafeFuture;
import tech.devgao.hailong.util.config.Constants;

public class PeerChainValidator {
  private static final Logger LOG = LogManager.getLogger();

  private final ChainStorageClient storageClient;
  private final HistoricalChainData historicalChainData;
  private final Eth2Peer peer;
  private final AtomicBoolean hasRun = new AtomicBoolean(false);
  private final PeerStatus status;
  private SafeFuture<Boolean> result;

  private PeerChainValidator(
      final ChainStorageClient storageClient,
      final HistoricalChainData historicalChainData,
      final Eth2Peer peer,
      final PeerStatus status) {
    this.storageClient = storageClient;
    this.historicalChainData = historicalChainData;
    this.peer = peer;
    this.status = status;
  }

  public static PeerChainValidator create(
      final ChainStorageClient storageClient,
      final HistoricalChainData historicalChainData,
      final Eth2Peer peer,
      final PeerStatus status) {
    return new PeerChainValidator(storageClient, historicalChainData, peer, status);
  }

  public SafeFuture<Boolean> run() {
    if (hasRun.compareAndSet(false, true)) {
      result = executeCheck();
    }
    return result;
  }

  private SafeFuture<Boolean> executeCheck() {
    LOG.trace("Validate chain of peer: {}", peer);
    return checkRemoteChain()
        .thenApply(
            isValid -> {
              if (!isValid) {
                // We are not on the same chain
                LOG.trace("Disconnecting peer on different chain: {}", peer);
                peer.sendGoodbye(GoodbyeMessage.REASON_IRRELEVANT_NETWORK).reportExceptions();
              } else {
                LOG.trace("Validated peer's chain: {}", peer);
                peer.markChainValidated();
              }
              return isValid;
            })
        .exceptionally(
            err -> {
              LOG.debug("Unable to validate peer's chain, disconnecting: " + peer, err);
              peer.sendGoodbye(GoodbyeMessage.REASON_UNABLE_TO_VERIFY_NETWORK).reportExceptions();
              return false;
            });
  }

  private SafeFuture<Boolean> checkRemoteChain() {
    // Check fork compatibility
    Bytes4 expectedFork = storageClient.getForkAtSlot(status.getHeadSlot());
    if (!Objects.equals(expectedFork, status.getHeadForkVersion())) {
      LOG.trace(
          "Peer's fork ({}) differs from our fork ({}): {}",
          status.getHeadForkVersion(),
          expectedFork,
          peer);
      return SafeFuture.completedFuture(false);
    }

    // Shortcut finalized block checks if our node or our peer has not reached genesis
    if (storageClient.isPreGenesis()) {
      // If we haven't reached genesis, accept our peer at this point
      LOG.trace("Validating peer pre-genesis, skip finalized block checks for peer {}", peer);
      return SafeFuture.completedFuture(true);
    } else if (PeerStatus.isPreGenesisStatus(status, expectedFork)) {
      // Our peer hasn't reached genesis, accept them for now
      LOG.trace("Peer has not reached genesis, skip finalized block checks for peer {}", peer);
      return SafeFuture.completedFuture(true);
    }

    final Checkpoint finalizedCheckpoint = storageClient.getStore().getFinalizedCheckpoint();
    final UnsignedLong finalizedEpoch = finalizedCheckpoint.getEpoch();
    final UnsignedLong remoteFinalizedEpoch = status.getFinalizedEpoch();
    final UnsignedLong currentEpoch = getCurrentEpoch();

    // Make sure remote finalized epoch is reasonable
    if (remoteEpochIsInvalid(currentEpoch, remoteFinalizedEpoch)) {
      LOG.debug(
          "Peer is advertising invalid finalized epoch {} which is at or ahead of our current epoch {}: {}",
          remoteFinalizedEpoch,
          currentEpoch,
          peer);
      return SafeFuture.completedFuture(false);
    }

    // Check whether finalized checkpoints are compatible
    if (finalizedEpoch.compareTo(remoteFinalizedEpoch) == 0) {
      return verifyFinalizedCheckpointsAreTheSame(finalizedCheckpoint);
    } else if (finalizedEpoch.compareTo(remoteFinalizedEpoch) > 0) {
      // We're ahead of our peer, check that we agree with our peer's finalized epoch
      return verifyPeersFinalizedCheckpointIsCanonical();
    } else {
      // Our peer is ahead of us, check that they agree on our finalized epoch
      return verifyPeerAgreesWithOurFinalizedCheckpoint(finalizedCheckpoint);
    }
  }

  private UnsignedLong getCurrentEpoch() {
    final UnsignedLong currentSlot = get_current_slot(storageClient.getStore());
    return compute_epoch_at_slot(currentSlot);
  }

  private boolean remoteEpochIsInvalid(
      final UnsignedLong currentEpoch, final UnsignedLong remoteFinalizedEpoch) {
    // Remote finalized epoch is invalid if it is from the future
    return remoteFinalizedEpoch.compareTo(currentEpoch) > 0
        // Remote finalized epoch is invalid if is from the current epoch (unless we're at genesis)
        || (remoteFinalizedEpoch.compareTo(currentEpoch) == 0
            && !remoteFinalizedEpoch.equals(UnsignedLong.valueOf(Constants.GENESIS_EPOCH)));
  }

  private SafeFuture<Boolean> verifyFinalizedCheckpointsAreTheSame(Checkpoint finalizedCheckpoint) {
    final boolean chainsAreConsistent =
        Objects.equals(finalizedCheckpoint.getRoot(), status.getFinalizedRoot());
    return SafeFuture.completedFuture(chainsAreConsistent);
  }

  private SafeFuture<Boolean> verifyPeersFinalizedCheckpointIsCanonical() {
    final UnsignedLong remoteFinalizedEpoch = status.getFinalizedEpoch();
    final UnsignedLong remoteFinalizedSlot = compute_start_slot_at_epoch(remoteFinalizedEpoch);
    return historicalChainData
        .getFinalizedBlockAtSlot(remoteFinalizedSlot)
        .thenApply(maybeBlock -> toBlock(remoteFinalizedSlot, maybeBlock))
        .thenApply((block) -> validateBlockRootsMatch(block, status.getFinalizedRoot()));
  }

  private SafeFuture<Boolean> verifyPeerAgreesWithOurFinalizedCheckpoint(
      Checkpoint finalizedCheckpoint) {
    final UnsignedLong finalizedEpochSlot = finalizedCheckpoint.getEpochSlot();

    return historicalChainData
        .getFinalizedBlockAtSlot(finalizedEpochSlot)
        .thenApply(maybeBlock -> blockToSlot(finalizedEpochSlot, maybeBlock))
        .thenCompose(blockSlot -> peer.requestBlockBySlot(status.getHeadRoot(), blockSlot))
        .thenApply(block -> validateBlockRootsMatch(block, finalizedCheckpoint.getRoot()));
  }

  private SignedBeaconBlock toBlock(
      UnsignedLong lookupSlot, Optional<SignedBeaconBlock> maybeBlock) {
    return maybeBlock.orElseThrow(
        () -> new IllegalStateException("Missing finalized block at slot " + lookupSlot));
  }

  private UnsignedLong blockToSlot(
      UnsignedLong lookupSlot, Optional<SignedBeaconBlock> maybeBlock) {
    return maybeBlock
        .map(SignedBeaconBlock::getSlot)
        .orElseThrow(
            () -> new IllegalStateException("Missing historical block for slot " + lookupSlot));
  }

  private boolean validateBlockRootsMatch(final SignedBeaconBlock block, final Bytes32 root) {
    final Bytes32 blockRoot = block.getMessage().hash_tree_root();
    final boolean rootsMatch = Objects.equals(blockRoot, root);
    if (rootsMatch) {
      LOG.trace("Verified finalized blocks match for peer: {}", peer);
    } else {
      LOG.warn(
          "Detected peer with inconsistent finalized block at slot {}: {}", block.getSlot(), peer);
    }
    return rootsMatch;
  }
}
