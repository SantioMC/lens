package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import me.santio.lens.Lens;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;

@AutoService(Listener.class)
public class PlayerHostnameMetric implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPlayedBefore()) return;
        
        final String hostname = event.getPlayer().getVirtualHost() != null
            ? event.getPlayer().getVirtualHost().getHostName()
            : "unknown";
        
        Lens.instance().counter("lens.players.hostnames", Map.of(
            "hostname", hostname
        )).increment();
    }
    
}
