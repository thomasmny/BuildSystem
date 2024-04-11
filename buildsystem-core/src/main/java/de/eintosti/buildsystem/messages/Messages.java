/*
 * Copyright (c) 2018-2023, Thomas Meaney
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
package de.eintosti.buildsystem.messages;

import de.eintosti.buildsystem.BuildSystemPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Messages {

    private static final BuildSystemPlugin PLUGIN = JavaPlugin.getPlugin(BuildSystemPlugin.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    /**
     * Gets the message with the given key as a component after it was parsed by {@link MiniMessage}
     * using the given tag resolvers.
     *
     * @param key          The key of the messages
     * @param tagResolvers The tag resolvers to apply extra tags from
     * @return The message as a component
     */
    public static Component getMessage(String key, TagResolver... tagResolvers) {
        String message = MessagesProvider.MESSAGES.get(key);
        if (message == null) {
            PLUGIN.getLogger().warning("Could not find message with key '" + key + "'");
            return Component.empty();
        }

        return MINI_MESSAGE.deserialize(message, mergeTagResolvers(tagResolvers));
    }

    /**
     * Gets the messages with the given key as a list of components after they were parsed by {@link MiniMessage}
     * using the given tag resolvers.
     *
     * @param key          The key of the messages
     * @param tagResolvers The tag resolvers to apply extra tags from
     * @return The messages as a list of components
     */
    public static List<Component> getMessages(String key, TagResolver... tagResolvers) {
        String message = MessagesProvider.MESSAGES.get(key);
        if (message == null) {
            PLUGIN.getLogger().warning("Could not find message with key '" + key + "'");
            return new ArrayList<>();
        }

        String[] split = message.split("\n");
        Component[] parsed = new Component[split.length];
        for (int i = 0; i < split.length; i++) {
            parsed[i] = MINI_MESSAGE.deserialize(split[i], mergeTagResolvers(tagResolvers));
        }

        return Arrays.asList(parsed);
    }

    /**
     * Merges an array of {@link TagResolver} objects with the prefix tag resolver.
     *
     * @param tagResolvers The tag resolvers to merge
     * @return An array of {@link TagResolver} objects containing the merged resolvers
     */
    private static TagResolver[] mergeTagResolvers(TagResolver... tagResolvers) {
        return Stream.concat(
                Stream.of(Placeholder.component("prefix", MINI_MESSAGE.deserialize(MessagesProvider.MESSAGES.get("prefix")))),
                Arrays.stream(tagResolvers)
        ).toArray(TagResolver[]::new);
    }

    /**
     * Gets the message with the given key as a legacy string after it was parsed by {@link MiniMessage}
     * using the given tag resolvers.
     *
     * @param key          The key of the messages
     * @param tagResolvers The tag resolvers to apply extra tags from
     * @return The message a legacy string
     * @see #getMessage(String, TagResolver...)
     */
    public static String getString(String key, TagResolver... tagResolvers) {
        return LEGACY_SERIALIZER.serialize(getMessage(key, tagResolvers));
    }

    /**
     * Gets the message with the given key as a list of legacy strings after it was parsed by {@link MiniMessage}
     * using the given tag resolvers.
     *
     * @param key          The key of the messages
     * @param tagResolvers The tag resolvers to apply extra tags from
     * @return The message a legacy string
     * @see #getMessages(String, TagResolver...)
     */
    public static List<String> getStringList(String key, TagResolver... tagResolvers) {
        List<Component> messages = getMessages(key, tagResolvers);
        return messages.stream()
                .map(LEGACY_SERIALIZER::serialize)
                .collect(Collectors.toList());
    }

    /**
     * Sends a message to the given sender.
     *
     * @param sender       The sender
     * @param key          The key of the message to send
     * @param tagResolvers The tag resolvers to apply extra tags from
     */
    public static void sendMessage(CommandSender sender, String key, TagResolver... tagResolvers) {
        Component message = getMessage(key, tagResolvers);
        PLUGIN.adventure().sender(sender).sendMessage(message);
    }
}
