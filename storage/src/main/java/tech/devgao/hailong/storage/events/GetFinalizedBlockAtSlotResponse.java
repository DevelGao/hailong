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
import java.util.Optional;
import tech.devgao.hailong.datastructures.blocks.SignedBeaconBlock;

public class GetFinalizedBlockAtSlotResponse {
  private final UnsignedLong slot;
  private final Optional<SignedBeaconBlock> block;

  public GetFinalizedBlockAtSlotResponse(
      final UnsignedLong slot, final Optional<SignedBeaconBlock> block) {
    this.slot = slot;
    this.block = block;
  }

  public UnsignedLong getSlot() {
    return slot;
  }

  public Optional<SignedBeaconBlock> getBlock() {
    return block;
  }
}
