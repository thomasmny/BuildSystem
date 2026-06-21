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
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.util.TaskScheduler;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NullMarked;

/**
 * A pending chat-input prompt: a player has been asked to type a value, and the {@link Request}'s completion callback
 * fires with the first non-{@code cancel} line they send. Prompts are constructed through {@link Prompts} (via its
 * {@link Prompts.Builder}), never directly.
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

    private final Messages messages;
    private final TaskScheduler scheduler;
    private final BukkitTask titleTask;
    private final Request request;

    PlayerChatInput(Messages messages, TaskScheduler scheduler, Request request) {
        this.messages = messages;
        this.scheduler = scheduler;
        this.request = request;

        Player player = request.player();
        String title = messages.getString(request.titleKey(), player);
        String subtitle = messages.getString("cancel_subtitle", player);
        this.titleTask = scheduler.runTimer(() -> player.sendTitle(title, subtitle, 0, 30, 0), 0L, 20L);

        player.closeInventory();
        XSound.ENTITY_PLAYER_LEVELUP.play(player);

        // Replace any prompt the player already had, cancelling its title task so nothing is orphaned.
        UUID playerUuid = request.player().getUniqueId();
        PlayerChatInput previous = ACTIVE.put(playerUuid, this);
        if (previous != null) {
            previous.titleTask.cancel();
        }
    }

    /**
     * The immutable specification of a prompt: who to ask, the title to show, and what to do on completion or
     * cancellation. Assembled by {@link Prompts.Builder} and consumed once to start a {@link PlayerChatInput}.
     *
     * @param player The player to request input from
     * @param titleKey The message key for the input prompt title
     * @param onComplete Receives the first non-{@code cancel} line the player sends
     * @param onCancel Runs on the main thread when the player cancels the prompt
     */
    record Request(Player player, String titleKey, InputRunnable onComplete, Runnable onCancel) {}

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
        input.scheduler.run(() -> {
            player.resetTitle();
            if (cancelled) {
                XSound.ENTITY_ITEM_BREAK.play(player);
                input.messages.sendMessage(player, "input_cancelled");
                input.request.onCancel().run();
            } else {
                input.request.onComplete().run(message);
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
