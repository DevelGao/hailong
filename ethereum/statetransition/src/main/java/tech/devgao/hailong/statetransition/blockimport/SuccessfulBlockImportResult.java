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

package tech.devgao.hailong.statetransition.blockimport;

import com.google.common.base.MoreObjects;
import java.util.Optional;
import tech.devgao.hailong.data.BlockProcessingRecord;

public class SuccessfulBlockImportResult implements BlockImportResult {

  private final BlockProcessingRecord record;

  public SuccessfulBlockImportResult(final BlockProcessingRecord record) {
    this.record = record;
  }

  @Override
  public boolean isSuccessful() {
    return true;
  }

  @Override
  public BlockProcessingRecord getBlockProcessingRecord() {
    return record;
  }

  @Override
  public FailureReason getFailureReason() {
    return null;
  }

  @Override
  public Optional<Throwable> getFailureCause() {
    return Optional.empty();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("block", record.getBlock()).toString();
  }
}
