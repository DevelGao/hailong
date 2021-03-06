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

package tech.devgao.hailong.networking.p2p.hobbits.rpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.undercouch.bson4jackson.BsonFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.hobbits.Message;
import org.apache.tuweni.hobbits.Protocol;
import tech.devgao.hailong.util.json.BytesModule;

public final class RPCCodec {

  static final ObjectMapper mapper =
      new ObjectMapper(new BsonFactory()).registerModule(new BytesModule());

  private static final AtomicLong counter = new AtomicLong(1);

  private static BigInteger nextRequestNumber() {
    long requestNumber = counter.getAndIncrement();
    if (requestNumber < 1) {
      counter.set(1);
      return BigInteger.ONE;
    }
    return BigInteger.valueOf(requestNumber);
  }

  private RPCCodec() {}

  /**
   * Gets the json object mapper
   *
   * @return
   */
  public static ObjectMapper getMapper() {
    return mapper;
  }

  /**
   * Creates an empty goodbye message.
   *
   * @return the encoded bytes of a goodbye message.
   */
  public static Message createGoodbye() {
    return encode(RPCMethod.GOODBYE.code(), Collections.emptyMap(), Collections.emptySet());
  }

  /**
   * Encodes a message into a RPC request
   *
   * @param methodId the RPC method
   * @param request the payload of the request
   * @param pendingResponses the set of pending responses code to update
   * @return the encoded RPC message
   */
  public static Message encode(int methodId, Object request, Set<BigInteger> pendingResponses) {
    BigInteger requestNumber = nextRequestNumber();
    if (!pendingResponses.isEmpty()) {
      pendingResponses.add(requestNumber);
    }
    return encode(methodId, request, requestNumber);
  }

  /**
   * Encodes a message into a RPC request
   *
   * @param methodId the RPC method
   * @param request the payload of the request
   * @param requestNumber a request number
   * @return the encoded RPC message
   */
  public static Message encode(int methodId, Object request, BigInteger requestNumber) {

    ObjectNode headerNode = mapper.createObjectNode();
    headerNode.put("method_id", methodId);
    headerNode.put("id", requestNumber);
    ObjectNode bodyNode = mapper.createObjectNode();
    bodyNode.putPOJO("body", request);
    try {

      Bytes header = Bytes.wrap(mapper.writer().writeValueAsBytes(headerNode));
      Bytes body = Bytes.wrap(mapper.writer().writeValueAsBytes(bodyNode));
      Message message = new Message(3, Protocol.RPC, header, body);
      return message;
    } catch (IOException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }

  /**
   * Decodes a RPC message into a payload.
   *
   * @param message the bytes of the message to read
   * @return the payload, decoded
   */
  public static RPCMessage decode(Message message) {
    try {
      byte[] header = message.getHeaders().toArrayUnsafe();
      byte[] body = message.getBody().toArrayUnsafe();
      ObjectNode headerNode = (ObjectNode) mapper.readTree(header);
      int methodId = headerNode.get("method_id").intValue();
      BigInteger id = headerNode.get("id").bigIntegerValue();
      ObjectNode bodyNode = (ObjectNode) mapper.readTree(body);
      return new RPCMessage(methodId, id, bodyNode.get("body"), message.size());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
