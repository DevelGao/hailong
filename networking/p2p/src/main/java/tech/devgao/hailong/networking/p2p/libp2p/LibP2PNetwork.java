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

package tech.devgao.hailong.networking.p2p.libp2p;

import static tech.devgao.hailong.util.alogger.ALogger.STDOUT;
import static tech.devgao.hailong.util.async.SafeFuture.reportExceptions;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import identify.pb.IdentifyOuterClass;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.crypto.KEY_TYPE;
import io.libp2p.core.crypto.KeyKt;
import io.libp2p.core.crypto.PrivKey;
import io.libp2p.core.dsl.Builder.Defaults;
import io.libp2p.core.dsl.BuilderJKt;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.core.multistream.ProtocolBinding;
import io.libp2p.core.pubsub.PubsubPublisherApi;
import io.libp2p.etc.types.ByteArrayExtKt;
import io.libp2p.mux.mplex.MplexStreamMuxer;
import io.libp2p.protocol.Identify;
import io.libp2p.protocol.Ping;
import io.libp2p.pubsub.gossip.Gossip;
import io.libp2p.security.secio.SecIoSecureChannel;
import io.libp2p.transport.tcp.TcpTransport;
import io.netty.handler.logging.LogLevel;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import tech.devgao.hailong.networking.p2p.gossip.GossipNetwork;
import tech.devgao.hailong.networking.p2p.gossip.TopicChannel;
import tech.devgao.hailong.networking.p2p.gossip.TopicHandler;
import tech.devgao.hailong.networking.p2p.libp2p.gossip.LibP2PGossipNetwork;
import tech.devgao.hailong.networking.p2p.libp2p.rpc.RpcHandler;
import tech.devgao.hailong.networking.p2p.network.NetworkConfig;
import tech.devgao.hailong.networking.p2p.network.P2PNetwork;
import tech.devgao.hailong.networking.p2p.network.PeerHandler;
import tech.devgao.hailong.networking.p2p.peer.NodeId;
import tech.devgao.hailong.networking.p2p.peer.Peer;
import tech.devgao.hailong.networking.p2p.peer.PeerConnectedSubscriber;
import tech.devgao.hailong.networking.p2p.rpc.RpcMethod;
import tech.devgao.hailong.util.async.SafeFuture;
import tech.devgao.hailong.util.cli.VersionProvider;

public class LibP2PNetwork implements P2PNetwork<Peer> {
  private final PrivKey privKey;
  private final NetworkConfig config;
  private final NodeId nodeId;

  private final Host host;
  private final ScheduledExecutorService scheduler;
  private final PeerManager peerManager;
  private final Multiaddr advertisedAddr;
  private final Gossip gossip;
  private final GossipNetwork gossipNetwork;

  private final AtomicReference<State> state = new AtomicReference<>(State.IDLE);
  private final Map<RpcMethod, RpcHandler> rpcHandlers = new ConcurrentHashMap<>();

  public LibP2PNetwork(
      final NetworkConfig config,
      final MetricsSystem metricsSystem,
      final List<RpcMethod> rpcMethods,
      final List<PeerHandler> peerHandlers) {
    this.privKey =
        config
            .getPrivateKey()
            .orElseGet(() -> KeyKt.generateKeyPair(KEY_TYPE.SECP256K1).component1());
    this.nodeId = new LibP2PNodeId(PeerId.fromPubKey(privKey.publicKey()));
    this.config = config;

    advertisedAddr = new Multiaddr("/ip4/127.0.0.1/tcp/" + config.getAdvertisedPort());
    scheduler =
        Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setDaemon(true).setNameFormat("libp2p-%d").build());

    // Setup gossip
    gossip = new Gossip();
    final PubsubPublisherApi publisher = gossip.createPublisher(privKey, new Random().nextLong());
    gossipNetwork = new LibP2PGossipNetwork(gossip, publisher);

    // Setup rpc methods
    rpcMethods.forEach(method -> rpcHandlers.put(method, new RpcHandler(method)));

    // Setup peers
    peerManager = new PeerManager(scheduler, metricsSystem, peerHandlers, rpcHandlers);

