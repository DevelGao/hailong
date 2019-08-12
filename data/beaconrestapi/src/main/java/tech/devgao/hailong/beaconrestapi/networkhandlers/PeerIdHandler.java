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

package tech.devgao.hailong.beaconrestapi.networkhandlers;

import tech.devgao.hailong.beaconrestapi.handlerinterfaces.BeaconRestApiHandler;
import tech.devgao.hailong.networking.p2p.network.P2PNetwork;

public class PeerIdHandler implements BeaconRestApiHandler {

  private final P2PNetwork<?> network;

  public PeerIdHandler(P2PNetwork<?> network) {
    this.network = network;
  }

  @Override
  public String getPath() {
    return "/network/peer_id";
  }

  @Override
  public Object handleRequest(RequestParams params) {
    return network.getNodeAddress();
  }
}