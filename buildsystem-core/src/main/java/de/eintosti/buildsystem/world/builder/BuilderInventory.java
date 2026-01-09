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
package de.eintosti.buildsystem.world.builder;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.command.subcommand.worlds.AddBuilderSubCommand;
import de.eintosti.buildsystem.command.tabcomplete.WorldsTabCompleter.WorldsArgument;
import de.eintosti.buildsystem.util.UUIDFetcher;
import de.eintosti.buildsystem.util.inventory.BuildWorldHolder;
import de.eintosti.buildsystem.util.inventory.InventoryManager;
import de.eintosti.buildsystem.util.inventory.InventoryUtils;
import de.eintosti.buildsystem.util.inventory.PaginatedInventory;
import de.eintosti.buildsystem.world.modification.EditInventory;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
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
public class BuilderInventory extends PaginatedInventory {

    private static final int MAX_BUILDERS_PER_PAGE = 9;

    private final BuildSystemPlugin plugin;
    private final InventoryManager inventoryManager;
    private final NamespacedKey builderNameKey;

    private int numBuilders = 0;

    public BuilderInventory(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        this.builderNameKey = new NamespacedKey(plugin, "builder_name");
    }

    public void openInventory(BuildWorld buildWorld, Player player) {
        Inventory inventory = getInventory(buildWorld, player);
        this.inventoryManager.registerInventoryHandler(inventory, this);
        player.openInventory(inventory);
    }

    private Inventory getInventory(BuildWorld buildWorld, Player player) {
        addItems(buildWorld, player);
        return inventories[getInvIndex(player)];
    }

    private void addItems(BuildWorld buildWorld, Player player) {
        Collection<Builder> builders = buildWorld.getBuilders().getAllBuilders();
        this.numBuilders = builders.size();
        int numInventories = calculateNumPages(this.numBuilders, MAX_BUILDERS_PER_PAGE);

        int index = 0;
        Inventory inventory = createInventory(buildWorld, player);
        inventories = new Inventory[numInventories];
        inventories[index] = inventory;

        int columnSkull = 9, maxColumnSkull = 17;
        for (Builder builder : builders) {
            inventory.setItem(columnSkull++, createBuilderItem(builder, player));

            if (columnSkull > maxColumnSkull) {
                columnSkull = 9;
                inventory = createInventory(buildWorld, player);
                inventories[++index] = inventory;
            }
        }
    }

    private Inventory createInventory(BuildWorld buildWorld, Player player) {
        Inventory inventory = new BuilderInventoryHolder(buildWorld, player).getInventory();
        fillGuiWithGlass(inventory, player);

        addCreatorInfoItem(inventory, buildWorld.getBuilders(), player);
        addBuilderAddItem(inventory, buildWorld, player);

        inventory.setItem(18, InventoryUtils.createSkull(Messages.getString("gui_previous_page", player), Profileable.detect("f7aacad193e2226971ed95302dba433438be4644fbab5ebf818054061667fbe2")));
        inventory.setItem(26, InventoryUtils.createSkull(Messages.getString("gui_next_page", player), Profileable.detect("d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158")));

        return inventory;
    }

    private void addCreatorInfoItem(Inventory inventory, Builders builders, Player player) {
        ItemStack creatorInfoItem;
        Builder creator = builders.getCreator();

        if (!builders.hasCreator()) {
            creatorInfoItem = InventoryUtils.createItem(XMaterial.BARRIER,
                    Messages.getString("worldeditor_builders_no_creator_item", player)
            );
        } else {
            creatorInfoItem = InventoryUtils.createSkull(Messages.getString("worldeditor_builders_creator_item", player), Profileable.of(creator.getUniqueId()),
                    Messages.getString("worldeditor_builders_creator_lore", player,
                            Map.entry("%creator%", builders.getCreator().getName())
                    )
            );
        }
        inventory.setItem(4, creatorInfoItem);
    }

    private void addBuilderAddItem(Inventory inventory, BuildWorld buildWorld, Player player) {
        ItemStack builderAddItem;
        if (buildWorld.getBuilders().isCreator(player) || player.hasPermission(BuildSystemPlugin.ADMIN_PERMISSION)) {
            builderAddItem = InventoryUtils.createSkull(Messages.getString("worldeditor_builders_add_builder_item", player), Profileable.detect("3edd20be93520949e6ce789dc4f43efaeb28c717ee6bfcbbe02780142f716"));
        } else {
            builderAddItem = InventoryUtils.getColoredGlassPane(player).parseItem();
        }
        inventory.setItem(22, builderAddItem);
    }

    private ItemStack createBuilderItem(Builder builder, Player player) {
        ItemStack itemStack = InventoryUtils.createSkull(
                Messages.getString("worldeditor_builders_builder_item", player,
                        Map.entry("%builder%", builder.getName())
                ),
                Profileable.username(builder.getName()),
                Messages.getStringList("worldeditor_builders_builder_lore", player)
        );
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.getPersistentDataContainer().set(this.builderNameKey, PersistentDataType.STRING, builder.getName());
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private void fillGuiWithGlass(Inventory inventory, Player player) {
        for (int i = 0; i <= 8; i++) {
            InventoryUtils.addGlassPane(player, inventory, i);
        }
        for (int i = 18; i <= 26; i++) {
            InventoryUtils.addGlassPane(player, inventory, i);
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BuilderInventoryHolder holder)) {
            return;
        }

        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        BuildWorld buildWorld = holder.getBuildWorld();

        if (itemStack.getType() != XMaterial.PLAYER_HEAD.get()) {
            if (buildWorld.getPermissions().canPerformCommand(player, WorldsArgument.EDIT.getPermission())) {
                XSound.BLOCK_CHEST_OPEN.play(player);
                new EditInventory(plugin).openInventory(player, buildWorld);
            }
            return;
        }

        int slot = event.getSlot();
        switch (slot) {
            case 18:
                if (decrementInv(player, numBuilders, MAX_BUILDERS_PER_PAGE)) {
                    openInventory(buildWorld, player);
                }
                break;
            case 22:
                XSound.ENTITY_CHICKEN_EGG.play(player);
                new AddBuilderSubCommand(plugin, buildWorld.getName()).getAddBuilderInput(player, buildWorld, false);
                return;
            case 26:
                if (incrementInv(player, numBuilders, MAX_BUILDERS_PER_PAGE)) {
                    openInventory(buildWorld, player);
                }
                break;
            default:
                if (slot == 4 || !event.isShiftClick()) {
                    return;
                }

                String builderName = itemStack.getItemMeta().getPersistentDataContainer().get(this.builderNameKey, PersistentDataType.STRING);
                UUID builderId;
                if (builderName == null || (builderId = UUIDFetcher.getUUID(builderName)) == null) {
                    player.closeInventory();
                    Messages.sendMessage(player, "worlds_removebuilder_error");
                    plugin.getLogger().warning("Could not find UUID for " + builderName);
                    return;
                }

                buildWorld.getBuilders().removeBuilder(builderId);
                XSound.ENTITY_ENDERMAN_TELEPORT.play(player);
                Messages.sendMessage(player, "worlds_removebuilder_removed", Map.entry("%builder%", builderName));
        }

        XSound.ENTITY_CHICKEN_EGG.play(player);
        player.openInventory(getInventory(buildWorld, player));
    }

    private static class BuilderInventoryHolder extends BuildWorldHolder {

        public BuilderInventoryHolder(BuildWorld buildWorld, Player player) {
            super(buildWorld, 27, Messages.getString("worldeditor_builders_title", player));
        }
    }
}