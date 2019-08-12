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

import org.junit.jupiter.api.Test;
import tech.devgao.hailong.test.acceptance.dsl.AcceptanceTestBase;
import tech.devgao.hailong.test.acceptance.dsl.HailongNode;
import tech.devgao.hailong.test.acceptance.dsl.BesuNode;

public class GenesisStateAcceptanceTest extends AcceptanceTestBase {

  @Test
  public void shouldCreateTheSameGenesisState() throws Exception {
    final BesuNode eth1Node = createBesuNode();
    eth1Node.start();

    final String validatorKeys = createHailongDepositSender().sendValidatorDeposits(eth1Node, 64);

    final HailongNode firstHailong =
        createHailongNode(
            config -> config.withDepositsFrom(eth1Node).withValidatorKeys(validatorKeys));
    firstHailong.start();
    firstHailong.waitForGenesis();

    final HailongNode lateJoinHailong =
        createHailongNode(config -> config.withDepositsFrom(eth1Node));
    lateJoinHailong.start();
    lateJoinHailong.waitForGenesis();

    // Even though the nodes aren't connected to each other they should generate the same genesis
    // state because they processed the same deposits from the same ETH1 chain.
    lateJoinHailong.waitUntilInSyncWith(firstHailong);
  }
}
