package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.Counter;
import me.santio.lens.Lens;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.Locale;
import java.util.Map;

/**
 * Counts player teleports by their cause, this includes travelling through portals and end gateways
 * @author santio
 */
@AutoService(Listener.class)
public class PlayerTeleportMetric implements Listener {

    private static final String METRIC_NAME = "lens.players.teleports";

    public PlayerTeleportMetric() {
        for (TeleportCause cause : TeleportCause.values()) {
            this.counter(cause);
        }
    }

    private Counter counter(TeleportCause cause) {
        return Lens.instance().counter(METRIC_NAME, Map.of(
            "cause", cause.name().toLowerCase(Locale.ROOT)
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onTeleport(PlayerTeleportEvent event) {
        this.counter(event.getCause()).increment();
    }

}
