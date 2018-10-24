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

package tech.devgao.artemis.statetransition.util;

import java.util.List;
import net.develgao.cava.bytes.Bytes;
import net.develgao.cava.bytes.Bytes32;
import net.develgao.cava.ssz.SSZ;
import tech.devgao.artemis.datastructures.state.Validator;
import tech.devgao.artemis.statetransition.BeaconState;

/** This class is a collection of tree hash root convenience methods */
public final class TreeHashUtil {

  /**
   * Calculate the hash tree root of the provided value
   *
   * @param value
   * @return
   */
  public static Bytes32 hash_tree_root(Bytes value) {
    return SSZ.hashTreeRoot(value);
  }

  /**
   * Calculate the hash tree root of the list of validators provided
   *
   * @param validators
   * @return
   */
  public static Bytes32 hash_tree_root(List<Validator> validators) {
    // TODO
    return Bytes32.ZERO;
  }

  /**
   * Calculate the hash tree root of the BeaconState provided
   *
   * @param state
   * @return
   */
  public static Bytes32 hash_tree_root(BeaconState state) {
    return hash_tree_root(state.toBytes());
  }
}
