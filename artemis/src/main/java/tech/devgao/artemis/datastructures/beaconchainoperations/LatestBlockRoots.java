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

package tech.devgao.artemis.datastructures.beaconchainoperations;

import tech.devgao.artemis.Constants;
import tech.devgao.artemis.ethereum.core.Hash;
import tech.devgao.artemis.util.uint.UInt64;

import java.util.LinkedHashMap;
import java.util.Map;

public class LatestBlockRoots extends LinkedHashMap<UInt64, Hash> {

    @Override
    protected boolean removeEldestEntry(Map.Entry<UInt64, Hash> eldest) {
        return this.size() > Constants.LATEST_BLOCK_ROOTS_LENGTH;
    }
}
