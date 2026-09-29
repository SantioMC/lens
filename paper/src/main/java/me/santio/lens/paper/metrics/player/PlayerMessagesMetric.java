package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.santio.lens.Lens;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;

@AutoService(Listener.class)
public class PlayerMessagesMetric implements Listener {

    private static final String METRIC_NAME = "lens.players.messages";

    private Map<String, String> tags(Player player) {
        return Map.of("player", player.getUniqueId().toString());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onMessage(AsyncChatEvent event) {
        Lens.instance().counter(METRIC_NAME, this.tags(event.getPlayer())).increment();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onQuit(PlayerQuitEvent event) {
        Lens.instance().remove(METRIC_NAME, this.tags(event.getPlayer()));
    }

}
