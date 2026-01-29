package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import me.santio.lens.Lens;
import me.santio.lens.meter.SimpleGauge;
import me.santio.lens.paper.LensPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@AutoService(Listener.class)
public class PlayerVersionMetric implements Listener {
    
    private final Map<String, SimpleGauge> versionGauge = new ConcurrentHashMap<>();
    private final Map<UUID, String> versionCache = new ConcurrentHashMap<>();
    
    private @Nullable SimpleGauge getVersionGauge(Player player) {
        final String version = versionCache.computeIfAbsent(player.getUniqueId(), (key) -> {
            final @Nullable ProtocolVersion protocolVersion = Via.getAPI().getPlayerProtocolVersion(player.getUniqueId());
            if (protocolVersion == null) {
                return null;
            }
            
            return protocolVersion.getName();
        });
        
        if (version == null) {
            LensPlugin.instance().getLogger().warning("Failed to get protocol version for " + player.getName() + ", they will be skipped in metrics");
            return null;
        }
        
        return this.versionGauge.computeIfAbsent(version, (key) -> {
            return Lens.instance().gauge("lens.players.version", Map.of(
                "version", version
            ));
        });
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerJoin(PlayerJoinEvent event) {
        if (!Bukkit.getServer().getPluginManager().isPluginEnabled("ViaVersion")) return;

        final @Nullable SimpleGauge gauge = this.getVersionGauge(event.getPlayer());
        if (gauge == null) return;
        
        gauge.increment();
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerQuit(PlayerQuitEvent event) {
        if (!Bukkit.getServer().getPluginManager().isPluginEnabled("ViaVersion")) return;
        
        final @Nullable SimpleGauge gauge = this.getVersionGauge(event.getPlayer());
        if (gauge == null) return;
        
        this.versionCache.remove(event.getPlayer().getUniqueId());
        gauge.decrement();
    }
    
}
