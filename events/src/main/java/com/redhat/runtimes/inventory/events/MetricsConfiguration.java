/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.events;

import static com.redhat.runtimes.inventory.events.EventConsumer.CONSUMED_TIMER_NAME;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class MetricsConfiguration {

  @ConfigProperty(name = "metrics.events.slo", defaultValue = "150,500,1000,5000")
  List<Integer> slo;

  @Produces
  @Singleton
  public MeterFilter enableHistogram() {
    return new MeterFilter() {
      @Override
      public DistributionStatisticConfig configure(
          Meter.Id id, DistributionStatisticConfig config) {
        if (id.getName().startsWith(CONSUMED_TIMER_NAME)) {
          // convert to nanoseconds
          double[] values = slo.stream().map(i -> i * 1_000_000d).mapToDouble(d -> d).toArray();
          return DistributionStatisticConfig.builder()
              .serviceLevelObjectives(values)
              .build()
              .merge(config);
        }
        return config;
      }
    };
  }
}
