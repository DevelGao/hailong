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

package tech.devgao.hailong.datastructures.networking.libp2p.rpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.primitives.UnsignedLong;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import tech.devgao.hailong.datastructures.state.Fork;
import tech.devgao.hailong.datastructures.util.SimpleOffsetSerializer;

class StatusMessageTest {
  @Test
  public void shouldRoundTripViaSsz() {
    final StatusMessage message =
        new StatusMessage(
            Fork.VERSION_ZERO,
            Bytes32.fromHexStringLenient("0x01"),
            UnsignedLong.valueOf(2),
            Bytes32.fromHexStringLenient("0x03"),
            UnsignedLong.valueOf(4));

    final Bytes data = SimpleOffsetSerializer.serialize(message);
    final StatusMessage result = SimpleOffsetSerializer.deserialize(data, StatusMessage.class);
    assertThat(result).isEqualToComparingFieldByField(message);
  }
}
