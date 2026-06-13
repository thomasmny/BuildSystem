/*
 * Copyright (c) 2018-2026, Thomas Meaney
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.eintosti.buildsystem.menu;

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.util.StringCleaner;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PlayerChatInput implements Listener {

    private final BuildSystemPlugin plugin;

    private final BukkitTask taskId;
    private final InputRunnable runWhenComplete;
    private final UUID playerUuid;
    private final boolean inputMode;

    private final Map<UUID, PlayerChatInput> inputs = new HashMap<>();

    public PlayerChatInput(BuildSystemPlugin plugin, Player player, String titleKey, InputRunnable runWhenComplete) {
        this.plugin = plugin;

        String title = plugin.getMessages().getString(titleKey, player);
        String subtitle = plugin.getMessages().getString("cancel_subtitle", player);

        this.taskId = new BukkitRunnable() {
            public void run() {
                player.sendTitle(title, subtitle, 0, 30, 0);
            }
        }.runTaskTimer(plugin, 0L, 20L);

        this.playerUuid = player.getUniqueId();
        this.runWhenComplete = runWhenComplete;
        this.inputMode = true;

        player.closeInventory();
        XSound.ENTITY_PLAYER_LEVELUP.play(player);

        this.register();
    }

    /**
     * Requests a chat input that names a world or folder. The raw input is sanitized against the configured invalid
     * characters before being handed to {@code onValidName}; the player is warned when characters were stripped and the
     * input is rejected entirely when nothing survives sanitization.
     *
     * @param plugin The plugin instance
     * @param player The player to request input from
     * @param titleKey The message key for the input prompt title
     * @param invalidCharactersMessageKey The message key sent when invalid characters were stripped
     * @param emptyNameMessageKey The message key sent when the sanitized name is empty
     * @param onValidName Receives the sanitized, non-empty name
     */
    public static void requestSanitizedName(
            BuildSystemPlugin plugin,
            Player player,
            String titleKey,
            String invalidCharactersMessageKey,
            String emptyNameMessageKey,
            InputRunnable onValidName) {
        String invalidCharacters = plugin.getConfigService().current().world().invalidCharacters();
        new PlayerChatInput(plugin, player, titleKey, input -> {
            if (StringCleaner.hasInvalidNameCharacters(input, invalidCharacters)) {
                plugin.getMessages().sendMessage(player, invalidCharactersMessageKey);
            }

            String sanitizedName = StringCleaner.sanitize(input, invalidCharacters);
            if (sanitizedName.isEmpty()) {
                plugin.getMessages().sendMessage(player, emptyNameMessageKey);
                return;
            }

            onValidName.run(sanitizedName);
        });
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String input = event.getMessage();
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        if (!inputs.containsKey(playerUuid)) {
            return;
        }

        PlayerChatInput current = inputs.get(playerUuid);
        if (!current.inputMode) {
            return;
        }

        event.setCancelled(true);

        if (input.equalsIgnoreCase("cancel")) {
            current.taskId.cancel();
            current.unregister();

            XSound.ENTITY_ITEM_BREAK.play(player);
            player.resetTitle();
            plugin.getMessages().sendMessage(player, "input_cancelled");
            return;
        }

        current.taskId.cancel();
        Bukkit.getScheduler().runTask(current.plugin, () -> current.runWhenComplete.run(input));
        player.resetTitle();
        current.unregister();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PlayerChatInput current = inputs.remove(event.getPlayer().getUniqueId());
        if (current != null) {
            current.taskId.cancel();
            HandlerList.unregisterAll(current);
        }
    }

    private void register() {
        inputs.put(this.playerUuid, this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void unregister() {
        inputs.remove(this.playerUuid);
        HandlerList.unregisterAll(this);
    }

    @FunctionalInterface
    public interface InputRunnable {

        void run(String input);
    }
}
