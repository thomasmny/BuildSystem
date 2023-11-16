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
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MessagesProvider {

    protected static final Map<String, String> MESSAGES = new HashMap<>();

    private final BuildSystemPlugin plugin;

    public MessagesProvider(BuildSystemPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Sets up the plugin messages.
     */
    public void setup() {
        copyDefaultMessages();
        updateMissingMessages();
        loadMessages();
    }

    /**
     * Gets the file in which the plugin messages are stored.
     *
     * @return The file in which the plugin messages are stored
     */
    private File getMessagesFile() {
        return new File(plugin.getDataFolder(), "messages.properties");
    }

    /**
     * Copies the default messages from the resources folder to the plugin's data folder.
     */
    private void copyDefaultMessages() {
        File messages = getMessagesFile();
        if (messages.exists()) {
            return;
        }

        plugin.getLogger().info("Copying default messages file...");
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            inputStream = plugin.getResource("lang/messages.properties");
            outputStream = Files.newOutputStream(messages.toPath());
            IOUtils.copy(new InputStreamReader(inputStream, StandardCharsets.UTF_8), outputStream);
        } catch (IOException e) {
            plugin.getLogger().severe("Error while copying default messages:");
            e.printStackTrace();
        } finally {
            close(inputStream);
            close(outputStream);
        }
    }

    /**
     * Checks if any messages are missing in the user's {@code messages.properties} file.
     * If so, they are added using the default messages as a reference.
     */
    private void updateMissingMessages() {
        Path messagesPath = getMessagesFile().toPath();
        InputStream messagesInputStream = null;
        InputStream defaultMessagesInputStream = null;
        Writer writer = null;

        try {
            OrderedProperties messagesProperties = new OrderedProperties();
            messagesInputStream = Files.newInputStream(messagesPath);
            messagesProperties.load(new InputStreamReader(messagesInputStream, StandardCharsets.UTF_8));

            OrderedProperties defaultProperties = new OrderedProperties();
            defaultMessagesInputStream = plugin.getResource("lang/messages.properties");
            defaultProperties.load(new InputStreamReader(defaultMessagesInputStream, StandardCharsets.UTF_8));

            if (messagesProperties.equals(defaultProperties)) {
                return;
            }

            plugin.getLogger().info("Updating missing messages...");
            messagesProperties.entrySet().forEach(property -> defaultProperties.setProperty(property.getKey(), property.getValue()));
            writer = Files.newBufferedWriter(messagesPath, StandardCharsets.UTF_8);
            defaultProperties.store(writer, null);
        } catch (IOException e) {
            plugin.getLogger().severe("Error while updating missing messages:");
            e.printStackTrace();
        } finally {
            close(messagesInputStream);
            close(defaultMessagesInputStream);
            close(writer);
        }
    }

    /**
     * Loads and caches the messages stored in the user's {@code messages.properties} file.
     */
    private void loadMessages() {
        InputStream inputStream = null;
        try {
            Properties messages = new Properties();
            inputStream = Files.newInputStream(getMessagesFile().toPath());
            messages.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            messages.forEach((key, value) -> MESSAGES.put(key.toString(), value.toString()));
        } catch (IOException e) {
            plugin.getLogger().severe("Error while updating missing messages:");
            e.printStackTrace();
        } finally {
            close(inputStream);
        }
    }

    private void close(@Nullable Closeable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
