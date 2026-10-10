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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Services;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.Backup;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.player.customblock.CustomBlockMenu;
import de.eintosti.buildsystem.player.menu.DesignMenu;
import de.eintosti.buildsystem.player.menu.SettingsMenu;
import de.eintosti.buildsystem.player.menu.SpeedMenu;
import de.eintosti.buildsystem.util.Permissions;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.menu.BackupsMenu;
import de.eintosti.buildsystem.world.menu.BuilderMenu;
import de.eintosti.buildsystem.world.menu.CategoryWorldsMenu;
import de.eintosti.buildsystem.world.menu.CreateMenu;
import de.eintosti.buildsystem.world.menu.DisplayablesContext;
import de.eintosti.buildsystem.world.menu.DisplayablesMenu;
import de.eintosti.buildsystem.world.menu.EditMenu;
import de.eintosti.buildsystem.world.menu.FolderContentMenu;
import de.eintosti.buildsystem.world.menu.GameRulesMenu;
import de.eintosti.buildsystem.world.menu.NavigatorMenu;
import de.eintosti.buildsystem.world.menu.PhysicsMenu;
import de.eintosti.buildsystem.world.menu.SetupMenu;
import de.eintosti.buildsystem.world.menu.StatusMenu;
import de.eintosti.buildsystem.world.menu.setup.CategoryEditorMenu;
import de.eintosti.buildsystem.world.menu.setup.CategoryStatusesMenu;
import de.eintosti.buildsystem.world.menu.setup.DefaultIconsMenu;
import de.eintosti.buildsystem.world.menu.setup.DeletionConfirmMenu;
import de.eintosti.buildsystem.world.menu.setup.DyePickerMenu;
import de.eintosti.buildsystem.world.menu.setup.MaterialPickerMenu;
import de.eintosti.buildsystem.world.menu.setup.NavigatorLayoutMenu;
import de.eintosti.buildsystem.world.menu.setup.StatusEditorMenu;
import de.eintosti.buildsystem.world.menu.setup.StatusLayoutMenu;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Factory and navigation hub for the plugin's GUIs. As the composition root for the menu layer it resolves each menu's
 * collaborators and constructs it, so a menu (or command/listener) opens another menu by calling {@code openX(...)}
 * here rather than constructing it directly. This confines menu wiring to one place and — crucially given that menus
 * open one another cyclically — lets each menu depend only on its own collaborators plus this hub, instead of the
 * compounding constructor parameters that direct construction would force.
 */
@NullMarked
public final class Menus {

    private final BuildSystemPlugin plugin;
    private final Services services;
    private final TaskScheduler scheduler;
    private final MenuContext context;

    public Menus(BuildSystemPlugin plugin, Services services) {
        this.plugin = plugin;
        this.services = services;
        this.scheduler = services.scheduler();
        this.context = new MenuContext(services.messages(), services.menuItems(), this);
    }

    public void openSpeed(Player player) {
        new SpeedMenu(context, services.settings(), player).open(player);
    }

    public void openBlocks(Player player) {
        new CustomBlockMenu(context, player).open(player);
    }

    public void openDesign(Player player) {
        new DesignMenu(context, services.settings(), player).open(player);
    }

    public void openSettings(Player player) {
        new SettingsMenu(
                        context,
                        services.settings(),
                        services.config(),
                        services.navigator(),
                        services.noClip(),
                        player)
                .open(player);
    }

    public void openBackups(BuildWorld buildWorld, Player player) {
        new BackupsMenu(
                        context,
                        services.backup(),
                        services.config(),
                        plugin.getLogger(),
                        scheduler,
                        buildWorld,
                        player)
                .open(player);
    }

    public void openBackupsConfirmation(Backup backup, Player player) {
        Messages messages = services.messages();
        new ConfirmMenu(
                        context,
                        messages.getString("restore_backup_title", player),
                        new ConfirmMenu.Choice(
                                ItemBuilder.of(Material.LIME_DYE)
                                        .name(messages.getString("restore_backup_confirm_name", player))
                                        .lore(messages.getStringList(
                                                "restore_backup_confirm_lore",
                                                player,
                                                Placeholders.of(
                                                        "%timestamp%", messages.formatDateTime(backup.creationTime()))))
                                        .build(),
                                p -> backup.owner().restoreBackup(backup, p)),
                        Permissions.BACKUP,
                        new ConfirmMenu.Choice(
                                ItemBuilder.of(Material.RED_DYE)
                                        .name(messages.getString("restore_backup_cancel_name", player))
                                        .build(),
                                p -> {}),
                        null)
                .open(player);
    }

