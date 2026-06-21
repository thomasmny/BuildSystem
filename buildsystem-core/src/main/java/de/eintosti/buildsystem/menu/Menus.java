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

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.Backup;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.command.subcommand.worlds.AddBuilderSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetPermissionSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetProjectSubCommand;
import de.eintosti.buildsystem.player.customblock.CustomBlockMenu;
import de.eintosti.buildsystem.player.menu.DesignMenu;
import de.eintosti.buildsystem.player.menu.SettingsMenu;
import de.eintosti.buildsystem.player.menu.SpeedMenu;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.menu.BackupsConfirmationMenu;
import de.eintosti.buildsystem.world.menu.BackupsMenu;
import de.eintosti.buildsystem.world.menu.BuilderMenu;
import de.eintosti.buildsystem.world.menu.CategoryWorldsMenu;
import de.eintosti.buildsystem.world.menu.CreateMenu;
import de.eintosti.buildsystem.world.menu.DeleteMenu;
import de.eintosti.buildsystem.world.menu.DisplayablesContext;
import de.eintosti.buildsystem.world.menu.DisplayablesMenu;
import de.eintosti.buildsystem.world.menu.EditMenu;
import de.eintosti.buildsystem.world.menu.FolderContentMenu;
import de.eintosti.buildsystem.world.menu.GameRulesMenu;
import de.eintosti.buildsystem.world.menu.NavigatorMenu;
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
import org.bukkit.NamespacedKey;
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
    private final TaskScheduler scheduler;
    private final NamespacedKey builderNameKey;

    public Menus(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = new TaskScheduler(plugin);
        this.builderNameKey = new NamespacedKey(plugin, "builder_name");
    }

    public void openSpeed(Player player) {
        new SpeedMenu(plugin.getMessages(), plugin.getSettingsService(), player).open(player);
    }

    public void openBlocks(Player player) {
        new CustomBlockMenu(plugin.getMessages(), plugin.getMenuItems(), player).open(player);
    }

    public void openDesign(Player player) {
        new DesignMenu(plugin.getMessages(), plugin.getSettingsService(), plugin.getMenuItems(), this, player)
                .open(player);
    }

    public void openSettings(Player player) {
        new SettingsMenu(
                        plugin.getMessages(),
                        plugin.getSettingsService(),
                        plugin.getConfigService(),
                        plugin.getMenuItems(),
                        plugin.getNavigatorService(),
                        plugin.getNoClipService(),
                        this,
                        player)
                .open(player);
    }

    public void openBackups(BuildWorld buildWorld, Player player) {
        new BackupsMenu(
                        plugin.getMessages(),
                        plugin.getBackupService(),
                        plugin.getMenuItems(),
                        plugin.getConfigService(),
                        plugin.getLogger(),
                        scheduler,
                        this,
                        buildWorld,
                        player)
                .open(player);
    }

    public void openBackupsConfirmation(Backup backup, Player player) {
        new BackupsConfirmationMenu(plugin.getMessages(), plugin.getConfigService(), backup, player).open(player);
    }

    public void openEdit(BuildWorld buildWorld, Player player) {
        new EditMenu(
                        plugin.getMessages(),
                        plugin.getPlayerService(),
                        plugin.getMenuItems(),
                        plugin.getConfigService(),
                        plugin.getPrompts(),
                        this,
                        buildWorld,
                        player)
                .open(player);
    }

    public void openBuilder(BuildWorld buildWorld, Player player) {
        new BuilderMenu(
                        plugin.getMessages(),
                        plugin.getMenuItems(),
                        this,
                        plugin.getPlayerLookupService(),
                        scheduler,
                        plugin.getLogger(),
                        builderNameKey,
                        buildWorld,
                        player)
                .open(player);
    }

    /**
     * Opens the world-project chat prompt. A transitional bridge that reuses the subcommand's input flow so the editor
     * menu need not depend on the plugin; folds into the command layer once that is constructor-injected.
     */
    public void promptWorldProject(BuildWorld buildWorld, Player player) {
        new SetProjectSubCommand(
                        plugin.getMessages(),
                        plugin.getWorldService(),
                        this,
                        plugin.getPrompts(),
                        plugin.getSettingsService())
                .getProjectInput(player, buildWorld, false);
    }

    /** Opens the world-permission chat prompt; transitional bridge, see {@link #promptWorldProject}. */
    public void promptWorldPermission(BuildWorld buildWorld, Player player) {
        new SetPermissionSubCommand(
                        plugin.getMessages(),
                        plugin.getWorldService(),
                        plugin.getConfigService(),
                        this,
                        plugin.getPrompts(),
                        plugin.getSettingsService())
                .getPermissionInput(player, buildWorld, false);
    }

    /** Opens the add-builder chat prompt; transitional bridge, see {@link #promptWorldProject}. */
    public void promptAddBuilder(BuildWorld buildWorld, Player player) {
        new AddBuilderSubCommand(
                        plugin.getMessages(),
                        plugin.getWorldService(),
                        this,
                        plugin.getPlayerLookupService(),
                        plugin.getPrompts(),
                        scheduler)
                .getAddBuilderInput(player, buildWorld, false);
    }

    public void openNavigator(Player player) {
        new NavigatorMenu(
                        plugin.getMessages(),
                        plugin.getMenuItems(),
                        this,
                        plugin.getNavigatorCategoryRegistry(),
                        player)
                .open(player);
    }

    public void openCategoryWorlds(NavigatorCategory category, Player player) {
        new CategoryWorldsMenu(displayablesContext(), plugin.getWorldStatusRegistry(), player, category).open(player);
    }

    public void openFolderContent(NavigatorCategory category, Folder folder, DisplayablesMenu parent, Player player) {
        new FolderContentMenu(displayablesContext(), player, category, folder, parent).open(player);
    }

    /** Bundles the collaborators shared by every {@link DisplayablesMenu} so its constructors stay small. */
    private DisplayablesContext displayablesContext() {
        return new DisplayablesContext(
                plugin.getMessages(),
                plugin.getPlayerService(),
                plugin.getSettingsService(),
                plugin.getWorldService(),
                plugin.getMenuItems(),
                plugin.getPrompts(),
                plugin.getNavigatorService(),
                this);
    }

    public void openCreate(CreateMenu.Page page, Visibility visibility, @Nullable Folder folder, Player player) {
        new CreateMenu(
                        plugin.getMessages(),
                        plugin.getMenuItems(),
                        this,
                        plugin.getWorldService(),
                        plugin.getCustomizableIcons(),
                        plugin.getDataFolder(),
                        page,
                        visibility,
                        folder,
                        player)
                .open(player);
    }

    public void openDelete(BuildWorld buildWorld, Player player) {
        new DeleteMenu(plugin.getMessages(), plugin.getWorldService(), buildWorld, player).open(player);
    }

    public void openGameRules(BuildWorld buildWorld, Player player) {
        new GameRulesMenu(plugin.getMessages(), plugin.getMenuItems(), plugin.getLogger(), this, buildWorld, player)
                .open(player);
    }

    public void openMaterialPicker(Player player, Consumer<XMaterial> onPick, Runnable onBack) {
        new MaterialPickerMenu(plugin.getMessages(), plugin.getMenuItems(), plugin.getPrompts(), player, onPick, onBack)
                .open(player);
    }

    public void openDyePicker(Player player, String currentToken, Consumer<String> onPick, Runnable onBack) {
        new DyePickerMenu(plugin.getMessages(), plugin.getMenuItems(), player, currentToken, onPick, onBack)
                .open(player);
    }

    public void openCategoryEditor(NavigatorCategory category, Player player) {
        new CategoryEditorMenu(
                        plugin.getMessages(),
                        plugin.getPrompts(),
                        this,
                        plugin.getMenuItems(),
                        plugin.getNavigatorCategoryRegistry(),
                        plugin.getWorldStatusRegistry(),
                        player,
                        category)
                .open(player);
    }

    public void openStatusEditor(BuildWorldStatus status, Player player) {
        new StatusEditorMenu(
                        plugin.getMessages(),
                        plugin.getPrompts(),
                        this,
                        plugin.getMenuItems(),
                        plugin.getWorldStatusRegistry(),
                        player,
                        status)
                .open(player);
    }

    public void openCategoryStatuses(NavigatorCategory category, Player player) {
        new CategoryStatusesMenu(
                        plugin.getMessages(),
                        plugin.getMenuItems(),
                        this,
                        plugin.getNavigatorCategoryRegistry(),
                        plugin.getWorldStatusRegistry(),
                        player,
                        category)
                .open(player);
    }

    public void openSetup(Player player) {
        new SetupMenu(plugin.getMessages(), plugin.getMenuItems(), this, player).open(player);
    }

    public void openDefaultIcons(Player player) {
        new DefaultIconsMenu(plugin.getMessages(), plugin.getMenuItems(), this, plugin.getCustomizableIcons(), player)
                .open(player);
    }

    public void openNavigatorLayout(Player player) {
        new NavigatorLayoutMenu(
                        plugin.getMessages(),
                        plugin.getMenuItems(),
                        this,
                        scheduler,
                        plugin.getPrompts(),
                        plugin.getNavigatorCategoryRegistry(),
                        plugin.getNavigatorEditorService(),
                        player)
                .open(player);
    }

    public void openStatusLayout(Player player) {
        new StatusLayoutMenu(
                        plugin.getMessages(),
                        plugin.getMenuItems(),
                        this,
                        scheduler,
                        plugin.getPrompts(),
                        plugin.getWorldStatusRegistry(),
                        plugin.getNavigatorEditorService(),
                        player)
                .open(player);
    }

    public void openDeletionConfirm(
            Player player, String infoName, List<String> infoLore, Runnable onConfirm, Runnable onCancel) {
        new DeletionConfirmMenu(
                        plugin.getMessages(), plugin.getMenuItems(), player, infoName, infoLore, onConfirm, onCancel)
                .open(player);
    }

    public void openStatus(BuildWorld buildWorld, Player player) {
        new StatusMenu(
                        plugin.getMessages(),
                        plugin.getWorldStatusRegistry(),
                        plugin.getSettingsService(),
                        plugin.getMenuItems(),
                        this,
                        buildWorld,
                        player)
                .open(player);
    }
}
