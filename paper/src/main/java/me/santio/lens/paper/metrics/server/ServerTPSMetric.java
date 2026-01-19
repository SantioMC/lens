package me.santio.lens.paper.metrics.server;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.bukkit.Bukkit;
import org.jspecify.annotations.NonNull;

@AutoService(MeterBinder.class)
public class ServerTPSMetric implements MeterBinder {
    
    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        Gauge.builder("lens.server.tps", () -> {
            final double tps = Bukkit.getServer().getTPS()[0];
            return tps > 19.95 ? 20.0 : tps;
        }).description("Shows the current ticks per second (TPS) on the server").register(registry);
    }
    
}
