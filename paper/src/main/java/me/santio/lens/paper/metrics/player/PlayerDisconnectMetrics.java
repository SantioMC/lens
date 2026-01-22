package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.santio.lens.ext.ResponsiveMeterBinder;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@AutoService(MeterBinder.class)
@Accessors(fluent = true)
public class PlayerDisconnectMetrics implements ResponsiveMeterBinder {
    
    @Getter
    private static PlayerDisconnectMetrics instance;
    
    private @MonotonicNonNull MultiGauge gauge;
    private final Map<String, LongAdder> reasons = new ConcurrentHashMap<>();
    
    @SuppressWarnings({"ThisEscapedInObjectConstruction", "AssignmentToStaticFieldFromInstanceMethod"})
    public PlayerDisconnectMetrics() {
        instance = this;
    }
    
    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        this.gauge = MultiGauge.builder("lens.player.disconnects")
            .description("Tracks the disconnect reason for why a player might have left")
            .register(registry);
    }
    
    @Override
    public void onUpdate() {
        final Collection<MultiGauge.Row<?>> rows = new ArrayList<>();
        
        for (var entry : this.reasons.entrySet()) {
            rows.add(MultiGauge.Row.of(
                Tags.of(
                    "reason", entry.getKey()
                ),
                entry.getValue()
            ));
        }
        
        this.reasons.clear();
        this.gauge.register(rows, true);
    }
    
    public void track(String reason) {
        reasons.computeIfAbsent(reason, key -> new LongAdder()).increment();
    }
}
