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

package tech.devgao.hailong.datastructures.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static tech.devgao.hailong.datastructures.util.DataStructureUtil.randomUnsignedLong;

import com.google.common.primitives.UnsignedLong;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import tech.devgao.hailong.util.bls.BLSPublicKey;
import tech.devgao.hailong.util.bls.BLSSignature;

class DepositDataTest {

  private BLSPublicKey pubkey = BLSPublicKey.random();
  private Bytes32 withdrawalCredentials = Bytes32.random();
  private UnsignedLong amount = randomUnsignedLong();
  private BLSSignature signature = BLSSignature.random();

  private DepositData depositData =
      new DepositData(pubkey, withdrawalCredentials, amount, signature);

  @Test
  void equalsReturnsTrueWhenObjectsAreSame() {
    DepositData testDepositInput = depositData;

    assertEquals(depositData, testDepositInput);
  }

  @Test
  void equalsReturnsTrueWhenObjectFieldsAreEqual() {
    DepositData testDepositInput =
        new DepositData(pubkey, withdrawalCredentials, amount, signature);

    assertEquals(depositData, testDepositInput);
  }

  @Test
  void equalsReturnsFalseWhenPubkeysAreDifferent() {
    BLSPublicKey differentPublicKey = BLSPublicKey.random();
    while (pubkey.equals(differentPublicKey)) {
      differentPublicKey = BLSPublicKey.random();
    }
    DepositData testDepositInput =
        new DepositData(differentPublicKey, withdrawalCredentials, amount, signature);

    assertNotEquals(depositData, testDepositInput);
  }

  @Test
  void equalsReturnsFalseWhenWithdrawalCredentialsAreDifferent() {
    DepositData testDepositInput =
        new DepositData(pubkey, withdrawalCredentials.not(), amount, signature);

    assertNotEquals(depositData, testDepositInput);
  }

  @Test
  void equalsReturnsFalseWhenProofsOfPosessionAreDifferent() {
    BLSSignature differentSignature = BLSSignature.random();
    while (differentSignature.equals(signature)) {
      differentSignature = BLSSignature.random();
    }

    DepositData testDepositInput =
        new DepositData(pubkey, withdrawalCredentials, amount, differentSignature);

    assertNotEquals(depositData, testDepositInput);
  }

  @Test
  void roundtripSSZ() {
    Bytes sszDepositInputBytes = depositData.toBytes();
    assertEquals(depositData, DepositData.fromBytes(sszDepositInputBytes));
  }
}
