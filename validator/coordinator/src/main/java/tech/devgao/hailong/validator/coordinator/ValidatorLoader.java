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

package tech.devgao.hailong.validator.coordinator;

import static tech.devgao.hailong.util.alogger.ALogger.STDOUT;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import tech.devgao.hailong.util.bls.BLSKeyPair;
import tech.devgao.hailong.util.bls.BLSPublicKey;
import tech.devgao.hailong.util.config.HailongConfiguration;
import tech.devgao.hailong.util.config.Constants;
import tech.devgao.hailong.validator.client.ValidatorClient;

class ValidatorLoader {

  static Map<BLSPublicKey, ValidatorInfo> initializeValidators(HailongConfiguration config) {
    int naughtinessPercentage = config.getNaughtinessPercentage();
    int numValidators = config.getNumValidators();

    long numNaughtyValidators = Math.round((naughtinessPercentage * numValidators) / 100.0);
    ValidatorKeyProvider keyProvider;
    if (config.getValidatorsKeyFile() != null) {
      keyProvider = new YamlValidatorKeyProvider();
    } else {
      keyProvider = new MockStartValidatorKeyProvider();
    }
    final List<BLSKeyPair> keypairs = keyProvider.loadValidatorKeys(config);
    final Map<BLSPublicKey, ValidatorInfo> validators = new HashMap<>();

    // Get validator connection info and create a new ValidatorInfo object and put it into the
    // Validators map
    for (int i = 0; i < keypairs.size(); i++) {
      BLSKeyPair keypair = keypairs.get(i);
      int port =
          Constants.VALIDATOR_CLIENT_PORT_BASE + i + keyProvider.getValidatorPortStartIndex();
      new ValidatorClient(keypair, port);
      ManagedChannel channel =
          ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
      STDOUT.log(Level.DEBUG, "Validator " + i + ": " + keypair.getPublicKey().toString());

      validators.put(keypair.getPublicKey(), new ValidatorInfo(numNaughtyValidators > 0, channel));
      numNaughtyValidators--;
    }
    return validators;
  }
}
