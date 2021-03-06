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

package tech.devgao.artemis.services.chainstorage;

import com.google.common.eventbus.EventBus;
import tech.devgao.artemis.service.serviceutils.ServiceConfig;
import tech.devgao.artemis.service.serviceutils.ServiceInterface;
import tech.devgao.artemis.storage.ChainStorage;
import tech.devgao.artemis.storage.ChainStorageServer;
import tech.devgao.artemis.util.alogger.ALogger;

public class ChainStorageService implements ServiceInterface {
  private EventBus eventBus;
  private ChainStorageServer chainStore;
  private static final ALogger LOG = new ALogger(ChainStorageService.class.getName());

  public ChainStorageService() {}

  @Override
  public void init(ServiceConfig config) {
    this.eventBus = config.getEventBus();
    this.chainStore = ChainStorage.Create(ChainStorageServer.class, eventBus);
    this.eventBus.register(this);
  }

  @Override
  public void run() {
    // TODO Still do something...maybe
  }

  @Override
  public void stop() {
    this.eventBus.unregister(this);
  }
}
