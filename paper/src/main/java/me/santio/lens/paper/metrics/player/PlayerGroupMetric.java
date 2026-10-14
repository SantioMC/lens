package me.santio.lens.paper.metrics.player;

import me.santio.lens.meter.GroupedGauge;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * Groups online players by a set of tags, exposing the amount of players currently in each group.
 * Players are grouped when they join and removed when they quit, implementations may regroup a player
 * at any time through {@link #track(Player)}.
 * @author santio
 */
public abstract class PlayerGroupMetric implements Listener {

    private final GroupedGauge<UUID> gauge;

    protected PlayerGroupMetric(String name) {
        this.gauge = new GroupedGauge<>(name);
    }

    /**
     * Gets the tags a player should be grouped under
     * @param player The player to get the tags for
     * @return The tags of the group, or null if the player should not be tracked
     */
    protected abstract @Nullable Map<String, String> tags(Player player);

    /**
     * Groups the player under the provided tags, replacing the group they were previously in
     * @param player The player to group
     * @param tags The tags of the group, nothing happens if this is null
     */
    protected void track(Player player, @Nullable Map<String, String> tags) {
        if (tags == null) return;

        this.gauge.put(player.getUniqueId(), tags);
    }

    /**
     * Groups the player under their current tags, replacing the group they were previously in
     * @param player The player to group
     */
    protected void track(Player player) {
        this.track(player, this.tags(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGroupedPlayerJoin(PlayerJoinEvent event) {
        this.track(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGroupedPlayerQuit(PlayerQuitEvent event) {
        this.gauge.remove(event.getPlayer().getUniqueId());
    }

}
