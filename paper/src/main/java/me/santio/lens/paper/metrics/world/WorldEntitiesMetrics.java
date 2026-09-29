package me.santio.lens.paper.metrics.world;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import me.santio.lens.Lens;
import me.santio.lens.model.Metric;
import me.santio.lens.model.ResponsiveMetric;
import me.santio.lens.paper.tracker.WorldTracker;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.ArrayList;
import java.util.Collection;

@AutoService(Metric.class)
public class WorldEntitiesMetrics implements ResponsiveMetric {

    private @MonotonicNonNull MultiGauge gauge;

    @Override
    public void initialize(Lens lens, MeterRegistry registry) {
        this.gauge = MultiGauge.builder("lens.world.entities")
            .description("Shows the amount of loaded entities in each world")
            .register(registry);
    }

    @Override
    public void onUpdate() {
        final Collection<MultiGauge.Row<?>> rows = new ArrayList<>();

        for (var entry : WorldTracker.instance().entityCounts().entrySet()) {
            rows.add(MultiGauge.Row.of(
                Tags.of(
                    "world", entry.getKey().world().name(),
                    "type", entry.getKey().entityType()
                ),
                entry.getValue()
            ));
        }

        this.gauge.register(rows, true);
    }

}
