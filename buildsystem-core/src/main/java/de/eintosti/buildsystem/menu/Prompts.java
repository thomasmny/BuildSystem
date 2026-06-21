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

import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.PlayerChatInput.InputRunnable;
import de.eintosti.buildsystem.util.StringCleaner;
import de.eintosti.buildsystem.util.TaskScheduler;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Entry point for chat-input prompts. Owns the collaborators a {@link PlayerChatInput} needs, so a listener, command, or
 * menu can request player input by depending on this single injectable instead of the whole {@code BuildSystemPlugin}.
 *
 * <p>Prompts are assembled fluently through a per-call {@link Builder} — {@code prompts.prompt(player).title(key)
 * .request(input -> ...)} — with the optional pieces ({@link Builder#onCancel onCancel}, {@link Builder#sanitizeName
 * name sanitization}) added only where needed, rather than via a family of telescoping overloads.
 */
@NullMarked
public final class Prompts {

    private final Messages messages;
    private final ConfigService configService;
    private final TaskScheduler scheduler;

    public Prompts(Messages messages, ConfigService configService, TaskScheduler scheduler) {
        this.messages = messages;
        this.configService = configService;
        this.scheduler = scheduler;
    }

    /**
     * Begins assembling a chat-input prompt for the given player. Call {@link Builder#request} to open it.
     *
     * @param player The player to request input from
     * @return A fresh builder for this prompt
     */
    public Builder prompt(Player player) {
        return new Builder(player);
    }

    /**
     * Fluent accumulator for a single chat-input prompt, obtained from {@link Prompts#prompt(Player)}. Only {@link
     * #title} and the terminal {@link #request} are required; {@link #onCancel} and {@link #sanitizeName} are opt-in.
     * Being bound to its enclosing {@link Prompts}, it reads the shared collaborators directly and so holds only the
     * per-prompt state.
     */
    public final class Builder {

        private final Player player;

        private @Nullable String titleKey;
        private Runnable onCancel = () -> {};
        private @Nullable String invalidCharactersMessageKey;
        private @Nullable String emptyNameMessageKey;

        private Builder(Player player) {
            this.player = player;
        }

        /**
         * Sets the message key shown as the prompt's title while it is pending. Required.
         *
         * @param titleKey The message key for the input prompt title
         * @return This builder
         */
        public Builder title(String titleKey) {
            this.titleKey = titleKey;
            return this;
        }

        /**
         * Runs {@code onCancel} when the player types {@code cancel} instead of the completion callback — typically used
         * to reopen the menu the prompt was launched from. Defaults to doing nothing.
         *
         * @param onCancel Runs on the main thread when the player cancels the prompt
         * @return This builder
         */
        public Builder onCancel(Runnable onCancel) {
            this.onCancel = onCancel;
            return this;
        }

        /**
         * Sanitizes the input against the configured invalid name characters before it reaches the completion callback:
         * the player is warned when characters were stripped and the input is rejected entirely when nothing survives.
         * Used for world and folder names.
         *
         * @param invalidCharactersMessageKey The message key sent when invalid characters were stripped
         * @param emptyNameMessageKey The message key sent when the sanitized name is empty
         * @return This builder
         */
        public Builder sanitizeName(String invalidCharactersMessageKey, String emptyNameMessageKey) {
            this.invalidCharactersMessageKey = invalidCharactersMessageKey;
            this.emptyNameMessageKey = emptyNameMessageKey;
            return this;
        }

        /**
         * Opens the assembled prompt: closes the player's inventory and asks them to type the value, which is delivered
         * to {@code onComplete} (sanitized first when {@link #sanitizeName} was set).
         *
         * @param onComplete Receives the first non-{@code cancel} line the player sends
         */
        public void request(InputRunnable onComplete) {
            String title = Objects.requireNonNull(titleKey, "prompt title key must be set");
            InputRunnable completion = invalidCharactersMessageKey == null ? onComplete : sanitizing(onComplete);
            new PlayerChatInput(messages, scheduler, new PlayerChatInput.Request(player, title, completion, onCancel));
        }

        /** Wraps {@code onValidName} so it only fires for input that survives name sanitization. */
        private InputRunnable sanitizing(InputRunnable onValidName) {
            String invalidCharacters = configService.current().world().invalidCharacters();
            String invalidCharactersKey = Objects.requireNonNull(invalidCharactersMessageKey);
            String emptyNameKey = Objects.requireNonNull(emptyNameMessageKey);
            return input -> {
                if (StringCleaner.hasInvalidNameCharacters(input, invalidCharacters)) {
                    messages.sendMessage(player, invalidCharactersKey);
                }

                String sanitizedName = StringCleaner.sanitize(input, invalidCharacters);
                if (sanitizedName.isEmpty()) {
                    messages.sendMessage(player, emptyNameKey);
                    return;
                }

                onValidName.run(sanitizedName);
            };
        }
    }
}
