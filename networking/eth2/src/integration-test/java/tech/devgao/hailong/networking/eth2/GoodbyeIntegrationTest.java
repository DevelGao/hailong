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

package tech.devgao.hailong.networking.eth2;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.devgao.hailong.util.Waiter.waitFor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.devgao.hailong.datastructures.networking.libp2p.rpc.GoodbyeMessage;
import tech.devgao.hailong.networking.eth2.peers.Eth2Peer;
import tech.devgao.hailong.util.Waiter;

public class GoodbyeIntegrationTest {
  private final Eth2NetworkFactory networkFactory = new Eth2NetworkFactory();
  private Eth2Peer peer1;
  private Eth2Peer peer2;
  private Eth2Network network1;
  private Eth2Network network2;

  @BeforeEach
  public void setUp() throws Exception {
    network1 = networkFactory.builder().startNetwork();
    network2 = networkFactory.builder().peer(network1).startNetwork();
    peer1 = network2.getPeer(network1.getNodeId()).orElseThrow();
    peer2 = network1.getPeer(network2.getNodeId()).orElseThrow();
  }

  @AfterEach
  public void tearDown() {
    networkFactory.stopAll();
  }

  @Test
  public void shouldCloseConnectionAfterGoodbyeReceived() throws Exception {
    waitFor(peer1.sendGoodbye(GoodbyeMessage.REASON_CLIENT_SHUT_DOWN));
    Waiter.waitFor(() -> assertThat(peer1.isConnected()).isFalse());
    Waiter.waitFor(() -> assertThat(peer2.isConnected()).isFalse());
    assertThat(network1.getPeerCount()).isZero();
    assertThat(network2.getPeerCount()).isZero();
  }
}
