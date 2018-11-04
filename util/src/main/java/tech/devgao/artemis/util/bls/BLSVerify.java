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

package tech.devgao.artemis.util.bls;

import static java.nio.charset.StandardCharsets.UTF_8;
import static tech.devgao.artemis.util.mikuli.BLS12381.sign;
import static tech.devgao.artemis.util.mikuli.BLS12381.verify;

import com.google.common.primitives.UnsignedLong;
import java.util.List;
import net.develgao.cava.bytes.Bytes32;
import net.develgao.cava.bytes.Bytes48;
import tech.devgao.artemis.util.mikuli.KeyPair;
import tech.devgao.artemis.util.mikuli.PublicKey;
import tech.devgao.artemis.util.mikuli.Signature;

public class BLSVerify {

  public static boolean bls_verify(
      Bytes48 pubkey, Bytes32 message, BLSSignature signature, UnsignedLong domain) {

    // TODO: This is currently faked. Implement it properly

    KeyPair keyPair = KeyPair.random();
    byte[] m = "Hello".getBytes(UTF_8);
    Signature s = sign(keyPair, m, 48).signature();

    // TODO: use the real public key
    PublicKey p = keyPair.publicKey();

    // TODO: return verify() result
    return verify(p, s, m, 48);
  }

  public static boolean bls_verify_multiple(
      List<Bytes48> pubkeys,
      List<Bytes32> messages,
      BLSSignature aggregateSignature,
      UnsignedLong domain) {
    // todo
    return true;
  }

  public static Bytes48 bls_aggregate_pubkeys(List<Bytes48> pubkeys) {
    return Bytes48.ZERO;
  }
}
