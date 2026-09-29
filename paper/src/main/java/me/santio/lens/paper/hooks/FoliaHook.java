package me.santio.lens.paper.hooks;

import lombok.experimental.UtilityClass;
import me.santio.lens.Lens;
import me.santio.lens.paper.metrics.server.FoliaRegionMetrics;
import me.santio.lens.paper.tracker.RegionTracker;
import org.bukkit.plugin.Plugin;

@UtilityClass
public class FoliaHook {

    public void registerFolia(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(RegionTracker.instance(), plugin);
        RegionTracker.instance().start(plugin);
        Lens.instance().register(new FoliaRegionMetrics());
    }

    public void unregisterFolia() {
        RegionTracker.instance().stop();
    }

}
