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

import java.math.BigInteger;

/**
 * An unsigned 256-bits precision number.
 *
 * This class is essentially a "raw" {@link UInt256Value}, a 256-bits precision unsigned number of
 * no particular unit.
 */
public interface UInt256 extends UInt256Value<UInt256> {
  /** The value 0. */
  UInt256 ZERO = of(0);
  /** The value 1. */
  UInt256 ONE = of(1);
  /** The value 32. */
  UInt256 U_32 = of(32);

  static UInt256 of(long value) {
    return new DefaultUInt256(UInt256Bytes.of(value));
  }

  static UInt256 of(BigInteger value) {
    return new DefaultUInt256(UInt256Bytes.of(value));
  }

  static UInt256 wrap(BytesValue value) {
    return new DefaultUInt256((Bytes32) value);
  }

  static Counter<UInt256> newCounter() {
    return DefaultUInt256.newVar();
  }

  static Counter<UInt256> newCounter(UInt256Value<?> initialValue) {
    Counter<UInt256> c = DefaultUInt256.newVar();
    initialValue.bytes().copyTo(c.bytes());
    return c;
  }

  static UInt256 fromHexString(String str) {
    return new DefaultUInt256(Bytes32.fromHexStringLenient(str));
  }
}
