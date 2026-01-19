package me.santio.lens.paper;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.micrometer.core.instrument.binder.MeterBinder;
import me.santio.lens.Lens;
import me.santio.lens.paper.metrics.server.ServerPacketsMetric;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ServiceLoader;

public class LensPlugin extends JavaPlugin {
    
    @SuppressWarnings("FeatureEnvy")
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
            ServiceLoader.load(PacketListener.class, this.getClassLoader()).forEach((listener) -> {
                PacketEvents.getAPI().getEventManager().registerListener(listener, PacketListenerPriority.NORMAL);
            });
            
            lens.register(ServerPacketsMetric.instance());
            PacketEvents.getAPI().getEventManager().registerListener(
                ServerPacketsMetric.instance(),
                PacketListenerPriority.MONITOR
            );
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
