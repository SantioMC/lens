package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import me.santio.lens.Lens;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;

@AutoService(Listener.class)
public class PlayerDisconnectMetrics implements Listener {
    
    private final PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
    
    @EventHandler(priority = EventPriority.MONITOR)
    private void onQuit(PlayerQuitEvent event) {
        final PlayerQuitEvent.QuitReason quitReason = event.getReason();
        
        // We will use the kick event for this
        if (quitReason == PlayerQuitEvent.QuitReason.KICKED) {
            return;
        }
        
        final String sanitized = event.getReason().name().toLowerCase().replaceAll("_", " ");
        Lens.instance().counter("lens.players.disconnect", Map.of(
            "reason", sanitized,
            "kick", "false"
        )).increment();
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onKick(PlayerKickEvent event) {
        final String reason = serializer.serialize(event.reason());
        
        Lens.instance().counter("lens.players.disconnect", Map.of(
            "reason", reason,
            "kick", "true"
        )).increment();
    }
    
}
