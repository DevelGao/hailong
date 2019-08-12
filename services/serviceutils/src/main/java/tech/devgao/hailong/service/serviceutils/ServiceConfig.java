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

import com.google.common.eventbus.EventBus;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import tech.devgao.hailong.events.EventChannels;
import tech.devgao.hailong.util.config.HailongConfiguration;
import tech.devgao.hailong.util.time.TimeProvider;

public class ServiceConfig {

  private final TimeProvider timeProvider;
  private final EventBus eventBus;
  private final EventChannels eventChannels;
  private final MetricsSystem metricsSystem;
  private final HailongConfiguration config;

  public ServiceConfig(
      final TimeProvider timeProvider,
      final EventBus eventBus,
      final EventChannels eventChannels,
      final MetricsSystem metricsSystem,
      final HailongConfiguration config) {
    this.timeProvider = timeProvider;
    this.eventBus = eventBus;
    this.eventChannels = eventChannels;
    this.metricsSystem = metricsSystem;
    this.config = config;
  }

  public TimeProvider getTimeProvider() {
    return timeProvider;
  }

  public EventBus getEventBus() {
    return this.eventBus;
  }

  public EventChannels getEventChannels() {
    return eventChannels;
  }

  public HailongConfiguration getConfig() {
    return this.config;
  }

  public MetricsSystem getMetricsSystem() {
    return metricsSystem;
  }
}
