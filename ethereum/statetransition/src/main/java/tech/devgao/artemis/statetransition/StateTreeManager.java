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

package tech.devgao.artemis.statetransition;

import static tech.devgao.artemis.datastructures.util.DataStructureUtil.randomDeposits;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.UnsignedLong;
import java.util.ArrayList;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Optional;
import net.develgao.cava.bytes.Bytes32;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.devgao.artemis.datastructures.Constants;
import tech.devgao.artemis.datastructures.blocks.BeaconBlock;
import tech.devgao.artemis.datastructures.blocks.Eth1Data;
import tech.devgao.artemis.datastructures.operations.Deposit;
import tech.devgao.artemis.pow.api.ChainStartEvent;
import tech.devgao.artemis.pow.api.ValidatorRegistrationEvent;
import tech.devgao.artemis.statetransition.util.BeaconStateUtil;
import tech.devgao.artemis.storage.ChainStorage;
import tech.devgao.artemis.storage.ChainStorageClient;

/** Class to manage the state tree and initiate state transitions */
public class StateTreeManager {

  private final EventBus eventBus;
  private StateTransition stateTransition;
  private BeaconState state;
  private ChainStorageClient storage;
  private static final Logger LOG = LogManager.getLogger();

  public StateTreeManager(EventBus eventBus) {
    this.eventBus = eventBus;
    this.stateTransition = new StateTransition();
    this.eventBus.register(this);
    this.storage = ChainStorage.Create(ChainStorageClient.class, eventBus);
  }

  @Subscribe
  public void onChainStarted(ChainStartEvent event) {
    LOG.info("ChainStart Event Detected");
    // TODO: Startup Logic: Initial State Data
    ArrayList<Deposit> deposits = randomDeposits(100);
    this.state =
        BeaconStateUtil.get_initial_beacon_state(
            deposits,
            UnsignedLong.valueOf(Constants.GENESIS_SLOT),
            new Eth1Data(Bytes32.ZERO, Bytes32.ZERO));
  }

  @Subscribe
  public void onValidatorRegistered(ValidatorRegistrationEvent event) {
    LOG.info("Validator Registration Event detected");
    // LOG.info("   Validator Number: " + validatorRegisteredEvent.getInfo());
  }

  @Subscribe
  public void onNewSlot(Date date) {
    LOG.info("****** New Slot at: " + date + " ******");
    // TODO: get canonical state
    this.state = BeaconState.deepCopy(state);
    Optional<BeaconBlock> block = this.storage.getUnprocessedBlock();
    if (block.isPresent()) {
      LOG.info("Unprocessed block retrieved.");
      try {
        stateTransition.initiate(this.state, block.get());
        this.storage.addProcessedBlock(block.get().getState_root(), block.get());
      } catch (NoSuchElementException e) {
        LOG.warn(e.toString());
      }
    } else {
      stateTransition.initiate(this.state, null);
    }
  }
}
