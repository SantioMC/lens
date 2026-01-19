package me.santio.lens.paper.metrics.world;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import me.santio.lens.ext.ResponsiveMeterBinder;
import me.santio.lens.paper.tracker.AsyncEntityTracker;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@AutoService(MeterBinder.class)
public class WorldEntitiesMetrics implements ResponsiveMeterBinder {
    
    private @MonotonicNonNull MultiGauge gauge;
    
    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        this.gauge = MultiGauge.builder("lens.world.entities")
            .description("Shows the amount of loaded entities in each world")
            .register(registry);
    }
    
    @SuppressWarnings({"FeatureEnvy", "MethodWithMultipleLoops"})
    @Override
    public void onUpdate() {
        final Collection<MultiGauge.Row<?>> rows = new ArrayList<>();
        final Map<EntityKey, Integer> counts = new HashMap<>();
        
        for (var entry : AsyncEntityTracker.instance().entities().entrySet()) {
            final @Nullable Entity entity = entry.getValue().get();
            if (entity == null) {
                AsyncEntityTracker.instance().untrack(entry.getKey());
                continue;
            }
            
            final EntityKey key = new EntityKey(entity.getWorld().getName(), entity.getType().getKey().asString());
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }
        
        for (var entry : counts.entrySet()) {
            rows.add(MultiGauge.Row.of(
                Tags.of(
                    "world", entry.getKey().world(),
                    "type", entry.getKey().entityType()
                ),
                entry.getValue()
            ));
        }
        
        this.gauge.register(rows, true);
    }
    
    private record EntityKey(
        String world,
        String entityType
    ) {}
}
