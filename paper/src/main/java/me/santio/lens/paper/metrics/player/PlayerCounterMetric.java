package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.MeterRegistry;
import me.santio.lens.Lens;
import me.santio.lens.model.Metric;
import org.bukkit.Bukkit;

@AutoService(Metric.class)
public class PlayerCounterMetric implements Metric {
    
    @Override
    public void initialize(Lens lens, MeterRegistry registry) {
        lens.gauge("lens.players.count", () -> {
            return (double) Bukkit.getOnlinePlayers().size();
        });
    }
    
}
