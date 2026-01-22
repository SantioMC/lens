package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.santio.lens.ext.ResponsiveMeterBinder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@AutoService(MeterBinder.class)
@Accessors(fluent = true)
@Getter
public class PlayerPingMetric implements ResponsiveMeterBinder {
    
    @Getter
    private static final PlayerPingMetric instance = new PlayerPingMetric();
    
    private @MonotonicNonNull MultiGauge gauge;
    private final Map<UUID, MultiGauge.Row<?>> rows = new HashMap<>();
    
    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        this.gauge = MultiGauge.builder("lens.player.ping")
            .description("Shows the latency players have to the server")
            .register(registry);
    }
    
    @Override
    public void onUpdate() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.updatePlayer(player);
        }
        
        this.gauge.register(this.rows.values(), true);
    }
    
    private void updatePlayer(Player player) {
        final String hostname = player.getVirtualHost() != null
            ? player.getVirtualHost().getHostName()
            : "unknown";
        
        boolean isBedrock = false;
        String device = "PC";
        String version = "server-compatible";
        
        if (Bukkit.getServer().getPluginManager().isPluginEnabled("floodgate")) {
            final FloodgateApi api = FloodgateApi.getInstance();
            isBedrock = api.isFloodgatePlayer(player.getUniqueId());
            
            if (isBedrock) {
                final FloodgatePlayer floodgatePlayer = api.getPlayer(player.getUniqueId());
                device = floodgatePlayer.getDeviceOs().toString();
            }
        }
        
        if (Bukkit.getServer().getPluginManager().isPluginEnabled("ViaVersion")) {
            final @Nullable ProtocolVersion protocolVersion = Via.getAPI().getPlayerProtocolVersion(player.getUniqueId());
            if (protocolVersion != null) version = protocolVersion.getName();
        }
        
        this.rows.put(player.getUniqueId(), MultiGauge.Row.of(
            Tags.of(
                "uuid", player.getUniqueId().toString(),
                "username", player.getName(),
                "bedrock", "" + isBedrock,
                "device", device,
                "version", version,
                "hostname", hostname,
                "firstJoin", "" + (!player.hasPlayedBefore())
            ),
            player.getPing()
        ));
    }
    
    public void removePlayer(Player player) {
        this.rows.remove(player.getUniqueId());
    }
    
}
