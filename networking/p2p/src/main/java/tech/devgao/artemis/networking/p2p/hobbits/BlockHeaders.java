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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.tuweni.bytes.Bytes;
import tech.devgao.artemis.datastructures.blocks.BeaconBlockHeader;

@JsonDeserialize(using = BlockHeaders.BlockHeadersDeserializer.class)
final class BlockHeaders {

  static class BlockHeadersDeserializer extends StdDeserializer<BlockHeaders> {

    protected BlockHeadersDeserializer() {
      super(BlockHeaders.class);
    }

    @Override
    public BlockHeaders deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException {
      JsonNode node = jp.getCodec().readTree(jp);
      Iterator<JsonNode> iterator = node.iterator();
      List<BeaconBlockHeader> elts = new ArrayList<>();
      while (iterator.hasNext()) {
        JsonNode child = iterator.next();
        elts.add(BeaconBlockHeader.fromBytes(Bytes.wrap(child.get("bytes").binaryValue())));
      }
      return new BlockHeaders(elts);
    }
  }

  static class BlockHeader {

    private final Bytes bytes;

    BlockHeader(Bytes bytes) {
      this.bytes = bytes;
    }

    @JsonProperty("bytes")
    public Bytes bytes() {
      return bytes;
    }
  }

  private final List<BeaconBlockHeader> headers;

  BlockHeaders(List<BeaconBlockHeader> headers) {
    this.headers = headers;
  }

  @JsonValue
  public List<BeaconBlockHeader> headers() {
    return headers;
  }
}
