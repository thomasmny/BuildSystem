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

import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.command.subcommand.worlds.WorldsArgument;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.MenuContext;
import de.eintosti.buildsystem.menu.PaginatedMenu;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.util.FeedbackSound;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BuilderMenu extends PaginatedMenu {

    private static final int MAX_BUILDERS_PER_PAGE = 9;

    private static final int SLOT_CREATOR_INFO = 4;
    private static final int FIRST_BUILDER_SLOT = 9;
    private static final int SLOT_PREVIOUS_PAGE = 18;
    private static final int SLOT_ADD_BUILDER = 22;
    private static final int SLOT_NEXT_PAGE = 26;

    private final BuildWorld buildWorld;

    public BuilderMenu(MenuContext context, BuildWorld buildWorld, Player player) {
        super(context, 27, context.messages().getString("worldeditor_builders_title", player));
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
                        ItemBuilder.of(Material.BARRIER)
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
                                    Placeholders.of("%creator%", creator.getName()))));
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
                    FeedbackSound.CLICK.play(player);
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
                        messages.getStringList("worldeditor_builders_builder_lore", player)))
                .usableBy(this::canManageBuilders)
                .onClick((player, event) -> {
                    // Only a shift-click removes a builder; a plain click returns to the editor.
                    if (!event.isShiftClick()) {
                        returnToEditor(player);
                        return;
                    }
                    removeBuilder(player, builder);
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
            menus.openEdit(buildWorld, player);
        }
    }

    private void removeBuilder(Player player, Builder builder) {
        buildWorld.getBuilders().removeBuilder(builder);
        messages.sendMessage(player, "worlds_removebuilder_removed", Placeholders.of("%builder%", builder.getName()));
        FeedbackSound.REMOVE.play(player);
        populate(player);
    }
}
