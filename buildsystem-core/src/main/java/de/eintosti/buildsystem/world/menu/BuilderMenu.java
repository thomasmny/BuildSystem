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
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.command.subcommand.worlds.AddBuilderSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.WorldsArgument;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.PaginatedMenu;
import de.eintosti.buildsystem.menu.SkullTextures;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
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

    private final BuildSystemPlugin plugin;
    private final BuildWorld buildWorld;
    private final NamespacedKey builderNameKey;

    public BuilderMenu(BuildSystemPlugin plugin, BuildWorld buildWorld, Player player) {
        super(plugin.getMessages(), 27, plugin.getMessages().getString("worldeditor_builders_title", player));
        this.plugin = plugin;
        this.buildWorld = buildWorld;
        this.builderNameKey = new NamespacedKey(plugin, "builder_name");
    }

    @Override
    protected int totalItems() {
        return buildWorld.getBuilders().getAllBuilders().size();
    }

    @Override
    protected void populate(Player player) {
        Inventory inv = getInventory();

        plugin.getMenuItems().fillRange(player, inv, 0, 9);
        plugin.getMenuItems().fillRange(player, inv, 18, 27);

        addCreatorInfoItem(inv, buildWorld.getBuilders(), player);
        addBuilderAddItem(inv, player);

        ItemBuilder.skull(Profileable.detect(SkullTextures.PREVIOUS_PAGE))
                .name(messages.getString("gui_previous_page", player))
                .into(inv, SLOT_PREVIOUS_PAGE);
        ItemBuilder.skull(Profileable.detect(SkullTextures.NEXT_PAGE))
                .name(messages.getString("gui_next_page", player))
                .into(inv, SLOT_NEXT_PAGE);

        // Clear builder slots from previous state
        plugin.getMenuItems().fillRange(player, inv, FIRST_BUILDER_SLOT, FIRST_BUILDER_SLOT + MAX_BUILDERS_PER_PAGE);

        List<Builder> builderList = new ArrayList<>(buildWorld.getBuilders().getAllBuilders());
        int startIndex = page() * MAX_BUILDERS_PER_PAGE;
        for (int i = 0; i < MAX_BUILDERS_PER_PAGE && startIndex + i < builderList.size(); i++) {
            inv.setItem(FIRST_BUILDER_SLOT + i, createBuilderItem(builderList.get(startIndex + i), player));
        }
    }

    private void addCreatorInfoItem(Inventory inventory, Builders builders, Player player) {
        ItemStack creatorInfoItem;
        Builder creator = builders.getCreator();

        if (creator == null) {
            creatorInfoItem = ItemBuilder.of(XMaterial.BARRIER)
                    .name(messages.getString("worldeditor_builders_no_creator_item", player))
                    .build();
        } else {
            creatorInfoItem = ItemBuilder.skull(Profileable.of(creator.getUniqueId()))
                    .name(messages.getString("worldeditor_builders_creator_item", player))
                    .lore(messages.getString(
                            "worldeditor_builders_creator_lore", player, Map.entry("%creator%", creator.getName())))
                    .build();
        }
        inventory.setItem(SLOT_CREATOR_INFO, creatorInfoItem);
    }

    private void addBuilderAddItem(Inventory inventory, Player player) {
        ItemStack builderAddItem;
        if (buildWorld.getBuilders().isCreator(player) || player.hasPermission(BuildSystemPlugin.ADMIN_PERMISSION)) {
            builderAddItem = ItemBuilder.skull(Profileable.detect(SkullTextures.ADD_ITEM))
                    .name(messages.getString("worldeditor_builders_add_builder_item", player))
                    .build();
        } else {
            builderAddItem = ItemBuilder.of(plugin.getMenuItems().getColoredGlassPane(player))
                    .build();
        }
        inventory.setItem(SLOT_ADD_BUILDER, builderAddItem);
    }

    private ItemStack createBuilderItem(Builder builder, Player player) {
        return ItemBuilder.skull(Profileable.username(builder.getName()))
                .name(messages.getString(
                        "worldeditor_builders_builder_item", player, Map.entry("%builder%", builder.getName())))
                .lore(messages.getStringList("worldeditor_builders_builder_lore", player))
                .pdc(this.builderNameKey, PersistentDataType.STRING, builder.getName())
                .build();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        if (itemStack.getType() != XMaterial.PLAYER_HEAD.get()) {
            if (buildWorld.getPermissions().canPerformCommand(player, WorldsArgument.EDIT.getPermission())) {
                XSound.BLOCK_CHEST_OPEN.play(player);
                new EditMenu(plugin, buildWorld, player).open(player);
            }
            return;
        }

        int slot = event.getSlot();
        switch (slot) {
            case SLOT_PREVIOUS_PAGE:
                if (!previousPage(player, MAX_BUILDERS_PER_PAGE)) {
                    return;
                }
                populate(player);
                return;
            case SLOT_ADD_BUILDER:
                XSound.ENTITY_CHICKEN_EGG.play(player);
                new AddBuilderSubCommand(plugin).getAddBuilderInput(player, buildWorld, false);
                return;
            case SLOT_NEXT_PAGE:
                if (!nextPage(player, MAX_BUILDERS_PER_PAGE)) {
                    return;
                }
                populate(player);
                return;
            default:
                if (slot == SLOT_CREATOR_INFO || !event.isShiftClick()) {
                    return;
                }
                removeBuilderByItem(player, itemStack);
        }
    }

    private void removeBuilderByItem(Player player, ItemStack itemStack) {
        String builderName = itemStack
                .getItemMeta()
                .getPersistentDataContainer()
                .get(this.builderNameKey, PersistentDataType.STRING);
        if (builderName == null) {
            player.closeInventory();
            messages.sendMessage(player, "worlds_removebuilder_error");
            plugin.getLogger().warning("Could not find UUID for null builder name");
            return;
        }

        plugin.getPlayerLookupService()
                .lookupUniqueId(builderName)
                .thenAccept(builderId -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (builderId == null) {
                        player.closeInventory();
                        messages.sendMessage(player, "worlds_removebuilder_error");
                        plugin.getLogger().warning("Could not find UUID for " + builderName);
                        return;
                    }

                    buildWorld.getBuilders().removeBuilder(builderId);
                    XSound.ENTITY_ENDERMAN_TELEPORT.play(player);
                    messages.sendMessage(player, "worlds_removebuilder_removed", Map.entry("%builder%", builderName));
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    populate(player);
                }));
    }
}
