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
package de.eintosti.buildsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;

import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.api.BuildSystem;
import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.storage.EntityKind;
import de.eintosti.buildsystem.storage.PlayerStorageImpl;
import de.eintosti.buildsystem.storage.StorageBackend;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.util.Permissions;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.data.WorldDataSchema;
import de.eintosti.buildsystem.world.menu.setup.LayoutEditorMenu;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * Drives the real plugin through a reload, and through a crash, with a player standing in an archive world.
 */
@NullMarked
class ArchiveModeReloadTest {

    private ServerMock server;
    private MockedConstruction<BuildSystemMetrics> metrics;
    private BuildSystemPlugin plugin;
    private World lobby;
    private World archive;
    private PlayerMock bystander;
    private PlayerMock archived;

    @TempDir
    File worldContainer;

    @BeforeEach
    void setUp() {
        // The stored worlds load off the main thread, so the folder they live in comes from the server itself.
        server = MockBukkit.mock(new ServerMock() {
            @Override
            public File getWorldContainer() {
                return worldContainer;
            }
        });
        // bStats refuses to run unrelocated, which it only is in the shaded jar.
        metrics = mockConstruction(BuildSystemMetrics.class);
        lobby = world("lobby");
        archive = world("archive");
        plugin = MockBukkit.load(BuildSystemPlugin.class);
        // The scoreboard needs a real server class, so it is turned off before anyone joins.
        plugin.getConfig().set("settings.scoreboard", false);
        plugin.saveConfig();
        plugin.reloadConfigData(false);

        worldStorage()
                .addBuildWorld(new BuildWorldImpl(
                        TestData.worldContext(),
                        UUID.randomUUID(),
                        "archive",
                        BuildWorldType.NORMAL,
                        WorldDataSchema.create("archive", TestData.ARCHIVE_STATUS),
                        Builder.of(UUID.randomUUID(), "Creator"),
                        List.of(),
                        System.currentTimeMillis(),
                        null,
                        null));

        bystander = SoundlessPlayer.join(server, "Bystander");
        archived = SoundlessPlayer.join(server, "Archived");
        // MockBukkit writes the navigator's item flags as Java objects, which YAML refuses to read back.
        // A server writes them as text.
        archived.getInventory().clear();
        archived.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));
        archived.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        moveTo(archived, archive);
    }

    @AfterEach
    void tearDown() {
        metrics.close();
        MockBukkit.unmock();
    }

    @Test
    void reload_handsTheInventoryBackOnDisable_andEntersArchiveModeAgainOnEnable() {
        assertInArchiveMode();

        server.getPluginManager().disablePlugin(plugin);
        assertOutOfArchiveMode();

        server.getPluginManager().enablePlugin(plugin);
        awaitStoredWorlds();
        assertInArchiveMode();
        flushWrites();
        assertTrue(snapshotStored());

        moveTo(archived, lobby);
        assertOutOfArchiveMode();
    }

    @Test
    void configReload_flippingTheVanish_showsOrHidesArchivedPlayers() {
        assertInArchiveMode();

        setVanish(false);
        assertTrue(bystander.canSee(archived));
        assertNull(archived.getPotionEffect(PotionEffectType.INVISIBILITY));
        assertEquals(0, diamonds(), "still in archive mode");

        setVanish(true);
        assertFalse(bystander.canSee(archived));
        assertNotNull(archived.getPotionEffect(PotionEffectType.INVISIBILITY));
    }

    @Test
    void configReload_withAnInvalidValue_keepsThePreviousConfigWhole() {
        plugin.getConfig().set("settings.archive.vanish", false);
        plugin.getConfig().set("storage.table-prefix", "Not A Prefix");
        plugin.saveConfig();

        assertFalse(plugin.reloadConfigData(true));

        assertFalse(bystander.canSee(archived), "the kept config still vanishes archived players");
    }

    @Test
    void crash_thenRejoinElsewhere_handsTheRealInventoryBackOnce() {
        assertEquals(GameMode.ADVENTURE, archived.getGameMode());
        flushWrites();
        assertTrue(snapshotStored(), "stored as soon as the player entered archive mode");

        crash();
        // Teleporting fires a world change, which a player logging in does not.
        archived.setLocation(lobby.getSpawnLocation());
        rejoin();

        assertOutOfArchiveMode();
        assertEquals(GameMode.SURVIVAL, archived.getGameMode());
        flushWrites();
        assertFalse(snapshotStored());

        archived.getInventory().clear();
        archived.getInventory().setItem(0, new ItemStack(Material.EMERALD));
        crash();
        rejoin();

        assertEquals(0, diamonds(), "a snapshot is never applied twice");
        assertEquals(
                Material.EMERALD,
                Objects.requireNonNull(archived.getInventory().getItem(0)).getType());
    }

    @Test
    void crash_thenRejoinInTheArchive_handsTheRealInventoryBack_andEntersArchiveModeAgain() {
        flushWrites();
        crash();
        rejoin();

        assertInArchiveMode();
        assertEquals(GameMode.ADVENTURE, archived.getGameMode());

        moveTo(archived, lobby);
        assertOutOfArchiveMode();
        assertEquals(GameMode.SURVIVAL, archived.getGameMode());
    }

    @Test
    void crashTwice_inTheArchive_neverStoresTheArchiveInventory() {
        flushWrites();
        crash();
        rejoin();
        assertInArchiveMode();

        flushWrites();
        crash();
        // Teleporting fires a world change, which a player logging in does not.
        archived.setLocation(lobby.getSpawnLocation());
        rejoin();

        assertOutOfArchiveMode();
    }

    @Test
    void joiningInTheArchive_storesTheSnapshot() {
        // The navigator item again, see setUp.
        bystander.getInventory().clear();
        bystander.setLocation(archive.getSpawnLocation());
        server.getPluginManager().callEvent(new PlayerJoinEvent(bystander, ""));

        flushWrites();
        assertTrue(snapshotStored(bystander));
    }

    @Test
    void leavingTheArchive_clearsTheStoredSnapshot() {
        flushWrites();
        assertTrue(snapshotStored());

        moveTo(archived, lobby);

        flushWrites();
        assertFalse(snapshotStored());
    }

    @Test
    void quitting_clearsTheStoredSnapshot() {
        flushWrites();
        assertTrue(snapshotStored());

        archived.disconnect();

        flushWrites();
        assertFalse(snapshotStored());
    }

    @Test
    void disabling_clearsTheStoredSnapshot() {
        flushWrites();
        assertTrue(snapshotStored());

        server.getPluginManager().disablePlugin(plugin);

        assertFalse(snapshotStored());
    }

    @Test
    void disabling_withTheLayoutEditorOpen_handsTheRealInventoryBack() {
        archived.addAttachment(plugin, Permissions.SETUP, true);
        archived.performCommand("setup");
        // XSkull needs authlib, which only the server provides; plain heads are enough for the editor's controls.
        try (MockedStatic<ItemBuilder> itemBuilder = mockStatic(ItemBuilder.class, CALLS_REAL_METHODS)) {
            itemBuilder
                    .when(() -> ItemBuilder.skull(any(Profileable.class)))
                    .thenAnswer(invocation -> ItemBuilder.of(Material.PLAYER_HEAD));
            // The statuses button in the setup hub.
            server.getPluginManager()
                    .callEvent(new InventoryClickEvent(
                            archived.getOpenInventory(),
                            SlotType.CONTAINER,
                            13,
                            ClickType.LEFT,
                            InventoryAction.PICKUP_ALL));
        }
        assertInstanceOf(
                LayoutEditorMenu.class,
                archived.getOpenInventory().getTopInventory().getHolder());

        server.getPluginManager().disablePlugin(plugin);

        assertOutOfArchiveMode();
    }

    private void assertInArchiveMode() {
        assertEquals(0, diamonds());
        assertNull(archived.getInventory().getHelmet());
        assertNotNull(archived.getPotionEffect(PotionEffectType.INVISIBILITY));
        assertFalse(bystander.canSee(archived));
    }

    private void assertOutOfArchiveMode() {
        assertEquals(5, diamonds());
        assertEquals(
                Material.DIAMOND_HELMET,
                Objects.requireNonNull(archived.getInventory().getHelmet()).getType());
        assertNull(archived.getPotionEffect(PotionEffectType.INVISIBILITY));
        assertTrue(bystander.canSee(archived));
    }

    /**
     * Ticks the server until the re-enabled plugin has registered the stored archive world, which it loads off the main
     * thread.
     */
    private void awaitStoredWorlds() {
        long deadline = System.currentTimeMillis() + 10_000;
        while (worldStorage().getBuildWorld("archive") == null) {
            assertTrue(System.currentTimeMillis() < deadline, "the stored worlds never loaded");
            server.getScheduler().performOneTick();
            Thread.onSpinWait();
        }
    }

    /**
     * Drops what the plugin holds in memory and loads the players from disk again, the way a server start after a crash
     * would. The player keeps the emptied archive inventory, which is what the server saved for them.
     */
    private void crash() {
        PlayerStorageImpl storage = playerStorage();
        BuildPlayer before = storage.getBuildPlayer(archived.getUniqueId());
        storage.loadPlayers();
        long deadline = System.currentTimeMillis() + 10_000;
        while (storage.getBuildPlayer(archived.getUniqueId()) == before) {
            assertTrue(System.currentTimeMillis() < deadline, "the players never loaded");
            Thread.onSpinWait();
        }
    }

    private void rejoin() {
        server.getPluginManager().callEvent(new PlayerJoinEvent(archived, ""));
    }

    /**
     * Waits for every queued write to the player storage: writes run in order, so an empty save lands after them.
     */
    private void flushWrites() {
        playerStorage().save(List.of()).join();
    }

    private boolean snapshotStored() {
        return snapshotStored(archived);
    }

    private boolean snapshotStored(PlayerMock player) {
        // A connection of its own, so this also reads what a disabled plugin left behind.
        PluginConfig.Storage.Database settings;
        try {
            settings = new ConfigService(plugin).readStorageSettings();
        } catch (IOException | InvalidConfigurationException e) {
            throw new IllegalStateException(e);
        }
        StorageBackend backend =
                StorageBackend.connect(settings.type(), settings, plugin.getDataFolder(), plugin.getLogger());
        try {
            Map<String, Object> stored = backend.collection(EntityKind.PLAYERS)
                    .loadAll()
                    .get(player.getUniqueId().toString());
            return stored != null && stored.containsKey("archive-snapshot");
        } finally {
            backend.close();
        }
    }

    private PlayerStorageImpl playerStorage() {
        return (PlayerStorageImpl)
                Objects.requireNonNull(server.getServicesManager().load(BuildSystem.class))
                        .getPlayerService()
                        .getPlayerStorage();
    }

    private WorldStorageImpl worldStorage() {
        return (WorldStorageImpl)
                Objects.requireNonNull(server.getServicesManager().load(BuildSystem.class))
                        .getWorldService()
                        .getWorldStorage();
    }

    private int diamonds() {
        return Arrays.stream(archived.getInventory().getContents())
                .filter(item -> item != null && item.getType() == Material.DIAMOND)
                .mapToInt(ItemStack::getAmount)
                .sum();
    }

    private void setVanish(boolean vanish) {
        plugin.getConfig().set("settings.archive.vanish", vanish);
        plugin.saveConfig();
        plugin.reloadConfigData(true);
    }

    /**
     * {@return a world with a folder in the world container} Loading the stored worlds looks at their folders, which a
     * plain MockBukkit world does not have.
     */
    private World world(String name) {
        WorldMock world = new WorldMock(new WorldCreator(name)) {
            @Override
            public File getWorldFolder() {
                return new File(worldContainer, name);
            }
        };
        server.addWorld(world);
        return world;
    }

    private void moveTo(PlayerMock player, World world) {
        World from = player.getWorld();
        player.setLocation(world.getSpawnLocation());
        server.getPluginManager().callEvent(new PlayerChangedWorldEvent(player, from));
    }
}
