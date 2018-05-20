/*
 * Copyright 2018 Developer Gao.
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

package net.develgao.artemis.datastructures.BeaconChainBlocks;

import net.develgao.artemis.ethereum.core.Hash;
import net.develgao.artemis.util.uint.UInt384;
import net.develgao.artemis.util.uint.UInt64;

public class BeaconBlock {

  // Header
  private UInt64 slot;
  private Hash[] ancestor_hashes;
  private Hash state_root;
  private Hash randao_reveal;
  private Hash candidate_pow_receipt_root;
  private UInt384[] signature;

  // Body
  private BeaconBlockBody body;


  public BeaconBlock() {

  }
}
