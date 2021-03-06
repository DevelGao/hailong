/*
 * Copyright 2018 Developer Gao.
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

package tech.devgao.artemis.datastructures.BeaconChainState;

import tech.devgao.artemis.ethereum.core.Hash;
import tech.devgao.artemis.util.uint.UInt64;

public class CrosslinkRecord {

  private Hash shard_block_hash;
  private UInt64 slot;

  public CrosslinkRecord(Hash shard_block_hash, UInt64 slot) {
    this.shard_block_hash = shard_block_hash;
    this.slot = slot;
  }

  public Hash getShard_block_hash() {
    return shard_block_hash;
  }

  public void setShard_block_hash(Hash shard_block_hash) {
    this.shard_block_hash = shard_block_hash;
  }

  public UInt64 getSlot() {
    return slot;
  }

  public void setSlot(UInt64 slot) {
    this.slot = slot;
  }
}
