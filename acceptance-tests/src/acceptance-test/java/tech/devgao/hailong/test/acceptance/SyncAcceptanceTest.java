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

package tech.devgao.hailong.test.acceptance;

import java.util.function.Consumer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tech.devgao.hailong.test.acceptance.dsl.AcceptanceTestBase;
import tech.devgao.hailong.test.acceptance.dsl.HailongNode;
import tech.devgao.hailong.test.acceptance.dsl.HailongNode.Config;
import tech.devgao.hailong.test.acceptance.dsl.tools.GenesisStateConfig;

public class SyncAcceptanceTest extends AcceptanceTestBase {

  @Test
  @Disabled("This test currently takes too long to run.")
  public void shouldSyncToNodeWithGreaterFinalizedEpoch() throws Exception {
    final int validatorCount = 2;
    final GenesisStateConfig genesisStateConfig = GenesisStateConfig.create(validatorCount);

    final HailongNode primaryNode =
        createHailongNode(configurePrimaryNode(genesisStateConfig, validatorCount));
    final HailongNode lateJoiningNode =
        createHailongNode(configureLateJoiningNode(genesisStateConfig, primaryNode));

    primaryNode.start();
    primaryNode.waitForGenesis();
    primaryNode.waitForNewFinalization();

    lateJoiningNode.start();
    lateJoiningNode.waitForGenesis();
    lateJoiningNode.waitUntilInSyncWith(primaryNode);
  }

  private Consumer<Config> configurePrimaryNode(
      final GenesisStateConfig genesisConfig, final int validatorCount) {
    return c ->
        c.withGenesisConfig(genesisConfig)
            .withRealNetwork()
            .withInteropValidators(0, validatorCount);
  }

  private Consumer<Config> configureLateJoiningNode(
      final GenesisStateConfig genesisConfig, final HailongNode primaryNode) {
    return c ->
        c.withGenesisConfig(genesisConfig)
            .withRealNetwork()
            .withPeers(primaryNode)
            .withInteropValidators(0, 0);
  }
}
