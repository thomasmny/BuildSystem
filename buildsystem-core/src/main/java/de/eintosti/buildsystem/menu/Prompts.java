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
import java.util.ArrayList;
import java.util.List;
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
     * Begins assembling a multi-step chat-input flow for the given player. Each step validates its input and, on
     * failure, is re-prompted; the flow advances only once a step accepts.
     *
     * @param player The player to request input from
     * @return A fresh flow for this player
     */
    public PromptFlow flow(Player player) {
        return new PromptFlow(player);
    }

    /**
     * Sanitizes a typed name against the configured invalid characters: warns the player when characters were stripped
     * and rejects the input entirely when nothing survives. Shared by single-prompt name sanitization and {@link
     * PromptFlow} name steps.
     *
     * @param player The player who typed the name
     * @param input The raw input
     * @param invalidCharsKey The message key sent when invalid characters were stripped
     * @param emptyNameKey The message key sent when the sanitized name is empty
     * @return The sanitized name, or {@code null} if nothing survived (a message has already been sent)
     */
    public @Nullable String sanitizeName(Player player, String input, String invalidCharsKey, String emptyNameKey) {
        String invalidCharacters = configService.current().world().invalidCharacters();
        if (StringCleaner.hasInvalidNameCharacters(input, invalidCharacters)) {
            messages.sendMessage(player, invalidCharsKey);
        }

        String sanitized = StringCleaner.sanitize(input, invalidCharacters);
        if (sanitized.isEmpty()) {
            messages.sendMessage(player, emptyNameKey);
            return null;
        }
        return sanitized;
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
            String invalidCharactersKey = Objects.requireNonNull(invalidCharactersMessageKey);
            String emptyNameKey = Objects.requireNonNull(emptyNameMessageKey);
            return input -> {
                String sanitizedName = Prompts.this.sanitizeName(player, input, invalidCharactersKey, emptyNameKey);
                if (sanitizedName != null) {
                    onValidName.run(sanitizedName);
                }
            };
        }
    }

    /**
     * A sequence of chat-input steps run one after another, obtained from {@link Prompts#flow(Player)}. Each step
     * validates the line the player types; returning {@code false} re-prompts the same step (keeping earlier answers),
     * returning {@code true} advances. Each step reuses {@link Builder}, so cancellation, the pending-title task, and
     * sounds behave exactly like a single prompt.
     */
    public final class PromptFlow {

        @FunctionalInterface
        public interface StepValidator {

            /**
             * @param input The line the player typed
             * @return {@code true} to advance to the next step, {@code false} to re-prompt this step
             */
            boolean validate(String input);
        }

        private record Step(String titleKey, StepValidator validator) {}

        private final Player player;
        private final List<Step> steps = new ArrayList<>();
        private Runnable onCancel = () -> {};

        private PromptFlow(Player player) {
            this.player = player;
        }

        /**
         * Appends a step to the flow.
         *
         * @param titleKey The message key shown as the prompt title while this step is pending
         * @param validator Validates the typed input, returning whether to advance
         * @return This flow
         */
        public PromptFlow step(String titleKey, StepValidator validator) {
            steps.add(new Step(titleKey, validator));
            return this;
        }

        /**
         * Runs {@code onCancel} when the player types {@code cancel} at any step. Defaults to doing nothing.
         *
         * @param onCancel Runs on the main thread when the player cancels the flow
         * @return This flow
         */
        public PromptFlow onCancel(Runnable onCancel) {
            this.onCancel = onCancel;
            return this;
        }

        /**
         * Opens the first step. {@code onComplete} runs once the final step has advanced.
         *
         * @param onComplete Runs on the main thread after all steps accept
         */
        public void start(Runnable onComplete) {
            runStep(0, onComplete);
        }

        private void runStep(int index, Runnable onComplete) {
            if (index >= steps.size()) {
                onComplete.run();
                return;
            }

            Step step = steps.get(index);
            // request() returns after registering the listener, so re-prompting on failure does not grow the stack.
            prompt(player).title(step.titleKey()).onCancel(onCancel).request(input -> {
                if (step.validator().validate(input)) {
                    runStep(index + 1, onComplete);
                } else {
                    runStep(index, onComplete);
                }
            });
        }
    }
}
