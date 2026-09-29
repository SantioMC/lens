package me.santio.lens.paper.tracker;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the loaded chunks and entities of every world through events, this allows their counts to be
 * read from any thread (such as the HTTP server) without touching world state, which Folia does not allow.
 * Only identifiers are stored, no references to chunks, entities or worlds are kept.
 * @author santio
 */
@Accessors(fluent = true)
public final class WorldTracker implements Listener {
    @Getter
    private static final WorldTracker instance = new WorldTracker();
    private WorldTracker() {}

    private final Map<WorldKey, Set<Long>> chunks = new ConcurrentHashMap<>();
    private final Map<UUID, EntityKey> entities = new ConcurrentHashMap<>();

    /**
     * Tracks all chunks and entities currently loaded in a world, this must be called from a thread
     * that owns the world (the main thread on Paper)
     * @param world The world to seed from
     */
    public void seed(World world) {
        for (Chunk chunk : world.getLoadedChunks()) {
            this.trackChunk(chunk);
        }

        for (Entity entity : world.getEntities()) {
            this.trackEntity(entity);
        }
    }

    /**
     * Removes all tracked chunks and entities
     */
    public void clear() {
        this.chunks.clear();
        this.entities.clear();
    }

    /**
     * @return The amount of loaded chunks in each world
     */
    public Map<WorldKey, Integer> chunkCounts() {
        final Map<WorldKey, Integer> counts = new HashMap<>();

        for (var entry : this.chunks.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().size());
        }

        return counts;
    }

    /**
     * @return The amount of loaded entities for each world and entity type
     */
    public Map<EntityKey, Integer> entityCounts() {
        final Map<EntityKey, Integer> counts = new HashMap<>();

        for (EntityKey key : this.entities.values()) {
            counts.merge(key, 1, Integer::sum);
        }

        return counts;
    }

    private void trackChunk(Chunk chunk) {
        final WorldKey world = WorldKey.of(chunk.getWorld());
        this.chunks.computeIfAbsent(world, key -> ConcurrentHashMap.newKeySet()).add(chunk.getChunkKey());
    }

    private void trackEntity(Entity entity) {
        this.entities.put(entity.getUniqueId(), EntityKey.of(entity));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onChunkLoad(ChunkLoadEvent event) {
        this.trackChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onChunkUnload(ChunkUnloadEvent event) {
        final WorldKey world = WorldKey.of(event.getWorld());
        final @Nullable Set<Long> loaded = this.chunks.get(world);
        if (loaded == null) return;

        loaded.remove(event.getChunk().getChunkKey());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onEntityAdd(EntityAddToWorldEvent event) {
        this.trackEntity(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onEntityRemove(EntityRemoveFromWorldEvent event) {
        this.entities.remove(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onWorldUnload(WorldUnloadEvent event) {
        final WorldKey world = WorldKey.of(event.getWorld());

        this.chunks.remove(world);
        this.entities.values().removeIf(key -> key.world().equals(world));
    }

    public record WorldKey(
        UUID uniqueId,
        String name
    ) {
        static WorldKey of(World world) {
            return new WorldKey(world.getUID(), world.getName());
        }
    }

    public record EntityKey(
        WorldKey world,
        String entityType
    ) {
        static EntityKey of(Entity entity) {
            return new EntityKey(WorldKey.of(entity.getWorld()), entity.getType().getKey().asString());
        }
    }
}
