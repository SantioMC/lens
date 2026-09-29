package me.santio.lens.paper.metrics.server;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import me.santio.lens.Lens;
import me.santio.lens.model.ResponsiveMetric;
import me.santio.lens.paper.tracker.RegionTracker;
import me.santio.lens.paper.tracker.RegionTracker.RegionSample;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Exposes the statistics of the regions players are in, only available on Folia
 * @see RegionTracker
 * @author santio
 */
public class FoliaRegionMetrics implements ResponsiveMetric {

    private static final double LAGGING_TPS_THRESHOLD = 18.0;

    private @MonotonicNonNull MultiGauge tpsGauge;
    private @MonotonicNonNull MultiGauge msptGauge;
    private @MonotonicNonNull MultiGauge latencyGauge;

    @Override
    public void initialize(Lens lens, MeterRegistry registry) {
        this.tpsGauge = MultiGauge.builder("lens.folia.region.tps")
            .description("The min, average and max TPS of regions containing players, weighted by player")
            .register(registry);

        this.msptGauge = MultiGauge.builder("lens.folia.region.mspt")
            .description("The min, average and max MSPT of regions containing players, weighted by player")
            .register(registry);

        this.latencyGauge = MultiGauge.builder("lens.folia.region.latency")
            .description("The min, average and max milliseconds a task waits before running on a player's region")
            .register(registry);

        lens.gauge("lens.folia.region.lagging.players", () -> {
            return (double) RegionTracker.instance().samples().stream()
                .filter(sample -> sample.tps() < LAGGING_TPS_THRESHOLD)
                .count();
        });

        lens.gauge("lens.folia.region.stalled.millis", () -> {
            return (double) RegionTracker.instance().oldestPendingMillis().orElse(0L);
        });
    }

    @Override
    public void onUpdate() {
        final Collection<RegionSample> samples = RegionTracker.instance().samples();

        this.tpsGauge.register(this.statistics(samples, RegionSample::tps), true);
        this.msptGauge.register(this.statistics(samples, RegionSample::mspt), true);
        this.latencyGauge.register(this.statistics(samples, RegionSample::latencyMillis), true);
    }

    private List<MultiGauge.Row<?>> statistics(Collection<RegionSample> samples, ToDoubleFunction<RegionSample> mapper) {
        final DoubleSummaryStatistics statistics = samples.stream().mapToDouble(mapper).summaryStatistics();
        if (statistics.getCount() == 0) return List.of();

        return List.of(
            MultiGauge.Row.of(Tags.of("stat", "min"), statistics.getMin()),
            MultiGauge.Row.of(Tags.of("stat", "avg"), statistics.getAverage()),
            MultiGauge.Row.of(Tags.of("stat", "max"), statistics.getMax())
        );
    }

}
