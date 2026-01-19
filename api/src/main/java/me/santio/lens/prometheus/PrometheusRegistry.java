package me.santio.lens.prometheus;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;

@UtilityClass
@Accessors(fluent = true)
public class PrometheusRegistry {
    
    @Getter
    private final PrometheusMeterRegistry meterRegistry =
        new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    
}
