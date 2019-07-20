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

package tech.devgao.hailong.datastructures.state;

import com.google.common.primitives.UnsignedLong;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.ssz.SSZ;
import tech.devgao.hailong.datastructures.Copyable;
import tech.devgao.hailong.util.hashtree.HashTreeUtil;
import tech.devgao.hailong.util.hashtree.HashTreeUtil.SSZTypes;
import tech.devgao.hailong.util.sos.SimpleOffsetSerializable;

public class Crosslink implements Copyable<Crosslink>, SimpleOffsetSerializable {

  // The number of SimpleSerialize basic types in this SSZ Container/POJO.
  public static final int SSZ_FIELD_COUNT = 5;

  private UnsignedLong shard;
  private Bytes32 parent_root;
  private UnsignedLong start_epoch;
  private UnsignedLong end_epoch;
  private Bytes32 data_root;

  public Crosslink(
      UnsignedLong shard,
      Bytes32 parent_root,
      UnsignedLong start_epoch,
      UnsignedLong end_epoch,
      Bytes32 data_root) {
    this.shard = shard;
    this.parent_root = parent_root;
    this.start_epoch = start_epoch;
    this.end_epoch = end_epoch;
    this.data_root = data_root;
  }

  public Crosslink(Crosslink crosslink) {
    this.shard = crosslink.getShard();
    this.parent_root = crosslink.getParent_root();
    this.start_epoch = crosslink.getStart_epoch();
    this.end_epoch = crosslink.getEnd_epoch();
    this.data_root = crosslink.getData_root();
  }

  public Crosslink() {
    this.shard = UnsignedLong.ZERO;
    this.parent_root = Bytes32.ZERO;
    this.start_epoch = UnsignedLong.ZERO;
    this.end_epoch = UnsignedLong.ZERO;
    this.data_root = Bytes32.ZERO;
  }

  @Override
  public int getSSZFieldCount() {
    return SSZ_FIELD_COUNT;
  }

  @Override
  public List<Bytes> get_fixed_parts() {
    return List.of(
        SSZ.encodeUInt64(shard.longValue()),
        SSZ.encode(writer -> writer.writeFixedBytes(parent_root)),
        SSZ.encodeUInt64(start_epoch.longValue()),
        SSZ.encodeUInt64(end_epoch.longValue()),
        SSZ.encode(writer -> writer.writeFixedBytes(data_root)));
  }

  public static Crosslink fromBytes(Bytes bytes) {
    return SSZ.decode(
        bytes,
        reader ->
            new Crosslink(
                UnsignedLong.fromLongBits(reader.readUInt64()),
                Bytes32.wrap(reader.readFixedBytes(32)),
                UnsignedLong.fromLongBits(reader.readUInt64()),
                UnsignedLong.fromLongBits(reader.readUInt64()),
                Bytes32.wrap(reader.readFixedBytes(32))));
  }

  @Override
  public Crosslink copy() {
    return new Crosslink(this);
  }

  public Bytes toBytes() {
    return SSZ.encode(
        writer -> {
          writer.writeUInt64(shard.longValue());
          writer.writeFixedBytes(parent_root);
          writer.writeUInt64(start_epoch.longValue());
          writer.writeUInt64(end_epoch.longValue());
          writer.writeFixedBytes(data_root);
        });
  }

  @Override
  public int hashCode() {
    return Objects.hash(shard, parent_root, start_epoch, end_epoch, data_root);
  }

  @Override
  public boolean equals(Object obj) {
    if (Objects.isNull(obj)) {
      return false;
    }

    if (this == obj) {
      return true;
    }

    if (!(obj instanceof Crosslink)) {
      return false;
    }

    Crosslink other = (Crosslink) obj;
    return Objects.equals(this.getShard(), other.getShard())
        && Objects.equals(this.getParent_root(), other.getParent_root())
        && Objects.equals(this.getStart_epoch(), other.getStart_epoch())
        && Objects.equals(this.getEnd_epoch(), other.getEnd_epoch())
        && Objects.equals(this.getData_root(), other.getData_root());
  }

  /** ******************* * GETTERS & SETTERS * * ******************* */
  public UnsignedLong getShard() {
    return shard;
  }

  public void setShard(UnsignedLong shard) {
    this.shard = shard;
  }

  public Bytes32 getParent_root() {
    return parent_root;
  }

  public void setParent_root(Bytes32 parent_root) {
    this.parent_root = parent_root;
  }

  public UnsignedLong getStart_epoch() {
    return start_epoch;
  }

  public void setStart_epoch(UnsignedLong start_epoch) {
    this.start_epoch = start_epoch;
  }

  public UnsignedLong getEnd_epoch() {
    return end_epoch;
  }

  public void setEnd_epoch(UnsignedLong end_epoch) {
    this.end_epoch = end_epoch;
  }

  public Bytes32 getData_root() {
    return data_root;
  }

  public void setData_root(Bytes32 data_root) {
    this.data_root = data_root;
  }

  public Bytes32 hash_tree_root() {
    return HashTreeUtil.merkleize(
        Arrays.asList(
            HashTreeUtil.hash_tree_root(SSZTypes.BASIC, SSZ.encodeUInt64(shard.longValue())),
            HashTreeUtil.hash_tree_root(SSZTypes.TUPLE_OF_BASIC, parent_root),
            HashTreeUtil.hash_tree_root(SSZTypes.BASIC, SSZ.encodeUInt64(start_epoch.longValue())),
            HashTreeUtil.hash_tree_root(SSZTypes.BASIC, SSZ.encodeUInt64(end_epoch.longValue())),
            HashTreeUtil.hash_tree_root(SSZTypes.TUPLE_OF_BASIC, data_root)));
  }
}
