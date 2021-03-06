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

package tech.devgao.hailong.datastructures.util;

import static tech.devgao.hailong.datastructures.util.BeaconStateUtil.compute_domain;
import static tech.devgao.hailong.util.config.Constants.BLS_WITHDRAWAL_PREFIX;
import static tech.devgao.hailong.util.config.Constants.DOMAIN_DEPOSIT;

import com.google.common.primitives.UnsignedLong;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import tech.devgao.hailong.datastructures.operations.DepositData;
import tech.devgao.hailong.datastructures.operations.DepositMessage;
import tech.devgao.hailong.util.bls.BLSKeyPair;
import tech.devgao.hailong.util.bls.BLSPublicKey;
import tech.devgao.hailong.util.bls.BLSSignature;
import tech.devgao.hailong.util.message.BouncyCastleMessageDigestFactory;

public class DepositGenerator {

  private final boolean signDeposit;

  public DepositGenerator() {
    this(true);
  }

  public DepositGenerator(boolean signDeposit) {
    this.signDeposit = signDeposit;
  }

  public DepositData createDepositData(
      final BLSKeyPair validatorKeyPair,
      final UnsignedLong amountInGwei,
      final BLSPublicKey withdrawalPublicKey) {
    final Bytes32 withdrawalCredentials = createWithdrawalCredentials(withdrawalPublicKey);
    final DepositMessage depositMessage =
        new DepositMessage(validatorKeyPair.getPublicKey(), withdrawalCredentials, amountInGwei);

    final BLSSignature signature =
        signDeposit
            ? BLSSignature.sign(
                validatorKeyPair, depositMessage.hash_tree_root(), compute_domain(DOMAIN_DEPOSIT))
            : BLSSignature.empty();
    return new DepositData(depositMessage, signature);
  }

  private Bytes32 createWithdrawalCredentials(final BLSPublicKey withdrawalPublicKey) {
    final Bytes publicKeyHash = sha256(withdrawalPublicKey.toBytesCompressed());
    final Bytes credentials = Bytes.wrap(BLS_WITHDRAWAL_PREFIX, publicKeyHash.slice(1));
    return Bytes32.wrap(credentials);
  }

  private Bytes sha256(final Bytes indexBytes) {
    final MessageDigest sha256Digest = getSha256Digest();
    indexBytes.update(sha256Digest);
    return Bytes.wrap(sha256Digest.digest());
  }

  private MessageDigest getSha256Digest() {
    try {
      return BouncyCastleMessageDigestFactory.create("sha256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
