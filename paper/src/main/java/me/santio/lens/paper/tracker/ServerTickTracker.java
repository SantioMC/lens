package me.santio.lens.paper.tracker;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

/**
 * Samples the server's tick statistics on the global region, as Folia only allows reading them from a
 * ticking region. On Paper the global region is the main thread, so the values are identical to the server's.
 * @author santio
 */
@Accessors(fluent = true)
public final class ServerTickTracker {
    @Getter
    private static final ServerTickTracker instance = new ServerTickTracker();
    private ServerTickTracker() {}

    private static final long SAMPLE_INTERVAL_TICKS = 20L;

    private @Nullable ScheduledTask task;

    @Getter private volatile double tps = 20.0;
    @Getter private volatile double mspt = 0.0;

    /**
     * Starts sampling tick statistics
     * @param plugin The plugin owning the sampling task
     */
    public void start(Plugin plugin) {
        if (this.task != null) return;

        this.task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
            plugin,
            (scheduledTask) -> this.sample(),
            1L,
            SAMPLE_INTERVAL_TICKS
        );
    }

    /**
     * Stops sampling tick statistics
     */
    public void stop() {
        if (this.task == null) return;

        this.task.cancel();
        this.task = null;
    }

    private void sample() {
        this.tps = Bukkit.getServer().getTPS()[0];
        this.mspt = Bukkit.getServer().getAverageTickTime();
    }
}
