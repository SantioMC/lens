package me.santio.lens.paper.hooks;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.EventManager;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import lombok.experimental.UtilityClass;
import me.santio.lens.Lens;
import me.santio.lens.paper.metrics.server.ServerPacketsMetric;

import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

@UtilityClass
public class PacketEventsHook {

    private final List<PacketListenerCommon> listeners = new CopyOnWriteArrayList<>();

    public void registerPacketEvents() {
        final EventManager eventManager = PacketEvents.getAPI().getEventManager();

        ServiceLoader.load(PacketListener.class, PacketEventsHook.class.getClassLoader()).forEach((listener) -> {
            listeners.add(eventManager.registerListener(listener, PacketListenerPriority.NORMAL));
        });

        Lens.instance().register(ServerPacketsMetric.instance());
        listeners.add(eventManager.registerListener(ServerPacketsMetric.instance(), PacketListenerPriority.MONITOR));
    }

    public void unregisterPacketEvents() {
        final EventManager eventManager = PacketEvents.getAPI().getEventManager();

        listeners.forEach(eventManager::unregisterListener);
        listeners.clear();
        ServerPacketsMetric.instance().clear();
    }

}
