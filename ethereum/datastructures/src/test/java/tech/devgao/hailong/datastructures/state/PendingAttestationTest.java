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

package tech.devgao.hailong.datastructures.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static tech.devgao.hailong.datastructures.util.DataStructureUtil.randomAttestationData;
import static tech.devgao.hailong.datastructures.util.DataStructureUtil.randomBitlist;
import static tech.devgao.hailong.datastructures.util.DataStructureUtil.randomUnsignedLong;

import com.google.common.primitives.UnsignedLong;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import tech.devgao.hailong.datastructures.operations.AttestationData;
import tech.devgao.hailong.util.SSZTypes.Bitlist;

class PendingAttestationTest {
  private int seed = 100;
  private Bitlist participationBitfield = randomBitlist(seed);
  private AttestationData data = randomAttestationData(seed++);
  private UnsignedLong inclusionDelay = randomUnsignedLong(seed++);
  private UnsignedLong proposerIndex = randomUnsignedLong(seed++);

  private PendingAttestation pendingAttestation =
      new PendingAttestation(participationBitfield, data, inclusionDelay, proposerIndex);

  @Test
  void equalsReturnsTrueWhenObjectAreSame() {
    PendingAttestation testPendingAttestation = pendingAttestation;

    assertEquals(pendingAttestation, testPendingAttestation);
  }

  @Test
  void equalsReturnsTrueWhenObjectFieldsAreEqual() {
    PendingAttestation testPendingAttestation =
        new PendingAttestation(participationBitfield, data, inclusionDelay, proposerIndex);

    assertEquals(pendingAttestation, testPendingAttestation);
  }

  @Test
  void equalsReturnsFalseWhenAttestationDataIsDifferent() {
    // BeaconBlock is rather involved to create. Just create a random one until it is not the same
    // as the original.
    AttestationData otherAttestationData = randomAttestationData(seed++);
    while (Objects.equals(otherAttestationData, data)) {
      otherAttestationData = randomAttestationData(seed++);
    }
    PendingAttestation testPendingAttestation =
        new PendingAttestation(
            participationBitfield, otherAttestationData, inclusionDelay, proposerIndex);

    assertNotEquals(pendingAttestation, testPendingAttestation);
  }

  @Test
  void equalsReturnsFalseWhenParticipationBitfieldsAreDifferent() {
    PendingAttestation testPendingAttestation =
        new PendingAttestation(randomBitlist(seed++), data, inclusionDelay, proposerIndex);

    assertNotEquals(pendingAttestation, testPendingAttestation);
  }

  @Test
  void equalsReturnsFalseWhenCustodyBitfieldsAreDifferent() {
    PendingAttestation testPendingAttestation =
        new PendingAttestation(
            participationBitfield,
            data,
            inclusionDelay.plus(randomUnsignedLong(seed++)),
            proposerIndex);

    assertNotEquals(pendingAttestation, testPendingAttestation);
  }

  @Test
  void equalsReturnsFalseWhenProposerIndicesAreDifferent() {
    PendingAttestation testPendingAttestation =
        new PendingAttestation(
            participationBitfield,
            data,
            inclusionDelay,
            proposerIndex.plus(randomUnsignedLong(seed++)));

    assertNotEquals(pendingAttestation, testPendingAttestation);
  }
}
