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

import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.apache.tuweni.bytes.Bytes;

/** This class represents a Signature on G2 */
public final class Signature {

  public static final int COMPRESSED_SIG_SIZE = 96;
  public static final int UNCOMPRESSED_SIG_SIZE = 192;

  /**
   * Aggregates list of Signature pairs, returns the signature that corresponds to G2 point at
   * infinity if list is empty
   *
   * @param signatures The list of signatures to aggregate
   * @return Signature
   */
  public static Signature aggregate(List<Signature> signatures) {
    if (signatures.isEmpty()) {
      return new Signature(new G2Point());
    }
    return signatures.stream().reduce(Signature::combine).get();
  }

  /**
   * Decode a signature from its serialized representation.
   *
   * <p>Note that this uses uncompressed form, and requires 192 bytes of input.
   *
   * @param bytes the bytes of the signature
   * @return the signature
   */
  public static Signature fromBytes(Bytes bytes) {
    return new Signature(bytes);
  }

  /**
   * Decode a signature from its <em>compressed</em> form serialized representation.
   *
   * @param bytes the bytes of the signature
   * @return the signature
   */
  public static Signature fromBytesCompressed(Bytes bytes) {
    checkArgument(
        bytes.size() == COMPRESSED_SIG_SIZE,
        "Expected " + COMPRESSED_SIG_SIZE + " bytes of input but got %s",
        bytes.size());
    return new Signature(bytes);
  }

  /**
   * Create a random signature for testing
   *
   * @return a random, valid signature
   */
  public static Signature random() {
    KeyPair keyPair = KeyPair.random();
    byte[] message = "Hello, world!".getBytes(UTF_8);
    SignatureAndPublicKey sigAndPubKey = BLS12381.sign(keyPair, message, Bytes.ofUnsignedLong(48L));
    return sigAndPubKey.signature();
  }

  /**
   * Create a random signature for testing
   *
   * @param entropy to seed the key pair generation
   * @return a random, valid signature
   */
  public static Signature random(int entropy) {
    KeyPair keyPair = KeyPair.random(entropy);
    byte[] message = "Hello, world!".getBytes(UTF_8);
    SignatureAndPublicKey sigAndPubKey = BLS12381.sign(keyPair, message, Bytes.ofUnsignedLong(48L));
    return sigAndPubKey.signature();
  }

  // Sometimes we are dealing with random, invalid signature points, e.g. when testing.
  // Let's only interpret the raw data into a point when necessary to do so.
  private final Bytes rawData;
  private final Supplier<G2Point> point;

  /**
   * Construct signature from a given G2 point
   *
   * @param point the G2 point corresponding to the signature
   */
  Signature(G2Point point) {
    this.rawData = point.toBytes();
    this.point = () -> point;
  }

  Signature(Bytes rawData) {
    this.rawData = rawData;
    this.point = Suppliers.memoize(() -> parseSignatureBytes(this.rawData));
  }

  /**
   * Construct a copy of a signature
   *
   * @param signature the signature to be copied
   */
  Signature(Signature signature) {
    this.rawData = signature.rawData;
    this.point = signature.point;
  }

  private G2Point parseSignatureBytes(Bytes signatureBytes) {
    if (signatureBytes.size() == COMPRESSED_SIG_SIZE) {
      return G2Point.fromBytesCompressed(signatureBytes);
    } else if (signatureBytes.size() == UNCOMPRESSED_SIG_SIZE) {
      return G2Point.fromBytes(signatureBytes);
    }
    throw new RuntimeException(
        "Expected either "
            + COMPRESSED_SIG_SIZE
            + " or "
            + UNCOMPRESSED_SIG_SIZE
            + " bytes for signature, but found "
            + signatureBytes.size());
  }

  /**
   * Combines this signature with another signature, creating a new signature.
   *
   * @param signature the signature to combine with
   * @return a new signature as combination of both signatures
   */
  public Signature combine(Signature signature) {
    return new Signature(point.get().add(signature.point.get()));
  }

  /**
   * Signature serialization
   *
   * @return byte array representation of the signature, not null
   */
  public Bytes toBytes() {
    return (rawData.size() == UNCOMPRESSED_SIG_SIZE) ? rawData : point.get().toBytes();
  }

  /**
   * Signature serialization to compressed form
   *
   * @return byte array representation of the signature, not null
   */
  public Bytes toBytesCompressed() {
    return (rawData.size() == COMPRESSED_SIG_SIZE) ? rawData : point.get().toBytesCompressed();
  }

  @Override
  public String toString() {
    return toBytesCompressed().toHexString();
  }

  @Override
  public int hashCode() {
    return point.hashCode();
  }

  @VisibleForTesting
  public G2Point g2Point() {
    return point.get();
  }

  @Override
  public boolean equals(Object obj) {
    if (Objects.isNull(obj)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Signature)) {
      return false;
    }
    Signature other = (Signature) obj;
    if (rawData.size() == other.rawData.size() && rawData.equals(other.rawData)) {
      return true;
    }
    return point.get().equals(other.point.get());
  }
}
