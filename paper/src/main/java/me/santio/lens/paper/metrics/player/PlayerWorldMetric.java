package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.util.Map;

/**
 * Groups online players by the world they are currently in
 * @author santio
 */
@AutoService(Listener.class)
public class PlayerWorldMetric extends PlayerGroupMetric {

    public PlayerWorldMetric() {
        super("lens.players.world");
    }

    @Override
    protected Map<String, String> tags(Player player) {
        return Map.of("world", player.getWorld().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onWorldChange(PlayerChangedWorldEvent event) {
        this.track(event.getPlayer());
    }

}
