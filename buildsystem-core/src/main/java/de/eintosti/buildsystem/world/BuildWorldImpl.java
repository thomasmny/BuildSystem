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
package de.eintosti.buildsystem.world;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.access.WorldPermissions;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.creation.generator.CustomGenerator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.lifecycle.WorldTeleporter;
import de.eintosti.buildsystem.command.subcommand.worlds.WorldsArgument;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.world.builder.BuildersImpl;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import de.eintosti.buildsystem.world.data.WorldDataImpl.WorldDataBuilder;
import de.eintosti.buildsystem.world.lifecycle.WorldLoaderImpl;
import de.eintosti.buildsystem.world.lifecycle.WorldPermissionsImpl;
import de.eintosti.buildsystem.world.lifecycle.WorldTeleporterImpl;
import de.eintosti.buildsystem.world.lifecycle.WorldUnloaderImpl;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class BuildWorldImpl implements BuildWorld {

    private final UUID uuid;
    private String name;
    private boolean loaded;

    @Nullable private Folder folder;

    private final BuildWorldType worldType;
    private final WorldDataImpl worldData;
    private final BuildersImpl builders;

    @Nullable private final CustomGenerator customGenerator;

    private final long creation;

    private final BuildSystemPlugin plugin;
    private final WorldLoaderImpl worldLoader;
    private final WorldUnloaderImpl worldUnloader;

    public BuildWorldImpl(
            BuildSystemPlugin plugin,
            String name,
            @Nullable Builder creator,
            BuildWorldType worldType,
            long creation,
            boolean privateWorld,
            @Nullable CustomGenerator customGenerator,
            @Nullable Folder folder) {
        this(
                plugin,
                UUID.randomUUID(),
                name,
                worldType,
                defaultWorldData(plugin, name, worldType, privateWorld),
                creator,
                new ArrayList<>(),
                creation,
                customGenerator,
                folder);
    }

    /**
     * Builds the {@link WorldDataImpl} for a freshly created world, seeded from the configured world defaults.
     * Extracted from the constructor so the repeated {@code config.world().defaults()} lookup can be bound to a single
     * local instead of being re-walked for every field.
     */
    private static WorldDataImpl defaultWorldData(
            BuildSystemPlugin plugin, String name, BuildWorldType worldType, boolean privateWorld) {
        var defaults = plugin.getConfigService().current().world().defaults();
        String permission = (privateWorld
                        ? defaults.permission().privatePermission()
                        : defaults.permission().publicPermission())
                .replace("%world%", name);
        boolean buildersEnabled = privateWorld
                ? defaults.buildersEnabled().privateBuilders()
                : defaults.buildersEnabled().publicBuilders();
        return new WorldDataBuilder(name)
                .withPrivateWorld(privateWorld)
                .withMaterial(
                        privateWorld
                                ? XMaterial.PLAYER_HEAD
                                : plugin.getCustomizableIcons().getIcon(worldType))
                .withPermission(permission)
                .withDifficulty(defaults.difficulty())
                .withBlockBreaking(defaults.blockBreaking())
                .withBlockInteractions(defaults.blockInteractions())
                .withBlockPlacement(defaults.blockPlacement())
                .withExplosions(defaults.explosions())
                .withMobAi(defaults.mobAi())
                .withPhysics(defaults.physics())
                .withBuildersEnabled(buildersEnabled)
                .withPermissionOverrideEnabled(
                        () -> plugin.getConfigService().current().folder().overridePermissions())
                .withProjectOverrideEnabled(
                        () -> plugin.getConfigService().current().folder().overrideProjects())
                .build();
    }

    public BuildWorldImpl(
            BuildSystemPlugin plugin,
            UUID uuid,
            String name,
            BuildWorldType worldType,
            WorldDataImpl worldData,
            @Nullable Builder creator,
            List<Builder> builders,
            long creation,
            @Nullable CustomGenerator customGenerator,
            @Nullable Folder folder) {
        this.plugin = plugin;
        this.uuid = uuid;
        this.name = name;
        this.worldType = worldType;
        this.worldData = worldData;
        this.builders = new BuildersImpl(plugin.getMessages(), creator, builders);
        this.creation = creation;
        this.customGenerator = customGenerator;
        this.folder = folder;

        this.worldData.setFolderResolver(this::getFolder);
        this.worldLoader = WorldLoaderImpl.of(plugin, this);
        this.worldUnloader = WorldUnloaderImpl.of(plugin, this);
        this.worldUnloader.manageUnload();
    }

    /**
     * Gets the Bukkit {@link World} associated with this {@link BuildWorld}.
     *
     * @return The Bukkit world, or {@code null} if not loaded
     */
    @Nullable public World getWorld() {
        return Bukkit.getWorld(name);
    }

    @Override
    public UUID getUniqueId() {
        return this.uuid;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
        this.worldData.setWorldName(name);
    }

    @Override
    public XMaterial getIcon() {
        return this.worldData.material().get();
    }

    @Override
    public void setIcon(XMaterial material) {
        this.worldData.material().set(material);
    }

    @Override
    public String getDisplayName(Player player) {
        return plugin.getMessages().getString("world_item_title", player, Map.entry("%world%", this.name));
    }

    @Override
    public List<String> getLore(Player player) {
        @SuppressWarnings("unchecked")
        Map.Entry<String, Object>[] placeholders = List.of(
                        Map.entry(
                                "%status%",
                                plugin.getMessages()
                                        .getString(
                                                Messages.getMessageKey(
                                                        worldData.status().get()),
                                                player)),
                        Map.entry("%project%", worldData.project().get()),
                        Map.entry("%permission%", worldData.permission().get()),
                        Map.entry(
                                "%creator%",
                                builders.hasCreator() ? builders.getCreator().getName() : "-"),
                        Map.entry("%creation%", plugin.getMessages().formatDate(getCreation())),
                        Map.entry(
                                "%lastedited%",
                                plugin.getMessages()
                                        .formatDate(worldData.lastEdited().get())),
                        Map.entry(
                                "%lastloaded%",
                                plugin.getMessages()
                                        .formatDate(worldData.lastLoaded().get())),
                        Map.entry(
                                "%lastunloaded%",
                                plugin.getMessages()
                                        .formatDate(worldData.lastUnloaded().get())))
                .toArray(Map.Entry[]::new);

        List<String> messageList = getPermissions().canPerformCommand(player, WorldsArgument.EDIT.getPermission())
                ? plugin.getMessages().getStringList("world_item_lore_edit", player, placeholders)
                : plugin.getMessages().getStringList("world_item_lore_normal", player, placeholders);

        List<String> lore = new ArrayList<>();

        for (String line : messageList) {
            if (!line.contains("%builders%")) {
                lore.add(line);
                continue;
            }

            // The first builder line replaces the placeholder in-line; overflow lines become own lore entries
            List<String> builderLines = builders.formatBuildersForLore(player, 3);
            lore.add(line.replace("%builders%", builderLines.getFirst()));
            for (int i = 1; i < builderLines.size(); i++) {
                lore.add(builderLines.get(i));
            }
        }

        return lore;
    }

    @Override
    public void addToInventory(Inventory inventory, int slot, Player player) {
        if (getIcon() == XMaterial.PLAYER_HEAD) {
            plugin.getMenuItems().addWorldItem(inventory, slot, this, getDisplayName(player), getLore(player));
            return;
        }
        BuildWorld.super.addToInventory(inventory, slot, player);
    }

    @Override
    public Profileable asProfilable() {
        return builders.hasCreator() ? Profileable.of(builders.getCreator().getUniqueId()) : Profileable.username(name);
    }

    @Override
    public BuildWorldType getType() {
        return worldType;
    }

    @Override
    public WorldData getData() {
        return worldData;
    }

    @Override
    public long getCreation() {
        return creation;
    }

    @Override
    @Nullable public CustomGenerator getCustomGenerator() {
        return customGenerator;
    }

    @Override
    public Difficulty cycleDifficulty() {
        Difficulty newDifficulty =
                switch (worldData.difficulty().get()) {
                    case PEACEFUL -> Difficulty.EASY;
                    case EASY -> Difficulty.NORMAL;
                    case NORMAL -> Difficulty.HARD;
                    case HARD -> Difficulty.PEACEFUL;
                };
        worldData.difficulty().set(newDifficulty);
        return newDifficulty;
    }

    @Override
    public Builders getBuilders() {
        return builders;
    }

    @Override
    public String getWorldTime() {
        World bukkitWorld = getWorld();
        if (bukkitWorld == null) {
            return "?";
        }
        return String.valueOf(bukkitWorld.getTime());
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    @Override
    public WorldLoaderImpl getLoader() {
        return worldLoader;
    }

    @Override
    public WorldUnloaderImpl getUnloader() {
        return worldUnloader;
    }

    @Override
    public WorldTeleporter getTeleporter() {
        return WorldTeleporterImpl.of(plugin, this);
    }

    @Override
    public WorldPermissions getPermissions() {
        return WorldPermissionsImpl.of(plugin, this);
    }

    @Override
    @Nullable public Folder getFolder() {
        return folder;
    }

    @Override
    public boolean isAssignedToFolder() {
        return getFolder() != null;
    }

    @Override
    public void setFolder(@Nullable Folder folder) {
        if (Objects.equals(this.folder, folder)) {
            return;
        }

        Folder oldFolder = this.folder;
        this.folder = folder;
        if (oldFolder != null) {
            oldFolder.removeWorld(this);
        }
        if (folder != null) {
            folder.addWorld(this);
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BuildWorldImpl that = (BuildWorldImpl) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
