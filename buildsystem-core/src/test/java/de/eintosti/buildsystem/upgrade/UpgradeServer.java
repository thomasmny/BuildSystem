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
package de.eintosti.buildsystem.upgrade;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Services;
import de.eintosti.buildsystem.api.BuildSystem;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

/**
 * Boots the real plugin under MockBukkit on a data folder copied from disk, the way a server starts BuildSystem on an
 * existing {@code plugins/BuildSystem} directory, and stops it again so every storage flushes. Everything the upgrade
 * tests read goes through the plugin's own startup path: config migration, message merge, storages and registries.
 */
final class UpgradeServer implements AutoCloseable {

    private final ServerMock server;
    private final BuildSystemPlugin plugin;
    private final ProxySelector previousProxySelector = ProxySelector.getDefault();
    private final NoNetwork network = new NoNetwork();

    private UpgradeServer(Path dataSource, Path worldContainer, Collection<String> worldNames) {
        // bStats refuses to start unless it was relocated, which only the shaded jar is.
        System.setProperty("bstats.relocatecheck", "false");
        ProxySelector.setDefault(network);
        this.server = MockBukkit.mock(new ContainerServer(worldContainer.toFile()));
        try {
            PluginDescriptionFile description = description();
            File dataFolder = server.getPluginManager()
                    .createTemporaryDirectory(description.getName() + "-" + description.getVersion());
            copyTree(dataSource, dataFolder.toPath());
            stayOffline(dataFolder.toPath());
            // A server that ran the old version has a folder for every world it created or imported.
            for (String worldName : worldNames) {
                Files.createDirectories(worldContainer.resolve(worldName));
            }
            this.plugin = MockBukkit.load(BuildSystemPlugin.class);
        } catch (IOException | RuntimeException e) {
            tearDown();
            throw e instanceof IOException io ? new UncheckedIOException(io) : (RuntimeException) e;
        }
    }

    /**
     * Starts the plugin on a copy of {@code dataSource} and waits until the stored worlds, folders and players are
     * registered.
     */
    static UpgradeServer start(
            Path dataSource, Path worldContainer, Collection<String> worldNames, int folders, int players) {
        UpgradeServer upgradeServer = new UpgradeServer(dataSource, worldContainer, worldNames);
        try {
            upgradeServer.awaitLoaded(worldNames.size(), folders, players);
        } catch (RuntimeException | Error e) {
            // Left mocked, the server would fail every test class after this one.
            upgradeServer.tearDown();
            throw e;
        }
        return upgradeServer;
    }

    /**
     * Turns off the update checker and bStats in the copied data folder, so no start contacts GitHub or bStats. A data
     * folder without a config gets the bundled one, as a fresh install would.
     */
    private static void stayOffline(Path dataFolder) throws IOException {
        File config = dataFolder.resolve("config.yml").toFile();
        if (!config.exists()) {
            try (InputStream in = UpgradeServer.class.getClassLoader().getResourceAsStream("config.yml")) {
                Files.copy(Objects.requireNonNull(in, "config.yml is not on the test classpath"), config.toPath());
            }
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(config);
        if (yaml.getBoolean("settings.update-checker", true)) {
            yaml.set("settings.update-checker", false);
            yaml.save(config);
        }
        Path bStats = dataFolder.resolveSibling("bStats").resolve("config.yml");
        Files.createDirectories(bStats.getParent());
        Files.writeString(bStats, "enabled: false\n");
    }

    static Path resource(String path) {
        URL url = UpgradeServer.class.getClassLoader().getResource(path);
        if (url == null) {
            throw new IllegalArgumentException("Missing test resource " + path);
        }
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    BuildSystemPlugin plugin() {
        return plugin;
    }

    BuildSystem api() {
        return server.getServicesManager().load(BuildSystem.class);
    }

    Path dataFolder() {
        return plugin.getDataFolder().toPath();
    }

    /**
     * The plugin's service graph. The plugin keeps it private, and these tests need the spawn, the setup icons and the
     * parsed config, which no public API exposes.
     */
    Services services() {
        try {
            Field field = BuildSystemPlugin.class.getDeclaredField("services");
            field.setAccessible(true);
            return (Services) field.get(plugin);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Stops the plugin, which saves every storage, and copies the data folder to {@code target} so a later start can
     * read what this one wrote.
     */
    void stopAndCopyTo(Path target) throws IOException {
        server.getPluginManager().disablePlugin(plugin);
        copyTree(dataFolder(), target);
    }

    @Override
    public void close() {
        tearDown();
        if (!network.requests.isEmpty()) {
            throw new AssertionError("The plugin reached the network: " + network.requests);
        }
    }

    private void tearDown() {
        MockBukkit.unmock();
        ProxySelector.setDefault(previousProxySelector);
    }

    private void awaitLoaded(int worlds, int folders, int players) {
        BuildSystem api = api();
        await(
                () -> api.getWorldService().getWorldStorage().getBuildWorlds().size() >= worlds
                        && api.getWorldService().getFolderStorage().getFolders().size() >= folders
                        && api.getPlayerService()
                                        .getPlayerStorage()
                                        .getBuildPlayers()
                                        .size()
                                >= players,
                "worlds, folders and players to load");
        // Let the world-registration task finish its follow-up work (folder assignment, preloads).
        server.getScheduler().performTicks(5);
    }

    private void await(BooleanSupplier condition, String what) {
        // Generous, because the build machine is often busy with other builds.
        long deadline = System.currentTimeMillis() + 60_000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Timed out waiting for " + what);
            }
            server.getScheduler().performOneTick();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
    }

    private static PluginDescriptionFile description() throws IOException {
        try (InputStream in = UpgradeServer.class.getClassLoader().getResourceAsStream("plugin.yml")) {
            if (in == null) {
                throw new IOException("plugin.yml is not on the test classpath");
            }
            return new PluginDescriptionFile(in);
        } catch (InvalidDescriptionException e) {
            throw new IOException(e);
        }
    }

    static void copyTree(Path source, Path target) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * MockBukkit leaves the world container and world folders unimplemented. World loading and import need both.
     */
    /**
     * Refuses every connection made through the JDK's HTTP clients, which ask the default proxy selector first, and
     * remembers where it went, so {@link #close()} can fail the test.
     */
    private static final class NoNetwork extends ProxySelector {

        private final List<URI> requests = new CopyOnWriteArrayList<>();

        @Override
        public List<Proxy> select(URI uri) {
            requests.add(uri);
            throw new IllegalStateException("Upgrade tests must not reach the network: " + uri);
        }

        @Override
        public void connectFailed(URI uri, SocketAddress address, IOException e) {}
    }

    private static final class ContainerServer extends ServerMock {

        private final File container;

        ContainerServer(File container) {
            this.container = container;
        }

        @Override
        public File getWorldContainer() {
            return container;
        }

        @Override
        public World createWorld(WorldCreator creator) {
            World existing = getWorld(creator.name());
            if (existing != null) {
                return existing;
            }
            FolderWorld world = new FolderWorld(creator, new File(container, creator.name()));
            addWorld(world);
            return world;
        }
    }

    private static final class FolderWorld extends WorldMock {

        private final File folder;

        FolderWorld(WorldCreator creator, File folder) {
            super(creator);
            this.folder = folder;
        }

        @Override
        public File getWorldFolder() {
            folder.mkdirs();
            return folder;
        }
    }
}
