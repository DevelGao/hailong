/*
 * Copyright 2020 Developer Gao.
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

import com.google.common.eventbus.EventBus;
import java.util.List;
import java.util.function.Consumer;
import tech.devgao.hailong.networking.eth2.Eth2NetworkFactory.Eth2P2PNetworkBuilder;
import tech.devgao.hailong.statetransition.BeaconChainUtil;
import tech.devgao.hailong.storage.ChainStorageClient;
import tech.devgao.hailong.util.bls.BLSKeyPair;

public class NodeManager {
  private final EventBus eventBus;
  private final ChainStorageClient storageClient;
  private final BeaconChainUtil chainUtil;
  private final Eth2Network eth2Network;

  private NodeManager(
      final EventBus eventBus,
      final ChainStorageClient storageClient,
      final BeaconChainUtil chainUtil,
      final Eth2Network eth2Network) {
    this.eventBus = eventBus;
    this.storageClient = storageClient;
    this.chainUtil = chainUtil;
    this.eth2Network = eth2Network;
  }

  public static NodeManager create(
      Eth2NetworkFactory networkFactory, final List<BLSKeyPair> validatorKeys) throws Exception {
    return create(networkFactory, validatorKeys, c -> {});
  }

  public static NodeManager create(
      Eth2NetworkFactory networkFactory,
      final List<BLSKeyPair> validatorKeys,
      Consumer<Eth2P2PNetworkBuilder> configureNetwork)
      throws Exception {
    final EventBus eventBus = new EventBus();
    final ChainStorageClient storageClient = ChainStorageClient.memoryOnlyClient(eventBus);
    final Eth2P2PNetworkBuilder networkBuilder =
        networkFactory.builder().eventBus(eventBus).chainStorageClient(storageClient);

    configureNetwork.accept(networkBuilder);

    final Eth2Network eth2Network = networkBuilder.startNetwork();

    final BeaconChainUtil chainUtil = BeaconChainUtil.create(storageClient, validatorKeys);
    chainUtil.initializeStorage();

    return new NodeManager(eventBus, storageClient, chainUtil, eth2Network);
  }

  public EventBus eventBus() {
    return eventBus;
  }

  public BeaconChainUtil chainUtil() {
    return chainUtil;
  }

  public Eth2Network network() {
    return eth2Network;
  }

  public ChainStorageClient storageClient() {
    return storageClient;
  }
}
