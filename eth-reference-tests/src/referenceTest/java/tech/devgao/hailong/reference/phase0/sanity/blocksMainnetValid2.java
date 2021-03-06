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

package tech.devgao.hailong.reference.phase0.sanity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.errorprone.annotations.MustBeClosed;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import org.apache.tuweni.junit.BouncyCastleExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tech.devgao.hailong.datastructures.blocks.SignedBeaconBlock;
import tech.devgao.hailong.datastructures.state.BeaconState;
import tech.devgao.hailong.datastructures.state.BeaconStateWithCache;
import tech.devgao.hailong.ethtests.TestSuite;
import tech.devgao.hailong.statetransition.StateTransition;

@ExtendWith(BouncyCastleExtension.class)
public class blocksMainnetValid2 extends TestSuite {

  @ParameterizedTest(name = "{index}.{2} Sanity blocks valid (Mainnet)")
  @MethodSource({
    "sanityEmptyEpochTransitionSetup",
    "sanityHistoricalBatchSetup",
    "sanityProposerSlashingSetup",
    "sanitySameSlotBlockTransitionSetup",
    "sanitySkippedSlotsSetup",
    "sanityVoluntaryExitSetup",
  })
  void sanityProcessBlock(
      BeaconState pre, BeaconState post, String testName, List<SignedBeaconBlock> blocks) {
    BeaconStateWithCache preWithCache = BeaconStateWithCache.fromBeaconState(pre);
    StateTransition stateTransition = new StateTransition(false);
    blocks.forEach(
        block -> assertDoesNotThrow(() -> stateTransition.initiate(preWithCache, block, true)));
    assertEquals(preWithCache, post);
  }

  @MustBeClosed
  static Stream<Arguments> sanityEmptyEpochTransitionSetup() throws Exception {
    Path configPath = Paths.get("mainnet", "phase0");
    Path path = Paths.get("/mainnet/phase0/sanity/blocks/pyspec_tests/empty_epoch_transition");
    return sanityMultiBlockSetup(path, configPath);
  }

  @MustBeClosed
  static Stream<Arguments> sanityHistoricalBatchSetup() throws Exception {
    Path configPath = Paths.get("mainnet", "phase0");
    Path path = Paths.get("/mainnet/phase0/sanity/blocks/pyspec_tests/historical_batch");
    return sanityMultiBlockSetup(path, configPath);
  }

  @MustBeClosed
  static Stream<Arguments> sanityProposerSlashingSetup() throws Exception {
    Path configPath = Paths.get("mainnet", "phase0");
    Path path = Paths.get("/mainnet/phase0/sanity/blocks/pyspec_tests/proposer_slashing");
    return sanityMultiBlockSetup(path, configPath);
  }

  @MustBeClosed
  static Stream<Arguments> sanitySameSlotBlockTransitionSetup() throws Exception {
    Path configPath = Paths.get("mainnet", "phase0");
    Path path = Paths.get("/mainnet/phase0/sanity/blocks/pyspec_tests/same_slot_block_transition");
    return sanityMultiBlockSetup(path, configPath);
  }

  @MustBeClosed
  static Stream<Arguments> sanitySkippedSlotsSetup() throws Exception {
    Path configPath = Paths.get("mainnet", "phase0");
    Path path = Paths.get("/mainnet/phase0/sanity/blocks/pyspec_tests/skipped_slots");
    return sanityMultiBlockSetup(path, configPath);
  }

  @MustBeClosed
  static Stream<Arguments> sanityVoluntaryExitSetup() throws Exception {
    Path configPath = Paths.get("mainnet", "phase0");
    Path path = Paths.get("/mainnet/phase0/sanity/blocks/pyspec_tests/voluntary_exit");
    return sanityMultiBlockSetup(path, configPath);
  }
}
