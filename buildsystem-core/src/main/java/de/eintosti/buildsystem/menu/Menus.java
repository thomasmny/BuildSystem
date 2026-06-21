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
import de.eintosti.buildsystem.Services;
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
    private final Services services;
    private final TaskScheduler scheduler;
    private final NamespacedKey builderNameKey;

    public Menus(BuildSystemPlugin plugin, Services services) {
        this.plugin = plugin;
        this.services = services;
        this.scheduler = new TaskScheduler(plugin);
        this.builderNameKey = new NamespacedKey(plugin, "builder_name");
    }

    public void openSpeed(Player player) {
        new SpeedMenu(services.messages(), services.settings(), player).open(player);
    }

    public void openBlocks(Player player) {
        new CustomBlockMenu(services.messages(), services.menuItems(), player).open(player);
    }

    public void openDesign(Player player) {
        new DesignMenu(services.messages(), services.settings(), services.menuItems(), this, player).open(player);
    }

    public void openSettings(Player player) {
        new SettingsMenu(
                        services.messages(),
                        services.settings(),
                        services.config(),
                        services.menuItems(),
                        services.navigator(),
                        services.noClip(),
                        this,
                        player)
                .open(player);
    }

    public void openBackups(BuildWorld buildWorld, Player player) {
        new BackupsMenu(
                        services.messages(),
                        services.backup(),
                        services.menuItems(),
                        services.config(),
                        plugin.getLogger(),
                        scheduler,
                        this,
                        buildWorld,
                        player)
                .open(player);
    }

    public void openBackupsConfirmation(Backup backup, Player player) {
        new BackupsConfirmationMenu(services.messages(), services.config(), backup, player).open(player);
    }

    public void openEdit(BuildWorld buildWorld, Player player) {
        new EditMenu(
                        services.messages(),
                        services.player(),
                        services.menuItems(),
                        services.config(),
                        services.prompts(),
                        this,
                        buildWorld,
                        player)
                .open(player);
    }

    public void openBuilder(BuildWorld buildWorld, Player player) {
        new BuilderMenu(
                        services.messages(),
                        services.menuItems(),
                        this,
                        services.playerLookup(),
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
        new SetProjectSubCommand(services.messages(), services.world(), this, services.prompts(), services.settings())
                .getProjectInput(player, buildWorld, false);
    }

    /** Opens the world-permission chat prompt; transitional bridge, see {@link #promptWorldProject}. */
    public void promptWorldPermission(BuildWorld buildWorld, Player player) {
        new SetPermissionSubCommand(
                        services.messages(),
                        services.world(),
                        services.config(),
                        this,
                        services.prompts(),
                        services.settings())
                .getPermissionInput(player, buildWorld, false);
    }

    /** Opens the add-builder chat prompt; transitional bridge, see {@link #promptWorldProject}. */
    public void promptAddBuilder(BuildWorld buildWorld, Player player) {
        new AddBuilderSubCommand(
                        services.messages(),
                        services.world(),
                        this,
                        services.playerLookup(),
                        services.prompts(),
                        scheduler)
                .getAddBuilderInput(player, buildWorld, false);
    }

    public void openNavigator(Player player) {
        new NavigatorMenu(services.messages(), services.menuItems(), this, services.navigatorCategoryRegistry(), player)
                .open(player);
    }

    public void openCategoryWorlds(NavigatorCategory category, Player player) {
        new CategoryWorldsMenu(displayablesContext(), services.worldStatusRegistry(), player, category).open(player);
    }

    public void openFolderContent(NavigatorCategory category, Folder folder, DisplayablesMenu parent, Player player) {
        new FolderContentMenu(displayablesContext(), player, category, folder, parent).open(player);
    }

    /** Bundles the collaborators shared by every {@link DisplayablesMenu} so its constructors stay small. */
    private DisplayablesContext displayablesContext() {
        return new DisplayablesContext(
                services.messages(),
                services.player(),
                services.settings(),
                services.world(),
                services.menuItems(),
                services.prompts(),
                services.navigator(),
                this);
    }

    public void openCreate(CreateMenu.Page page, Visibility visibility, @Nullable Folder folder, Player player) {
        new CreateMenu(
                        services.messages(),
                        services.menuItems(),
                        this,
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
        new DeleteMenu(services.messages(), services.world(), buildWorld, player).open(player);
    }

    public void openGameRules(BuildWorld buildWorld, Player player) {
        new GameRulesMenu(services.messages(), services.menuItems(), plugin.getLogger(), this, buildWorld, player)
                .open(player);
    }

    public void openMaterialPicker(Player player, Consumer<XMaterial> onPick, Runnable onBack) {
        new MaterialPickerMenu(services.messages(), services.menuItems(), services.prompts(), player, onPick, onBack)
                .open(player);
    }

    public void openDyePicker(Player player, String currentToken, Consumer<String> onPick, Runnable onBack) {
        new DyePickerMenu(services.messages(), services.menuItems(), player, currentToken, onPick, onBack).open(player);
    }

    public void openCategoryEditor(NavigatorCategory category, Player player) {
        new CategoryEditorMenu(
                        services.messages(),
                        services.prompts(),
                        this,
                        services.menuItems(),
                        services.navigatorCategoryRegistry(),
                        services.worldStatusRegistry(),
                        player,
                        category)
                .open(player);
    }

    public void openStatusEditor(BuildWorldStatus status, Player player) {
        new StatusEditorMenu(
                        services.messages(),
                        services.prompts(),
                        this,
                        services.menuItems(),
                        services.worldStatusRegistry(),
                        player,
                        status)
                .open(player);
    }

    public void openCategoryStatuses(NavigatorCategory category, Player player) {
        new CategoryStatusesMenu(
                        services.messages(),
                        services.menuItems(),
                        this,
                        services.navigatorCategoryRegistry(),
                        services.worldStatusRegistry(),
                        player,
                        category)
                .open(player);
    }

    public void openSetup(Player player) {
        new SetupMenu(services.messages(), services.menuItems(), this, player).open(player);
    }

    public void openDefaultIcons(Player player) {
        new DefaultIconsMenu(services.messages(), services.menuItems(), this, services.customizableIcons(), player)
                .open(player);
    }

    public void openNavigatorLayout(Player player) {
        new NavigatorLayoutMenu(
                        services.messages(),
                        services.menuItems(),
                        this,
                        scheduler,
                        services.prompts(),
                        services.navigatorCategoryRegistry(),
                        services.navigatorEditor(),
                        player)
                .open(player);
    }

    public void openStatusLayout(Player player) {
        new StatusLayoutMenu(
                        services.messages(),
                        services.menuItems(),
                        this,
                        scheduler,
                        services.prompts(),
                        services.worldStatusRegistry(),
                        services.navigatorEditor(),
                        player)
                .open(player);
    }

    public void openDeletionConfirm(
            Player player, String infoName, List<String> infoLore, Runnable onConfirm, Runnable onCancel) {
        new DeletionConfirmMenu(
                        services.messages(), services.menuItems(), player, infoName, infoLore, onConfirm, onCancel)
                .open(player);
    }

    public void openStatus(BuildWorld buildWorld, Player player) {
        new StatusMenu(
                        services.messages(),
                        services.worldStatusRegistry(),
                        services.settings(),
                        services.menuItems(),
                        this,
                        buildWorld,
                        player)
                .open(player);
    }
}
