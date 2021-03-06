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

package tech.devgao.artemis.datastructures.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static tech.devgao.artemis.datastructures.util.DataStructureUtil.randomAttestationData;

import java.util.Objects;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import tech.devgao.artemis.util.bls.BLSSignature;

class AttestationTest {

  private Bytes aggregationBitfield = Bytes32.random();
  private AttestationData data = randomAttestationData();
  private Bytes custodyBitfield = Bytes32.random();
  private BLSSignature aggregateSignature = BLSSignature.random();

  private Attestation attestation =
      new Attestation(aggregationBitfield, data, custodyBitfield, aggregateSignature);

  @Test
  void equalsReturnsTrueWhenObjectsAreSame() {
    Attestation testAttestation = attestation;

    assertEquals(attestation, testAttestation);
  }

  @Test
  void equalsReturnsTrueWhenObjectFieldsAreEqual() {
    Attestation testAttestation =
        new Attestation(aggregationBitfield, data, custodyBitfield, aggregateSignature);

    assertEquals(attestation, testAttestation);
  }

  @Test
  void equalsReturnsFalseWhenAggregationBitfieldsAreDifferent() {
    Attestation testAttestation =
        new Attestation(aggregationBitfield.not(), data, custodyBitfield, aggregateSignature);

    assertNotEquals(attestation, testAttestation);
  }

  @Test
  void equalsReturnsFalseWhenAttestationDataIsDifferent() {
    // AttestationData is rather involved to create. Just create a random one until it is not the
    // same as the original.
    AttestationData otherData = randomAttestationData();
    while (Objects.equals(otherData, data)) {
      otherData = randomAttestationData();
    }

    Attestation testAttestation =
        new Attestation(aggregationBitfield, otherData, custodyBitfield, aggregateSignature);

    assertNotEquals(attestation, testAttestation);
  }

  @Test
  void equalsReturnsFalseWhenCustodyBitfieldsAreDifferent() {
    Attestation testAttestation =
        new Attestation(aggregationBitfield, data, custodyBitfield.not(), aggregateSignature);

    assertNotEquals(attestation, testAttestation);
  }

  @Test
  void equalsReturnsFalseWhenAggregrateSignaturesAreDifferent() {
    BLSSignature differentAggregateSignature = BLSSignature.random();
    while (differentAggregateSignature.equals(aggregateSignature)) {
      differentAggregateSignature = BLSSignature.random();
    }

    Attestation testAttestation =
        new Attestation(aggregationBitfield, data, custodyBitfield, differentAggregateSignature);

    assertNotEquals(attestation, testAttestation);
  }

  @Test
  void roundtripSSZ() {
    Bytes sszAttestationBytes = attestation.toBytes();
    assertEquals(attestation, Attestation.fromBytes(sszAttestationBytes));
  }

  @Test
  void roundtripSSZVariableLengthBitfield() {
    Attestation byte1BitfieldAttestation =
        new Attestation(
            Bytes.fromHexString("0x00"), data, Bytes.fromHexString("0x00"), aggregateSignature);
    Attestation byte4BitfieldAttestation =
        new Attestation(
            Bytes.fromHexString("0x00000000"),
            data,
            Bytes.fromHexString("0x00000000"),
            aggregateSignature);
    Attestation byte8BitfieldAttestation =
        new Attestation(
            Bytes.fromHexString("0x0000000000000000"),
            data,
            Bytes.fromHexString("0x0000000000000000"),
            aggregateSignature);
    Attestation byte16BitfieldAttestation =
        new Attestation(
            Bytes.fromHexString("0x00000000000000000000000000000000"),
            data,
            Bytes.fromHexString("0x00000000000000000000000000000000"),
            aggregateSignature);
    Bytes byte1BitfieldAttestationBytes = byte1BitfieldAttestation.toBytes();
    Bytes byte4BitfieldAttestationBytes = byte4BitfieldAttestation.toBytes();
    Bytes byte8BitfieldAttestationBytes = byte8BitfieldAttestation.toBytes();
    Bytes byte16BitfieldAttestationBytes = byte16BitfieldAttestation.toBytes();
    assertEquals(byte1BitfieldAttestation, Attestation.fromBytes(byte1BitfieldAttestationBytes));
    assertEquals(byte4BitfieldAttestation, Attestation.fromBytes(byte4BitfieldAttestationBytes));
    assertEquals(byte8BitfieldAttestation, Attestation.fromBytes(byte8BitfieldAttestationBytes));
    assertEquals(byte16BitfieldAttestation, Attestation.fromBytes(byte16BitfieldAttestationBytes));
  }
}
