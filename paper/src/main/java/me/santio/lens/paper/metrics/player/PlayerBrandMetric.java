package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import me.santio.lens.paper.LensPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Groups online players by the client they are playing on, brands are matched against a list of
 * common clients since the brand is an arbitrary string sent by the client
 * @author santio
 */
@AutoService(Listener.class)
public class PlayerBrandMetric extends PlayerGroupMetric {

    private static final long BRAND_RETRY_TICKS = 40L;
    private static final List<String> BRANDS = List.of(
        "lunar",
        "feather",
        "badlion",
        "labymod",
        "geyser",
        "neoforge",
        "forge",
        "quilt",
        "fabric",
        "vanilla"
    );

    public PlayerBrandMetric() {
        super("lens.players.brand");
    }

    private String normalize(@Nullable String brand) {
        if (brand == null) return "unknown";

        final String lowercase = brand.toLowerCase(Locale.ROOT);

        return BRANDS.stream()
            .filter(lowercase::contains)
            .findFirst()
            .orElse("other");
    }

    @Override
    protected Map<String, String> tags(Player player) {
        return Map.of("brand", this.normalize(player.getClientBrandName()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (player.getClientBrandName() != null) return;

        player.getScheduler().runDelayed(LensPlugin.instance(), (task) -> this.track(player), null, BRAND_RETRY_TICKS);
    }

}
