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

package tech.devgao.hailong.networking.p2p.network;

import tech.devgao.hailong.networking.p2p.gossip.TopicChannel;
import tech.devgao.hailong.networking.p2p.gossip.TopicHandler;
import tech.devgao.hailong.networking.p2p.peer.NodeId;
import tech.devgao.hailong.networking.p2p.peer.Peer;
import tech.devgao.hailong.util.async.SafeFuture;

public abstract class DelegatingP2PNetwork<T extends Peer> implements P2PNetwork<T> {
  private final P2PNetwork<?> network;

  public DelegatingP2PNetwork(final P2PNetwork<?> network) {
    this.network = network;
  }

  @Override
  public SafeFuture<?> connect(final String peer) {
    return network.connect(peer);
  }

  @Override
  public long getPeerCount() {
    return network.getPeerCount();
  }

  @Override
  public String getNodeAddress() {
    return network.getNodeAddress();
  }

  @Override
  public NodeId getNodeId() {
    return network.getNodeId();
  }

  @Override
  public SafeFuture<?> start() {
    return network.start();
  }

  @Override
  public void stop() {
    network.stop();
  }

  @Override
  public TopicChannel subscribe(final String topic, final TopicHandler topicHandler) {
    return network.subscribe(topic, topicHandler);
  }
}