    /**
     * Opens the world editor with the chest sound, or tells the player the world is not loaded.
     */
    public void openEdit(BuildWorld buildWorld, Player player) {
        if (showEdit(buildWorld, player)) {
            player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1f, 1f);
        }
    }

    /**
     * Opens the world editor again after an action inside it, without the opening sound.
     */
    public void reopenEdit(BuildWorld buildWorld, Player player) {
        showEdit(buildWorld, player);
    }

    /**
     * Every way into the editor goes through here, so a world that unloaded while a menu or chat prompt was open never
     * reaches the menu.
     *
     * @return {@code true} if the editor opened, {@code false} if the world is not loaded
     */
    private boolean showEdit(BuildWorld buildWorld, Player player) {
        if (buildWorld.getWorld().isEmpty()) {
            player.closeInventory();
            player.playSound(player, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1f, 1f);
            player.sendTitle(" ", services.messages().getString("world_not_loaded", player), 5, 70, 20);
            return false;
        }

        new EditMenu(context, services.player(), services.config(), services.prompts(), buildWorld, player)
                .open(player);
        return true;
    }

    public void openBuilder(BuildWorld buildWorld, Player player) {
        new BuilderMenu(context, buildWorld, player).open(player);
    }

    public void promptWorldProject(BuildWorld buildWorld, Player player) {
        services.worldPrompts().promptProject(player, buildWorld, () -> reopenEdit(buildWorld, player));
    }

    public void promptWorldPermission(BuildWorld buildWorld, Player player) {
        services.worldPrompts().promptPermission(player, buildWorld, () -> reopenEdit(buildWorld, player));
    }

    public void promptAddBuilder(BuildWorld buildWorld, Player player) {
        // The builder menu reaches this without the permission check /worlds addBuilder runs first.
        if (!buildWorld.getPermissions().canPerformCommand(player, Permissions.ADDBUILDER)) {
            services.messages().sendPermissionError(player);
            return;
        }
        services.worldPrompts().promptAddBuilder(player, buildWorld, () -> openBuilder(buildWorld, player));
    }

    public void openNavigator(Player player) {
        new NavigatorMenu(context, services.navigatorCategoryRegistry(), player).open(player);
    }

    public void openCategoryWorlds(NavigatorCategory category, Player player) {
        new CategoryWorldsMenu(displayablesContext(), services.worldStatusRegistry(), player, category).open(player);
    }

    public void openFolderContent(NavigatorCategory category, Folder folder, DisplayablesMenu parent, Player player) {
        new FolderContentMenu(displayablesContext(), player, category, folder, parent).open(player);
    }

    /**
     * Bundles the collaborators shared by every {@link DisplayablesMenu} so its constructors stay small.
     */
    private DisplayablesContext displayablesContext() {
        return new DisplayablesContext(
                context,
                services.player(),
                services.settings(),
                services.world(),
                services.prompts(),
                services.navigator());
    }

    public void openCreate(CreateMenu.Page page, Visibility visibility, @Nullable Folder folder, Player player) {
        new CreateMenu(
                        context,
                        services.world(),
                        services.customizableIcons(),
                        plugin.getDataFolder(),
                        page,
                        visibility,
                        folder,
                        player)
                .open(player);
    }

    public void openDelete(BuildWorld buildWorld, Player player) {
        Messages messages = services.messages();
        Placeholders world = Placeholders.of("%world%", buildWorld.getName());
        new ConfirmMenu(
                        context,
                        messages.getString("delete_title", player),
                        new ConfirmMenu.Choice(
                                ItemBuilder.of(Material.LIME_DYE)
                                        .name(messages.getString("delete_world_confirm", player))
                                        .build(),
                                p -> services.world().deleteWorld(p, buildWorld)),
                        null,
                        new ConfirmMenu.Choice(
                                ItemBuilder.of(Material.RED_DYE)
                                        .name(messages.getString("delete_world_cancel", player))
                                        .build(),
                                p -> messages.sendMessage(p, "worlds_delete_canceled", world)),
                        ItemBuilder.of(Material.FILLED_MAP)
                                .name(messages.getString("delete_world_name", player, world))
                                .lore(messages.getStringList("delete_world_name_lore", player))
                                .build())
                .open(player);
    }

    public void openGameRules(BuildWorld buildWorld, Player player) {
        new GameRulesMenu(context, plugin.getLogger(), buildWorld, player).open(player);
    }

    public void openPhysics(BuildWorld buildWorld, Player player) {
        new PhysicsMenu(context, buildWorld, player).open(player);
    }

    public void openMaterialPicker(Player player, Consumer<Material> onPick, Runnable onBack) {
        new MaterialPickerMenu(context, services.prompts(), player, onPick, onBack).open(player);
    }

    public void openDyePicker(Player player, String currentToken, Consumer<String> onPick, Runnable onBack) {
        new DyePickerMenu(context, player, currentToken, onPick, onBack).open(player);
    }

    public void openCategoryEditor(NavigatorCategory category, Player player) {
        new CategoryEditorMenu(
                        context,
                        services.prompts(),
                        services.navigatorCategoryRegistry(),
                        services.worldStatusRegistry(),
                        player,
                        category)
                .open(player);
    }

    public void openStatusEditor(BuildWorldStatus status, Player player) {
        new StatusEditorMenu(context, services.prompts(), services.worldStatusRegistry(), player, status).open(player);
    }

    public void openCategoryStatuses(NavigatorCategory category, Player player) {
        new CategoryStatusesMenu(
                        context, services.navigatorCategoryRegistry(), services.worldStatusRegistry(), player, category)
                .open(player);
    }

    public void openSetup(Player player) {
        new SetupMenu(context, player).open(player);
    }

    public void openDefaultIcons(Player player) {
        new DefaultIconsMenu(context, services.customizableIcons(), player).open(player);
    }

    public void openNavigatorLayout(Player player) {
        new NavigatorLayoutMenu(
                        context,
                        scheduler,
                        services.prompts(),
                        services.navigatorCategoryRegistry(),
                        services.navigatorEditor(),
                        player)
                .open(player);
    }

    public void openStatusLayout(Player player) {
        new StatusLayoutMenu(
                        context,
                        scheduler,
                        services.prompts(),
                        services.worldStatusRegistry(),
                        services.navigatorEditor(),
                        player)
                .open(player);
    }

    public void openDeletionConfirm(
            Player player, String infoName, List<String> infoLore, Runnable onConfirm, Runnable onCancel) {
        new DeletionConfirmMenu(context, player, infoName, infoLore, onConfirm, onCancel).open(player);
    }

    public void openStatus(BuildWorld buildWorld, Player player) {
        new StatusMenu(context, services.worldStatusRegistry(), services.settings(), buildWorld, player).open(player);
    }
}
