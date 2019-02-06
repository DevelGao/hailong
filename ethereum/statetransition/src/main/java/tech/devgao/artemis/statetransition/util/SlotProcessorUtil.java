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

package tech.devgao.artemis.statetransition.util;

import static java.lang.Math.toIntExact;
import static tech.devgao.artemis.datastructures.Constants.LATEST_BLOCK_ROOTS_LENGTH;
import static tech.devgao.artemis.datastructures.Constants.LATEST_RANDAO_MIXES_LENGTH;

import com.google.common.primitives.UnsignedLong;
import java.util.List;
import java.util.Objects;
import net.develgao.cava.bytes.Bytes32;
import tech.devgao.artemis.datastructures.Constants;
import tech.devgao.artemis.datastructures.blocks.BeaconBlock;
import tech.devgao.artemis.datastructures.state.BeaconState;
import tech.devgao.artemis.datastructures.util.BeaconStateUtil;
import tech.devgao.artemis.statetransition.StateTransitionException;
import tech.devgao.artemis.util.hashtree.HashTreeUtil;

public class SlotProcessorUtil {
  public static void updateLatestRandaoMixes(BeaconState state) {
    // TODO: change values to UnsignedLong
    int currSlot = state.getSlot().intValue();
    List<Bytes32> latestRandaoMixes = state.getLatest_randao_mixes();
    int index = (currSlot - 1) % LATEST_RANDAO_MIXES_LENGTH;
    Bytes32 prevSlotRandaoMix = latestRandaoMixes.get(index);
    latestRandaoMixes.set(currSlot % LATEST_RANDAO_MIXES_LENGTH, prevSlotRandaoMix);
  }

  public static void updateRecentBlockHashes(BeaconState state, BeaconBlock block)
      throws Exception {

    Bytes32 previous_block_root = Bytes32.ZERO;
    if (state.getSlot().compareTo(UnsignedLong.valueOf(Constants.GENESIS_SLOT)) > 0) {
      previous_block_root =
          BeaconStateUtil.get_block_root(state, state.getSlot().minus(UnsignedLong.ONE));
    } else if (!Objects.isNull(block)) {
      previous_block_root = HashTreeUtil.hash_tree_root(block.toBytes());
    }

    if (!previous_block_root.equals(Bytes32.ZERO)) {
      long index = state.getSlot().minus(UnsignedLong.ONE).longValue() % LATEST_BLOCK_ROOTS_LENGTH;
      List<Bytes32> latest_block_roots = state.getLatest_block_roots();

      latest_block_roots.set(toIntExact(index), previous_block_root);
      state.setLatest_block_roots(latest_block_roots);
    }

    if (state
            .getSlot()
            .mod(UnsignedLong.valueOf(LATEST_BLOCK_ROOTS_LENGTH))
            .compareTo(UnsignedLong.ZERO)
        == 0) {
      List<Bytes32> batched_block_roots = state.getBatched_block_roots();
      List<Bytes32> latest_block_roots = state.getLatest_block_roots();
      if (batched_block_roots != null && latest_block_roots != null) {
        Bytes32 merkle_root = BeaconStateUtil.merkle_root(latest_block_roots);
        batched_block_roots.add(merkle_root);
      } else
        throw new StateTransitionException(
            "StateTransitionException: BeaconState cannot be updated due to "
                + "batched_block_roots and latest_block_roots returning a null");
    }
  }
}
