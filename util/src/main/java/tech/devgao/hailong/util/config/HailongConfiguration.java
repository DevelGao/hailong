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

package tech.devgao.hailong.util.config;

import static java.util.Arrays.asList;

import com.google.common.base.Strings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.tuweni.config.Configuration;
import org.apache.tuweni.config.PropertyValidator;
import org.apache.tuweni.config.Schema;
import org.apache.tuweni.config.SchemaBuilder;

/** Configuration of an instance of Hailong. */
public class HailongConfiguration {

  private static final int NO_VALUE = -1;

  @SuppressWarnings({"DoubleBraceInitialization"})
  static final Schema createSchema() {
    SchemaBuilder builder =
        SchemaBuilder.create()
            .addString(
                "node.networkMode",
                "mock",
                "represents what network to use",
                PropertyValidator.anyOf("mock", "jvmlibp2p"));

    builder.addString("node.networkInterface", "0.0.0.0", "Peer to peer network interface", null);
    builder.addInteger("node.port", 9000, "Peer to peer port", PropertyValidator.inRange(0, 65535));
    builder.addInteger(
        "node.advertisedPort",
        NO_VALUE,
        "Peer to peer advertised port",
        PropertyValidator.inRange(0, 65535));
    builder.addString("node.discovery", "", "static or discv5", null);
    builder.addString("node.bootnodes", "", "ENR of the bootnode", null);
    builder.addInteger(
        "node.naughtinessPercentage",
        0,
        "Percentage of Validator Clients that are naughty",
        PropertyValidator.inRange(0, 101));
    builder.addString(
        "validator.validatorsKeyFile", "", "The file to load validator keys from", null);
    builder.addInteger(
        "deposit.numValidators",
        64,
        "represents the total number of validators in the network",
        PropertyValidator.inRange(1, 65535));
    builder.addInteger(
        "deposit.numNodes",
        1,
        "represents the total number of nodes on the network",
        PropertyValidator.inRange(1, 65535));
    builder.addString("deposit.mode", "normal", "PoW Deposit Mode", null);
    builder.addString("deposit.inputFile", "", "PoW simulation optional input file", null);
    builder.addString("deposit.nodeUrl", null, "URL for Eth 1.0 node", null);
    builder.addString(
        "deposit.contractAddr", null, "Contract address for the deposit contract", null);
    builder.addListOfString("node.peers", Collections.emptyList(), "Static peers", null);
    builder.addLong(
        "node.networkID", 1L, "The identifier of the network (mainnet, testnet, sidechain)", null);
    builder.addString(
        "node.constants",
        "minimal",
        "Determines whether to use minimal or mainnet constants",
        null);

    // Interop
    builder.addLong("interop.genesisTime", null, "Time of mocked genesis", null);
    builder.addInteger(
        "interop.ownedValidatorStartIndex", 0, "Index of first validator owned by this node", null);
    builder.addInteger(
        "interop.ownedValidatorCount", 0, "Number of validators owned by this node", null);
    builder.addString("interop.startState", "", "Initial BeaconState to load", null);
    builder.addString("interop.privateKey", "", "This node's private key", null);

    // Metrics
    builder.addBoolean("metrics.enabled", false, "Enables metrics collection via Prometheus", null);
    builder.addString(
        "metrics.metricsNetworkInterface",
        "0.0.0.0",
        "Metrics network interface to expose metrics for Prometheus",
        null);
    builder.addInteger(
        "metrics.metricsPort",
        8008,
        "Metrics port to expose metrics for Prometheus",
        PropertyValidator.inRange(0, 65535));
    builder.addListOfString(
        "metrics.metricsCategories",
        asList("JVM", "PROCESS", "BEACONCHAIN", "NETWORK"),
        "Metric categories to enable",
        null);
    // Outputs
    builder.addString(
        "output.logPath", ".", "Path to output the log file", PropertyValidator.isPresent());
    builder.addString(
        "output.logFile", "hailong.log", "Log file name", PropertyValidator.isPresent());
    builder.addString(
        "output.transitionRecordDir",
        "",
        "Directory to record transition pre and post states",
        null);

    // Database
    builder.addBoolean("database.startFromDisk", false, "Start from the disk if set to true", null);

    // Beacon Rest API
    builder.addInteger("beaconrestapi.portNumber", 5051, "Port number of Beacon Rest API", null);

    builder.validateConfiguration(
        config -> {
          return null;
        });

    return builder.toSchema();
  }

  private static final Schema schema = createSchema();

