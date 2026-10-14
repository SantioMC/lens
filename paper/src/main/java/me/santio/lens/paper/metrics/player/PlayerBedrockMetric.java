package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.Map;

@AutoService(Listener.class)
public class PlayerBedrockMetric extends PlayerGroupMetric {

    public PlayerBedrockMetric() {
        super("lens.players.floodgate");
    }

    private boolean isBedrock(Player player) {
        final FloodgateApi api = FloodgateApi.getInstance();
        return api.isFloodgatePlayer(player.getUniqueId());
    }

    private String getDevice(Player player) {
        final FloodgateApi api = FloodgateApi.getInstance();
        String device = "java";

        if (api.isFloodgatePlayer(player.getUniqueId())) {
            final FloodgatePlayer floodgatePlayer = api.getPlayer(player.getUniqueId());
            device = floodgatePlayer.getDeviceOs().toString();
        }

        return device;
    }

    @Override
    protected @Nullable Map<String, String> tags(Player player) {
        if (!Bukkit.getServer().getPluginManager().isPluginEnabled("floodgate")) return null;

        return Map.of(
            "bedrock", "" + this.isBedrock(player),
            "device", this.getDevice(player)
        );
    }

}
