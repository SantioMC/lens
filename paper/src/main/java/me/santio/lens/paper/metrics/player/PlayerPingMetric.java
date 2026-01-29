package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.santio.lens.Lens;
import me.santio.lens.model.Metric;
import me.santio.lens.model.ResponsiveMetric;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

@AutoService(Metric.class)
@Accessors(fluent = true)
@Getter
public class PlayerPingMetric implements ResponsiveMetric {
    
    @Getter
    private static final PlayerPingMetric instance = new PlayerPingMetric();
    
    private boolean isBedrock(Player player) {
        if (!Bukkit.getServer().getPluginManager().isPluginEnabled("floodgate")) return false;
        
        final FloodgateApi api = FloodgateApi.getInstance();
        return api.isFloodgatePlayer(player.getUniqueId());
    }
    
    @Override
    public void onUpdate() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            final double ping = player.getPing();
            if (ping == 0 || this.isBedrock(player)) continue;
            
            Lens.instance().summary("lens.player.ping").record(ping);
        }
    }
    
}
