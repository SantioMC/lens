package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import me.santio.lens.meter.GroupedGauge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.Map;
import java.util.UUID;

@AutoService(Listener.class)
public class PlayerBedrockMetric implements Listener {

    private final GroupedGauge<UUID> devices = new GroupedGauge<>("lens.players.floodgate");

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

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerJoin(PlayerJoinEvent event) {
        if (!Bukkit.getServer().getPluginManager().isPluginEnabled("floodgate")) return;

        this.devices.put(event.getPlayer().getUniqueId(), Map.of(
            "bedrock", "" + this.isBedrock(event.getPlayer()),
            "device", this.getDevice(event.getPlayer())
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerQuit(PlayerQuitEvent event) {
        this.devices.remove(event.getPlayer().getUniqueId());
    }

}
