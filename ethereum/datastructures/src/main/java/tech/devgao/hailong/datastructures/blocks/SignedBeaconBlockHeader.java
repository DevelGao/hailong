/*
 * Copyright 2020 Developer Gao.
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

package tech.devgao.hailong.datastructures.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import tech.devgao.hailong.util.SSZTypes.SSZContainer;
import tech.devgao.hailong.util.bls.BLSSignature;
import tech.devgao.hailong.util.hashtree.HashTreeUtil;
import tech.devgao.hailong.util.hashtree.HashTreeUtil.SSZTypes;
import tech.devgao.hailong.util.sos.SimpleOffsetSerializable;

public class SignedBeaconBlockHeader implements SimpleOffsetSerializable, SSZContainer {
  private final BeaconBlockHeader message;
  private final BLSSignature signature;

  public SignedBeaconBlockHeader(final BeaconBlockHeader message, final BLSSignature signature) {
    this.message = message;
    this.signature = signature;
  }

  public BeaconBlockHeader getMessage() {
    return message;
  }

  public BLSSignature getSignature() {
    return signature;
  }

  @Override
  public int getSSZFieldCount() {
    return message.getSSZFieldCount() + signature.getSSZFieldCount();
  }

  @Override
  public List<Bytes> get_fixed_parts() {
    List<Bytes> fixedPartsList = new ArrayList<>();
    fixedPartsList.addAll(message.get_fixed_parts());
    fixedPartsList.addAll(signature.get_fixed_parts());
    return fixedPartsList;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final SignedBeaconBlockHeader that = (SignedBeaconBlockHeader) o;
    return Objects.equals(message, that.message) && Objects.equals(signature, that.signature);
  }

  @Override
  public int hashCode() {
    return Objects.hash(message, signature);
  }

  public Bytes32 hash_tree_root() {
    return HashTreeUtil.merkleize(
        Arrays.asList(
            message.hash_tree_root(),
            HashTreeUtil.hash_tree_root(SSZTypes.VECTOR_OF_BASIC, signature.toBytes())));
  }
}
