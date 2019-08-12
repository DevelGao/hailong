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

package tech.devgao.hailong.sync;

import tech.devgao.hailong.datastructures.blocks.SignedBeaconBlock;
import tech.devgao.hailong.networking.eth2.rpc.core.InvalidResponseException;
import tech.devgao.hailong.statetransition.blockimport.BlockImportResult;

public class FailedBlockImportException extends InvalidResponseException {
  private final SignedBeaconBlock block;
  private final BlockImportResult result;

  public FailedBlockImportException(final SignedBeaconBlock block, final BlockImportResult result) {
    super("Unable to import block due to error " + result.getFailureReason() + ": " + block);
    this.block = block;
    this.result = result;
  }

  public SignedBeaconBlock getBlock() {
    return block;
  }

  public BlockImportResult getResult() {
    return result;
  }
}
