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

package tech.devgao.artemis.statetransition.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.devgao.artemis.datastructures.Constants.ACTIVE_PENDING_EXIT;
import static tech.devgao.artemis.datastructures.Constants.EXITED_WITHOUT_PENALTY;

import com.google.common.primitives.UnsignedLong;
import java.util.Arrays;
import net.develgao.cava.bytes.Bytes;
import net.develgao.cava.bytes.Bytes32;
import net.develgao.cava.bytes.Bytes48;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.devgao.artemis.datastructures.state.ValidatorRecord;
import tech.devgao.artemis.datastructures.state.Validators;

class ValidatorsUtilTest {
  private static final double DEFAULT_BALANCE = 0.0d;
  private int validatorSizeExpected = 0;
  private double effectiveBalanceExpected = 0.0d;
  private double balance;
  private Validators validatorRecordTest;

  @BeforeEach
  void setup() {
    validatorSizeExpected = 0;
    effectiveBalanceExpected = 0.0d;
    balance = 0.0d;
    validatorRecordTest = null;
  }

  @AfterEach
  void teardown() {
    validatorSizeExpected = 0;
    effectiveBalanceExpected = 0.0d;
    validatorRecordTest = null;
  }

  @Test
  void assert_get_zero_as_effective_balance_for_nullValidators() {
    // when
    double effectiveBalanceActual = ValidatorsUtil.get_effective_balance(null);

    // then
    assertEquals(effectiveBalanceExpected, effectiveBalanceActual);
  }

  @Test
  void
      assert_get_zero_as_effective_balance_for_validators_with_singleValidatorRecord_and_zero_balance() {
    // given
    validatorRecordTest =
        getValidatorsList(
            getDefaultValidatorRecordWithStatus(200, ACTIVE_PENDING_EXIT, DEFAULT_BALANCE));

    // when
    double effectiveBalanceActual = ValidatorsUtil.get_effective_balance(validatorRecordTest);

    // then
    assertEquals(effectiveBalanceExpected, effectiveBalanceActual);
  }

  @Test
  void
      assert_get_nonZero_as_effective_balance_for_validators_with_singleValidatorRecord_and_nonZero_balance() {
    // given
    balance = 112.32d;
    effectiveBalanceExpected = balance;
    validatorRecordTest =
        getValidatorsList(getDefaultValidatorRecordWithStatus(200, ACTIVE_PENDING_EXIT, balance));

    // when
    double effectiveBalanceActual = ValidatorsUtil.get_effective_balance(validatorRecordTest);

    // then
    assertEquals(effectiveBalanceExpected, effectiveBalanceActual);
  }

  @Test
  void
      assert_get_nonZero_value_as_effective_balance_for_validators_with_multipleValidatorRecords() {
    // given
    double validatorRecordBal1 = 112.32d;
    double validatorRecordBal2 = 100.445311d;
    validatorRecordTest =
        getValidatorsList(
            getDefaultValidatorRecordWithStatus(100, ACTIVE_PENDING_EXIT, validatorRecordBal1),
            getDefaultValidatorRecordWithStatus(200, ACTIVE_PENDING_EXIT, validatorRecordBal2));
    effectiveBalanceExpected = validatorRecordBal1 + validatorRecordBal2;

    // when
    double effectiveBalanceActual = ValidatorsUtil.get_effective_balance(validatorRecordTest);

    // then
    assertEquals(effectiveBalanceExpected, effectiveBalanceActual);
  }

  @Test
  void assert_get_active_validator_indices() {
    // given
    validatorRecordTest =
        getValidatorsList(
            getDefaultValidatorRecordWithStatus(100, ACTIVE_PENDING_EXIT, DEFAULT_BALANCE),
            getDefaultValidatorRecordWithStatus(200, ACTIVE_PENDING_EXIT, DEFAULT_BALANCE));
    validatorSizeExpected = 2;

    // when
    Validators activeValidatorsActual = ValidatorsUtil.get_active_validators(validatorRecordTest);

    // then
    assertNotNull(activeValidatorsActual);
    assertEquals(validatorSizeExpected, activeValidatorsActual.size());
    assertEquals(validatorRecordTest.get(0), activeValidatorsActual.get(0));
    assertEquals(validatorRecordTest.get(1), activeValidatorsActual.get(1));
  }

