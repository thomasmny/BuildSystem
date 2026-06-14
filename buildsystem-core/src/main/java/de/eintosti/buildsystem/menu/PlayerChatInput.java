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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NullMarked;

/**
 * A pending chat-input prompt: a player has been asked to type a value, and {@link #runWhenComplete} fires with the
 * first non-{@code cancel} line they send.
 *
 * <p><strong>One listener, one registry.</strong> Active prompts live in a single shared {@link #ACTIVE} map keyed by
 * player, drained by the single {@link ChatInputListener} that is registered once at startup. Creating a prompt only
 * inserts a map entry (and starts the title task); it does <em>not</em> register a Bukkit listener. Starting a new
 * prompt for a player who already has one cancels the old prompt's title task and replaces it, so repeated prompts can
 * never leak listeners or repeating tasks.
 *
 * <p>The map is a {@link ConcurrentHashMap} because {@link AsyncPlayerChatEvent} is delivered off the main thread; all
 * Bukkit interactions on completion are hopped back to the main thread.
 */
@NullMarked
public final class PlayerChatInput {

    /** All currently-pending prompts, keyed by player UUID. Read on the async chat thread; written on the main thread. */
    private static final Map<UUID, PlayerChatInput> ACTIVE = new ConcurrentHashMap<>();

    private final BuildSystemPlugin plugin;
    private final BukkitTask titleTask;
    private final InputRunnable runWhenComplete;
    private final UUID playerUuid;

    public PlayerChatInput(BuildSystemPlugin plugin, Player player, String titleKey, InputRunnable runWhenComplete) {
        this.plugin = plugin;
        this.playerUuid = player.getUniqueId();
        this.runWhenComplete = runWhenComplete;

        String title = plugin.getMessages().getString(titleKey, player);
        String subtitle = plugin.getMessages().getString("cancel_subtitle", player);
        this.titleTask = new BukkitRunnable() {
            @Override
            public void run() {
                player.sendTitle(title, subtitle, 0, 30, 0);
            }
        }.runTaskTimer(plugin, 0L, 20L);

        player.closeInventory();
        XSound.ENTITY_PLAYER_LEVELUP.play(player);

        // Replace any prompt the player already had, cancelling its title task so nothing is orphaned.
        PlayerChatInput previous = ACTIVE.put(playerUuid, this);
        if (previous != null) {
            previous.titleTask.cancel();
        }
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

    /**
     * Consumes a chat line for the player's pending prompt, if any. Cancels the chat event, hops to the main thread, and
     * either runs the completion callback or (for {@code cancel}) reports cancellation.
     */
    private static void handleChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PlayerChatInput input = ACTIVE.remove(player.getUniqueId());
        if (input == null) {
            return;
        }

        event.setCancelled(true);
        input.titleTask.cancel();

        String message = event.getMessage();
        boolean cancelled = message.equalsIgnoreCase("cancel");
        Bukkit.getScheduler().runTask(input.plugin, () -> {
            player.resetTitle();
            if (cancelled) {
                XSound.ENTITY_ITEM_BREAK.play(player);
                input.plugin.getMessages().sendMessage(player, "input_cancelled");
            } else {
                input.runWhenComplete.run(message);
            }
        });
    }

    /** Drops a player's pending prompt (if any) when they disconnect, cancelling its title task. */
    private static void handleQuit(UUID playerUuid) {
        PlayerChatInput input = ACTIVE.remove(playerUuid);
        if (input != null) {
            input.titleTask.cancel();
        }
    }

    /**
     * The single, long-lived listener that drains pending chat-input prompts. Registered once at startup (see
     * {@code ListenerRegistrar}); never per prompt.
     */
    @NullMarked
    public static final class ChatInputListener implements Listener {

        @EventHandler
        public void onPlayerChat(AsyncPlayerChatEvent event) {
            handleChat(event);
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            handleQuit(event.getPlayer().getUniqueId());
        }
    }

    @FunctionalInterface
    public interface InputRunnable {

        void run(String input);
    }
}
