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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.tuweni.bytes.Bytes;
import tech.devgao.hailong.datastructures.operations.AggregateAndProof;
import tech.devgao.hailong.datastructures.util.SimpleOffsetSerializer;
import tech.devgao.hailong.networking.eth2.gossip.topics.AggregateTopicHandler;
import tech.devgao.hailong.networking.p2p.gossip.GossipNetwork;
import tech.devgao.hailong.networking.p2p.gossip.TopicChannel;
import tech.devgao.hailong.storage.ChainStorageClient;

public class AggregateGossipManager {
  private final EventBus eventBus;
  private final TopicChannel channel;
  private final AtomicBoolean shutdown = new AtomicBoolean(false);

  public AggregateGossipManager(
      final GossipNetwork gossipNetwork,
      final EventBus eventBus,
      final ChainStorageClient chainStorageClient) {
    final AggregateTopicHandler aggregateTopicHandler =
        new AggregateTopicHandler(eventBus, chainStorageClient);
    this.eventBus = eventBus;
    channel = gossipNetwork.subscribe(aggregateTopicHandler.getTopic(), aggregateTopicHandler);
    eventBus.register(this);
  }

  @Subscribe
  public void onNewAggregate(final AggregateAndProof aggregateAndProof) {
    final Bytes data = SimpleOffsetSerializer.serialize(aggregateAndProof);
    channel.gossip(data);
  }

  public void shutdown() {
    if (shutdown.compareAndSet(false, true)) {
      eventBus.unregister(this);
      // Close gossip channels
      channel.close();
    }
  }
}
