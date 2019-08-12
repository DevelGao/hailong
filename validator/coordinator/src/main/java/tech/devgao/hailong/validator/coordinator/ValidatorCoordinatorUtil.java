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

package tech.devgao.hailong.validator.coordinator;

import static tech.devgao.hailong.util.config.Constants.GENESIS_SLOT;
import static tech.devgao.hailong.util.config.Constants.SLOTS_PER_EPOCH;

import com.google.common.primitives.UnsignedLong;
import com.google.protobuf.ByteString;
import java.util.Map;
import org.apache.tuweni.bytes.Bytes;
import tech.devgao.hailong.proto.messagesigner.MessageSignerGrpc;
import tech.devgao.hailong.proto.messagesigner.SignatureRequest;
import tech.devgao.hailong.proto.messagesigner.SignatureResponse;
import tech.devgao.hailong.util.bls.BLSPublicKey;
import tech.devgao.hailong.util.bls.BLSSignature;

public class ValidatorCoordinatorUtil {

  static BLSSignature getSignature(
      Map<BLSPublicKey, ValidatorInfo> validators,
      Bytes message,
      Bytes domain,
      BLSPublicKey signer) {
    SignatureRequest request =
        SignatureRequest.newBuilder()
            .setMessage(ByteString.copyFrom(message.toArray()))
            .setDomain(ByteString.copyFrom(domain.toArray()))
            .build();

    SignatureResponse response;
    response =
        MessageSignerGrpc.newBlockingStub(validators.get(signer).getChannel()).signMessage(request);
    return BLSSignature.fromBytes(Bytes.wrap(response.getMessage().toByteArray()));
  }

  static boolean isEpochStart(UnsignedLong slot) {
    return slot.mod(UnsignedLong.valueOf(SLOTS_PER_EPOCH)).equals(UnsignedLong.ZERO);
  }

  static boolean isGenesis(UnsignedLong slot) {
    return slot.equals(UnsignedLong.valueOf(GENESIS_SLOT));
  }
}
