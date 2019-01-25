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

package tech.devgao.artemis.datastructures.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static tech.devgao.artemis.datastructures.util.DataStructureUtil.randomUnsignedLong;

import com.google.common.primitives.UnsignedLong;
import net.develgao.cava.bytes.Bytes;
import net.develgao.cava.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import tech.devgao.artemis.util.bls.BLSPublicKey;

class ValidatorTest {

  private BLSPublicKey pubkey = BLSPublicKey.random();
  private Bytes32 withdrawalCredentials = Bytes32.random();
  private UnsignedLong activationEpoch = randomUnsignedLong();
  private UnsignedLong exitEpoch = randomUnsignedLong();
  private UnsignedLong withdrawalEpoch = randomUnsignedLong();
  private UnsignedLong penalizedEpoch = randomUnsignedLong();
  private UnsignedLong statusFlags = randomUnsignedLong();

  private Validator validator =
      new Validator(
          pubkey,
          withdrawalCredentials,
          activationEpoch,
          exitEpoch,
          withdrawalEpoch,
          penalizedEpoch,
          statusFlags);

  @Test
  void equalsReturnsTrueWhenObjectAreSame() {
    Validator testValidator = validator;

    assertEquals(validator, testValidator);
  }

  @Test
  void equalsReturnsTrueWhenObjectFieldsAreEqual() {
    Validator testValidator =
        new Validator(
            pubkey,
            withdrawalCredentials,
            activationEpoch,
            exitEpoch,
            withdrawalEpoch,
            penalizedEpoch,
            statusFlags);

    assertEquals(validator, testValidator);
  }

  @Test
  void equalsReturnsFalseWhenPubkeysAreDifferent() {
    BLSPublicKey differentPublicKey = BLSPublicKey.random();
    while (pubkey.equals(differentPublicKey)) {
      differentPublicKey = BLSPublicKey.random();
    }
    Validator testValidator =
        new Validator(
            differentPublicKey,
            withdrawalCredentials,
            activationEpoch,
            exitEpoch,
            withdrawalEpoch,
            penalizedEpoch,
            statusFlags);

    assertNotEquals(validator, testValidator);
  }

  @Test
  void equalsReturnsFalseWhenWithdrawalCredentialsAreDifferent() {
    Validator testValidator =
        new Validator(
            pubkey,
            withdrawalCredentials.not(),
            activationEpoch,
            exitEpoch,
            withdrawalEpoch,
            penalizedEpoch,
            statusFlags);

    assertNotEquals(validator, testValidator);
  }

  @Test
  void equalsReturnsFalseWhenActivationEpochsAreDifferent() {
    Validator testValidator =
        new Validator(
            pubkey,
            withdrawalCredentials,
            activationEpoch.plus(randomUnsignedLong()),
            exitEpoch,
            withdrawalEpoch,
            penalizedEpoch,
            statusFlags);

    assertNotEquals(validator, testValidator);
  }

  @Test
  void equalsReturnsFalseWhenExitEpochsAreDifferent() {
    Validator testValidator =
        new Validator(
            pubkey,
            withdrawalCredentials,
            activationEpoch,
            exitEpoch.plus(randomUnsignedLong()),
            withdrawalEpoch,
            penalizedEpoch,
            statusFlags);

    assertNotEquals(validator, testValidator);
  }

  @Test
  void equalsReturnsFalseWhenWithdrawalEpochsAreDifferent() {
    Validator testValidator =
        new Validator(
            pubkey,
            withdrawalCredentials,
            activationEpoch,
            exitEpoch,
            withdrawalEpoch.plus(randomUnsignedLong()),
            penalizedEpoch,
            statusFlags);

    assertNotEquals(validator, testValidator);
  }

  @Test
  void equalsReturnsFalseWhenPenalizedEpochsAreDifferent() {
    Validator testValidator =
        new Validator(
            pubkey,
            withdrawalCredentials,
            activationEpoch,
            exitEpoch,
            withdrawalEpoch,
            penalizedEpoch.plus(randomUnsignedLong()),
            statusFlags);

    assertNotEquals(validator, testValidator);
  }

  @Test
  void equalsReturnsFalseWhenStatusFlagsaAreDifferent() {
    Validator testValidator =
        new Validator(
            pubkey,
            withdrawalCredentials,
            activationEpoch,
            exitEpoch,
            withdrawalEpoch,
            penalizedEpoch,
            statusFlags.plus(randomUnsignedLong()));

    assertNotEquals(validator, testValidator);
  }

  @Test
  void roundtripSSZ() {
    Bytes sszValidatorBytes = validator.toBytes();
    assertEquals(validator, Validator.fromBytes(sszValidatorBytes));
  }
}
