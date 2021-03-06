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

package tech.devgao.hailong.statetransition.util;

import static tech.devgao.hailong.util.alogger.ALogger.STDOUT;
import static tech.devgao.hailong.util.config.Constants.SLOTS_PER_EPOCH;
import static tech.devgao.hailong.util.config.Constants.SLOTS_PER_ETH1_VOTING_PERIOD;

import com.google.common.primitives.UnsignedLong;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.crypto.Hash;
import org.apache.tuweni.ssz.SSZ;
import tech.devgao.hailong.datastructures.blocks.Eth1Data;
import tech.devgao.hailong.datastructures.operations.DepositData;
import tech.devgao.hailong.datastructures.state.BeaconState;
import tech.devgao.hailong.datastructures.state.BeaconStateWithCache;
import tech.devgao.hailong.datastructures.util.DepositGenerator;
import tech.devgao.hailong.datastructures.util.MockStartBeaconStateGenerator;
import tech.devgao.hailong.datastructures.util.MockStartDepositGenerator;
import tech.devgao.hailong.datastructures.util.MockStartValidatorKeyPairFactory;
import tech.devgao.hailong.datastructures.util.SimpleOffsetSerializer;
import tech.devgao.hailong.storage.ChainStorageClient;
import tech.devgao.hailong.util.alogger.ALogger;
import tech.devgao.hailong.util.alogger.ALogger.Color;
import tech.devgao.hailong.util.bls.BLSKeyPair;

public final class StartupUtil {

  public static Eth1Data get_eth1_data_stub(BeaconState state, UnsignedLong current_epoch) {
    UnsignedLong epochs_per_period =
        UnsignedLong.valueOf(SLOTS_PER_ETH1_VOTING_PERIOD)
            .dividedBy(UnsignedLong.valueOf(SLOTS_PER_EPOCH));
    UnsignedLong voting_period = current_epoch.dividedBy(epochs_per_period);
    return new Eth1Data(
        Hash.sha2_256(SSZ.encodeUInt64(epochs_per_period.longValue())),
        state.getEth1_deposit_index(),
        Hash.sha2_256(Hash.sha2_256(SSZ.encodeUInt64(voting_period.longValue()))));
  }

  public static BeaconStateWithCache createMockedStartInitialBeaconState(
      final long genesisTime, List<BLSKeyPair> validatorKeys) {
    return createMockedStartInitialBeaconState(genesisTime, validatorKeys, true);
  }

  public static BeaconStateWithCache createMockedStartInitialBeaconState(
      final long genesisTime, List<BLSKeyPair> validatorKeys, boolean signDeposits) {
    final List<DepositData> initialDepositData =
        new MockStartDepositGenerator(new DepositGenerator(signDeposits))
            .createDeposits(validatorKeys);
    return new MockStartBeaconStateGenerator()
        .createInitialBeaconState(UnsignedLong.valueOf(genesisTime), initialDepositData);
  }

  public static BeaconStateWithCache loadBeaconStateFromFile(final String stateFile)
      throws IOException {
    return BeaconStateWithCache.fromBeaconState(
        SimpleOffsetSerializer.deserialize(
            Bytes.wrap(Files.readAllBytes(new File(stateFile).toPath())), BeaconState.class));
  }

  public static void setupInitialState(
      final ChainStorageClient chainStorageClient,
      final long genesisTime,
      final String startState,
      final int numValidators) {
    final List<BLSKeyPair> validatorKeys =
        new MockStartValidatorKeyPairFactory().generateKeyPairs(0, numValidators);
    setupInitialState(chainStorageClient, genesisTime, startState, validatorKeys, true);
  }

  public static void setupInitialState(
      final ChainStorageClient chainStorageClient,
      final long genesisTime,
      final String startState,
      final List<BLSKeyPair> validatorKeyPairs,
      final boolean signDeposits) {
    BeaconStateWithCache initialState;
    if (startState != null) {
      try {
        STDOUT.log(Level.INFO, "Loading initial state from " + startState, ALogger.Color.GREEN);
        initialState = StartupUtil.loadBeaconStateFromFile(startState);
      } catch (final IOException e) {
        throw new IllegalStateException("Failed to load initial state", e);
      }
    } else {
      STDOUT.log(
          Level.INFO,
          "Starting with mocked start interoperability mode with genesis time "
              + genesisTime
              + " and "
              + validatorKeyPairs.size()
              + " validators",
          Color.GREEN);
      initialState =
          StartupUtil.createMockedStartInitialBeaconState(
              genesisTime, validatorKeyPairs, signDeposits);
    }

    chainStorageClient.initializeFromGenesis(initialState);
  }
}
