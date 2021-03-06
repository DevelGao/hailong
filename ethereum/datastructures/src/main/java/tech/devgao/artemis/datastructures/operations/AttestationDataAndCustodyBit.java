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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.ssz.SSZ;
import tech.devgao.artemis.util.hashtree.HashTreeUtil;
import tech.devgao.artemis.util.hashtree.HashTreeUtil.SSZTypes;
import tech.devgao.artemis.util.sos.SimpleOffsetSerializable;

public class AttestationDataAndCustodyBit implements SimpleOffsetSerializable {

  // The number of SimpleSerialize basic types in this SSZ Container/POJO.
  public static final int SSZ_FIELD_COUNT = 1;

  private AttestationData data;
  private boolean custody_bit;

  public AttestationDataAndCustodyBit(AttestationData data, boolean custody_bit) {
    this.data = data;
    this.custody_bit = custody_bit;
  }

  @Override
  public int getSSZFieldCount() {
    return data.getSSZFieldCount() + SSZ_FIELD_COUNT;
  }

  @Override
  public List<Bytes> get_fixed_parts() {
    List<Bytes> fixedPartsList = new ArrayList<>();
    fixedPartsList.addAll(data.get_fixed_parts());
    fixedPartsList.addAll(List.of(SSZ.encodeBoolean(custody_bit)));
    return fixedPartsList;
  }

  public static AttestationDataAndCustodyBit fromBytes(Bytes bytes) {
    return SSZ.decode(
        bytes,
        reader ->
            new AttestationDataAndCustodyBit(
                AttestationData.fromBytes(reader.readBytes()), reader.readBoolean()));
  }

  public Bytes toBytes() {
    return SSZ.encode(
        writer -> {
          writer.writeBytes(data.toBytes());
          writer.writeBoolean(custody_bit);
        });
  }

  @Override
  public int hashCode() {
    return Objects.hash(data, custody_bit);
  }

  @Override
  public boolean equals(Object obj) {
    if (Objects.isNull(obj)) {
      return false;
    }

    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AttestationDataAndCustodyBit)) {
      return false;
    }

    AttestationDataAndCustodyBit other = (AttestationDataAndCustodyBit) obj;
    return Objects.equals(this.getData(), other.getData())
        && Objects.equals(this.getCustody_bit(), other.getCustody_bit());
  }

  /** ******************* * GETTERS & SETTERS * * ******************* */
  public boolean getCustody_bit() {
    return custody_bit;
  }

  public void setCustody_bit(boolean custody_bit) {
    this.custody_bit = custody_bit;
  }

  public AttestationData getData() {
    return data;
  }

  public void setData(AttestationData data) {
    this.data = data;
  }

  public Bytes32 hash_tree_root() {
    return HashTreeUtil.merkleize(
        Arrays.asList(
            data.hash_tree_root(),
            HashTreeUtil.hash_tree_root(SSZTypes.BASIC, SSZ.encodeBoolean(custody_bit))));
  }
}
