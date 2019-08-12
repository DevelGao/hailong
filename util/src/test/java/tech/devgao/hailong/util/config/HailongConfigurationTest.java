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

package tech.devgao.hailong.util.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class HailongConfigurationTest {

  @Test
  void validMinimum() {
    HailongConfiguration.fromString("");
  }

  @Test
  void wrongPort() {
    assertThrows(
        IllegalArgumentException.class,
        () -> HailongConfiguration.fromString("node.identity=\"2345\"\nnode.port=100000"));
  }

  @Test
  void invalidAdvertisedPort() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            HailongConfiguration.fromString("node.identity=\"2345\"\nnode.advertisedPort=100000"));
  }

  @Test
  void advertisedPortDefaultsToPort() {
    final HailongConfiguration config = HailongConfiguration.fromString("node.port=1234");
    assertThat(config.getAdvertisedPort()).isEqualTo(1234);
  }

  @Test
  void invalidNetworkMode() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            HailongConfiguration.fromString(
                "node.identity=\"2345\"\nnode.networkMode=\"tcpblah\""));
  }

  @Test
  void invalidMinimalHailongConfig() {
    Constants.setConstants("minimal");
    HailongConfiguration config = HailongConfiguration.fromString("deposit.numValidators=7");
    assertThrows(IllegalArgumentException.class, () -> config.validateConfig());
  }

  @Test
  void invalidMainnetHailongConfig() {
    Constants.setConstants("mainnet");
    HailongConfiguration config = HailongConfiguration.fromString("deposit.numValidators=31");
    assertThrows(IllegalArgumentException.class, () -> config.validateConfig());
  }
}
