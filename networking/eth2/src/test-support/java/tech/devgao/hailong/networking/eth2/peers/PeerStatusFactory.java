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

package tech.devgao.hailong.networking.eth2.peers;

import com.google.common.primitives.UnsignedLong;
import java.util.Random;
import org.apache.tuweni.bytes.Bytes32;
import tech.devgao.hailong.datastructures.state.Fork;
import tech.devgao.hailong.util.SSZTypes.Bytes4;
import tech.devgao.hailong.util.config.Constants;

public class PeerStatusFactory {

  private final Random random;

  private PeerStatusFactory(final long seed) {
    random = new Random(seed);
  }

  public static PeerStatusFactory create(final long seed) {
    return new PeerStatusFactory(seed);
  }

  public PeerStatus random() {
    final Bytes4 fork = Fork.VERSION_ZERO;
    final Bytes32 finalizedRoot = randomBytes32();
    final UnsignedLong finalizedEpoch = randomLong(0, 10);
    final Bytes32 headRoot = randomBytes32();
    final long minHeadSlot = (finalizedEpoch.longValue() + 2) * Constants.SLOTS_PER_EPOCH;
    final UnsignedLong headSlot = randomLong(minHeadSlot, minHeadSlot + 5);
    return new PeerStatus(fork, finalizedRoot, finalizedEpoch, headRoot, headSlot);
  }

  private final UnsignedLong randomLong(final long min, final long max) {
    final int range = Math.toIntExact(max - min);
    final long randomLong = random.nextInt(range) + min;
    return UnsignedLong.valueOf(randomLong);
  }

  private final Bytes32 randomBytes32() {
    return Bytes32.wrap(randomBytes(32));
  }

  private final byte[] randomBytes(final int numBytes) {
    final byte[] bytes = new byte[numBytes];
    random.nextBytes(bytes);
    return bytes;
  }
}
