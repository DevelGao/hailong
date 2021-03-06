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

package tech.devgao.hailong.datastructures.operations;

import com.google.common.collect.Sets;
import com.google.common.primitives.UnsignedLong;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import tech.devgao.hailong.datastructures.util.SimpleOffsetSerializer;
import tech.devgao.hailong.util.SSZTypes.Bitlist;
import tech.devgao.hailong.util.SSZTypes.SSZContainer;
import tech.devgao.hailong.util.bls.BLSSignature;
import tech.devgao.hailong.util.config.Constants;
import tech.devgao.hailong.util.hashtree.HashTreeUtil;
import tech.devgao.hailong.util.hashtree.HashTreeUtil.SSZTypes;
import tech.devgao.hailong.util.hashtree.Merkleizable;
import tech.devgao.hailong.util.sos.SimpleOffsetSerializable;

public class Attestation implements Merkleizable, SimpleOffsetSerializable, SSZContainer {

  // The number of SimpleSerialize basic types in this SSZ Container/POJO.
  public static final int SSZ_FIELD_COUNT = 2;

  private Bitlist aggregation_bits; // Bitlist bounded by MAX_VALIDATORS_PER_COMMITTEE
  private AttestationData data;
  private BLSSignature signature;

  public Attestation(Bitlist aggregation_bits, AttestationData data, BLSSignature signature) {
    this.aggregation_bits = aggregation_bits;
    this.data = data;
    this.signature = signature;
  }

  public Attestation(Attestation attestation) {
    this.aggregation_bits = attestation.getAggregation_bits().copy();
    this.data = attestation.getData();
    this.signature = BLSSignature.fromBytes(attestation.getAggregate_signature().toBytes());
  }

  public Attestation() {
    this.aggregation_bits =
        new Bitlist(Constants.MAX_VALIDATORS_PER_COMMITTEE, Constants.MAX_VALIDATORS_PER_COMMITTEE);
  }

  public UnsignedLong getEarliestSlotForProcessing() {
    // Attestations can't be processed until their slot is in the past and until we are in the same
    // epoch as their target.
    return max(data.getSlot().plus(UnsignedLong.ONE), data.getTarget().getEpochSlot());
  }

  public Collection<Bytes32> getDependentBlockRoots() {
    return Sets.newHashSet(data.getTarget().getRoot(), data.getBeacon_block_root());
  }

  private static UnsignedLong max(final UnsignedLong a, final UnsignedLong b) {
    return a.compareTo(b) > 0 ? a : b;
  }

  @Override
  public int getSSZFieldCount() {
    return SSZ_FIELD_COUNT + data.getSSZFieldCount() + signature.getSSZFieldCount();
  }

  @Override
  public List<Bytes> get_fixed_parts() {
    List<Bytes> fixedPartsList = new ArrayList<>();
    fixedPartsList.addAll(List.of(Bytes.EMPTY));
    fixedPartsList.addAll(data.get_fixed_parts());
    fixedPartsList.addAll(signature.get_fixed_parts());
    return fixedPartsList;
  }

  @Override
  public List<Bytes> get_variable_parts() {
    List<Bytes> variablePartsList = new ArrayList<>();
    // TODO The below lines are a hack while Tuweni SSZ/SOS is being upgraded. To be uncommented
    // once we shift from Bitlist to a real bitlist type.
    // Bitlist serialized_aggregation_bits =
    // Bitlist.fromHexString("0x01").shiftLeft(aggregation_bits.bitLength()).or(aggregation_bits);
    // variablePartsList.addAll(List.of(serialized_aggregation_bits));
    variablePartsList.addAll(List.of(aggregation_bits.serialize()));
    variablePartsList.addAll(Collections.nCopies(data.getSSZFieldCount(), Bytes.EMPTY));
    // TODO The below lines are a hack while Tuweni SSZ/SOS is being upgraded. To be uncommented
    // once we shift from Bitlist to a real bitlist type.
    // Bitlist serialized_custody_bitfield =
    // Bitlist.fromHexString("0x01").shiftLeft(aggregation_bits.bitLength()).or(custody_bitfield);
    // variablePartsList.addAll(List.of(serialized_custody_bitfield));
    variablePartsList.addAll(Collections.nCopies(signature.getSSZFieldCount(), Bytes.EMPTY));
    return variablePartsList;
  }

  @Override
  public int hashCode() {
    return Objects.hash(aggregation_bits, data, signature);
  }

  @Override
  public boolean equals(Object obj) {
    if (Objects.isNull(obj)) {
      return false;
    }

    if (this == obj) {
      return true;
    }

    if (!(obj instanceof Attestation)) {
      return false;
    }

    Attestation other = (Attestation) obj;
    return Objects.equals(this.getAggregation_bits(), other.getAggregation_bits())
        && Objects.equals(this.getData(), other.getData())
        && Objects.equals(this.getAggregate_signature(), other.getAggregate_signature());
  }

  /** ******************* * GETTERS & SETTERS * * ******************* */
  public Bitlist getAggregation_bits() {
    return aggregation_bits;
  }

  public void setAggregation_bits(Bitlist aggregation_bits) {
    this.aggregation_bits = aggregation_bits;
  }

  public AttestationData getData() {
    return data;
  }

  public void setData(AttestationData data) {
    this.data = data;
  }

  public BLSSignature getAggregate_signature() {
    return signature;
  }

  public void setAggregate_signature(BLSSignature aggregate_signature) {
    this.signature = aggregate_signature;
  }

  @Override
  public Bytes32 hash_tree_root() {
    return HashTreeUtil.merkleize(
        Arrays.asList(
            HashTreeUtil.hash_tree_root_bitlist(aggregation_bits),
            data.hash_tree_root(),
            HashTreeUtil.hash_tree_root(
                SSZTypes.VECTOR_OF_BASIC, SimpleOffsetSerializer.serialize(signature))));
  }
}
