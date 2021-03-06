/*
 * Copyright 2018 Developer Gao.
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

package net.develgao.artemis.util.uint;

import net.develgao.artemis.util.bytes.Bytes32;
import net.develgao.artemis.util.bytes.BytesValue;
import net.develgao.artemis.util.bytes.BytesValues;
import net.develgao.artemis.util.bytes.MutableBytes32;

import java.math.BigInteger;
import java.util.function.BinaryOperator;

/**
 * Static operations to work on bytes interpreted as 256 bytes signed integers.
 *
 * <p>
 * This class is the base of the operations on {@link Int256} but can also be used to work directly
 * on bytes if necessary.
 *
 * <p>
 * All operations that write a result are written assuming that the result may be the same object
 * than one or more of the operands.
 */
abstract class Int256Bytes {

  private Int256Bytes() {}

  private static final byte ALL_ZERO_BYTE = (byte) 0x00;
  private static final byte ALL_ONE_BYTE = (byte) 0xFF;

  private static void copy(BigInteger result, MutableBytes32 destination) {
    byte padding = result.signum() < 0 ? ALL_ONE_BYTE : ALL_ZERO_BYTE;
    UInt256Bytes.copyPadded(BytesValue.wrap(result.toByteArray()), destination, padding);
  }

  private static void doOnSignedBigInteger(Bytes32 v1, Bytes32 v2, MutableBytes32 dest,
      BinaryOperator<BigInteger> operator) {
    BigInteger i1 = BytesValues.asSignedBigInteger(v1);
    BigInteger i2 = BytesValues.asSignedBigInteger(v2);
    BigInteger result = operator.apply(i1, i2);
    copy(result, dest);
  }

  // Tests if this value represents -2^255, that is the first byte is 1 followed by only 0. Used to
  // implement the overflow condition of the Yellow Paper in signedDivide().
  private static boolean isMinusP255(Bytes32 v) {
    if (v.get(0) != (byte) 0x80)
      return false;

    byte b = 0;
    for (int i = 1; i < v.size(); i++) {
      b |= v.get(i);
    }
    return b == 0;
  }

  static void divide(Bytes32 v1, Bytes32 v2, MutableBytes32 result) {
    if (v2.isZero()) {
      result.clear();
    } else if (v2.equals(Int256.MINUS_ONE.bytes()) && isMinusP255(v2)) {
      // Set to -2^255.
      result.clear();
      result.set(0, (byte) 0x80);
    } else {
      doOnSignedBigInteger(v1, v2, result, BigInteger::divide);
    }
  }

  static void mod(Bytes32 v1, Bytes32 v2, MutableBytes32 result) {
    if (v2.isZero()) {
      result.clear();
    } else {
      doOnSignedBigInteger(v1, v2, result, (val, mod) -> {
        BigInteger absModulo = val.abs().mod(mod.abs());
        return val.signum() < 0 ? absModulo.negate() : absModulo;
      });
    }
  }

}
