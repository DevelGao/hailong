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

package tech.devgao.hailong.validator.coordinator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static tech.devgao.hailong.datastructures.blocks.BeaconBlockBodyLists.createAttestations;
import static tech.devgao.hailong.util.Waiter.ensureConditionRemainsMet;

import com.google.common.eventbus.EventBus;
import com.google.common.primitives.UnsignedLong;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.devgao.hailong.datastructures.util.MockStartValidatorKeyPairFactory;
import tech.devgao.hailong.statetransition.AttestationAggregator;
import tech.devgao.hailong.statetransition.BeaconChainUtil;
import tech.devgao.hailong.statetransition.BlockAttestationsPool;
import tech.devgao.hailong.statetransition.events.BlockProposedEvent;
import tech.devgao.hailong.statetransition.events.BroadcastAttestationEvent;
import tech.devgao.hailong.storage.ChainStorageClient;
import tech.devgao.hailong.storage.events.SlotEvent;
import tech.devgao.hailong.util.bls.BLSKeyPair;
import tech.devgao.hailong.util.config.HailongConfiguration;
import tech.devgao.hailong.util.time.StubTimeProvider;

public class ValidatorCoordinatorTest {

  private final StubTimeProvider timeProvider = StubTimeProvider.withTimeInSeconds(1000);
  private BlockAttestationsPool blockAttestationsPool;
  private AttestationAggregator attestationAggregator;
  private EventBus eventBus;
  private ChainStorageClient storageClient;
  private HailongConfiguration config;
  private BeaconChainUtil chainUtil;

  private static final int NUM_VALIDATORS = 12;

  @BeforeEach
  void setup() {
    config = mock(HailongConfiguration.class);
    when(config.getNaughtinessPercentage()).thenReturn(0);
    when(config.getNumValidators()).thenReturn(NUM_VALIDATORS);
    when(config.getValidatorsKeyFile()).thenReturn(null);
    when(config.getInteropOwnedValidatorStartIndex()).thenReturn(0);
    when(config.getInteropOwnedValidatorCount()).thenReturn(NUM_VALIDATORS);

    attestationAggregator = mock(AttestationAggregator.class);
    blockAttestationsPool = mock(BlockAttestationsPool.class);

    when(blockAttestationsPool.getAggregatedAttestationsForBlockAtSlot(any()))
        .thenReturn(createAttestations());

    eventBus = spy(new EventBus());
    storageClient = ChainStorageClient.memoryOnlyClient(eventBus);
    List<BLSKeyPair> blsKeyPairList =
        new MockStartValidatorKeyPairFactory().generateKeyPairs(0, NUM_VALIDATORS);
    chainUtil = BeaconChainUtil.create(storageClient, blsKeyPairList);
  }

  @Test
  void onAttestationEvent_noAttestationAssignments() throws Exception {
    ValidatorCoordinator vc = spy(createValidatorCoordinator(0));
    eventBus.post(
        new BroadcastAttestationEvent(
            storageClient.getBestBlockRoot(), storageClient.getBestSlot()));

    ensureConditionRemainsMet(
        () -> verify(attestationAggregator, never()).updateAggregatorInformations(any()));
    ensureConditionRemainsMet(
        () -> verify(vc, never()).asyncProduceAttestations(any(), any(), any()));
  }

  @Test
  void createBlockAfterNormalSlot() {
    createValidatorCoordinator(NUM_VALIDATORS);
    UnsignedLong newBlockSlot = storageClient.getBestSlot().plus(UnsignedLong.ONE);
    eventBus.post(new SlotEvent(newBlockSlot));
    verify(eventBus, atLeastOnce()).post(blockWithSlot(newBlockSlot));
  }

  @Test
  void createBlockAfterSkippedSlot() {
    createValidatorCoordinator(NUM_VALIDATORS);
    UnsignedLong newBlockSlot = storageClient.getBestSlot().plus(UnsignedLong.valueOf(2));
    eventBus.post(new SlotEvent(newBlockSlot));
    verify(eventBus, atLeastOnce()).post(blockWithSlot(newBlockSlot));
  }

  @Test
  void createBlockAfterMultipleSkippedSlots() {
    createValidatorCoordinator(NUM_VALIDATORS);
    UnsignedLong newBlockSlot = storageClient.getBestSlot().plus(UnsignedLong.valueOf(10));
    eventBus.post(new SlotEvent(newBlockSlot));
    verify(eventBus, atLeastOnce()).post(blockWithSlot(newBlockSlot));
  }

  private ValidatorCoordinator createValidatorCoordinator(final int ownedValidatorCount) {
    when(config.getInteropOwnedValidatorCount()).thenReturn(ownedValidatorCount);
    ValidatorCoordinator vc =
        new ValidatorCoordinator(
            timeProvider,
            eventBus,
            storageClient,
            attestationAggregator,
            blockAttestationsPool,
            config);

    chainUtil.initializeStorage();
    return vc;
  }

  private Object blockWithSlot(final UnsignedLong slotNumber) {
    return argThat(
        argument -> {
          if (!(argument instanceof BlockProposedEvent)) {
            return false;
          }
          final BlockProposedEvent block = (BlockProposedEvent) argument;
          return block.getBlock().getMessage().getSlot().equals(slotNumber);
        });
  }
}