    host =
        BuilderJKt.hostJ(
            Defaults.None,
            b -> {
              b.getIdentity().setFactory(() -> privKey);
              b.getTransports().add(TcpTransport::new);
              b.getSecureChannels().add(SecIoSecureChannel::new);
              b.getMuxers().add(MplexStreamMuxer::new);
              b.getNetwork()
                  .listen(
                      "/ip4/" + config.getNetworkInterface() + "/tcp/" + config.getListenPort());

              b.getProtocols().addAll(getDefaultProtocols());
              b.getProtocols().addAll(rpcHandlers.values());

              if (config.isLogWireCipher()) {
                b.getDebug().getBeforeSecureHandler().setLogger(LogLevel.DEBUG, "wire.ciphered");
              }
              if (config.isLogWirePlain()) {
                b.getDebug().getAfterSecureHandler().setLogger(LogLevel.DEBUG, "wire.plain");
              }
              if (config.isLogMuxFrames()) {
                b.getDebug().getMuxFramesHandler().setLogger(LogLevel.DEBUG, "wire.mux");
              }

              b.getConnectionHandlers().add(peerManager);
            });
  }

  private List<ProtocolBinding<?>> getDefaultProtocols() {
    final Ping ping = new Ping();
    IdentifyOuterClass.Identify identifyMsg =
        IdentifyOuterClass.Identify.newBuilder()
            .setProtocolVersion("ipfs/0.1.0")
            .setAgentVersion(VersionProvider.CLIENT_IDENTITY + "/" + VersionProvider.VERSION)
            .setPublicKey(ByteArrayExtKt.toProtobuf(privKey.publicKey().bytes()))
            .addListenAddrs(ByteArrayExtKt.toProtobuf(advertisedAddr.getBytes()))
            .setObservedAddr(
                ByteArrayExtKt.toProtobuf( // TODO: Report external IP?
                    advertisedAddr.getBytes()))
            .addProtocols(ping.getAnnounce())
            .addProtocols(gossip.getAnnounce())
            .build();
    return List.of(ping, new Identify(identifyMsg), gossip);
  }

  @Override
  public SafeFuture<?> start() {
    if (!state.compareAndSet(State.IDLE, State.RUNNING)) {
      return SafeFuture.failedFuture(new IllegalStateException("Network already started"));
    }
    STDOUT.log(Level.INFO, "Starting libp2p network...");
    return SafeFuture.of(host.start())
        .thenApply(
            i -> {
              STDOUT.log(Level.INFO, "Listening for connections on: " + getNodeAddress());
              return null;
            })
        .thenRun(() -> config.getPeers().forEach(reportExceptions(this::connect)));
  }

  @Override
  public String getNodeAddress() {
    return advertisedAddr + "/p2p/" + nodeId.toBase58();
  }

  @Override
  public SafeFuture<?> connect(final String peer) {
    return peerManager.connect(new Multiaddr(peer), host.getNetwork());
  }

  @Override
  public long subscribeConnect(final PeerConnectedSubscriber<Peer> subscriber) {
    return peerManager.subscribeConnect(subscriber);
  }

  @Override
  public void unsubscribeConnect(final long subscriptionId) {
    peerManager.unsubscribeConnect(subscriptionId);
  }

  @Override
  public Optional<Peer> getPeer(final NodeId id) {
    return peerManager.getPeer(id);
  }

  @Override
  public Stream<Peer> streamPeers() {
    return peerManager.streamPeers();
  }

  @Override
  public long getPeerCount() {
    return peerManager.getPeerCount();
  }

  @Override
  public void stop() {
    if (!state.compareAndSet(State.RUNNING, State.STOPPED)) {
      return;
    }
    STDOUT.log(Level.DEBUG, "JvmLibP2PNetwork.stop()");
    reportExceptions(host.stop());
    scheduler.shutdownNow();
  }

  @Override
  public NodeId getNodeId() {
    return nodeId;
  }

  @Override
  public TopicChannel subscribe(final String topic, final TopicHandler topicHandler) {
    return gossipNetwork.subscribe(topic, topicHandler);
  }
}
