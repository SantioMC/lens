package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.Counter;
import me.santio.lens.Lens;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;

import java.util.Locale;
import java.util.Map;

/**
 * Counts the responses players give to resource packs, including intermediate steps such as accepting
 * and downloading the pack
 * @author santio
 */
@AutoService(Listener.class)
public class PlayerResourcePackMetric implements Listener {

    private static final String METRIC_NAME = "lens.players.resourcepack";

    public PlayerResourcePackMetric() {
        for (Status status : Status.values()) {
            this.counter(status);
        }
    }

    private Counter counter(Status status) {
        return Lens.instance().counter(METRIC_NAME, Map.of(
            "status", status.name().toLowerCase(Locale.ROOT)
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onStatus(PlayerResourcePackStatusEvent event) {
        this.counter(event.getStatus()).increment();
    }

}
