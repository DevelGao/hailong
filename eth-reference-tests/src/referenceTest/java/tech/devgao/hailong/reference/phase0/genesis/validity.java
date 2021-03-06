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

package tech.devgao.hailong.reference.phase0.genesis;

import com.google.errorprone.annotations.MustBeClosed;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.apache.tuweni.junit.BouncyCastleExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tech.devgao.hailong.datastructures.state.BeaconState;
import tech.devgao.hailong.ethtests.TestSuite;

@ExtendWith(BouncyCastleExtension.class)
@Disabled
public class validity extends TestSuite {

  @ParameterizedTest(name = "{index} root of Merkleizable")
  @MethodSource({"genesisGenericValiditySetup"})
  void genesisValidity(BeaconState genesis, Boolean is_valid) {
    // TODO: Proto says this is probably changing
  }

  @MustBeClosed
  static Stream<Arguments> genesisGenericValiditySetup() throws Exception {
    Path configPath = Paths.get("minimal", "phase0");
    Path path = Paths.get("/minimal/phase0/genesis/validity/pyspec_tests");
    return genesisValiditySetup(path, configPath);
  }
}
