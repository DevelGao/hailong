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

package tech.devgao.hailong.networking.eth2.rpc.beaconchain.methods;

import com.google.common.primitives.UnsignedLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.Counter;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;
import tech.devgao.hailong.datastructures.networking.libp2p.rpc.GoodbyeMessage;
import tech.devgao.hailong.metrics.HailongMetricCategory;
import tech.devgao.hailong.networking.eth2.peers.Eth2Peer;
import tech.devgao.hailong.networking.eth2.rpc.core.LocalMessageHandler;
import tech.devgao.hailong.networking.eth2.rpc.core.ResponseCallback;

public class GoodbyeMessageHandler implements LocalMessageHandler<GoodbyeMessage, GoodbyeMessage> {

  private static final Logger LOG = LogManager.getLogger();
  private final LabelledMetric<Counter> goodbyeCounter;

  public GoodbyeMessageHandler(final MetricsSystem metricsSystem) {
    goodbyeCounter =
        metricsSystem.createLabelledCounter(
            HailongMetricCategory.NETWORK,
            "peer_goodbye_total",
            "Total number of goodbye messages received from peers",
            "reason");
  }

  @Override
  public void onIncomingMessage(
      final Eth2Peer peer,
      final GoodbyeMessage message,
      final ResponseCallback<GoodbyeMessage> callback) {
    LOG.trace("Peer {} said goodbye.", peer.getId());
    goodbyeCounter.labels(labelForReason(message.getReason())).inc();
    peer.disconnect();
    callback.completeSuccessfully();
  }

  private String labelForReason(final UnsignedLong reason) {
    if (GoodbyeMessage.REASON_CLIENT_SHUT_DOWN.equals(reason)) {
      return "SHUTDOWN";
    } else if (GoodbyeMessage.REASON_IRRELEVANT_NETWORK.equals(reason)) {
      return "IRRELEVANT_NETWORK";
    } else if (GoodbyeMessage.REASON_FAULT_ERROR.equals(reason)) {
      return "FAULT";
    }
    return "UNKNOWN";
  }
}
