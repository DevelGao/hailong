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

package tech.devgao.hailong.storage.events;

import com.google.common.primitives.UnsignedLong;
import java.util.Map;
import java.util.Optional;
import org.apache.tuweni.bytes.Bytes32;
import tech.devgao.hailong.datastructures.blocks.SignedBeaconBlock;
import tech.devgao.hailong.datastructures.state.BeaconState;
import tech.devgao.hailong.datastructures.state.Checkpoint;

public class StoreDiskUpdateEvent {

  private final long transactionId;
  private final Optional<UnsignedLong> time;
  private final Optional<UnsignedLong> genesisTime;
  private final Optional<Checkpoint> justifiedCheckpoint;
  private final Optional<Checkpoint> finalizedCheckpoint;
  private final Optional<Checkpoint> bestJustifiedCheckpoint;
  private final Map<Bytes32, SignedBeaconBlock> blocks;
  private final Map<Bytes32, BeaconState> blockStates;
  private final Map<Checkpoint, BeaconState> checkpointStates;
  private final Map<UnsignedLong, Checkpoint> latestMessages;

  public StoreDiskUpdateEvent(
      final long transactionId,
      final Optional<UnsignedLong> time,
      final Optional<UnsignedLong> genesisTime,
      final Optional<Checkpoint> justifiedCheckpoint,
      final Optional<Checkpoint> finalizedCheckpoint,
      final Optional<Checkpoint> bestJustifiedCheckpoint,
      final Map<Bytes32, SignedBeaconBlock> blocks,
      final Map<Bytes32, BeaconState> blockStates,
      final Map<Checkpoint, BeaconState> checkpointStates,
      final Map<UnsignedLong, Checkpoint> latestMessages) {
    this.transactionId = transactionId;
    this.time = time;
    this.genesisTime = genesisTime;
    this.justifiedCheckpoint = justifiedCheckpoint;
    this.finalizedCheckpoint = finalizedCheckpoint;
    this.bestJustifiedCheckpoint = bestJustifiedCheckpoint;
    this.blocks = blocks;
    this.blockStates = blockStates;
    this.checkpointStates = checkpointStates;
    this.latestMessages = latestMessages;
  }

  public long getTransactionId() {
    return transactionId;
  }

  public Optional<UnsignedLong> getTime() {
    return time;
  }

  public Optional<UnsignedLong> getGenesisTime() {
    return genesisTime;
  }

  public Optional<Checkpoint> getJustifiedCheckpoint() {
    return justifiedCheckpoint;
  }

  public Optional<Checkpoint> getFinalizedCheckpoint() {
    return finalizedCheckpoint;
  }

  public Optional<Checkpoint> getBestJustifiedCheckpoint() {
    return bestJustifiedCheckpoint;
  }

  public Map<Bytes32, SignedBeaconBlock> getBlocks() {
    return blocks;
  }

  public Map<Bytes32, BeaconState> getBlockStates() {
    return blockStates;
  }

  public Map<Checkpoint, BeaconState> getCheckpointStates() {
    return checkpointStates;
  }

  public Map<UnsignedLong, Checkpoint> getLatestMessages() {
    return latestMessages;
  }
}
