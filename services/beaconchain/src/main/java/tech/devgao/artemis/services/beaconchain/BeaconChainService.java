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

package tech.devgao.artemis.services.beaconchain;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.vertx.core.Vertx;
import java.io.IOException;
import org.apache.logging.log4j.Level;
import tech.devgao.artemis.datastructures.Constants;
import tech.devgao.artemis.networking.p2p.HobbitsP2PNetwork;
import tech.devgao.artemis.networking.p2p.MockP2PNetwork;
import tech.devgao.artemis.networking.p2p.MothraP2PNetwork;
import tech.devgao.artemis.networking.p2p.api.P2PNetwork;
import tech.devgao.artemis.service.serviceutils.ServiceConfig;
import tech.devgao.artemis.service.serviceutils.ServiceInterface;
import tech.devgao.artemis.statetransition.StateProcessor;
import tech.devgao.artemis.storage.ChainStorage;
import tech.devgao.artemis.storage.ChainStorageClient;
import tech.devgao.artemis.util.alogger.ALogger;
import tech.devgao.artemis.util.time.Timer;
import tech.devgao.artemis.util.time.TimerFactory;
import tech.devgao.artemis.validator.coordinator.ValidatorCoordinator;

public class BeaconChainService implements ServiceInterface {
  private static final ALogger LOG = new ALogger(BeaconChainService.class.getName());
  private EventBus eventBus;
  private Timer timer;
  private Vertx vertx;
  private StateProcessor stateProcessor;
  private ValidatorCoordinator validatorCoordinator;
  private ChainStorageClient store;
  private P2PNetwork p2pNetwork;

  public BeaconChainService() {}

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void init(ServiceConfig config) {
    this.eventBus = config.getEventBus();
    this.eventBus.register(this);
    this.vertx = config.getVertx();
    try {
      this.timer =
          new TimerFactory()
              .create(
                  config.getConfig().getTimer(),
                  new Object[] {this.eventBus, 0, Constants.SECONDS_PER_SLOT},
                  new Class[] {EventBus.class, Integer.class, Integer.class});
    } catch (IllegalArgumentException e) {
      System.exit(1);
    }
    this.store = ChainStorage.Create(ChainStorageClient.class, eventBus);
    this.stateProcessor = new StateProcessor(config, store);
    this.validatorCoordinator = new ValidatorCoordinator(config, store);
    if ("mock".equals(config.getConfig().getNetworkMode())) {
      this.p2pNetwork = new MockP2PNetwork(eventBus);
    } else if ("hobbits".equals(config.getConfig().getNetworkMode())) {
      P2PNetwork.GossipProtocol gossipProtocol;
      switch (config.getConfig().getGossipProtocol()) {
        case "floodsub":
          gossipProtocol = P2PNetwork.GossipProtocol.FLOODSUB;
          break;
        case "gossipsub":
          gossipProtocol = P2PNetwork.GossipProtocol.GOSSIPSUB;
          break;
        case "plumtree":
          gossipProtocol = P2PNetwork.GossipProtocol.PLUMTREE;
          break;
        case "none":
          gossipProtocol = P2PNetwork.GossipProtocol.NONE;
          break;
        default:
          gossipProtocol = P2PNetwork.GossipProtocol.PLUMTREE;
      }

      this.p2pNetwork =
          new HobbitsP2PNetwork(
              eventBus,
              vertx,
              store,
              config.getConfig().getPort(),
              config.getConfig().getAdvertisedPort(),
              config.getConfig().getNetworkInterface(),
              config.getConfig().getStaticPeers(),
              gossipProtocol);
    } else if ("mothra".equals(config.getConfig().getNetworkMode())) {
      this.p2pNetwork =
          new MothraP2PNetwork(
              eventBus,
              store,
              config.getConfig().getPort(),
              config.getConfig().getNetworkInterface(),
              config.getConfig().getIdentity(),
              config.getConfig().getBootnodes(),
              config.getConfig().isBootnode());
    } else {
      throw new IllegalArgumentException(
          "Unsupported network mode " + config.getConfig().getNetworkMode());
    }
  }

  @Override
  public void run() {
    // Start p2p adapter
    this.p2pNetwork.run();
  }

  @Override
  public void stop() {
    try {
      this.p2pNetwork.close();
    } catch (IOException e) {
      LOG.log(Level.FATAL, e.toString());
    }
    this.timer.stop();
    this.eventBus.unregister(this);
  }

  @Subscribe
  public void afterChainStart(Boolean chainStarted) {
    if (chainStarted) {
      // slot scheduler fires an event that tells us when it is time for a new slot
      this.timer.start();
    }
  }

  P2PNetwork p2pNetwork() {
    return p2pNetwork;
  }
}
