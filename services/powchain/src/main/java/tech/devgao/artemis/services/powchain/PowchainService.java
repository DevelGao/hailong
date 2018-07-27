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

package tech.devgao.artemis.services.powchain;

import com.google.common.eventbus.EventBus;
import tech.devgao.artemis.pow.ValidatorRegistrationClient;
import tech.devgao.artemis.services.ServiceInterface;

public class PowchainService implements ServiceInterface {

  private EventBus eventBus;
  private final ValidatorRegistrationClient vrc;

  public PowchainService() {
    this.vrc = new ValidatorRegistrationClient(eventBus);
  }

  @Override
  public void init(EventBus eventBus) {
    this.eventBus = eventBus;
    this.eventBus.register(this);
  }

  @Override
  public void run() {
    this.vrc.listenToPoWChain();
  }

  @Override
  public void stop() {
    this.eventBus.unregister(this);
  }
}