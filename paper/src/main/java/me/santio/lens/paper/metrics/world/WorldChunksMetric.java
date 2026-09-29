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
public class WorldChunksMetric implements ResponsiveMetric {

    private @MonotonicNonNull MultiGauge gauge;

    @Override
    public void initialize(Lens lens, MeterRegistry registry) {
        this.gauge = MultiGauge.builder("lens.world.chunks")
            .description("Shows the amount of loaded chunks in each world")
            .register(registry);
    }

    @Override
    public void onUpdate() {
        final Collection<MultiGauge.Row<?>> rows = new ArrayList<>();

        for (var entry : WorldTracker.instance().chunkCounts().entrySet()) {
            rows.add(MultiGauge.Row.of(
                Tags.of(
                    "uuid", entry.getKey().uniqueId().toString(),
                    "name", entry.getKey().name()
                ),
                entry.getValue()
            ));
        }

        this.gauge.register(rows, true);
    }
}
