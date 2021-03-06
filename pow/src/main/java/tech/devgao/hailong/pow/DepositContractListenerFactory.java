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

package tech.devgao.hailong.pow;

import com.google.common.eventbus.EventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.ClientTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import tech.devgao.hailong.ganache.GanacheController;
import tech.devgao.hailong.pow.api.DepositEventChannel;
import tech.devgao.hailong.pow.contract.DepositContract;
import tech.devgao.hailong.util.time.TimeProvider;

public class DepositContractListenerFactory {
  private static final Logger LOG = LogManager.getLogger();

  public static DepositContractListener simulationDeployDepositContract(
      EventBus eventBus,
      DepositEventChannel depositEventChannel,
      GanacheController controller,
      TimeProvider timeProvider) {
    Web3j web3j = Web3j.build(new HttpService(controller.getProvider()));
    Credentials credentials =
        Credentials.create(controller.getAccounts().get(0).secretKey().bytes().toHexString());
    DepositContract contract = null;
    try {
      contract = DepositContract.deploy(web3j, credentials, new DefaultGasProvider()).send();
    } catch (Exception e) {
      LOG.fatal(
          "DepositContractListenerFactory.simulationDeployDepositContract: DepositContract failed to deploy in the simulation environment",
          e);
    }
    return new DepositContractListener(
        web3j, contract, createDepositHandler(timeProvider, eventBus, depositEventChannel));
  }

  public static DepositContractListener eth1DepositContract(
      Web3j web3j,
      EventBus eventBus,
      DepositEventChannel depositEventChannel,
      String address,
      TimeProvider timeProvider) {
    DepositContract contract =
        DepositContract.load(
            address, web3j, new ClientTransactionManager(web3j, address), new DefaultGasProvider());
    return new DepositContractListener(
        web3j, contract, createDepositHandler(timeProvider, eventBus, depositEventChannel));
  }

  private static PublishOnInactivityDepositHandler createDepositHandler(
      TimeProvider timeProvider, EventBus eventBus, DepositEventChannel depositEventChannel) {
    PublishOnInactivityDepositHandler handler =
        new PublishOnInactivityDepositHandler(
            timeProvider,
            new BatchByBlockDepositHandler(depositEventChannel::notifyDepositsFromBlock));
    eventBus.register(handler);
    return handler;
  }
}