  @Test
  void assert_that_inactive_validators_are_excluded_for_get_active_validator_indices() {
    // given
    validatorRecordTest =
        getValidatorsList(
            getDefaultValidatorRecordWithStatus(100, ACTIVE_PENDING_EXIT, DEFAULT_BALANCE),
            getDefaultValidatorRecordWithStatus(200, EXITED_WITHOUT_PENALTY, DEFAULT_BALANCE));
    validatorSizeExpected = 1;

    // when
    Validators activeValidatorsActual = ValidatorsUtil.get_active_validators(validatorRecordTest);

    // then
    assertNotNull(activeValidatorsActual);
    assertEquals(validatorSizeExpected, activeValidatorsActual.size());
    assertTrue(activeValidatorsActual.contains(validatorRecordTest.get(0)));
    assertFalse(activeValidatorsActual.contains(validatorRecordTest.get(1)));
  }

  @Test
  void assert_get_active_validator_indices_for_all_inactive_validators_scenario() {
    // given
    validatorRecordTest =
        getValidatorsList(
            getDefaultValidatorRecordWithStatus(100, EXITED_WITHOUT_PENALTY, DEFAULT_BALANCE),
            getDefaultValidatorRecordWithStatus(200, EXITED_WITHOUT_PENALTY, DEFAULT_BALANCE));
    validatorSizeExpected = 0;

    // when
    Validators activeValidatorsActual = ValidatorsUtil.get_active_validators(validatorRecordTest);

    // then
    assertNotNull(activeValidatorsActual);
    assertEquals(validatorSizeExpected, activeValidatorsActual.size());
    assertFalse(activeValidatorsActual.contains(validatorRecordTest.get(0)));
    assertFalse(activeValidatorsActual.contains(validatorRecordTest.get(1)));
  }

  @Test
  void assert_get_active_validator_indices_as_emptyList_for_nullInput() {
    // given
    validatorSizeExpected = 0;

    // when
    Validators activeValidatorsActual = ValidatorsUtil.get_active_validators(null);

    // then
    assertNotNull(activeValidatorsActual);
    assertEquals(validatorSizeExpected, activeValidatorsActual.size());
  }

  ValidatorRecord getAValidatorRecordTestDataFromParameters(
      Bytes48 pubkey,
      Bytes32 withdrawalCredentials,
      Bytes32 randaoCommitment,
      UnsignedLong randaoLayers,
      UnsignedLong status,
      UnsignedLong slot,
      UnsignedLong exitCount,
      UnsignedLong lastPocChangeSlot,
      UnsignedLong secondLastPocChangeSlot,
      double balance) {
    ValidatorRecord validatorRecord =
        new ValidatorRecord(
            pubkey,
            withdrawalCredentials,
            randaoCommitment,
            randaoLayers,
            status,
            slot,
            exitCount,
            lastPocChangeSlot,
            secondLastPocChangeSlot);
    validatorRecord.setBalance(balance);

    return validatorRecord;
  }

  ValidatorRecord getDefaultValidatorRecordWithStatus(
      int pubKeyInt, int statusAsInt, double balance) {
    Bytes32 withdrawal_credentials = Bytes32.ZERO;
    Bytes32 randaoCommitment = Bytes32.ZERO;
    UnsignedLong randaoLayers = UnsignedLong.ZERO;
    UnsignedLong status = UnsignedLong.valueOf(statusAsInt);
    UnsignedLong slot = UnsignedLong.ZERO;
    UnsignedLong exitCount = UnsignedLong.ZERO;
    UnsignedLong lastPocChangeSlot = UnsignedLong.ZERO;
    UnsignedLong secondLastPocChangeSlot = UnsignedLong.ZERO;

    return getAValidatorRecordTestDataFromParameters(
        Bytes48.leftPad(Bytes.ofUnsignedInt(pubKeyInt)),
        withdrawal_credentials,
        randaoCommitment,
        randaoLayers,
        status,
        slot,
        exitCount,
        lastPocChangeSlot,
        secondLastPocChangeSlot,
        balance);
  }

  Validators getValidatorsList(ValidatorRecord... validatorRecords) {
    return new Validators(Arrays.asList(validatorRecords));
  }
}
