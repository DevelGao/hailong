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

package tech.devgao.hailong.networking.eth2.gossip.topics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.google.common.base.Suppliers;
import com.google.common.eventbus.EventBus;
import java.util.function.Supplier;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.ssz.SSZException;
import org.junit.jupiter.api.Test;
import tech.devgao.hailong.datastructures.operations.Attestation;
import tech.devgao.hailong.datastructures.util.DataStructureUtil;

public class Eth2TopicHandlerTest {
  private static final String TOPIC = "testing";

  private final EventBus eventBus = mock(EventBus.class);
  private final MockTopicHandler topicHandler = spy(new MockTopicHandler(eventBus));
  private final Bytes message = Bytes.fromHexString("0x01");

  private final Attestation deserialized = DataStructureUtil.randomAttestation(1);
  private Supplier<Attestation> deserializer = Suppliers.ofInstance(deserialized);
  private Supplier<Boolean> validator = Suppliers.ofInstance(true);

  @Test
  public void handleMessage_valid() {
    final boolean result = topicHandler.handleMessage(message);

    assertThat(result).isEqualTo(true);
    verify(eventBus).post(deserialized);
  }

  @Test
  public void handleMessage_invalid() {
    validator = Suppliers.ofInstance(false);
    final boolean result = topicHandler.handleMessage(message);

    assertThat(result).isEqualTo(false);
    verify(eventBus, never()).post(deserialized);
  }

  @Test
  public void handleMessage_whenDeserializationFails() {
    deserializer =
        () -> {
          throw new SSZException("whoops");
        };
    doThrow(new SSZException("whoops")).when(topicHandler).deserialize(message);
    final boolean result = topicHandler.handleMessage(message);

    assertThat(result).isEqualTo(false);
    verify(eventBus, never()).post(deserialized);
  }

  @Test
  public void handleMessage_whenDeserializationThrowsUnexpectedException() {
    deserializer =
        () -> {
          throw new RuntimeException("whoops");
        };
    final boolean result = topicHandler.handleMessage(message);

    assertThat(result).isEqualTo(false);
    verify(eventBus, never()).post(deserialized);
  }

  @Test
  public void handleMessage_whenDeserializeReturnsNull() {
    deserializer = () -> null;
    final boolean result = topicHandler.handleMessage(message);

    assertThat(result).isEqualTo(false);
    verify(eventBus, never()).post(deserialized);
  }

  @Test
  public void handleMessage_whenValidationThrowsAnException() {
    validator =
        () -> {
          throw new RuntimeException("whoops");
        };
    final boolean result = topicHandler.handleMessage(message);

    assertThat(result).isEqualTo(false);
    verify(eventBus, never()).post(deserialized);
  }

  private class MockTopicHandler extends Eth2TopicHandler<Attestation> {

    protected MockTopicHandler(final EventBus eventBus) {
      super(eventBus);
    }

    @Override
    public String getTopic() {
      return TOPIC;
    }

    @Override
    protected Attestation deserialize(final Bytes bytes) throws SSZException {
      return deserializer.get();
    }

    @Override
    protected boolean validateData(final Attestation attestation) {
      return validator.get();
    }
  }
}
