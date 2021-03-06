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

package tech.devgao.hailong.reference.phase0.operations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tech.devgao.hailong.statetransition.util.BlockProcessorUtil.process_attester_slashings;

import com.google.errorprone.annotations.MustBeClosed;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.tuweni.junit.BouncyCastleExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tech.devgao.hailong.datastructures.operations.AttesterSlashing;
import tech.devgao.hailong.datastructures.state.BeaconState;
import tech.devgao.hailong.ethtests.TestSuite;
import tech.devgao.hailong.statetransition.util.BlockProcessingException;

@ExtendWith(BouncyCastleExtension.class)
public class attester_slashing extends TestSuite {

  @ParameterizedTest(name = "{index}.{3} process attester slashing success")
  @MethodSource({"mainnetAttesterSlashingSuccessSetup", "minimalAttesterSlashingSuccessSetup"})
  void processAttesterSlashingSuccess(
      AttesterSlashing attester_slashing, BeaconState pre, BeaconState post, String testName) {
    List<AttesterSlashing> attester_slashings = new ArrayList<>();
    attester_slashings.add(attester_slashing);
    assertDoesNotThrow(() -> process_attester_slashings(pre, attester_slashings));
    assertEquals(pre, post);
  }

  @ParameterizedTest(name = "{index}.{2} process attester slashing rejection")
  @MethodSource({"mainnetAttesterSlashingSetup", "minimalAttesterSlashingSetup"})
  void processAttesterSlashing(
      AttesterSlashing attester_slashing, BeaconState pre, String testName) {
    List<AttesterSlashing> attester_slashings = new ArrayList<>();
    attester_slashings.add(attester_slashing);
    assertThrows(
        BlockProcessingException.class, () -> process_attester_slashings(pre, attester_slashings));
  }

  @MustBeClosed
  static Stream<Arguments> attester_slashingSuccessSetup(String config) throws Exception {
    Path path = Paths.get(config, "phase0", "operations", "attester_slashing", "pyspec_tests");
    return operationSuccessSetup(
        path, Paths.get(config), "attester_slashing.ssz", AttesterSlashing.class);
  }

  @MustBeClosed
  static Stream<Arguments> attester_slashingSetup(String config) throws Exception {
    Path path = Paths.get(config, "phase0", "operations", "attester_slashing", "pyspec_tests");
    return operationSetup(path, Paths.get(config), "attester_slashing.ssz", AttesterSlashing.class);
  }

  @MustBeClosed
  static Stream<Arguments> minimalAttesterSlashingSetup() throws Exception {
    return attester_slashingSetup("minimal");
  }

  @MustBeClosed
  static Stream<Arguments> mainnetAttesterSlashingSetup() throws Exception {
    return attester_slashingSetup("mainnet");
  }

  @MustBeClosed
  static Stream<Arguments> minimalAttesterSlashingSuccessSetup() throws Exception {
    return attester_slashingSuccessSetup("minimal");
  }

  @MustBeClosed
  static Stream<Arguments> mainnetAttesterSlashingSuccessSetup() throws Exception {
    return attester_slashingSuccessSetup("mainnet");
  }
}
