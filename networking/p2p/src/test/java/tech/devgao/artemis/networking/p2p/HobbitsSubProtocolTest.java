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

package tech.devgao.artemis.networking.p2p;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.eventbus.EventBus;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier;
import org.junit.jupiter.api.Test;
import tech.devgao.artemis.data.TimeSeriesRecord;

final class HobbitsSubProtocolTest {

  @Test
  void testId() {
    HobbitsSubProtocol subprotocol =
        new HobbitsSubProtocol(
            new EventBus(), "", new TimeSeriesRecord(), new ConcurrentHashMap<>());
    assertEquals("hob", subprotocol.id().name());
    assertEquals(1, subprotocol.id().version());
  }

  @Test
  void supportsCheck() {
    HobbitsSubProtocol subprotocol =
        new HobbitsSubProtocol(
            new EventBus(), "", new TimeSeriesRecord(), new ConcurrentHashMap<>());
    assertTrue(subprotocol.supports(SubProtocolIdentifier.of("hob", 1)));
  }
}
