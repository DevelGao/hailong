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

package tech.devgao.hailong.data.recorder;

import static tech.devgao.hailong.datastructures.util.SimpleOffsetSerializer.serialize;
import static tech.devgao.hailong.util.alogger.ALogger.STDOUT;

import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.UnsignedLong;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.Level;
import org.apache.tuweni.bytes.Bytes32;
import tech.devgao.hailong.data.BlockProcessingRecord;
import tech.devgao.hailong.datastructures.state.BeaconState;
import tech.devgao.hailong.datastructures.state.Checkpoint;
import tech.devgao.hailong.storage.Store;
import tech.devgao.hailong.storage.events.StoreGenesisDiskUpdateEvent;
import tech.devgao.hailong.util.config.Constants;
import tech.devgao.hailong.util.sos.SimpleOffsetSerializable;

public class SSZTransitionRecorder {
  private final Path outputDirectory;

  public SSZTransitionRecorder(final Path outputDirectory) {
    this.outputDirectory = mkdirs(outputDirectory);
  }

  @Subscribe
  public void onGenesisEvent(final StoreGenesisDiskUpdateEvent genesisEvent) {
    final Store store = genesisEvent.getStore();
    final Checkpoint finalizedCheckpoint = store.getFinalizedCheckpoint();
    if (isNotGenesis(finalizedCheckpoint)) {
      return;
    }
    final Bytes32 genesisRoot = finalizedCheckpoint.getRoot();
    final BeaconState genesisState = store.getBlockState(genesisRoot);
    store(outputDirectory.resolve("genesis.ssz"), genesisState);
  }

  private boolean isNotGenesis(final Checkpoint finalizedCheckpoint) {
    final UnsignedLong genesisEpoch = UnsignedLong.valueOf(Constants.GENESIS_EPOCH);
    return finalizedCheckpoint == null
        || finalizedCheckpoint.getEpoch().compareTo(genesisEpoch) != 0;
  }

  @Subscribe
  public void onBlockProcessingRecord(final BlockProcessingRecord record) {
    final Path slotDirectory = slotDirectory(record.getBlock().getSlot());
    store(slotDirectory.resolve("pre.ssz"), record.getPreState());
    store(slotDirectory.resolve("block.ssz"), record.getBlock());
    store(slotDirectory.resolve("post.ssz"), record.getPostState());
  }

  private void store(final Path file, SimpleOffsetSerializable data) {
    try {
      Files.write(file, serialize(data).toArrayUnsafe());
    } catch (final IOException e) {
      STDOUT.log(Level.ERROR, "Failed to record data to " + file + ": " + e.getMessage());
    }
  }

  private Path mkdirs(final Path dir) {
    if (!dir.toFile().mkdirs() && !dir.toFile().isDirectory()) {
      STDOUT.log(
          Level.ERROR, "Failed to create transition record directory " + dir.toAbsolutePath());
    }
    return dir;
  }

  private Path slotDirectory(final UnsignedLong slot) {
    return mkdirs(outputDirectory.resolve(slot.toString()));
  }
}
