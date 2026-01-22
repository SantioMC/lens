package me.santio.lens.paper.listener;

import com.google.auto.service.AutoService;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.santio.lens.paper.metrics.player.PlayerDisconnectMetrics;
import me.santio.lens.paper.metrics.player.PlayerMessagesMetric;
import me.santio.lens.paper.metrics.player.PlayerPingMetric;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@AutoService(Listener.class)
public class PlayerListener implements Listener {
    
    private final PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
    
    @EventHandler(priority = EventPriority.MONITOR)
    private void onQuit(PlayerQuitEvent event) {
        PlayerPingMetric.instance().removePlayer(event.getPlayer());
        
        final PlayerQuitEvent.QuitReason quitReason = event.getReason();
        if (quitReason != PlayerQuitEvent.QuitReason.KICKED) { // We will use the kick event for this
            final String sanitized = event.getReason().name().toLowerCase().replaceAll("_", " ");
            PlayerDisconnectMetrics.instance().track(sanitized);
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onKick(PlayerKickEvent event) {
        final String reason = serializer.serialize(event.reason());
        PlayerDisconnectMetrics.instance().track("kicked - " + reason);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onMessage(AsyncChatEvent event) {
        PlayerMessagesMetric.instance().increment(event.getPlayer());
    }
    
}
