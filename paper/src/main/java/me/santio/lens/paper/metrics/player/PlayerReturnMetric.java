package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.Counter;
import me.santio.lens.Lens;
import org.bukkit.Statistic;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;
import java.util.Set;

/**
 * Counts players reaching a return milestone, a player coming back for the third time increments the
 * {@code 3} milestone once. The amount of previous visits is read from the vanilla leave game statistic.
 * @author santio
 */
@AutoService(Listener.class)
public class PlayerReturnMetric implements Listener {

    private static final String METRIC_NAME = "lens.players.returns";
    private static final Set<Integer> MILESTONES = Set.of(1, 2, 3, 4, 5, 10, 25, 50, 100);

    public PlayerReturnMetric() {
        for (int milestone : MILESTONES) {
            this.counter(milestone);
        }
    }

    private Counter counter(int milestone) {
        return Lens.instance().counter(METRIC_NAME, Map.of(
            "times", String.valueOf(milestone)
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerJoin(PlayerJoinEvent event) {
        final int returns = event.getPlayer().getStatistic(Statistic.LEAVE_GAME);
        if (!MILESTONES.contains(returns)) return;

        this.counter(returns).increment();
    }

}
