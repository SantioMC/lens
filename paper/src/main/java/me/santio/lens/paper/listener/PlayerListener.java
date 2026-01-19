package me.santio.lens.paper.listener;

import com.google.auto.service.AutoService;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.santio.lens.paper.metrics.player.PlayerMessagesMetric;
import me.santio.lens.paper.metrics.player.PlayerPingMetric;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@AutoService(Listener.class)
public class PlayerListener implements Listener {
    
    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        PlayerPingMetric.instance().removePlayer(event.getPlayer());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onMessage(AsyncChatEvent event) {
        PlayerMessagesMetric.instance().increment(event.getPlayer());
    }
    
}
