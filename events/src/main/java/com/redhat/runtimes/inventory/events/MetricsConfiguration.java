/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.events;

import static com.redhat.runtimes.inventory.events.EventConsumer.CONSUMED_TIMER_NAME;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class MetricsConfiguration {

  @ConfigProperty(
      name = "metrics.event.processing.duration.minimumExpectedValue",
      defaultValue = "1")
  Integer minimumExpectedValue;

  @ConfigProperty(
      name = "metrics.event.processing.duration.maximumExpectedValue",
      defaultValue = "150")
  Integer maximumExpectedValue;

  @Produces
  @Singleton
  public MeterFilter enableHistogram() {
    return new MeterFilter() {
      @Override
      public DistributionStatisticConfig configure(
          Meter.Id id, DistributionStatisticConfig config) {
        if (id.getName().startsWith(CONSUMED_TIMER_NAME)) {
          return DistributionStatisticConfig.builder()
              .percentilesHistogram(true)
              .minimumExpectedValue(minimumExpectedValue * 1_000_000d)
              .maximumExpectedValue(maximumExpectedValue * 1_000_000d)
              .build()
              .merge(config);
        }
        return config;
      }
    };
  }
}
