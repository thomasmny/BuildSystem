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

import com.google.common.collect.Lists;
import de.eintosti.buildsystem.BuildSystemPlugin;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

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
    @NotNull
    public static Component getMessage(String key, TagResolver... tagResolvers) {
        String message = MessagesProvider.MESSAGES.get(key);
        if (message == null) {
            return Component.empty();
        }

        TagResolver[] allResolvers = Lists.newArrayList(
                tagResolvers,
                Placeholder.component("prefix", MINI_MESSAGE.deserialize(MessagesProvider.MESSAGES.get("prefix")))
        ).toArray(new TagResolver[0]);

        return MINI_MESSAGE.deserialize(message, allResolvers);
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
    @NotNull
    public static String getLegacyMessage(String key, TagResolver... tagResolvers) {
        return LEGACY_SERIALIZER.serialize(getMessage(key, tagResolvers));
    }

    /**
     * Sends a message to the given sender.
     *
     * @param sender       The sender
     * @param key          The key of the message to send
     * @param tagResolvers The tag resolvers to apply extra tags from
     */
    public static void sendMessage(CommandSender sender, String key, TagResolver... tagResolvers) {
        try (BukkitAudiences audiences = PLUGIN.adventure()) {
            Component message = getMessage(key, tagResolvers);
            audiences.sender(sender).sendMessage(message);
        }
    }
}
