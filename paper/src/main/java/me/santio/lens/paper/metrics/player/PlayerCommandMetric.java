package me.santio.lens.paper.metrics.player;

import com.google.auto.service.AutoService;
import me.santio.lens.Lens;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;
import java.util.Map;

/**
 * Counts the commands executed by players, only the base command is tracked. Unknown commands are
 * ignored to prevent players from creating an unbounded amount of series.
 * @author santio
 */
@AutoService(Listener.class)
public class PlayerCommandMetric implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onCommand(PlayerCommandPreprocessEvent event) {
        final String input = event.getMessage().substring(1);
        final String label = input.split(" ", 2)[0];

        final @Nullable Command command = Bukkit.getCommandMap().getCommand(label);
        if (command == null) return;

        Lens.instance().counter("lens.players.commands", Map.of(
            "command", this.baseName(command)
        )).increment();
    }

    /**
     * Gets the name of the command without its namespace, aliases resolve to the command they belong to
     * @param command The command to get the name of
     * @return The base name of the command, for example {@code minecraft:me} becomes {@code me}
     */
    private String baseName(Command command) {
        final String name = command.getName().toLowerCase(Locale.ROOT);
        final int namespaceEnd = name.lastIndexOf(':');

        return name.substring(namespaceEnd + 1);
    }

}
