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

package tech.devgao.hailong.service.serviceutils;

import java.util.concurrent.atomic.AtomicReference;
import tech.devgao.hailong.util.async.SafeFuture;

public abstract class Service {
  private static final SafeFuture<Void> FALLBACK_STOPPED_FUTURE = SafeFuture.completedFuture(null);

  enum State {
    IDLE,
    RUNNING,
    STOPPED
  }

  private final AtomicReference<State> state = new AtomicReference<>(State.IDLE);

  public SafeFuture<?> start() {
    if (!state.compareAndSet(State.IDLE, State.RUNNING)) {
      return SafeFuture.failedFuture(
          new IllegalStateException("Attempt to start an already started service."));
    }
    return doStart();
  }

  protected abstract SafeFuture<?> doStart();

  public SafeFuture<?> stop() {
    if (state.compareAndSet(State.RUNNING, State.STOPPED)) {
      return doStop();
    } else {
      // Return a successful future if there's nothing to do at this point
      return FALLBACK_STOPPED_FUTURE;
    }
  }

  protected abstract SafeFuture<?> doStop();
}
