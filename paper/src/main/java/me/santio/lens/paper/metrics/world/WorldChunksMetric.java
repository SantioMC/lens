package me.santio.lens.paper.metrics.world;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import me.santio.lens.ext.ResponsiveMeterBinder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;

@AutoService(MeterBinder.class)
public class WorldChunksMetric implements ResponsiveMeterBinder {
    
    private @MonotonicNonNull MultiGauge gauge;
    
    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        this.gauge = MultiGauge.builder("lens.world.chunks")
            .description("Shows the amount of loaded chunks in each world")
            .register(registry);
    }
    
    @Override
    public void onUpdate() {
        final Collection<MultiGauge.Row<?>> rows = new ArrayList<>();
        
        for (World world : Bukkit.getWorlds()) {
            rows.add(MultiGauge.Row.of(
                Tags.of(
                    "uuid", world.getUID().toString(),
                    "name", world.getName()
                ),
                world.getLoadedChunks().length
            ));
        }
        
        this.gauge.register(rows, true);
    }
}
