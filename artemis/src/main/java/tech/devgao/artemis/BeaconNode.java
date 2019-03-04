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

package tech.devgao.artemis;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.vertx.core.Vertx;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;
import tech.devgao.artemis.data.RawRecord;
import tech.devgao.artemis.data.TimeSeriesRecord;
import tech.devgao.artemis.data.adapter.TimeSeriesAdapter;
import tech.devgao.artemis.data.provider.CSVProvider;
import tech.devgao.artemis.data.provider.FileProvider;
import tech.devgao.artemis.data.provider.JSONProvider;
import tech.devgao.artemis.data.provider.ProviderTypes;
import tech.devgao.artemis.networking.p2p.HobbitsP2PNetwork;
import tech.devgao.artemis.networking.p2p.MockP2PNetwork;
import tech.devgao.artemis.networking.p2p.RLPxP2PNetwork;
import tech.devgao.artemis.networking.p2p.api.P2PNetwork;
import tech.devgao.artemis.services.ServiceConfig;
import tech.devgao.artemis.services.ServiceController;
import tech.devgao.artemis.services.beaconchain.BeaconChainService;
import tech.devgao.artemis.services.chainstorage.ChainStorageService;
import tech.devgao.artemis.services.powchain.PowchainService;
import tech.devgao.artemis.util.alogger.ALogger;
import tech.devgao.artemis.util.cli.CommandLineArguments;
import tech.devgao.artemis.util.config.ArtemisConfiguration;
import tech.devgao.artemis.validator.coordinator.ValidatorCoordinator;

public class BeaconNode {
  private static final ALogger LOG = new ALogger(BeaconNode.class.getName());
  private final Vertx vertx = Vertx.vertx();
  private final ExecutorService threadPool =
      Executors.newCachedThreadPool(
          new ThreadFactory() {
            @Override
            public Thread newThread(@NotNull Runnable r) {
              Thread t = new Thread(r);
              t.setDaemon(true);
              return t;
            }
          });

  private final ServiceController serviceController = new ServiceController();
  private final ServiceConfig serviceConfig;
  private P2PNetwork p2pNetwork;
  private ValidatorCoordinator validatorCoordinator;
  private EventBus eventBus;
  private String outputFilename;
  private FileProvider<?> fileProvider;

  private CommandLineArguments cliArgs;
  private CommandLine commandLine;

  public BeaconNode(CommandLine commandLine, CommandLineArguments cliArgs) {
    ArtemisConfiguration config = ArtemisConfiguration.fromFile(cliArgs.getConfigFile());
    this.eventBus = new AsyncEventBus(threadPool);
    if ("mock".equals(config.getNetworkMode())) {
      this.p2pNetwork = new MockP2PNetwork(eventBus);
    } else if ("rlpx".equals(config.getNetworkMode())) {
      this.p2pNetwork =
          new RLPxP2PNetwork(
              eventBus,
              vertx,
              config.getKeyPair(),
              config.getPort(),
              config.getAdvertisedPort(),
              config.getNetworkInterface());
    } else if ("hobbits".equals(config.getNetworkMode())) {
      this.p2pNetwork =
          new HobbitsP2PNetwork(
              eventBus,
              vertx,
              config.getPort(),
              config.getAdvertisedPort(),
              config.getNetworkInterface());
    } else {
      throw new IllegalArgumentException("Unsupported network mode " + config.getNetworkMode());
    }
    this.serviceConfig = new ServiceConfig(eventBus, config);
    this.validatorCoordinator = new ValidatorCoordinator(serviceConfig);
    this.cliArgs = cliArgs;
    this.commandLine = commandLine;
    if (cliArgs.isOutputEnabled()) {
      this.eventBus.register(this);
      this.outputFilename = FileProvider.uniqueFilename(cliArgs.getOutputFile());
      if (ProviderTypes.compare(CSVProvider.class, cliArgs.getProviderType())) {
        this.fileProvider = new CSVProvider();
      } else {
        this.fileProvider = new JSONProvider();
      }
    }
  }

  public void start() {
    if (commandLine.isUsageHelpRequested()) {
      commandLine.usage(System.out);
      return;
    }
    // set log level per CLI flags
    System.out.println("Setting logging level to " + cliArgs.getLoggingLevel().name());
    Configurator.setAllLevels("", cliArgs.getLoggingLevel());

    // Check output file

    // Initialize services
    serviceController.initAll(
        eventBus,
        cliArgs,
        serviceConfig,
        BeaconChainService.class,
        PowchainService.class,
        ChainStorageService.class);
    // Start services
    serviceController.startAll(cliArgs);
    // Start p2p adapter
    this.p2pNetwork.run();
  }

  public void stop() {
    try {
      serviceController.stopAll(cliArgs);
      this.p2pNetwork.close();
    } catch (IOException e) {
      LOG.log(Level.WARN, e.toString());
    }
  }

  @Subscribe
  public void onDataEvent(RawRecord record) {
    TimeSeriesAdapter adapter = new TimeSeriesAdapter(record);
    TimeSeriesRecord tsRecord = adapter.transform();
    fileProvider.setRecord(tsRecord);
    FileProvider.output(outputFilename, fileProvider);
  }
}