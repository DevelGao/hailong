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

package tech.devgao.hailong.networking.eth2.gossip;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.eventbus.EventBus;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.devgao.hailong.datastructures.operations.AggregateAndProof;
import tech.devgao.hailong.datastructures.util.DataStructureUtil;
import tech.devgao.hailong.datastructures.util.SimpleOffsetSerializer;
import tech.devgao.hailong.networking.eth2.gossip.topics.AggregateTopicHandler;
import tech.devgao.hailong.networking.p2p.gossip.GossipNetwork;
import tech.devgao.hailong.networking.p2p.gossip.TopicChannel;
import tech.devgao.hailong.storage.ChainStorageClient;

public class AggregateGossipManagerTest {

  private final EventBus eventBus = new EventBus();
  private final ChainStorageClient storageClient = ChainStorageClient.memoryOnlyClient(eventBus);
  private final GossipNetwork gossipNetwork = mock(GossipNetwork.class);
  private final TopicChannel topicChannel = mock(TopicChannel.class);

  @BeforeEach
  public void setup() {
    doReturn(topicChannel).when(gossipNetwork).subscribe(eq(AggregateTopicHandler.TOPIC), any());
    new AggregateGossipManager(gossipNetwork, eventBus, storageClient);
  }

  @Test
  public void onNewAggregate() {
    final AggregateAndProof aggregate = DataStructureUtil.randomAggregateAndProof(1);
    final Bytes serialized = SimpleOffsetSerializer.serialize(aggregate);

    eventBus.post(aggregate);
    verify(topicChannel).gossip(serialized);
  }
}
