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

package tech.devgao.artemis.datastructures.operations;

import com.google.common.primitives.UnsignedLong;
import java.util.Objects;
import net.develgao.cava.bytes.Bytes;
import net.develgao.cava.ssz.SSZ;
import tech.devgao.artemis.util.bls.BLSSignature;

public class Exit {

  private UnsignedLong epoch;
  private UnsignedLong validator_index;
  private BLSSignature signature;

  public Exit(UnsignedLong epoch, UnsignedLong validator_index, BLSSignature signature) {
    this.epoch = epoch;
    this.validator_index = validator_index;
    this.signature = signature;
  }

  public static Exit fromBytes(Bytes bytes) {
    return SSZ.decode(
        bytes,
        reader ->
            new Exit(
                UnsignedLong.fromLongBits(reader.readUInt64()),
                UnsignedLong.fromLongBits(reader.readUInt64()),
                BLSSignature.fromBytes(reader.readBytes())));
  }

  public Bytes toBytes() {
    return SSZ.encode(
        writer -> {
          writer.writeUInt64(epoch.longValue());
          writer.writeUInt64(validator_index.longValue());
          writer.writeBytes(signature.toBytes());
        });
  }

  @Override
  public int hashCode() {
    return Objects.hash(epoch, validator_index, signature);
  }

  @Override
  public boolean equals(Object obj) {
    if (Objects.isNull(obj)) {
      return false;
    }

    if (this == obj) {
      return true;
    }

    if (!(obj instanceof Exit)) {
      return false;
    }

    Exit other = (Exit) obj;
    return Objects.equals(this.getEpoch(), other.getEpoch())
        && Objects.equals(this.getValidator_index(), other.getValidator_index())
        && Objects.equals(this.getSignature(), other.getSignature());
  }

  /** ******************* * GETTERS & SETTERS * * ******************* */
  public UnsignedLong getEpoch() {
    return epoch;
  }

  public void setEpoch(UnsignedLong epoch) {
    this.epoch = epoch;
  }

  public UnsignedLong getValidator_index() {
    return validator_index;
  }

  public void setValidator_index(UnsignedLong validator_index) {
    this.validator_index = validator_index;
  }

  public BLSSignature getSignature() {
    return signature;
  }

  public void setSignature(BLSSignature signature) {
    this.signature = signature;
  }
}
