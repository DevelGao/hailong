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

package tech.devgao.artemis.services.powchain;

import static java.nio.charset.StandardCharsets.UTF_8;
import static tech.devgao.artemis.datastructures.Constants.DEPOSIT_NORMAL;
import static tech.devgao.artemis.datastructures.Constants.DEPOSIT_SIM;
import static tech.devgao.artemis.datastructures.Constants.DEPOSIT_TEST;

import com.google.common.eventbus.EventBus;
import com.google.common.primitives.UnsignedLong;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.SECP256K1;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;
import tech.devgao.artemis.datastructures.operations.DepositData;
import tech.devgao.artemis.datastructures.util.DepositUtil;
import tech.devgao.artemis.ganache.GanacheController;
import tech.devgao.artemis.pow.DepositContractListener;
import tech.devgao.artemis.pow.DepositContractListenerFactory;
import tech.devgao.artemis.pow.contract.DepositContract.Eth2GenesisEventResponse;
import tech.devgao.artemis.pow.event.Deposit;
import tech.devgao.artemis.pow.event.Eth2Genesis;
import tech.devgao.artemis.service.serviceutils.ServiceConfig;
import tech.devgao.artemis.service.serviceutils.ServiceInterface;
import tech.devgao.artemis.util.alogger.ALogger;
import tech.devgao.artemis.util.bls.BLSSignature;
import tech.devgao.artemis.util.mikuli.KeyPair;
import tech.devgao.artemis.validator.client.DepositSimulation;
import tech.devgao.artemis.validator.client.Validator;
import tech.devgao.artemis.validator.client.ValidatorClientUtil;

public class PowchainService implements ServiceInterface {

  public static final String SIM_DEPOSIT_VALUE_GWEI = "32000000000";
  public static final String EVENTS = "events";
  public static final String ROOT = "root";
  public static final String USER_DIR = "user.dir";
  private EventBus eventBus;
  private static final ALogger LOG = new ALogger();

  private GanacheController controller;
  private DepositContractListener listener;

  private String depositMode;
  private String contractAddr;
  private String provider;

  private String depositSimFile;
  private int validatorCount;
  private int nodeCount;

  List<DepositSimulation> simulations;

  public PowchainService() {}

  @Override
  public void init(ServiceConfig config) {
    this.eventBus = config.getEventBus();
    this.eventBus.register(this);
    this.depositMode = config.getConfig().getDepositMode();
    if (config.getConfig().getInputFile() != null)
      this.depositSimFile = System.getProperty(USER_DIR) + "/" + config.getConfig().getInputFile();
    validatorCount = config.getConfig().getNumValidators();
    nodeCount = config.getConfig().getNumNodes();
    contractAddr = config.getConfig().getContractAddr();
    provider = config.getConfig().getNodeUrl();
  }

  @Override
  public void run() {
    if (depositMode.equals(DEPOSIT_SIM) && depositSimFile == null) {
      controller = new GanacheController(10, 6000);
      listener =
          DepositContractListenerFactory.simulationDeployDepositContract(eventBus, controller);
      Web3j web3j = Web3j.build(new HttpService(controller.getProvider()));
      DefaultGasProvider gasProvider = new DefaultGasProvider();
      simulations = new ArrayList<DepositSimulation>();
      for (SECP256K1.KeyPair keyPair : controller.getAccounts()) {
        Validator validator = new Validator(Bytes32.random(), KeyPair.random(), keyPair);

        simulations.add(
            new DepositSimulation(
                validator,
                new DepositData(
                    validator.getPubkey(),
                    validator.getWithdrawal_credentials(),
                    UnsignedLong.valueOf(SIM_DEPOSIT_VALUE_GWEI),
                    BLSSignature.fromBytes(
                        ValidatorClientUtil.blsSignatureHelper(
                            validator.getBlsKeys(),
                            validator.getWithdrawal_credentials(),
                            Long.parseLong(SIM_DEPOSIT_VALUE_GWEI))))));
        try {
          ValidatorClientUtil.registerValidatorEth1(
              validator,
              Long.parseLong(SIM_DEPOSIT_VALUE_GWEI),
              listener.getContract().getContractAddress(),
              web3j,
              gasProvider);
        } catch (Exception e) {
          LOG.log(
              Level.WARN,
              "Failed to register Validator with SECP256k1 public key: "
                  + keyPair.publicKey()
                  + " : "
                  + e);
        }
      }
    } else if (depositMode.equals(DEPOSIT_SIM) && depositSimFile != null) {
      JsonParser parser = new JsonParser();
      try {
        Reader reader = Files.newBufferedReader(Paths.get(depositSimFile), UTF_8);
        JsonArray validatorsJSON = ((JsonArray) parser.parse(reader));
        validatorsJSON.forEach(
            object -> {
              if (object.getAsJsonObject().get(EVENTS) != null) {
                JsonArray events = object.getAsJsonObject().get(EVENTS).getAsJsonArray();
                events.forEach(
                    event -> {
                      Deposit deposit = DepositUtil.convertJsonDataToEventDeposit(event);
                      eventBus.post(deposit);
                    });
              } else {
                JsonObject event = object.getAsJsonObject();
                Eth2Genesis eth2Genesis = DepositUtil.convertJsonDataToEth2Genesis(event);
                eventBus.post(eth2Genesis);
              }
            });
      } catch (FileNotFoundException e) {
        LOG.log(Level.ERROR, e.getMessage());
      } catch (IOException e) {
        LOG.log(Level.ERROR, e.getMessage());
      }
    } else if (depositMode.equals(DEPOSIT_TEST)) {
      Eth2GenesisEventResponse response = new Eth2GenesisEventResponse();
      response.log =
          new Log(true, "1", "2", "3", "4", "5", "6", "7", "8", Collections.singletonList("9"));
      response.time = Bytes.ofUnsignedLong(UnsignedLong.ONE.longValue()).toArray();
      response.deposit_count = Bytes.ofUnsignedLong(UnsignedLong.ONE.longValue()).toArray();
      response.deposit_root = ROOT.getBytes(Charset.defaultCharset());
      Eth2Genesis eth2Genesis = new Eth2Genesis(response);
      this.eventBus.post(eth2Genesis);
    } else if (depositMode.equals(DEPOSIT_NORMAL)) {
      listener =
          DepositContractListenerFactory.eth1DepositContract(eventBus, provider, contractAddr);
    }
  }

  @Override
  public void stop() {
    this.eventBus.unregister(this);
  }
}
