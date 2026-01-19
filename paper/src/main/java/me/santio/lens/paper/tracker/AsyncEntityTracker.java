package me.santio.lens.paper.tracker;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@AutoService(Listener.class)
@Accessors(fluent = true)
@Getter
public class AsyncEntityTracker implements Listener {
    
    @Getter
    private static AsyncEntityTracker instance;
    private final Map<UUID, WeakReference<Entity>> entities = new HashMap<>();
    
    @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod", "ThisEscapedInObjectConstruction"})
    public AsyncEntityTracker() {
        instance = this;
    }
    
    public void untrack(UUID uniqueId) {
        this.entities.remove(uniqueId);
    }
    
    @EventHandler
    private void onEntitySpawn(EntitySpawnEvent event) {
        this.entities.put(event.getEntity().getUniqueId(), new WeakReference<>(event.getEntity()));
    }
    
    @EventHandler
    private void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            this.entities.put(entity.getUniqueId(), new WeakReference<>(entity));
        }
    }
    
    @EventHandler
    private void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            this.entities.remove(entity.getUniqueId());
        }
    }
    
    @EventHandler
    private void onEntityRemove(EntityRemoveFromWorldEvent event) {
        this.entities.remove(event.getEntity().getUniqueId());
    }
}
