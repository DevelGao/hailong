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

package tech.devgao.hailong.metrics;

import static java.util.stream.Collectors.toSet;
import static tech.devgao.hailong.util.async.SafeFuture.reportExceptions;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Vertx;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.hyperledger.besu.metrics.StandardMetricCategory;
import org.hyperledger.besu.metrics.prometheus.MetricsConfiguration;
import org.hyperledger.besu.metrics.prometheus.MetricsService;
import org.hyperledger.besu.metrics.prometheus.PrometheusMetricsSystem;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.MetricCategory;
import tech.devgao.hailong.util.config.HailongConfiguration;

public class MetricsEndpoint {

  private final Optional<MetricsService> metricsService;
  private final MetricsSystem metricsSystem;
  private static final Map<String, MetricCategory> SUPPORTED_CATEGORIES;

  static {
    final ImmutableMap.Builder<String, MetricCategory> builder = ImmutableMap.builder();
    addCategories(builder, StandardMetricCategory.class);
    addCategories(builder, HailongMetricCategory.class);
    SUPPORTED_CATEGORIES = builder.build();
  }

  public MetricsEndpoint(final HailongConfiguration hailongConfig, final Vertx vertx) {
    final MetricsConfiguration metricsConfig = createMetricsConfiguration(hailongConfig);
    metricsSystem = PrometheusMetricsSystem.init(metricsConfig);
    if (metricsConfig.isEnabled()) {
      metricsService = Optional.of(MetricsService.create(vertx, metricsConfig, metricsSystem));
    } else {
      metricsService = Optional.empty();
    }
  }

  public void start() {
    metricsService.ifPresent(reportExceptions(MetricsService::start));
  }

  public void stop() {
    metricsService.ifPresent(reportExceptions(MetricsService::stop));
  }

  public MetricsSystem getMetricsSystem() {
    return metricsSystem;
  }

  private MetricsConfiguration createMetricsConfiguration(
      final HailongConfiguration hailongConfig) {
    return MetricsConfiguration.builder()
        .enabled(hailongConfig.isMetricsEnabled())
        .port(hailongConfig.getMetricsPort())
        .host(hailongConfig.getMetricsNetworkInterface())
        .metricCategories(getEnabledMetricCategories(hailongConfig))
        .build();
  }

  private Set<MetricCategory> getEnabledMetricCategories(final HailongConfiguration hailongConfig) {
    return hailongConfig.getMetricCategories().stream()
        .map(SUPPORTED_CATEGORIES::get)
        .filter(Objects::nonNull)
        .collect(toSet());
  }

  private static <T extends Enum<T> & MetricCategory> void addCategories(
      ImmutableMap.Builder<String, MetricCategory> builder, Class<T> categoryEnum) {
    EnumSet.allOf(categoryEnum).forEach(category -> builder.put(category.name(), category));
  }
}
