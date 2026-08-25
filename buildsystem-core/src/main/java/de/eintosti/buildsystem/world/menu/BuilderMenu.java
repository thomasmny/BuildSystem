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
package de.eintosti.buildsystem.world.menu;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.command.subcommand.worlds.WorldsArgument;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.menu.PaginatedMenu;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.util.TaskScheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BuilderMenu extends PaginatedMenu {

    private static final int MAX_BUILDERS_PER_PAGE = 9;

    private static final int SLOT_CREATOR_INFO = 4;
    private static final int FIRST_BUILDER_SLOT = 9;
    private static final int SLOT_PREVIOUS_PAGE = 18;
    private static final int SLOT_ADD_BUILDER = 22;
    private static final int SLOT_NEXT_PAGE = 26;

    private final MenuItems menuItems;
    private final Menus menus;
    private final PlayerLookupService playerLookupService;
    private final TaskScheduler scheduler;
    private final Logger logger;
    private final BuildWorld buildWorld;
    private final NamespacedKey builderNameKey;

    public BuilderMenu(
            Messages messages,
            MenuItems menuItems,
            Menus menus,
            PlayerLookupService playerLookupService,
            TaskScheduler scheduler,
            Logger logger,
            NamespacedKey builderNameKey,
            BuildWorld buildWorld,
            Player player) {
        super(messages, 27, messages.getString("worldeditor_builders_title", player));
        this.menuItems = menuItems;
        this.menus = menus;
        this.playerLookupService = playerLookupService;
        this.scheduler = scheduler;
        this.logger = logger;
        this.builderNameKey = builderNameKey;
        this.buildWorld = buildWorld;
    }

    @Override
    protected int totalItems() {
        return buildWorld.getBuilders().getAllBuilders().size();
    }

    @Override
    protected void populate(Player player) {
        clearButtons();
        Inventory inv = getInventory();

        menuItems.fillRange(player, inv, 0, 9);
        menuItems.fillRange(player, inv, 18, 27);
        menuItems.fillRange(player, inv, FIRST_BUILDER_SLOT, FIRST_BUILDER_SLOT + MAX_BUILDERS_PER_PAGE);

        register(SLOT_CREATOR_INFO, creatorInfoButton());
        register(SLOT_ADD_BUILDER, addBuilderButton());
        register(SLOT_PREVIOUS_PAGE, previousPageButton(SkullTextures.PREVIOUS_PAGE, MAX_BUILDERS_PER_PAGE));
        register(SLOT_NEXT_PAGE, nextPageButton(SkullTextures.NEXT_PAGE, MAX_BUILDERS_PER_PAGE));

        List<Builder> builderList = new ArrayList<>(buildWorld.getBuilders().getAllBuilders());
        registerPageItems(FIRST_BUILDER_SLOT, MAX_BUILDERS_PER_PAGE, builderList, this::builderButton);

        renderButtons(player);
    }

    private MenuButton creatorInfoButton() {
        return MenuButton.builder()
                .render((player, inventory, slot) -> {
                    Builder creator = buildWorld.getBuilders().getCreator();
                    if (creator == null) {
                        ItemBuilder.of(XMaterial.BARRIER)
                                .name(messages.getString("worldeditor_builders_no_creator_item", player))
                                .into(inventory, slot);
                        return;
                    }
                    menuItems.applyHeadProfileAsync(
                            inventory,
                            slot,
                            Profileable.of(creator.getUniqueId()),
                            null,
                            messages.getString("worldeditor_builders_creator_item", player),
                            List.of(messages.getString(
                                    "worldeditor_builders_creator_lore",
                                    player,
                                    Placeholders.of("%creator%", creator.getName()))),
                            null);
                })
                .build();
    }

    private boolean canManageBuilders(Player player) {
        return buildWorld.getBuilders().isCreator(player) || player.hasPermission(BuildSystemPlugin.ADMIN_PERMISSION);
    }

    private MenuButton addBuilderButton() {
        return MenuButton.builder()
                .render((player, inventory, slot) -> {
                    if (canManageBuilders(player)) {
                        inventory.setItem(
                                slot,
                                ItemBuilder.skull(Profileable.detect(SkullTextures.ADD_ITEM))
                                        .name(messages.getString("worldeditor_builders_add_builder_item", player))
                                        .build());
                    } else {
                        inventory.setItem(
                                slot,
                                ItemBuilder.of(menuItems.getColoredGlassPane(player))
                                        .build());
                    }
                })
                .usableBy(this::canManageBuilders)
                .onClick((player, event) -> {
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    menus.promptAddBuilder(buildWorld, player);
                })
                .build();
    }

    private MenuButton builderButton(Builder builder) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> menuItems.applyHeadProfileAsync(
                        inventory,
                        slot,
                        Profileable.username(builder.getName()),
                        null,
                        messages.getString(
                                "worldeditor_builders_builder_item",
                                player,
                                Placeholders.of("%builder%", builder.getName())),
                        messages.getStringList("worldeditor_builders_builder_lore", player),
                        // The swap replaces the whole stack, so the name the click handler reads back has to be
                        // written onto the resolved head too, not just the placeholder.
                        itemStack -> {
                            ItemMeta itemMeta = itemStack.getItemMeta();
                            if (itemMeta != null) {
                                itemMeta.getPersistentDataContainer()
                                        .set(this.builderNameKey, PersistentDataType.STRING, builder.getName());
                                itemStack.setItemMeta(itemMeta);
                            }
                        }))
                .usableBy(this::canManageBuilders)
                .onClick((player, event) -> {
                    // Only a shift-click removes a builder; a plain click returns to the editor.
                    if (!event.isShiftClick()) {
                        returnToEditor(player);
                        return;
                    }
                    removeBuilder(player, builder.getName());
                })
                .build();
    }

    @Override
    protected void onUnhandledClick(Player player, InventoryClickEvent event) {
        returnToEditor(player);
    }

    /**
     * Sends a player who may not manage builders back to the editor instead of closing the inventory with an error.
     * They see a filler pane rather than the add-builder head, so a click on it reads as "go back", not as an attempt
     * to do something forbidden.
     */
    @Override
    protected void onPermissionDenied(Player player, InventoryClickEvent event) {
        returnToEditor(player);
    }

    private void returnToEditor(Player player) {
        if (buildWorld.getPermissions().canPerformCommand(player, WorldsArgument.EDIT.getPermission())) {
            XSound.BLOCK_CHEST_OPEN.play(player);
            menus.openEdit(buildWorld, player);
        }
    }

    private void removeBuilder(Player player, String builderName) {
        playerLookupService
                .lookupUniqueId(builderName)
                .thenAccept(builderId -> scheduler.run(() -> {
                    if (builderId == null) {
                        player.closeInventory();
                        messages.sendMessage(player, "worlds_removebuilder_error");
                        logger.warning("Could not find UUID for " + builderName);
                        return;
                    }

                    buildWorld.getBuilders().removeBuilder(builderId);
                    XSound.ENTITY_ENDERMAN_TELEPORT.play(player);
                    messages.sendMessage(
                            player, "worlds_removebuilder_removed", Placeholders.of("%builder%", builderName));
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    populate(player);
                }));
    }
}
