package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import me.santio.lens.Lens;
import me.santio.lens.paper.LensPlugin;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks how long players stay connected for, sessions are recorded once the player disconnects.
 * @author santio
 */
@AutoService(Listener.class)
public class PlayerSessionMetric implements Listener {

    private static final String SESSION_METRIC = "lens.players.session";
    private static final String BOUNCE_METRIC = "lens.players.bounces";
    private static final Duration[] BUCKETS = {
        Duration.ofSeconds(30),
        Duration.ofMinutes(1),
        Duration.ofMinutes(2),
        Duration.ofMinutes(5),
        Duration.ofMinutes(10),
        Duration.ofMinutes(15),
        Duration.ofMinutes(30),
        Duration.ofHours(1),
        Duration.ofHours(2),
        Duration.ofHours(3),
        Duration.ofHours(4),
        Duration.ofHours(6),
        Duration.ofHours(8)
    };

    private static final List<String> VISITS = List.of("1", "2", "3", "4", "5+");

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final Duration bounceThreshold = Duration.ofSeconds(
        LensPlugin.instance().getConfig().getLong("bounce-threshold", 30)
    );

    public PlayerSessionMetric() {
        for (String visit : VISITS) {
            this.timer(visit);
            this.bounces(visit);
        }
    }

    /**
     * Gets which visit the player is currently on, based on the amount of times they have left before
     * @param player The player to get the visit of
     * @return The visit number, visits past the last tracked visit are grouped together
     */
    private String visit(Player player) {
        // this is kinda a wacky way of doing this, but it does work fairly well and avoids persistence on our end
        final int visit = player.getStatistic(Statistic.LEAVE_GAME) + 1;
        final int index = Math.min(visit, VISITS.size()) - 1;

        return VISITS.get(index);
    }

    private Map<String, String> tags(String visit) {
        return Map.of("visit", visit);
    }

    private Timer timer(String visit) {
        return Lens.instance().timer(SESSION_METRIC, this.tags(visit), BUCKETS);
    }

    private Counter bounces(String visit) {
        return Lens.instance().counter(BOUNCE_METRIC, this.tags(visit));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final Session session = new Session(System.nanoTime(), this.visit(player));

        this.sessions.put(player.getUniqueId(), session);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerQuit(PlayerQuitEvent event) {
        final @Nullable Session session = this.sessions.remove(event.getPlayer().getUniqueId());
        if (session == null) return;

        final Duration duration = Duration.ofNanos(System.nanoTime() - session.joinedAt());
        this.timer(session.visit()).record(duration);

        if (duration.compareTo(this.bounceThreshold) >= 0) return;
        this.bounces(session.visit()).increment();
    }

    private record Session(long joinedAt, String visit) {}

}
