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

package tech.devgao.hailong.statetransition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static tech.devgao.hailong.statetransition.AttestationGenerator.diffSlotAttestationData;
import static tech.devgao.hailong.statetransition.AttestationGenerator.getSingleAttesterIndex;

import com.google.common.eventbus.EventBus;
import com.google.common.primitives.UnsignedLong;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.devgao.hailong.datastructures.operations.Attestation;
import tech.devgao.hailong.datastructures.validator.AggregatorInformation;
import tech.devgao.hailong.storage.ChainStorageClient;
import tech.devgao.hailong.util.bls.BLSAggregate;
import tech.devgao.hailong.util.bls.BLSKeyGenerator;
import tech.devgao.hailong.util.bls.BLSKeyPair;
import tech.devgao.hailong.util.bls.BLSSignature;

class AttestationAggregatorTest {

  private final List<BLSKeyPair> validatorKeys = BLSKeyGenerator.generateKeyPairs(12);
  private final ChainStorageClient storageClient =
      ChainStorageClient.memoryOnlyClient(mock(EventBus.class));
  private AttestationGenerator attestationGenerator = new AttestationGenerator(validatorKeys);
  private AttestationAggregator aggregator;

  @BeforeEach
  void setup() {
    BeaconChainUtil.initializeStorage(storageClient, validatorKeys);
    aggregator = new AttestationAggregator();
  }

  @Test
  void addOwnValidatorAttestation_newData() throws Exception {
    Attestation attestation = attestationGenerator.validAttestation(storageClient);
    int validatorIndex = new Random().nextInt(1000);
    aggregator.committeeIndexToAggregatorInformation.put(
        attestation.getData().getIndex(),
        new AggregatorInformation(BLSSignature.random(), validatorIndex));
    aggregator.addOwnValidatorAttestation(attestation);
    Attestation aggregateAttesation = aggregator.getAggregateAndProofs().get(0).getAggregate();
    assertEquals(attestation, aggregateAttesation);
  }

  @Test
  void addOwnValidatorAttestation_oldData_noNewAttester() throws Exception {
    Attestation attestation = attestationGenerator.validAttestation(storageClient);
    int validatorIndex = new Random().nextInt(1000);
    aggregator.committeeIndexToAggregatorInformation.put(
        attestation.getData().getIndex(),
        new AggregatorInformation(BLSSignature.random(), validatorIndex));
    aggregator.addOwnValidatorAttestation(attestation);
    Attestation newAttestation = new Attestation(attestation);
    newAttestation.setAggregate_signature(BLSSignature.random());
    aggregator.addOwnValidatorAttestation(newAttestation);
    assertEquals(aggregator.getAggregateAndProofs().size(), 1);
    assertEquals(attestation, aggregator.getAggregateAndProofs().get(0).getAggregate());
  }

  @Test
  void addOwnValidatorAttestation_oldData_newAttester() throws Exception {
    Attestation attestation = attestationGenerator.validAttestation(storageClient);
    BLSSignature sig1 = attestation.getAggregate_signature();
    int validatorIndex = new Random().nextInt(1000);
    aggregator.committeeIndexToAggregatorInformation.put(
        attestation.getData().getIndex(),
        new AggregatorInformation(BLSSignature.random(), validatorIndex));
    aggregator.addOwnValidatorAttestation(attestation);
    Attestation newAttestation = AttestationGenerator.withNewSingleAttesterBit(attestation);
    int newAttesterIndex = getSingleAttesterIndex(newAttestation);
    BLSSignature sig2 = BLSSignature.random();
    newAttestation.setAggregate_signature(sig2);
    aggregator.addOwnValidatorAttestation(newAttestation);
    assertEquals(aggregator.getAggregateAndProofs().size(), 1);
    assertTrue(
        aggregator
                .getAggregateAndProofs()
                .get(0)
                .getAggregate()
                .getAggregation_bits()
                .getBit(newAttesterIndex)
            == 1);
    assertEquals(
        aggregator.getAggregateAndProofs().get(0).getAggregate().getAggregate_signature(),
        BLSAggregate.bls_aggregate_signatures(List.of(sig1, sig2)));
  }

