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

package tech.devgao.artemis.ethereum.mainnet;

import tech.devgao.artemis.ethereum.core.BlockHeader;
import tech.devgao.artemis.ethereum.core.Hash;
import tech.devgao.artemis.ethereum.rlp.RLP;
import tech.devgao.artemis.util.bytes.BytesValue;

/** Implements the block hashing algorithm for MainNet as per the yellow paper. */
public class MainnetBlockHashFunction {

  public static Hash createHash(BlockHeader header) {
    BytesValue rlp = RLP.encode(header::writeTo);
    return Hash.hash(rlp);
  }
}
