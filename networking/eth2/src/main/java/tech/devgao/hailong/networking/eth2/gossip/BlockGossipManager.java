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
import tech.devgao.hailong.datastructures.util.SimpleOffsetSerializer;
import tech.devgao.hailong.networking.eth2.gossip.topics.BlockTopicHandler;
import tech.devgao.hailong.networking.p2p.gossip.GossipNetwork;
import tech.devgao.hailong.networking.p2p.gossip.TopicChannel;
import tech.devgao.hailong.statetransition.events.BlockProposedEvent;
import tech.devgao.hailong.storage.ChainStorageClient;

public class BlockGossipManager {
  private final EventBus eventBus;
  private final TopicChannel channel;
  private final AtomicBoolean shutdown = new AtomicBoolean(false);

  public BlockGossipManager(
      final GossipNetwork gossipNetwork,
      final EventBus eventBus,
      final ChainStorageClient chainStorageClient) {
    final BlockTopicHandler topicHandler = new BlockTopicHandler(eventBus, chainStorageClient);
    this.eventBus = eventBus;
    channel = gossipNetwork.subscribe(topicHandler.getTopic(), topicHandler);
    eventBus.register(this);
  }

  @Subscribe
  @SuppressWarnings("unused")
  void onBlockProposed(final BlockProposedEvent blockProposedEvent) {
    final Bytes data = SimpleOffsetSerializer.serialize(blockProposedEvent.getBlock());
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
