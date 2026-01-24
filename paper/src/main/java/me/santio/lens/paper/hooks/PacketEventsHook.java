package me.santio.lens.paper.hooks;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import lombok.experimental.UtilityClass;
import me.santio.lens.Lens;
import me.santio.lens.paper.metrics.server.ServerPacketsMetric;

import java.util.ServiceLoader;

@UtilityClass
public class PacketEventsHook {
    
    public void registerPacketEvents() {
        ServiceLoader.load(PacketListener.class, PacketEventsHook.class.getClassLoader()).forEach((listener) -> {
            PacketEvents.getAPI().getEventManager().registerListener(listener, PacketListenerPriority.NORMAL);
        });
        
        Lens.instance().register(ServerPacketsMetric.instance());
        PacketEvents.getAPI().getEventManager().registerListener(
            ServerPacketsMetric.instance(),
            PacketListenerPriority.MONITOR
        );
    }

}
