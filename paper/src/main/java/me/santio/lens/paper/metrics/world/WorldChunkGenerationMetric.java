package me.santio.lens.paper.metrics.world;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.Counter;
import me.santio.lens.Lens;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts newly generated chunks in each world. Pre-generation plugins can fire thousands of chunk loads
 * per second, so counters are cached per world to keep each event down to a map lookup.
 * @author santio
 */
@AutoService(Listener.class)
public class WorldChunkGenerationMetric implements Listener {

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    private Counter counter(String world) {
        return Lens.instance().counter("lens.world.chunks.generated", Map.of("world", world));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onChunkLoad(ChunkLoadEvent event) {
        if (!event.isNewChunk()) return;

        final String world = event.getWorld().getName();
        this.counters.computeIfAbsent(world, this::counter).increment();
    }

}
