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

import net.develgao.cava.bytes.Bytes48;
import tech.devgao.artemis.util.mikuli.KeyPair;
import tech.devgao.artemis.util.mikuli.PublicKey;
import tech.devgao.artemis.util.mikuli.SecretKey;

public class BLSKeyPair {

  private BLSPublicKey blsPublicKey;
  private BLSSecretKey blsSecretKey;

  public static BLSKeyPair random() {
    return new BLSKeyPair(KeyPair.random());
  }

  BLSKeyPair(KeyPair keyPair) {
    this.blsPublicKey = new BLSPublicKey(keyPair.publicKey());
    this.blsSecretKey = new BLSSecretKey(keyPair.secretKey());
  }

  public BLSPublicKey getBlsPublicKey() {
    return blsPublicKey;
  }

  public BLSSecretKey getBlsSecretKey() {
    return blsSecretKey;
  }

  public PublicKey publicKey() {
    return blsPublicKey.getPublicKey();
  }

  public SecretKey secretKey() {
    return blsSecretKey.getSecretKey();
  }

  // TODO: find out why this causes the deepCopyBeaconState test to fail
  public Bytes48 publicKeyAsBytesBroken() {
    return Bytes48.wrap(publicKey().toBytes());
  }

  public Bytes48 publicKeyAsBytes() {
    return Bytes48.wrap(publicKey().toBytes()).copy();
  }
}