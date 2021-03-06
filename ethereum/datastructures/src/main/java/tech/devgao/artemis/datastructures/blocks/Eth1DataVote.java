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

import com.google.common.primitives.UnsignedLong;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.ssz.SSZ;
import tech.devgao.artemis.datastructures.Copyable;
import tech.devgao.artemis.util.hashtree.HashTreeUtil;
import tech.devgao.artemis.util.hashtree.HashTreeUtil.SSZTypes;
import tech.devgao.artemis.util.hashtree.Merkleizable;
import tech.devgao.artemis.util.sos.SimpleOffsetSerializable;

public final class Eth1DataVote
    implements Copyable<Eth1DataVote>, Merkleizable, SimpleOffsetSerializable {

  // The number of SimpleSerialize basic types in this SSZ Container/POJO.
  public static final int SSZ_FIELD_COUNT = 1;

  private Eth1Data eth1_data;
  private UnsignedLong vote_count;

  public Eth1DataVote(Eth1Data eth1_data, UnsignedLong vote_count) {
    this.eth1_data = eth1_data;
    this.vote_count = vote_count;
  }

  public Eth1DataVote(Eth1DataVote eth1DataVote) {
    this.eth1_data = new Eth1Data(eth1DataVote.getEth1_data());
    this.vote_count = eth1DataVote.getVote_count();
  }

  @Override
  public Eth1DataVote copy() {
    return new Eth1DataVote(this);
  }

  @Override
  public int getSSZFieldCount() {
    return eth1_data.getSSZFieldCount() + SSZ_FIELD_COUNT;
  }

  @Override
  public List<Bytes> get_fixed_parts() {
    List<Bytes> fixedPartsList = new ArrayList<>();
    fixedPartsList.addAll(eth1_data.get_fixed_parts());
    fixedPartsList.addAll(List.of(SSZ.encodeUInt64(vote_count.longValue())));
    return fixedPartsList;
  }

  public static Eth1DataVote fromBytes(Bytes bytes) {
    return SSZ.decode(
        bytes,
        reader ->
            new Eth1DataVote(
                Eth1Data.fromBytes(reader.readBytes()),
                UnsignedLong.fromLongBits(reader.readUInt64())));
  }

  public Bytes toBytes() {
    return SSZ.encode(
        writer -> {
          writer.writeBytes(eth1_data.toBytes());
          writer.writeUInt64(vote_count.longValue());
        });
  }

  @Override
  public int hashCode() {
    return Objects.hash(eth1_data, vote_count);
  }

  @Override
  public boolean equals(Object obj) {
    if (Objects.isNull(obj)) {
      return false;
    }

    if (this == obj) {
      return true;
    }

    if (!(obj instanceof Eth1DataVote)) {
      return false;
    }

    Eth1DataVote other = (Eth1DataVote) obj;
    return Objects.equals(this.getEth1_data(), other.getEth1_data())
        && Objects.equals(this.getVote_count(), other.getVote_count());
  }

  /** @return the eth1_data */
  public Eth1Data getEth1_data() {
    return eth1_data;
  }

  /** @param eth1_data the eth1_data to set */
  public void setEth1_data(Eth1Data eth1_data) {
    this.eth1_data = eth1_data;
  }

  /** @return the vote_count */
  public UnsignedLong getVote_count() {
    return vote_count;
  }

  /** @param vote_count the vote_count to set */
  public void setVote_count(UnsignedLong vote_count) {
    this.vote_count = vote_count;
  }

  @Override
  public Bytes32 hash_tree_root() {
    return HashTreeUtil.merkleize(
        Arrays.asList(
            eth1_data.hash_tree_root(),
            HashTreeUtil.hash_tree_root(SSZTypes.BASIC, SSZ.encodeUInt64(vote_count.longValue()))));
  }
}
