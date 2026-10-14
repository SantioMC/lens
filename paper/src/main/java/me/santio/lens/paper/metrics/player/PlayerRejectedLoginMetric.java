package me.santio.lens.paper.metrics.player;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.Counter;
import io.papermc.paper.ban.BanListType;
import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import me.santio.lens.Lens;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Counts rejected logins by their reason. Plugins usually reject players during pre-login, while the
 * server rejects banned, non-whitelisted and full joins once the login is validated. Since validation
 * only provides a kick message, the reason is derived from the ban list, whitelist and player count.
 * @author santio
 */
@AutoService(Listener.class)
public class PlayerRejectedLoginMetric implements Listener {

    private static final String METRIC_NAME = "lens.players.rejected";
    private static final List<String> REASONS = List.of("banned", "whitelist", "full", "other");

    public PlayerRejectedLoginMetric() {
        REASONS.forEach(this::counter);
    }

    private Counter counter(String reason) {
        return Lens.instance().counter(METRIC_NAME, Map.of("reason", reason));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPreLogin(AsyncPlayerPreLoginEvent event) {
        final AsyncPlayerPreLoginEvent.Result result = event.getLoginResult();
        if (result == AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        final String reason = switch (result) {
            case KICK_BANNED -> "banned";
            case KICK_WHITELIST -> "whitelist";
            case KICK_FULL -> "full";
            default -> "other";
        };

        this.counter(reason).increment();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onValidateLogin(PlayerConnectionValidateLoginEvent event) {
        if (event.isAllowed()) return;

        final String reason = event.getConnection() instanceof PlayerLoginConnection connection
            ? this.reason(connection)
            : "other";

        this.counter(reason).increment();
    }

    private String reason(PlayerLoginConnection connection) {
        final @Nullable PlayerProfile authenticated = connection.getAuthenticatedProfile();
        final PlayerProfile profile = authenticated != null ? authenticated : connection.getUnsafeProfile();
        final InetAddress address = connection.getClientAddress().getAddress();

        if (Bukkit.getBanList(BanListType.PROFILE).isBanned(profile)) return "banned";
        if (!this.isWhitelisted(profile)) return "whitelist";
        if (Bukkit.getBanList(BanListType.IP).isBanned(address)) return "banned";
        if (Bukkit.getOnlinePlayers().size() >= Bukkit.getMaxPlayers()) return "full";

        return "other";
    }

    private boolean isWhitelisted(PlayerProfile profile) {
        if (!Bukkit.hasWhitelist()) return true;

        final @Nullable UUID uniqueId = profile.getId();
        if (uniqueId == null) return false;

        final OfflinePlayer player = Bukkit.getOfflinePlayer(uniqueId);

        return player.isWhitelisted() || player.isOp();
    }

}
