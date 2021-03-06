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

package tech.devgao.artemis.state;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.devgao.artemis.Constants;
import tech.devgao.artemis.datastructures.beaconchainblocks.BeaconBlock;
import tech.devgao.artemis.state.util.BlockProcessorUtil;
import tech.devgao.artemis.state.util.EpochProcessorUtil;
import tech.devgao.artemis.state.util.SlotProcessorUtil;

public class StateTransition {

  private static final Logger logger = LogManager.getLogger();

  public StateTransition() {}

  public void initiate(BeaconState state, BeaconBlock block) {

    try {
      // per-slot processing
      slotProcessor(state, block);
      // per-block processing
      if (block != null) {
        blockProcessor(state, block);
      }
      // per-epoch processing
      if (state.getSlot() % Constants.EPOCH_LENGTH == 0) {
        epochProcessor(state);
      }
    } catch (Exception e) {
      // e.printStackTrace();
    }
  }

  protected void slotProcessor(BeaconState state, BeaconBlock block) throws Exception {
    state.incrementSlot();
    logger.info("Processing new slot: " + state.getSlot());
    SlotProcessorUtil.updateProposerRandaoLayer(state);
    // Slots the proposer has skipped (i.e. layers of RANDAO expected)
    // should be in ValidatorRecord.randao_skips
    SlotProcessorUtil.updateLatestRandaoMixes(state);
    SlotProcessorUtil.updateRecentBlockHashes(state, block);
  }

  protected void blockProcessor(BeaconState state, BeaconBlock block) {
    logger.info("Processing new block in slot: " + block.getSlot());
    // block header
    BlockProcessorUtil.verify_signature(state, block);
    // verifyAndUpdateRandao(state, block);

    // block body operations
    // processAttestations(state, block);
  }

  protected void epochProcessor(BeaconState state) throws Exception {
    logger.info("Processing new epoch in slot: " + state.getSlot());
    EpochProcessorUtil.updateJustification(state);
    EpochProcessorUtil.updateFinalization(state);
    EpochProcessorUtil.updateCrosslinks(state);
    EpochProcessorUtil.finalBookKeeping(state);
  }
}
