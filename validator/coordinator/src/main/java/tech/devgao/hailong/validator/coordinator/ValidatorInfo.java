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

package tech.devgao.hailong.validator.coordinator;

import io.grpc.ManagedChannel;

public class ValidatorInfo {
  private final boolean naughty;
  private final ManagedChannel channel;
  private int validatorIndex = -1;

  public ValidatorInfo(final boolean naughty, final ManagedChannel channel) {
    this.naughty = naughty;
    this.channel = channel;
  }

  public boolean isNaughty() {
    return naughty;
  }

  public ManagedChannel getChannel() {
    return channel;
  }

  public int getValidatorIndex() {
    return validatorIndex;
  }

  public void setValidatorIndex(int validatorIndex) {
    this.validatorIndex = validatorIndex;
  }
}
