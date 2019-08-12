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

import com.google.common.primitives.UnsignedLong;
import java.io.File;
import org.junit.jupiter.api.Test;
import tech.devgao.hailong.test.acceptance.dsl.AcceptanceTestBase;
import tech.devgao.hailong.test.acceptance.dsl.HailongNode;
import tech.devgao.hailong.test.acceptance.dsl.BesuNode;

public class StartupAcceptanceTest extends AcceptanceTestBase {

  @Test
  public void shouldProgressChainAfterStartingFromMockGenesis() throws Exception {
    final HailongNode node = createHailongNode();
    node.start();
    node.waitForGenesis();
    node.waitForNewBlock();
  }

  @Test
  public void shouldProgressChainAfterStartingFromDisk() throws Exception {
    final HailongNode node1 = createHailongNode();
    node1.start();
    final UnsignedLong genesisTime = node1.getGenesisTime();
    File tempDatabaseFile = node1.getDatabaseFileFromContainer();
    node1.stop();

    final HailongNode node2 = createHailongNode(HailongNode.Config::startFromDisk);
    node2.copyDatabaseFileToContainer(tempDatabaseFile);
    node2.start();
    node2.waitForGenesisTime(genesisTime);
    node2.waitForNewBlock();
  }

  @Test
  public void shouldStartChainFromDepositContract() throws Exception {
    final BesuNode eth1Node = createBesuNode();
    eth1Node.start();

    final HailongNode hailongNode = createHailongNode(config -> config.withDepositsFrom(eth1Node));
    hailongNode.start();

    createHailongDepositSender().sendValidatorDeposits(eth1Node, 64);
    hailongNode.waitForGenesis();
  }
}