  @Test
  void processAttestation_newData_noOwnValidatorAttestationExists() throws Exception {
    Attestation attestation = attestationGenerator.validAttestation(storageClient);
    int validatorIndex = new Random().nextInt(1000);
    aggregator.committeeIndexToAggregatorInformation.put(
        attestation.getData().getIndex(),
        new AggregatorInformation(BLSSignature.random(), validatorIndex));
    aggregator.addOwnValidatorAttestation(attestation);
    Attestation newAttestation = new Attestation(attestation);
    newAttestation.setData(
        diffSlotAttestationData(
            attestation.getData().getSlot().plus(UnsignedLong.ONE), attestation.getData()));
    newAttestation.setAggregate_signature(BLSSignature.random());
    aggregator.processAttestation(newAttestation);
    assertEquals(aggregator.getAggregateAndProofs().size(), 1);
    assertEquals(attestation, aggregator.getAggregateAndProofs().get(0).getAggregate());
  }

  @Test
  void processAttestation_oldData_noNewAttester() throws Exception {
    Attestation attestation = attestationGenerator.validAttestation(storageClient);
    int validatorIndex = new Random().nextInt(1000);
    aggregator.committeeIndexToAggregatorInformation.put(
        attestation.getData().getIndex(),
        new AggregatorInformation(BLSSignature.random(), validatorIndex));
    aggregator.addOwnValidatorAttestation(attestation);
    Attestation newAttestation = new Attestation(attestation);
    newAttestation.setAggregate_signature(BLSSignature.random());
    aggregator.processAttestation(newAttestation);
    assertEquals(aggregator.getAggregateAndProofs().size(), 1);
    assertEquals(attestation, aggregator.getAggregateAndProofs().get(0).getAggregate());
  }

  @Test
  void processAttestation_oldData_newAttester() throws Exception {
    Attestation attestation = attestationGenerator.validAttestation(storageClient);
    BLSSignature sig1 = attestation.getAggregate_signature();
    int validatorIndex = new Random().nextInt(1000);
    aggregator.committeeIndexToAggregatorInformation.put(
        attestation.getData().getIndex(),
        new AggregatorInformation(BLSSignature.random(), validatorIndex));
    aggregator.addOwnValidatorAttestation(attestation);
    Attestation newAttestation = AttestationGenerator.withNewSingleAttesterBit(attestation);
    int newAttesterIndex = getSingleAttesterIndex(newAttestation);
    BLSSignature sig2 = BLSSignature.random();
    newAttestation.setAggregate_signature(sig2);
    aggregator.processAttestation(newAttestation);
    assertEquals(aggregator.getAggregateAndProofs().size(), 1);
    assertTrue(
        aggregator
                .getAggregateAndProofs()
                .get(0)
                .getAggregate()
                .getAggregation_bits()
                .getBit(newAttesterIndex)
            == 1);
    assertEquals(
        aggregator.getAggregateAndProofs().get(0).getAggregate().getAggregate_signature(),
        BLSAggregate.bls_aggregate_signatures(List.of(sig1, sig2)));
  }

  @Test
  void reset() throws Exception {
    Attestation attestation = attestationGenerator.validAttestation(storageClient);
    int validatorIndex = new Random().nextInt(1000);
    aggregator.committeeIndexToAggregatorInformation.put(
        attestation.getData().getIndex(),
        new AggregatorInformation(BLSSignature.random(), validatorIndex));
    aggregator.addOwnValidatorAttestation(attestation);
    Attestation newAttestation = AttestationGenerator.withNewSingleAttesterBit(attestation);
    BLSSignature sig2 = BLSSignature.random();
    newAttestation.setAggregate_signature(sig2);
    aggregator.processAttestation(newAttestation);
    aggregator.reset();
    assertEquals(aggregator.getAggregateAndProofs().size(), 0);
  }
}
