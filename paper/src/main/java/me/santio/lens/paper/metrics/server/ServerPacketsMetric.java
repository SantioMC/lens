package me.santio.lens.paper.metrics.server;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.santio.lens.Lens;
import me.santio.lens.model.ResponsiveMetric;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Accessors(fluent = true)
public class ServerPacketsMetric implements ResponsiveMetric, PacketListener {
    
    @Getter
    private static final ServerPacketsMetric instance = new ServerPacketsMetric();
    private final Map<PacketKey, LongAdder> counts = new ConcurrentHashMap<>();
    private @MonotonicNonNull MultiGauge gauge;
    
    @Override
    public void initialize(Lens lens, MeterRegistry registry) {
        this.gauge = MultiGauge.builder("lens.server.packets")
            .description("Shows the amount of ingoing and outgoing packets")
            .register(registry);
    }
    
    @Override
    public void onUpdate() {
        final Collection<MultiGauge.Row<?>> rows = new ArrayList<>();
        
        for (var entry : counts.entrySet()) {
            String uniqueId = entry.getKey().uuid();
            if (uniqueId == null) uniqueId = "null";
            
            rows.add(MultiGauge.Row.of(
                Tags.of(
                    "packet_type", entry.getKey().name(),
                    "side", entry.getKey().side(),
                    "user", uniqueId
                ),
                entry.getValue().sum()
            ));
        }
        
        this.gauge.register(rows, true);
    }
    
    @Override
    public void onPacketSend(@NonNull PacketSendEvent event) {
        if (event.isCancelled()) return;
        counts.computeIfAbsent(PacketKey.create(event), key -> new LongAdder()).increment();
    }
    
    @Override
    public void onPacketReceive(@NonNull PacketReceiveEvent event) {
        if (event.isCancelled()) return;
        counts.computeIfAbsent(PacketKey.create(event), key -> new LongAdder()).increment();
    }
    
    private record PacketKey(
        String name,
        String side,
        @Nullable String uuid
    ) {
        
        @SuppressWarnings("ConstantValue") // not constant, can be null
        static PacketKey create(ProtocolPacketEvent event) {
            final @Nullable UUID uniqueId = event.getUser().getUUID();
            
            return new PacketKey(
                event.getPacketType().getName(),
                event.getPacketType().getSide().name(),
                uniqueId == null ? null : uniqueId.toString()
            );
        }
        
    }
    
}
