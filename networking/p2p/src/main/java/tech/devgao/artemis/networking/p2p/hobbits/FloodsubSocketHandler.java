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

package tech.devgao.artemis.networking.p2p.hobbits;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.vertx.core.net.NetSocket;
import java.math.BigInteger;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.Level;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.plumtree.State;
import tech.devgao.artemis.datastructures.blocks.BeaconBlock;
import tech.devgao.artemis.datastructures.operations.Attestation;
import tech.devgao.artemis.networking.p2p.hobbits.gossip.GossipCodec;
import tech.devgao.artemis.networking.p2p.hobbits.gossip.GossipMessage;
import tech.devgao.artemis.storage.ChainStorageClient;
import tech.devgao.artemis.util.alogger.ALogger;

/** TCP persistent connection handler for hobbits messages. */
public final class FloodsubSocketHandler extends AbstractSocketHandler {
  private static final ALogger STDOUT = new ALogger("stdout");

  public FloodsubSocketHandler(
      EventBus eventBus,
      NetSocket netSocket,
      String userAgent,
      Peer peer,
      ChainStorageClient store,
      State p2pState,
      ConcurrentHashMap<String, Boolean> receivedMessages) {
    super(eventBus, netSocket, userAgent, peer, store, p2pState, receivedMessages);
  }

  @Override
  public void gossipMessage(
      int method, String topic, long timestamp, Bytes messageHash, Bytes32 hash, Bytes body) {
    // TODO: Hack to make compatible with HOBBITS FloodSub spec
    method = 0;
    hash = Bytes32.wrap(body);
    Bytes bytes =
        GossipCodec.encode(
                method,
                topic,
                BigInteger.valueOf(timestamp),
                messageHash.toArrayUnsafe(),
                hash.toArrayUnsafe(),
                Bytes.EMPTY.toArrayUnsafe())
            .toBytes();
    sendBytes(bytes);
  }

  @Override
  @SuppressWarnings("StringSplitter")
  protected void handleGossipMessage(GossipMessage gossipMessage) {
    if (gossipMessage.method() == 0) {
      Bytes32 hash = Bytes32.wrap(gossipMessage.hash());
      String key = hash.toHexString();
      if (!receivedMessages.containsKey(key)) {
        peer.setPeerGossip(hash);
        if (gossipMessage.getTopic().equalsIgnoreCase("ATTESTATION")) {
          this.sendGetAttestation(hash);
        } else if (gossipMessage.getTopic().equalsIgnoreCase("BLOCK")) {
          this.sendGetBlockBodies(hash);
        }
        receivedMessages.put(key, true);
      }
    }
  }

  @Subscribe
  public void onNewUnprocessedBlock(BeaconBlock block) {
    Bytes32 beaconBlockHash = block.hash_tree_root();
    if (!this.receivedMessages.containsKey(beaconBlockHash.toHexString())) {
      this.receivedMessages.put(beaconBlockHash.toHexString(), true);
      STDOUT.log(
          Level.DEBUG,
          "Gossiping new block with state root: " + block.getState_root().toHexString());
      String attributes = "BLOCK" + "," + String.valueOf(new Date().getTime());
      p2pState.sendGossipMessage(attributes, beaconBlockHash);
    }
  }

  @Subscribe
  public void onNewUnprocessedAttestation(Attestation attestation) {
    Bytes32 bytes = attestation.hash_tree_root();
    if (!this.receivedMessages.containsKey(bytes.toHexString())) {
      this.receivedMessages.put(bytes.toHexString(), true);
      STDOUT.log(Level.DEBUG, "Gossiping new attestation for block root: " + bytes.toHexString());
      String attributes = "ATTESTATION" + "," + String.valueOf(new Date().getTime());
      p2pState.sendGossipMessage(attributes, bytes);
    }
  }
}
