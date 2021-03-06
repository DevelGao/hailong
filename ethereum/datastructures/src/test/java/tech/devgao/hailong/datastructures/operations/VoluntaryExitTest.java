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

package tech.devgao.hailong.datastructures.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static tech.devgao.hailong.datastructures.util.DataStructureUtil.randomUnsignedLong;

import com.google.common.primitives.UnsignedLong;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import tech.devgao.hailong.datastructures.util.SimpleOffsetSerializer;

class VoluntaryExitTest {
  private int seed = 100;
  private UnsignedLong epoch = randomUnsignedLong(seed);
  private UnsignedLong validatorIndex = randomUnsignedLong(seed++);

  private VoluntaryExit voluntaryExit = new VoluntaryExit(epoch, validatorIndex);

  @Test
  void equalsReturnsTrueWhenObjectsAreSame() {
    VoluntaryExit testVoluntaryExit = voluntaryExit;

    assertEquals(voluntaryExit, testVoluntaryExit);
  }

  @Test
  void equalsReturnsTrueWhenObjectFieldsAreEqual() {
    VoluntaryExit testVoluntaryExit = new VoluntaryExit(epoch, validatorIndex);

    assertEquals(voluntaryExit, testVoluntaryExit);
  }

  @Test
  void equalsReturnsFalseWhenEpochsAreDifferent() {
    VoluntaryExit testVoluntaryExit =
        new VoluntaryExit(epoch.plus(randomUnsignedLong(seed++)), validatorIndex);

    assertNotEquals(voluntaryExit, testVoluntaryExit);
  }

  @Test
  void equalsReturnsFalseWhenValidatorIndicesAreDifferent() {
    VoluntaryExit testVoluntaryExit =
        new VoluntaryExit(epoch, validatorIndex.plus(randomUnsignedLong(seed++)));

    assertNotEquals(voluntaryExit, testVoluntaryExit);
  }

  @Test
  void roundtripSSZ() {
    Bytes sszVoluntaryExitBytes = SimpleOffsetSerializer.serialize(voluntaryExit);
    assertEquals(
        voluntaryExit,
        SimpleOffsetSerializer.deserialize(sszVoluntaryExitBytes, VoluntaryExit.class));
  }
}
