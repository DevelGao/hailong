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

package tech.devgao.artemis.networking.p2p.hobbits;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.develgao.cava.bytes.Bytes;
import net.develgao.cava.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import tech.devgao.artemis.datastructures.Constants;
import tech.devgao.artemis.datastructures.blocks.BeaconBlock;
import tech.devgao.artemis.datastructures.util.DataStructureUtil;

final class GossipCodecTest {

  @Test
  void testGossip() {
    BeaconBlock block = DataStructureUtil.randomBeaconBlock(Constants.GENESIS_SLOT);
    Bytes encoded =
        GossipCodec.encode(
            GossipMethod.GOSSIP, Bytes32.random(), Bytes32.random(), block.toBytes());
    GossipMessage message = GossipCodec.decode(encoded);
    assertEquals(GossipMethod.GOSSIP, message.method());
    BeaconBlock read = BeaconBlock.fromBytes(message.bodyAs(Bytes.class));
    assertEquals(read.getSignature(), block.getSignature());
  }
}