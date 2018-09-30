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

package tech.devgao.artemis.datastructures.blocks;

import java.util.Objects;
import net.develgao.cava.bytes.Bytes;
import net.develgao.cava.bytes.Bytes32;
import net.develgao.cava.ssz.SSZ;

public final class Eth1Data {

  private Bytes32 deposit_root;
  private Bytes32 block_root;

  public Eth1Data(Bytes32 deposit_root, Bytes32 block_root) {
    this.deposit_root = deposit_root;
    this.block_root = block_root;
  }

  public static Eth1Data fromBytes(Bytes bytes) {
    return SSZ.decode(
        bytes,
        reader -> new Eth1Data(Bytes32.wrap(reader.readBytes()), Bytes32.wrap(reader.readBytes())));
  }

  public Bytes toBytes() {
    return SSZ.encode(
        writer -> {
          writer.writeBytes(deposit_root);
          writer.writeBytes(block_root);
        });
  }

  @Override
  public int hashCode() {
    return Objects.hash(deposit_root, block_root);
  }

  @Override
  public boolean equals(Object obj) {
    if (Objects.isNull(obj)) {
      return false;
    }

    if (this == obj) {
      return true;
    }

    if (!(obj instanceof Eth1Data)) {
      return false;
    }

    Eth1Data other = (Eth1Data) obj;
    return Objects.equals(this.getDeposit_root(), other.getDeposit_root())
        && Objects.equals(this.getBlock_root(), other.getBlock_root());
  }

  /** @return the deposit_root */
  public Bytes32 getDeposit_root() {
    return deposit_root;
  }

  /** @param deposit_root the deposit_root to set */
  public void setDeposit_root(Bytes32 deposit_root) {
    this.deposit_root = deposit_root;
  }

  /** @return the block_root */
  public Bytes32 getBlock_root() {
    return block_root;
  }

  /** @param block_root the block_hash to set */
  public void setBlock_root(Bytes32 block_root) {
    this.block_root = block_root;
  }
}
