package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import me.santio.lens.meter.GroupedGauge;
import me.santio.lens.paper.LensPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;

@AutoService(Listener.class)
public class PlayerVersionMetric implements Listener {

    private final GroupedGauge<UUID> versions = new GroupedGauge<>("lens.players.version");

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerJoin(PlayerJoinEvent event) {
        if (!Bukkit.getServer().getPluginManager().isPluginEnabled("ViaVersion")) return;

        final Player player = event.getPlayer();
        final ProtocolVersion version = Via.getAPI().getPlayerProtocolVersion(player.getUniqueId());

        if (!version.isKnown()) {
            LensPlugin.instance().getLogger().warning("Failed to get protocol version for " + player.getName() + ", they will be skipped in metrics");
            return;
        }

        this.versions.put(player.getUniqueId(), Map.of(
            "version", version.getName()
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerQuit(PlayerQuitEvent event) {
        this.versions.remove(event.getPlayer().getUniqueId());
    }

}
