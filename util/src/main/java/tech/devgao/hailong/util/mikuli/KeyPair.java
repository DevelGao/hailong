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

package tech.devgao.hailong.util.mikuli;

import java.security.SecureRandom;
import org.apache.milagro.amcl.BLS381.BIG;
import org.apache.milagro.amcl.BLS381.ECP;
import org.apache.milagro.amcl.BLS381.ROM;
import org.apache.milagro.amcl.RAND;

/** KeyPair represents a public and private key. */
public final class KeyPair {

  private static final BIG curveOrder = new BIG(ROM.CURVE_Order);
  static final G1Point g1Generator = new G1Point(ECP.generator());

  /**
   * Generate a new random key pair
   *
   * @return a new random key pair
   */
  public static KeyPair random() {
    RAND rng = new RAND();
    byte[] b = new byte[128];
    SecureRandom srng = new SecureRandom();
    srng.nextBytes(b);
    rng.seed(128, b);
    Scalar secret = new Scalar(BIG.randomnum(curveOrder, rng));

    SecretKey secretKey = new SecretKey(secret);
    return new KeyPair(secretKey);
  }

  /**
   * Generate a new random key pair given entropy
   *
   * @param entropy to seed the key pair generation
   * @return a new random key pair
   */
  public static KeyPair random(int entropy) {
    RAND rng = new RAND();
    rng.sirand(entropy);
    Scalar secret = new Scalar(BIG.randomnum(curveOrder, rng));

    SecretKey secretKey = new SecretKey(secret);
    return new KeyPair(secretKey);
  }

  private final SecretKey secretKey;
  private final PublicKey publicKey;

  public KeyPair(SecretKey secretKey, PublicKey publicKey) {
    this.secretKey = secretKey;
    this.publicKey = publicKey;
  }

  public KeyPair(SecretKey secretKey) {
    this.secretKey = secretKey;
    this.publicKey = new PublicKey(this.secretKey);
  }

  public KeyPair(Scalar secretKey) {
    this.secretKey = new SecretKey(secretKey);
    this.publicKey = new PublicKey(this.secretKey);
  }

  public PublicKey publicKey() {
    return publicKey;
  }

  public SecretKey secretKey() {
    return secretKey;
  }
}
