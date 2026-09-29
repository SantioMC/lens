package me.santio.lens.paper;

import io.papermc.paper.ServerBuildInfo;
import me.santio.lens.Lens;
import me.santio.lens.paper.hooks.FoliaHook;
import me.santio.lens.paper.hooks.PacketEventsHook;
import me.santio.lens.paper.tracker.ServerTickTracker;
import me.santio.lens.paper.tracker.WorldTracker;
import net.kyori.adventure.key.Key;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ServiceLoader;

public class LensPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        final Lens lens = Lens.instance();

        this.saveDefaultConfig();
        this.getServer().getServicesManager().register(Lens.class, lens, this, ServicePriority.Normal);

        // Register auto-serviced components
        ServiceLoader.load(Listener.class, this.getClassLoader()).forEach((listener) -> {
            this.getServer().getPluginManager().registerEvents(listener, this);
        });

        // Start trackers used to safely read world and tick state off-thread
        this.getServer().getPluginManager().registerEvents(WorldTracker.instance(), this);
        ServerTickTracker.instance().start(this);

        if (!this.isFolia()) {
            for (World world : this.getServer().getWorlds()) {
                WorldTracker.instance().seed(world);
            }
        }

        // Register optional metrics based on the platform and installed plugins
        if (this.isFolia()) {
            FoliaHook.registerFolia(this);
        }

        if (this.isPacketEventsEnabled()) {
            PacketEventsHook.registerPacketEvents();
        }

        // Start HTTP server
        final String address = this.getConfig().getString("bind-address", "0.0.0.0");
        final int port = this.getConfig().getInt("port", 9000);

        this.getLogger().info("Starting HTTP server at http://" + address + ":" + port);
        lens.start(address, port);
    }

    @Override
    public void onDisable() {
        if (this.isPacketEventsEnabled()) {
            PacketEventsHook.unregisterPacketEvents();
        }

        if (this.isFolia()) {
            FoliaHook.unregisterFolia();
        }

        ServerTickTracker.instance().stop();
        WorldTracker.instance().clear();
        Lens.instance().stop();
    }

    private boolean isPacketEventsEnabled() {
        return this.getServer().getPluginManager().isPluginEnabled("packetevents");
    }

    private boolean isFolia() {
        return ServerBuildInfo.buildInfo().isBrandCompatible(Key.key("papermc", "folia"));
    }

    public static LensPlugin instance() {
        return getPlugin(LensPlugin.class);
    }

}
