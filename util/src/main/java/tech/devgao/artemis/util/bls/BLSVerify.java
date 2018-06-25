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

package tech.devgao.artemis.util.bls;

import tech.devgao.artemis.util.bytes.Bytes32;
import tech.devgao.artemis.util.uint.UInt384;
import tech.devgao.artemis.util.uint.UInt64;

public class BLSVerify {

  // TODO: Need to update UInt384 to UInt384[].
  public static boolean bls_verify(UInt384 pubkey, Bytes32 message, UInt384 signature, UInt64 domain) {
    return true;
  }

}