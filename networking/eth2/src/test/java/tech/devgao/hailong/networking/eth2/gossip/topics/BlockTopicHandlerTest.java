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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.common.eventbus.EventBus;
import com.google.common.primitives.UnsignedLong;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.devgao.hailong.datastructures.blocks.SignedBeaconBlock;
import tech.devgao.hailong.datastructures.util.DataStructureUtil;
import tech.devgao.hailong.datastructures.util.SimpleOffsetSerializer;
import tech.devgao.hailong.networking.eth2.gossip.events.GossipedBlockEvent;
import tech.devgao.hailong.statetransition.BeaconChainUtil;
import tech.devgao.hailong.storage.ChainStorageClient;

public class BlockTopicHandlerTest {
  private final EventBus eventBus = mock(EventBus.class);
  private final ChainStorageClient storageClient = ChainStorageClient.memoryOnlyClient(eventBus);
  private final BeaconChainUtil beaconChainUtil = BeaconChainUtil.create(2, storageClient);
  private final BlockTopicHandler topicHandler = new BlockTopicHandler(eventBus, storageClient);

  @BeforeEach
  public void setup() {
    beaconChainUtil.initializeStorage();
  }

  @Test
  public void handleMessage_validBlock() throws Exception {
    final UnsignedLong nextSlot = storageClient.getBestSlot().plus(UnsignedLong.ONE);
    final SignedBeaconBlock block = beaconChainUtil.createBlockAtSlot(nextSlot);
    Bytes serialized = SimpleOffsetSerializer.serialize(block);
    beaconChainUtil.setSlot(nextSlot);

    final boolean result = topicHandler.handleMessage(serialized);
    assertThat(result).isEqualTo(true);
    verify(eventBus).post(new GossipedBlockEvent(block));
  }

  @Test
  public void handleMessage_validFutureBlock() throws Exception {
    final UnsignedLong nextSlot = storageClient.getBestSlot().plus(UnsignedLong.ONE);
    final SignedBeaconBlock block = beaconChainUtil.createBlockAtSlot(nextSlot);
    Bytes serialized = SimpleOffsetSerializer.serialize(block);
    beaconChainUtil.setSlot(storageClient.getBestSlot());

    final boolean result = topicHandler.handleMessage(serialized);
    assertThat(result).isEqualTo(false);
    verify(eventBus).post(new GossipedBlockEvent(block));
  }

  @Test
  public void handleMessage_invalidBlock_unknownPreState() {
    SignedBeaconBlock block = DataStructureUtil.randomSignedBeaconBlock(1, 100);
    Bytes serialized = SimpleOffsetSerializer.serialize(block);

    final boolean result = topicHandler.handleMessage(serialized);
    assertThat(result).isEqualTo(false);
    verify(eventBus).post(new GossipedBlockEvent(block));
  }

  @Test
  public void handleMessage_invalidBlock_invalidSSZ() {
    Bytes serialized = Bytes.fromHexString("0x1234");

    final boolean result = topicHandler.handleMessage(serialized);
    assertThat(result).isEqualTo(false);
  }

  @Test
  public void handleMessage_invalidBlock_wrongProposer() throws Exception {
    final UnsignedLong nextSlot = storageClient.getBestSlot().plus(UnsignedLong.ONE);
    final SignedBeaconBlock block = beaconChainUtil.createBlockAtSlotFromInvalidProposer(nextSlot);
    Bytes serialized = SimpleOffsetSerializer.serialize(block);
    beaconChainUtil.setSlot(nextSlot);

    final boolean result = topicHandler.handleMessage(serialized);
    assertThat(result).isEqualTo(false);
    verify(eventBus, never()).post(new GossipedBlockEvent(block));
  }
}
