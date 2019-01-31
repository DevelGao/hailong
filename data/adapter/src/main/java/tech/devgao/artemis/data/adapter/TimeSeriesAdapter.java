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

package tech.devgao.artemis.data.adapter;

import com.google.common.primitives.UnsignedLong;
import net.develgao.cava.bytes.Bytes32;
import tech.devgao.artemis.data.RawRecord;
import tech.devgao.artemis.data.TimeSeriesRecord;
import tech.devgao.artemis.datastructures.blocks.BeaconBlock;
import tech.devgao.artemis.datastructures.state.BeaconState;
import tech.devgao.artemis.datastructures.util.BeaconStateUtil;
import tech.devgao.artemis.util.alogger.ALogger;
import tech.devgao.artemis.util.hashtree.HashTreeUtil;

/** Transforms a data record into a time series record */
public class TimeSeriesAdapter implements DataAdapter<TimeSeriesRecord> {
  private static final ALogger LOG = new ALogger(TimeSeriesAdapter.class.getName());
  RawRecord input;

  public TimeSeriesAdapter(RawRecord input) {
    this.input = input;
  }

  @Override
  public TimeSeriesRecord transform() {

    long slot = this.input.getHeadBlock().getSlot();
    // TODO: fix this war crime
    long epoch =
        BeaconStateUtil.slot_to_epoch(UnsignedLong.valueOf(this.input.getHeadBlock().getSlot()))
            .longValue();
    BeaconBlock headBlock = this.input.getHeadBlock();
    BeaconState headState = this.input.getHeadState();
    BeaconBlock justifiedBlock = this.input.getJustifiedBlock();
    BeaconState justifiedState = this.input.getJustifiedState();
    long numValidators = headState.getValidator_registry().size();

    Bytes32 headBlockRoot = HashTreeUtil.hash_tree_root(headBlock.toBytes());
    Bytes32 justifiedBlockRoot = HashTreeUtil.hash_tree_root(justifiedBlock.toBytes());
    Bytes32 justifiedStateRoot = HashTreeUtil.hash_tree_root(justifiedState.toBytes());
    return new TimeSeriesRecord(
        this.input.getIndex(),
        slot,
        epoch,
        headBlockRoot.toHexString(),
        headBlock.getState_root().toHexString(),
        headBlock.getParent_root().toHexString(),
        numValidators,
        justifiedBlockRoot.toHexString(),
        justifiedStateRoot.toHexString());
  }
}
