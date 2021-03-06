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

package tech.devgao.artemis.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import tech.devgao.artemis.util.bytes.Bytes32;
import tech.devgao.artemis.util.bytes.BytesValue;
import tech.devgao.artemis.util.message.BouncyCastleMessageDigestFactory;

/** Various utilities for providing hashes (digests) of arbitrary data. */
public abstract class Hash {
  private Hash() {}

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public static final String KECCAK256_ALG = "KECCAK-256";

  private static final String SHA256_ALG = "SHA-256";
  private static final String RIPEMD160 = "RIPEMD160";

  /**
   * Helper method to generate a digest using the provided algorithm.
   *
   * @param input The input bytes to produce the digest for.
   * @param alg The name of the digest algorithm to use.
   * @return A digest.
   */
  private static byte[] digestUsingAlgorithm(byte[] input, String alg) {
    MessageDigest digest;
    try {
      digest = BouncyCastleMessageDigestFactory.create(alg);
      digest.update(input);
      return digest.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Helper method to generate a digest using the provided algorithm.
   *
   * @param input The input bytes to produce the digest for.
   * @param alg The name of the digest algorithm to use.
   * @return A digest.
   */
  private static byte[] digestUsingAlgorithm(BytesValue input, String alg) {
    MessageDigest digest;
    try {
      digest = BouncyCastleMessageDigestFactory.create(alg);
      input.update(digest);
      return digest.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Digest using SHA2-256.
   *
   * @param input The input bytes to produce the digest for.
   * @return A digest.
   */
  public static Bytes32 sha256(BytesValue input) {
    return Bytes32.wrap(digestUsingAlgorithm(input, SHA256_ALG));
  }

  /**
   * Digest using keccak-256.
   *
   * @param input The input bytes to produce the digest for.
   * @return A digest.
   */
  public static Bytes32 keccak256(BytesValue input) {
    return Bytes32.wrap(digestUsingAlgorithm(input, KECCAK256_ALG));
  }

  /**
   * Digest using RIPEMD-160.
   *
   * @param input The input bytes to produce the digest for.
   * @return A digest.
   */
  public static BytesValue ripemd160(BytesValue input) {
    return BytesValue.wrap(digestUsingAlgorithm(input, RIPEMD160));
  }
}
