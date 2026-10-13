package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import me.santio.lens.Lens;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;

/**
 * Counts player deaths by the type of damage that killed them and the type of entity responsible
 * @author santio
 */
@AutoService(Listener.class)
public class PlayerDeathMetric implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onDeath(PlayerDeathEvent event) {
        final DamageSource source = event.getDamageSource();
        final @Nullable Entity killer = source.getCausingEntity();
        final String killerType = killer == null ? "none" : killer.getType().key().value();

        Lens.instance().counter("lens.players.deaths", Map.of(
            "cause", source.getDamageType().key().value(),
            "killer", killerType
        )).increment();
    }

}
