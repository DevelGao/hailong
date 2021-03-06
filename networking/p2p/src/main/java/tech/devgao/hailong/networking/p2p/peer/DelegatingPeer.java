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

package tech.devgao.hailong.networking.p2p.peer;

import java.util.Objects;
import org.apache.tuweni.bytes.Bytes;
import tech.devgao.hailong.networking.p2p.rpc.RpcMethod;
import tech.devgao.hailong.networking.p2p.rpc.RpcRequestHandler;
import tech.devgao.hailong.networking.p2p.rpc.RpcStream;
import tech.devgao.hailong.util.async.SafeFuture;

public class DelegatingPeer implements Peer {
  private final Peer peer;

  public DelegatingPeer(final Peer peer) {
    this.peer = peer;
  }

  @Override
  public NodeId getId() {
    return peer.getId();
  }

  @Override
  public boolean isConnected() {
    return peer.isConnected();
  }

  @Override
  public SafeFuture<RpcStream> sendRequest(
      final RpcMethod rpcMethod, final Bytes initialPayload, final RpcRequestHandler handler) {
    return peer.sendRequest(rpcMethod, initialPayload, handler);
  }

  @Override
  public boolean connectionInitiatedLocally() {
    return peer.connectionInitiatedLocally();
  }

  @Override
  public boolean connectionInitiatedRemotely() {
    return peer.connectionInitiatedRemotely();
  }

  @Override
  public void disconnect() {
    peer.disconnect();
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof DelegatingPeer)) {
      return false;
    }
    final DelegatingPeer that = (DelegatingPeer) o;
    return Objects.equals(peer, that.peer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(peer);
  }
}
