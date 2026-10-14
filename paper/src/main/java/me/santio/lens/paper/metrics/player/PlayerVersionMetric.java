package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import me.santio.lens.paper.LensPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;

@AutoService(Listener.class)
public class PlayerVersionMetric extends PlayerGroupMetric {

    public PlayerVersionMetric() {
        super("lens.players.version");
    }

    @Override
    protected @Nullable Map<String, String> tags(Player player) {
        if (!Bukkit.getServer().getPluginManager().isPluginEnabled("ViaVersion")) return null;

        final ProtocolVersion version = Via.getAPI().getPlayerProtocolVersion(player.getUniqueId());

        if (!version.isKnown()) {
            LensPlugin.instance().getLogger().warning("Failed to get protocol version for " + player.getName() + ", they will be skipped in metrics");
            return null;
        }

        return Map.of("version", version.getName());
    }

}
