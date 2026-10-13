package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import me.santio.lens.Lens;
import me.santio.lens.paper.LensPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks how long players stay connected for, sessions are recorded once the player disconnects.
 * Sessions shorter than the configured bounce threshold are additionally counted as bounces.
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

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final Duration bounceThreshold = Duration.ofSeconds(
        LensPlugin.instance().getConfig().getLong("bounce-threshold", 30)
    );

    public PlayerSessionMetric() {
        for (boolean firstJoin : new boolean[] {true, false}) {
            this.timer(firstJoin);
            this.bounces(firstJoin);
        }
    }

    private Map<String, String> tags(boolean firstJoin) {
        return Map.of("first_join", String.valueOf(firstJoin));
    }

    private Timer timer(boolean firstJoin) {
        return Lens.instance().timer(SESSION_METRIC, this.tags(firstJoin), BUCKETS);
    }

    private Counter bounces(boolean firstJoin) {
        return Lens.instance().counter(BOUNCE_METRIC, this.tags(firstJoin));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final Session session = new Session(System.nanoTime(), !player.hasPlayedBefore());

        this.sessions.put(player.getUniqueId(), session);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerQuit(PlayerQuitEvent event) {
        final @Nullable Session session = this.sessions.remove(event.getPlayer().getUniqueId());
        if (session == null) return;

        final Duration duration = Duration.ofNanos(System.nanoTime() - session.joinedAt());
        this.timer(session.firstJoin()).record(duration);

        if (duration.compareTo(this.bounceThreshold) >= 0) return;

        this.bounces(session.firstJoin()).increment();
    }

    private record Session(long joinedAt, boolean firstJoin) {}

}