  /**
   * Reads configuration from file.
   *
   * @param path a toml file to read configuration from
   * @return the new HailongConfiguration
   * @throws UncheckedIOException if the file is missing
   */
  public static HailongConfiguration fromFile(String path) {
    Path configPath = Paths.get(path);
    try {
      return new HailongConfiguration(Configuration.fromToml(configPath, schema));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Reads configuration from a toml text.
   *
   * @param configText the toml text
   * @return the new HailongConfiguration
   */
  public static HailongConfiguration fromString(String configText) {
    return new HailongConfiguration(Configuration.fromToml(configText, schema));
  }

  private final Configuration config;

  private HailongConfiguration(Configuration config) {
    this.config = config;
    if (config.hasErrors()) {
      throw new IllegalArgumentException(
          config.errors().stream()
              .map(error -> error.position() + " " + error.toString())
              .collect(Collectors.joining("\n")));
    }
  }

  public int getNaughtinessPercentage() {
    return config.getInteger("node.naughtinessPercentage");
  }

  public String getTimer() {
    return config.getString("node.timer");
  }

  /** @return the port this node will listen to */
  public int getPort() {
    return config.getInteger("node.port");
  }

  public String getDiscovery() {
    return config.getString("node.discovery");
  }

  public String getBootnodes() {
    return config.getString("node.bootnodes");
  }

  /** @return the port this node will advertise as its own */
  public int getAdvertisedPort() {
    final int advertisedPort = config.getInteger("node.advertisedPort");
    return advertisedPort == NO_VALUE ? getPort() : advertisedPort;
  }

  /** @return the network interface this node will bind to */
  public String getNetworkInterface() {
    return config.getString("node.networkInterface");
  }

  public String getConstants() {
    return config.getString("node.constants");
  }

  /** @return the total number of validators in the network */
  public int getNumValidators() {
    return config.getInteger("deposit.numValidators");
  }

  public String getStartState() {
    final String startState = config.getString("interop.startState");
    return startState == null || startState.isEmpty() ? null : startState;
  }

  public long getGenesisTime() {
    long genesisTime = config.getLong("interop.genesisTime");
    if (genesisTime == 0) {
      return (System.currentTimeMillis() / 1000) + 5;
    } else {
      return genesisTime;
    }
  }

  public int getInteropOwnedValidatorStartIndex() {
    return config.getInteger("interop.ownedValidatorStartIndex");
  }

  public int getInteropOwnedValidatorCount() {
    return config.getInteger("interop.ownedValidatorCount");
  }

  public String getInteropPrivateKey() {
    return config.getString("interop.privateKey");
  }

  /** @return the total number of nodes on the network */
  public int getNumNodes() {
    return config.getInteger("deposit.numNodes");
  }

  public String getValidatorsKeyFile() {
    final String keyFile = config.getString("validator.validatorsKeyFile");
    return keyFile == null || keyFile.isEmpty() ? null : keyFile;
  }

  /** @return the Deposit simulation flag, w/ optional input file */
  public String getInputFile() {
    String inputFile = config.getString("deposit.inputFile");
    if (inputFile == null || inputFile.equals("")) return null;
    return inputFile;
  }

  public String getContractAddr() {
    return config.getString("deposit.contractAddr");
  }

  public String getNodeUrl() {
    return config.getString("deposit.nodeUrl");
  }

  /** @return if simulation is enabled or not */
  public String getDepositMode() {
    return config.getString("deposit.mode");
  }

  /** @return the Output provider types: CSV, JSON */
  public String getProviderType() {
    return config.getString("output.providerType");
  }

  /** @return if metrics is enabled or not */
  public Boolean isMetricsEnabled() {
    return config.getBoolean("metrics.enabled");
  }

  public String getMetricsNetworkInterface() {
    return config.getString("metrics.metricsNetworkInterface");
  }

  public int getMetricsPort() {
    return config.getInteger("metrics.metricsPort");
  }

  public List<String> getMetricCategories() {
    return config.getListOfString("metrics.metricsCategories");
  }

  public String getTransitionRecordDir() {
    return Strings.emptyToNull(config.getString("output.transitionRecordDir"));
  }

  /** @return Hailong specific constants */
  public List<String> getStaticPeers() {
    return config.getListOfString("node.peers");
  }

  /** @return the identifier of the network (mainnet, testnet, sidechain) */
  public long getNetworkID() {
    return config.getLong("node.networkID");
  }

  /** @return the mode of the network to use - mock or libp2p */
  public String getNetworkMode() {
    return config.getString("node.networkMode");
  }

  /** @return the path to the log file */
  public String getLogPath() {
    return config.getString("output.logPath");
  }

  /** @return the name of the log file */
  public String getLogFile() {
    return config.getString("output.logFile");
  }

  public boolean startFromDisk() {
    return config.getBoolean("database.startFromDisk");
  }

  public void validateConfig() throws IllegalArgumentException {
    if (getNumValidators() < Constants.SLOTS_PER_EPOCH) {
      throw new IllegalArgumentException("Invalid config.toml");
    }
  }

  public int getBeaconRestAPIPortNumber() {
    return config.getInteger("beaconrestapi.portNumber");
  }
}
