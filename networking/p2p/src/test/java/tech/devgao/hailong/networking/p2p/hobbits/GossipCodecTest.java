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

package tech.devgao.hailong.networking.p2p.hobbits;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.util.Date;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.hobbits.Message;
import org.apache.tuweni.plumtree.MessageSender;
import org.junit.jupiter.api.Test;
import tech.devgao.hailong.datastructures.Constants;
import tech.devgao.hailong.datastructures.blocks.BeaconBlock;
import tech.devgao.hailong.datastructures.util.DataStructureUtil;
import tech.devgao.hailong.networking.p2p.hobbits.gossip.GossipCodec;
import tech.devgao.hailong.networking.p2p.hobbits.gossip.GossipMessage;

final class GossipCodecTest {

  @Test
  void testGossip() {
    BeaconBlock block = DataStructureUtil.randomBeaconBlock(Constants.GENESIS_SLOT);
    Message encoded =
        GossipCodec.encode(
            MessageSender.Verb.GOSSIP.ordinal(),
            "BLOCK",
            BigInteger.valueOf(new Date().getTime()),
            Bytes32.random().toArrayUnsafe(),
            Bytes32.random().toArrayUnsafe(),
            block.toBytes().toArrayUnsafe());
    GossipMessage message = GossipCodec.decode(encoded);
    assertEquals(MessageSender.Verb.GOSSIP.ordinal(), message.method());
    BeaconBlock read = BeaconBlock.fromBytes(Bytes.wrap(message.body()));
    assertEquals(read.getSignature(), block.getSignature());
  }
}
