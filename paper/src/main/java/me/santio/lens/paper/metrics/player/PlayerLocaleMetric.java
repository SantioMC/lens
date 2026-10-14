package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLocaleChangeEvent;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Groups online players by the language their client is set to, locales with an unknown language or
 * country (such as joke languages, or values sent by modified clients) are grouped as {@code other}
 * @author santio
 */
@AutoService(Listener.class)
public class PlayerLocaleMetric extends PlayerGroupMetric {

    private static final Set<String> LANGUAGES = Arrays.stream(Locale.getAvailableLocales())
        .map(Locale::getLanguage)
        .collect(Collectors.toUnmodifiableSet());
    private static final Set<String> COUNTRIES = Set.of(Locale.getISOCountries());

    public PlayerLocaleMetric() {
        super("lens.players.locale");
    }

    private String normalize(Locale locale) {
        final String language = locale.getLanguage();
        final String country = locale.getCountry();

        if (!LANGUAGES.contains(language)) return "other";
        if (country.isEmpty()) return language;
        if (!COUNTRIES.contains(country)) return "other";

        return language + "_" + country.toLowerCase(Locale.ROOT);
    }

    private Map<String, String> tags(Locale locale) {
        return Map.of("locale", this.normalize(locale));
    }

    @Override
    protected Map<String, String> tags(Player player) {
        return this.tags(player.locale());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onLocaleChange(PlayerLocaleChangeEvent event) {
        this.track(event.getPlayer(), this.tags(event.locale()));
    }

}
