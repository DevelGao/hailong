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

package tech.devgao.hailong.util.async;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface AsyncRunner {
  SafeFuture<Void> DELAY_COMPLETE_SIGNAL = SafeFuture.completedFuture(null);

  <U> SafeFuture<U> runAsync(final Supplier<SafeFuture<U>> action, final Executor executor);

  <U> SafeFuture<U> runAfterDelay(
      Supplier<SafeFuture<U>> action, long delayAmount, TimeUnit delayUnit);

  default SafeFuture<Void> getDelayedFuture(long delayAmount, TimeUnit delayUnit) {
    return runAfterDelay(() -> DELAY_COMPLETE_SIGNAL, delayAmount, delayUnit);
  }
}
