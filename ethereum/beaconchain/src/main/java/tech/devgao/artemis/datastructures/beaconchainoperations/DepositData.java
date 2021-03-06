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

package tech.devgao.artemis.datastructures.beaconchainoperations;

import com.google.common.primitives.UnsignedLong;
import net.develgao.cava.bytes.Bytes;
import net.develgao.cava.ssz.SSZ;

public class DepositData {

  private DepositInput deposit_input;
  private UnsignedLong value;
  private UnsignedLong timestamp;

  public DepositData(DepositInput deposit_input, UnsignedLong value, UnsignedLong timestamp) {
    this.deposit_input = deposit_input;
    this.value = value;
    this.timestamp = timestamp;
  }

  public Bytes toBytes() {
    return SSZ.encode(
        writer -> {
          writer.writeBytes(deposit_input.toBytes());
          writer.writeUInt64(value.longValue());
          writer.writeUInt64(value.longValue());
        });
  }

  /** ******************* * GETTERS & SETTERS * * ******************* */
  public DepositInput getDeposit_input() {
    return deposit_input;
  }

  public void setDeposit_input(DepositInput deposit_input) {
    this.deposit_input = deposit_input;
  }

  public UnsignedLong getValue() {
    return value;
  }

  public void setValue(UnsignedLong value) {
    this.value = value;
  }

  public UnsignedLong getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(UnsignedLong timestamp) {
    this.timestamp = timestamp;
  }
}
