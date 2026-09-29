package me.santio.lens.paper.metrics.server;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.MeterRegistry;
import me.santio.lens.Lens;
import me.santio.lens.model.Metric;
import me.santio.lens.paper.tracker.ServerTickTracker;

@AutoService(Metric.class)
public class ServerTPSMetric implements Metric {

    @Override
    public void initialize(Lens lens, MeterRegistry registry) {
        lens.gauge("lens.server.tps", () -> {
            final double tps = ServerTickTracker.instance().tps();
            return tps > 19.99 ? 20.0 : tps;
        });
    }

}
