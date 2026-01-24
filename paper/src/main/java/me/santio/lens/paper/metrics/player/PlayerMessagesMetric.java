package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.santio.lens.ext.ResponsiveMeterBinder;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Accessors(fluent = true)
@AutoService(MeterBinder.class)
public class PlayerMessagesMetric implements ResponsiveMeterBinder {
    
    @Getter
    private static PlayerMessagesMetric instance;
    private final Map<PlayerKey, LongAdder> counts = new ConcurrentHashMap<>();
    private @MonotonicNonNull MultiGauge gauge;
    
    @SuppressWarnings({"ThisEscapedInObjectConstruction", "AssignmentToStaticFieldFromInstanceMethod"})
    public PlayerMessagesMetric() {
        instance = this;
    }
    
    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        this.gauge = MultiGauge.builder("lens.player.messages")
            .description("Shows the amount of chat messages players have sent to the server")
            .register(registry);
    }
    
    @Override
    public void onUpdate() {
        final Collection<MultiGauge.Row<?>> rows = new ArrayList<>();
        
        for (var entry : counts.entrySet()) {
            rows.add(MultiGauge.Row.of(
                Tags.of(
                    "username", entry.getKey().name(),
                    "uuid", entry.getKey().uuid()
                ),
                entry.getValue().sum()
            ));
        }
        
        this.gauge.register(rows, true);
    }
    
    public void increment(Player player) {
        final PlayerKey playerKey = new PlayerKey(player.getName(), player.getUniqueId().toString());
        counts.computeIfAbsent(playerKey, key -> new LongAdder()).increment();
    }
    
    private record PlayerKey(
        String name,
        String uuid
    ) {}
    
}
