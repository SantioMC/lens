package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import me.santio.lens.Lens;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@AutoService(Listener.class)
public class PlayerFirstJoinMetric implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPlayedBefore()) return;
        Lens.instance().counter("lens.players.firstJoin").increment();
    }
    
}
