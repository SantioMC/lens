package me.santio.lens.paper.tracker;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Samples the tick statistics of the region each online player is in. Tasks ran on a player's entity scheduler
 * execute on the region owning that player, where Folia reports that region's statistics instead of the server's.
 * Regions without players are not sampled, as the API has no way to reference them.
 * @author santio
 */
@Accessors(fluent = true)
public final class RegionTracker implements Listener {
    @Getter
    private static final RegionTracker instance = new RegionTracker();
    private RegionTracker() {}

    private static final long SAMPLE_INTERVAL_TICKS = 20L;

    private @Nullable ScheduledTask task;

    private final Map<UUID, RegionSample> samples = new ConcurrentHashMap<>();
    private final Map<UUID, Long> pending = new ConcurrentHashMap<>();

    /**
     * Starts sampling the regions of online players
     * @param plugin The plugin owning the sampling tasks
     */
    public void start(Plugin plugin) {
        if (this.task != null) return;

        this.task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
            plugin,
            (scheduledTask) -> this.sampleAll(plugin),
            1L,
            SAMPLE_INTERVAL_TICKS
        );
    }

    /**
     * Stops sampling and removes all collected samples
     */
    public void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }

        this.samples.clear();
        this.pending.clear();
    }

    /**
     * @return The latest sample of the region each online player is in
     */
    public Collection<RegionSample> samples() {
        return this.samples.values();
    }

    /**
     * Gets how long the oldest sampling task has been waiting to run, a region that stops ticking
     * never runs its tasks, so this grows while a region is stalled
     * @return The wait time in milliseconds, or empty if no tasks are waiting
     */
    public OptionalLong oldestPendingMillis() {
        final long now = System.nanoTime();

        return this.pending.values().stream()
            .mapToLong(scheduledAt -> TimeUnit.NANOSECONDS.toMillis(now - scheduledAt))
            .max();
    }

    private void sampleAll(Plugin plugin) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.sample(plugin, player);
        }
    }

    private void sample(Plugin plugin, Player player) {
        final UUID uniqueId = player.getUniqueId();
        final long scheduledAt = System.nanoTime();

        if (this.pending.putIfAbsent(uniqueId, scheduledAt) != null) return;

        final @Nullable ScheduledTask scheduled = player.getScheduler().run(
            plugin,
            (scheduledTask) -> this.record(uniqueId, scheduledAt),
            () -> this.forget(uniqueId)
        );

        if (scheduled == null) {
            this.forget(uniqueId);
        }
    }

    private void record(UUID uniqueId, long scheduledAt) {
        final double latency = (System.nanoTime() - scheduledAt) / 1_000_000.0;
        final double tps = Bukkit.getServer().getTPS()[0];
        final double mspt = Bukkit.getServer().getAverageTickTime();

        this.samples.put(uniqueId, new RegionSample(tps, mspt, latency));
        this.pending.remove(uniqueId);
    }

    private void forget(UUID uniqueId) {
        this.samples.remove(uniqueId);
        this.pending.remove(uniqueId);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onQuit(PlayerQuitEvent event) {
        this.forget(event.getPlayer().getUniqueId());
    }

    /**
     * A snapshot of the statistics of a region
     * @param tps The region's ticks per second over the last minute
     * @param mspt The region's average milliseconds per tick over the last 5 seconds
     * @param latencyMillis How long a task took to start running on the region after being scheduled
     */
    public record RegionSample(
        double tps,
        double mspt,
        double latencyMillis
    ) {}
}
