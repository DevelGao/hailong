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

package tech.devgao.artemis.networking.p2p;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.vertx.core.buffer.Buffer;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.develgao.cava.bytes.Bytes;
import net.develgao.cava.bytes.Bytes32;
import net.develgao.cava.concurrent.AsyncCompletion;
import net.develgao.cava.crypto.Hash;
import net.develgao.cava.plumtree.EphemeralPeerRepository;
import net.develgao.cava.plumtree.MessageSender;
import net.develgao.cava.plumtree.State;
import net.develgao.cava.rlpx.RLPxService;
import net.develgao.cava.rlpx.wire.DisconnectReason;
import net.develgao.cava.rlpx.wire.SubProtocolHandler;
import tech.devgao.artemis.data.TimeSeriesRecord;
import tech.devgao.artemis.datastructures.blocks.BeaconBlock;
import tech.devgao.artemis.networking.p2p.hobbits.HobbitsSocketHandler;
import tech.devgao.artemis.networking.p2p.hobbits.Peer;

final class HobbitsSubProtocolHandler implements SubProtocolHandler {

  private final Map<String, HobbitsSocketHandler> handlerMap = new ConcurrentHashMap<>();
  private final RLPxService service;
  private final EventBus eventBus;
  private final String userAgent;
  private final TimeSeriesRecord chainData;
  private final State state;

  HobbitsSubProtocolHandler(
      RLPxService service, EventBus eventBus, String userAgent, TimeSeriesRecord chainData) {
    this.service = service;
    this.eventBus = eventBus;
    this.userAgent = userAgent;
    this.chainData = chainData;
    this.state =
        new State(
            new EphemeralPeerRepository(),
            Hash::sha2_256,
            this::sendMessage,
            this::processGossip,
            (bytes, peer) -> true);
    eventBus.register(this);
  }

  private void processGossip(Bytes bytes) {
    // TODO handle the new message
  }

  private void sendMessage(
      MessageSender.Verb verb, net.develgao.cava.plumtree.Peer peer, Bytes bytes) {
    HobbitsSocketHandler handler = handlerMap.get(((Peer) peer).uri().toString());
    handler.gossipMessage(verb, Bytes32.random(), Bytes32.random(), bytes);
  }

  @Override
  public AsyncCompletion handle(String connectionId, int messageType, Bytes message) {
    HobbitsSocketHandler handler = handlerMap.get("hob+rlpx://" + connectionId);
    handler.handleMessage(Buffer.buffer(message.toArrayUnsafe()));
    return AsyncCompletion.completed();
  }

  @Override
  public AsyncCompletion handleNewPeerConnection(String connectionId) {
    Peer peer = new Peer(URI.create("hob+rlpx://" + connectionId));
    handlerMap.computeIfAbsent(
        peer.uri().toString(),
        (id) -> {
          state.addPeer(peer);
          return new HobbitsSocketHandler(
              eventBus,
              userAgent,
              peer,
              chainData,
              bytes -> service.send(HobbitsSubProtocol.BEACON_ID, 1, connectionId, bytes),
              () -> service.disconnect(id, DisconnectReason.CLIENT_QUITTING),
              state);
        });
    return AsyncCompletion.completed();
  }

  @Override
  public AsyncCompletion stop() {
    return AsyncCompletion.completed();
  }

  @Subscribe
  public void onNewUnprocessedBlock(BeaconBlock block) {
    state.sendGossipMessage(block.toBytes());
  }

  Collection<HobbitsSocketHandler> handlers() {
    return handlerMap.values();
  }
}
