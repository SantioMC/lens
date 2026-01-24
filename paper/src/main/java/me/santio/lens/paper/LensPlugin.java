package me.santio.lens.paper;

import io.micrometer.core.instrument.binder.MeterBinder;
import me.santio.lens.Lens;
import me.santio.lens.paper.hooks.PacketEventsHook;
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
        ServiceLoader.load(MeterBinder.class, this.getClassLoader()).forEach(lens::register);
        ServiceLoader.load(Listener.class, this.getClassLoader()).forEach((listener) -> {
            this.getServer().getPluginManager().registerEvents(listener, this);
        });
        
        // Register optional metrics based on installed plugins
        if (this.getServer().getPluginManager().isPluginEnabled("packetevents")) {
            PacketEventsHook.registerPacketEvents();
        }
        
        // Start HTTP server
        final String address = this.getConfig().getString("bind-address", "0.0.0.0");
        final int port = this.getConfig().getInt("port", 9000);
        
        this.getLogger().info("Starting HTTP server at http://" + address + ":" + port);
        lens.start(address, port);
    }
    
    public static LensPlugin instance() {
        return getPlugin(LensPlugin.class);
    }
    
}
