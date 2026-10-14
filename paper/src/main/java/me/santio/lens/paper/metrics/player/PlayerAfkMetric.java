package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.MeterRegistry;
import me.santio.lens.Lens;
import me.santio.lens.model.Metric;
import me.santio.lens.paper.LensPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Counts the online players who have been idle for longer than the configured afk threshold
 * @author santio
 */
@AutoService(Metric.class)
public class PlayerAfkMetric implements Metric {

    private final Duration threshold = Duration.ofSeconds(
        LensPlugin.instance().getConfig().getLong("afk-threshold", 300)
    );

    @Override
    public void initialize(Lens lens, MeterRegistry registry) {
        lens.gauge("lens.players.afk", this::countAfk);
    }

    private boolean isAfk(Player player) {
        return player.getIdleDuration().compareTo(this.threshold) >= 0;
    }

    private double countAfk() {
        return Bukkit.getOnlinePlayers().stream()
            .filter(this::isAfk)
            .count();
    }

}
