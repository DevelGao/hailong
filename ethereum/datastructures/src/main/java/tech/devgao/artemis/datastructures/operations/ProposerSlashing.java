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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.ssz.SSZ;
import tech.devgao.artemis.datastructures.blocks.BeaconBlockHeader;
import tech.devgao.artemis.util.hashtree.HashTreeUtil;
import tech.devgao.artemis.util.hashtree.HashTreeUtil.SSZTypes;
import tech.devgao.artemis.util.hashtree.Merkleizable;
import tech.devgao.artemis.util.sos.SimpleOffsetSerializable;

public class ProposerSlashing implements Merkleizable, SimpleOffsetSerializable {

  // The number of SimpleSerialize basic types in this SSZ Container/POJO.
  public static final int SSZ_FIELD_COUNT = 1;

  private UnsignedLong proposer_index;
  private BeaconBlockHeader header_1;
  private BeaconBlockHeader header_2;

  public ProposerSlashing(
      UnsignedLong proposer_index, BeaconBlockHeader header_1, BeaconBlockHeader header_2) {
    this.proposer_index = proposer_index;
    this.header_1 = header_1;
    this.header_2 = header_2;
  }

  @Override
  public int getSSZFieldCount() {
    return SSZ_FIELD_COUNT + header_1.getSSZFieldCount() + header_2.getSSZFieldCount();
  }

  @Override
  public List<Bytes> get_fixed_parts() {
    List<Bytes> fixedPartsList =
        new ArrayList<>(List.of(SSZ.encodeUInt64(proposer_index.longValue())));
    fixedPartsList.addAll(header_1.get_fixed_parts());
    fixedPartsList.addAll(header_2.get_fixed_parts());
    return fixedPartsList;
  }

  public static ProposerSlashing fromBytes(Bytes bytes) {
    return SSZ.decode(
        bytes,
        reader ->
            new ProposerSlashing(
                UnsignedLong.fromLongBits(reader.readUInt64()),
                BeaconBlockHeader.fromBytes(reader.readBytes()),
                BeaconBlockHeader.fromBytes(reader.readBytes())));
  }

  public Bytes toBytes() {
    return SSZ.encode(
        writer -> {
          writer.writeUInt64(proposer_index.longValue());
          writer.writeBytes(header_1.toBytes());
          writer.writeBytes(header_2.toBytes());
        });
  }

  @Override
  public int hashCode() {
    return Objects.hash(proposer_index, header_1, header_2);
  }

  @Override
  public boolean equals(Object obj) {
    if (Objects.isNull(obj)) {
      return false;
    }

    if (this == obj) {
      return true;
    }

    if (!(obj instanceof ProposerSlashing)) {
      return false;
    }

    ProposerSlashing other = (ProposerSlashing) obj;
    return Objects.equals(this.getProposer_index(), other.getProposer_index())
        && Objects.equals(this.getHeader_1(), other.getHeader_1())
        && Objects.equals(this.getHeader_2(), other.getHeader_2());
  }

  /** ******************* * GETTERS & SETTERS * * ******************* */
  public UnsignedLong getProposer_index() {
    return proposer_index;
  }

  public void setProposer_index(UnsignedLong proposer_index) {
    this.proposer_index = proposer_index;
  }

  public BeaconBlockHeader getHeader_1() {
    return header_1;
  }

  public void setHeader_1(BeaconBlockHeader header_1) {
    this.header_1 = header_1;
  }

  public BeaconBlockHeader getHeader_2() {
    return header_2;
  }

  public void setHeader_2(BeaconBlockHeader header_2) {
    this.header_2 = header_2;
  }

  @Override
  public Bytes32 hash_tree_root() {
    return HashTreeUtil.merkleize(
        Arrays.asList(
            HashTreeUtil.hash_tree_root(
                SSZTypes.BASIC, SSZ.encodeUInt64(proposer_index.longValue())),
            header_1.hash_tree_root(),
            header_2.hash_tree_root()));
  }
}
