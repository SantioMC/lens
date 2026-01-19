package me.santio.lens.paper.metrics.server;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.bukkit.Bukkit;
import org.jspecify.annotations.NonNull;

@AutoService(MeterBinder.class)
public class ServerTPSTickTimes implements MeterBinder {
    
    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        Gauge.builder("lens.server.mspt", () -> {
            return Bukkit.getServer().getAverageTickTime();
        }).description("Shows the average millisecond duration per each tick time (mspt)").register(registry);
    }
    
}
