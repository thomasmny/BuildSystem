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
import de.eintosti.buildsystem.menu.PaginatedMenu;
import de.eintosti.buildsystem.menu.InventoryUtils;
import de.eintosti.buildsystem.world.menu.EditInventory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BuilderInventory extends PaginatedMenu {

    private static final int MAX_BUILDERS_PER_PAGE = 9;

    private final BuildSystemPlugin plugin;
    private final BuildWorld buildWorld;
    private final NamespacedKey builderNameKey;

    public BuilderInventory(BuildSystemPlugin plugin, BuildWorld buildWorld, Player player) {
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

        for (int i = 0; i <= 8; i++) {
            plugin.getMenuItems().addGlassPane(player, inv, i);
        }
        for (int i = 18; i <= 26; i++) {
            plugin.getMenuItems().addGlassPane(player, inv, i);
        }

        addCreatorInfoItem(inv, buildWorld.getBuilders(), player);
        addBuilderAddItem(inv, player);

        inv.setItem(18, InventoryUtils.createSkull(messages.getString("gui_previous_page", player), Profileable.detect("f7aacad193e2226971ed95302dba433438be4644fbab5ebf818054061667fbe2")));
        inv.setItem(26, InventoryUtils.createSkull(messages.getString("gui_next_page", player), Profileable.detect("d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158")));

        // Clear builder slots from previous state
        for (int i = 9; i <= 17; i++) {
            plugin.getMenuItems().addGlassPane(player, inv, i);
        }

        Collection<Builder> allBuilders = buildWorld.getBuilders().getAllBuilders();
        List<Builder> builderList = new ArrayList<>(allBuilders);
        int startIndex = page() * MAX_BUILDERS_PER_PAGE;
        for (int i = 0; i < MAX_BUILDERS_PER_PAGE && startIndex + i < builderList.size(); i++) {
            inv.setItem(9 + i, createBuilderItem(builderList.get(startIndex + i), player));
        }
    }

    private void addCreatorInfoItem(Inventory inventory, Builders builders, Player player) {
        ItemStack creatorInfoItem;
        Builder creator = builders.getCreator();

        if (!builders.hasCreator()) {
            creatorInfoItem = InventoryUtils.createItem(XMaterial.BARRIER,
                    messages.getString("worldeditor_builders_no_creator_item", player)
            );
        } else {
            creatorInfoItem = InventoryUtils.createSkull(messages.getString("worldeditor_builders_creator_item", player), Profileable.of(creator.getUniqueId()),
                    messages.getString("worldeditor_builders_creator_lore", player,
                            Map.entry("%creator%", builders.getCreator().getName())
                    )
            );
        }
        inventory.setItem(4, creatorInfoItem);
    }

    private void addBuilderAddItem(Inventory inventory, Player player) {
        ItemStack builderAddItem;
        if (buildWorld.getBuilders().isCreator(player) || player.hasPermission(BuildSystemPlugin.ADMIN_PERMISSION)) {
            builderAddItem = InventoryUtils.createSkull(messages.getString("worldeditor_builders_add_builder_item", player), Profileable.detect("3edd20be93520949e6ce789dc4f43efaeb28c717ee6bfcbbe02780142f716"));
        } else {
            builderAddItem = plugin.getMenuItems().getColoredGlassPane(player).parseItem();
        }
        inventory.setItem(22, builderAddItem);
    }

    private ItemStack createBuilderItem(Builder builder, Player player) {
        ItemStack itemStack = InventoryUtils.createSkull(
                messages.getString("worldeditor_builders_builder_item", player,
                        Map.entry("%builder%", builder.getName())
                ),
                Profileable.username(builder.getName()),
                messages.getStringList("worldeditor_builders_builder_lore", player)
        );
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.getPersistentDataContainer().set(this.builderNameKey, PersistentDataType.STRING, builder.getName());
        itemStack.setItemMeta(itemMeta);
        return itemStack;
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
                new EditInventory(plugin, buildWorld, player).open(player);
            }
            return;
        }

        int slot = event.getSlot();
        switch (slot) {
            case 18:
                if (!previousPage(player, MAX_BUILDERS_PER_PAGE)) {
                    return;
                }
                populate(player);
                return;
            case 22:
                XSound.ENTITY_CHICKEN_EGG.play(player);
                new AddBuilderSubCommand(plugin).getAddBuilderInput(player, buildWorld, false);
                return;
            case 26:
                if (!nextPage(player, MAX_BUILDERS_PER_PAGE)) {
                    return;
                }
                populate(player);
                return;
            default:
                if (slot == 4 || !event.isShiftClick()) {
                    return;
                }
                removeBuilderByItem(player, itemStack);
        }
    }

    private void removeBuilderByItem(Player player, ItemStack itemStack) {
        String builderName = itemStack.getItemMeta().getPersistentDataContainer().get(this.builderNameKey, PersistentDataType.STRING);
        if (builderName == null) {
            player.closeInventory();
            messages.sendMessage(player, "worlds_removebuilder_error");
            plugin.getLogger().warning("Could not find UUID for null builder name");
            return;
        }

        plugin.getPlayerLookupService().lookupUniqueId(builderName)
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
