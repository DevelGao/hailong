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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.UnsignedLong;
import java.nio.charset.Charset;
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
import tech.devgao.artemis.ganache.GanacheController;
import tech.devgao.artemis.pow.DepositContractListener;
import tech.devgao.artemis.pow.DepositContractListenerFactory;
import tech.devgao.artemis.pow.api.DepositEvent;
import tech.devgao.artemis.pow.contract.DepositContract.Eth2GenesisEventResponse;
import tech.devgao.artemis.pow.event.Deposit;
import tech.devgao.artemis.pow.event.Eth2Genesis;
import tech.devgao.artemis.services.ServiceConfig;
import tech.devgao.artemis.services.ServiceInterface;
import tech.devgao.artemis.util.alogger.ALogger;
import tech.devgao.artemis.util.mikuli.KeyPair;
import tech.devgao.artemis.validator.client.DepositSimulation;
import tech.devgao.artemis.validator.client.Validator;
import tech.devgao.artemis.validator.client.ValidatorClientUtil;

public class PowchainService implements ServiceInterface {

  public static final String SIM_DEPOSIT_VALUE_GWEI = "32000000000";
  private EventBus eventBus;
  private static final ALogger LOG = new ALogger();

  private GanacheController controller;
  private DepositContractListener listener;

  private boolean depositSimulation;

  List<DepositSimulation> simulations;

  public PowchainService() {
    depositSimulation = false;
  }

  @Override
  public void init(ServiceConfig config) {
    this.eventBus = config.getEventBus();
    this.eventBus.register(this);
    this.depositSimulation = config.getCliArgs().isSimulation();
  }

  @Override
  public void run() {
    // JSONProvider provider = new JSONProvider();
    if (depositSimulation) {
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
                ValidatorClientUtil.generateDepositData(
                    validator.getBlsKeys(),
                    validator.getWithdrawal_credentials(),
                    Long.parseLong(SIM_DEPOSIT_VALUE_GWEI))));
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
    } else {
      Eth2GenesisEventResponse response = new Eth2GenesisEventResponse();
      response.log =
          new Log(true, "1", "2", "3", "4", "5", "6", "7", "8", Collections.singletonList("9"));
      response.time = Bytes.ofUnsignedLong(UnsignedLong.ONE.longValue()).toArray();
      response.deposit_count = Bytes.ofUnsignedLong(UnsignedLong.ONE.longValue()).toArray();
      response.deposit_root = "root".getBytes(Charset.defaultCharset());
      Eth2Genesis eth2Genesis = new tech.devgao.artemis.pow.event.Eth2Genesis(response);
      this.eventBus.post(eth2Genesis);
    }
  }

  @Override
  public void stop() {
    this.eventBus.unregister(this);
  }

  @Subscribe
  public void onDepositEvent(DepositEvent event) {
    if (depositSimulation) {
      Deposit deposit = ((tech.devgao.artemis.pow.event.Deposit) event);
      eventBus.post(attributeDepositToSimulation(deposit));
    }
  }

  private DepositSimulation attributeDepositToSimulation(Deposit deposit) {
    for (int i = 0; i < simulations.size(); i++) {
      DepositSimulation simulation = simulations.get(i);
      if (simulation
          .getValidator()
          .getWithdrawal_credentials()
          .equals(deposit.getWithdrawal_credentials())) {
        simulation.getDeposits().add(deposit);
        return simulation;
      }
      ;
    }
    return null;
  }
}
