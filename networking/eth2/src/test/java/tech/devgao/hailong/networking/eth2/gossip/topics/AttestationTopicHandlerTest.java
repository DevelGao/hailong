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

package tech.devgao.hailong.networking.eth2.gossip.topics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.common.eventbus.EventBus;
import java.util.List;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.devgao.hailong.datastructures.operations.Attestation;
import tech.devgao.hailong.datastructures.util.SimpleOffsetSerializer;
import tech.devgao.hailong.statetransition.AttestationGenerator;
import tech.devgao.hailong.statetransition.BeaconChainUtil;
import tech.devgao.hailong.storage.ChainStorageClient;
import tech.devgao.hailong.storage.Store;
import tech.devgao.hailong.util.bls.BLSKeyGenerator;
import tech.devgao.hailong.util.bls.BLSKeyPair;

public class AttestationTopicHandlerTest {
  private final List<BLSKeyPair> validatorKeys = BLSKeyGenerator.generateKeyPairs(12);
  private final EventBus eventBus = mock(EventBus.class);
  private final ChainStorageClient storageClient = ChainStorageClient.memoryOnlyClient(eventBus);
  private final AttestationTopicHandler topicHandler =
      new AttestationTopicHandler(eventBus, storageClient, 1);

  @BeforeEach
  public void setup() {
    BeaconChainUtil.initializeStorage(storageClient, validatorKeys);
  }

  @Test
  public void handleMessage_validAttestation() throws Exception {
    final AttestationGenerator attestationGenerator = new AttestationGenerator(validatorKeys);
    final Attestation attestation = attestationGenerator.validAttestation(storageClient);
    final Bytes serialized = SimpleOffsetSerializer.serialize(attestation);

    final boolean result = topicHandler.handleMessage(serialized);
    assertThat(result).isEqualTo(true);
    verify(eventBus).post(attestation);
  }

  @Test
  public void handleMessage_invalidAttestationSignature() throws Exception {
    final AttestationGenerator attestationGenerator = new AttestationGenerator(validatorKeys);
    final Attestation attestation =
        attestationGenerator.attestationWithInvalidSignature(storageClient);
    final Bytes serialized = SimpleOffsetSerializer.serialize(attestation);

    final boolean result = topicHandler.handleMessage(serialized);
    assertThat(result).isEqualTo(false);
    verify(eventBus, never()).post(attestation);
  }

  @Test
  public void handleMessage_invalidAttestation_invalidSSZ() {
    final Bytes serialized = Bytes.fromHexString("0x3456");

    final boolean result = topicHandler.handleMessage(serialized);
    assertThat(result).isEqualTo(false);
  }

  @Test
  public void handleMessage_invalidAttestation_missingState() throws Exception {
    final AttestationGenerator attestationGenerator = new AttestationGenerator(validatorKeys);
    final Attestation attestation = attestationGenerator.validAttestation(storageClient);
    final Bytes serialized = SimpleOffsetSerializer.serialize(attestation);

    // Set up state to be missing
    final Bytes32 blockRoot = attestation.getData().getBeacon_block_root();
    Store mockStore = mock(Store.class);
    storageClient.setStore(mockStore);
    doReturn(null).when(mockStore).getBlockState(blockRoot);

    final boolean result = topicHandler.handleMessage(serialized);
    assertThat(result).isEqualTo(false);
    verify(eventBus, never()).post(attestation);
  }
}
