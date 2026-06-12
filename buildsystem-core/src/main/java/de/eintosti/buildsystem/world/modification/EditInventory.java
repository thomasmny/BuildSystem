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
package de.eintosti.buildsystem.world.modification;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XEntityType;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.google.common.collect.Sets;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.command.subcommand.worlds.SetPermissionSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetProjectSubCommand;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.Menu;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.util.inventory.InventoryUtils;
import de.eintosti.buildsystem.world.builder.BuilderInventory;
import de.eintosti.buildsystem.world.data.StatusInventory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class EditInventory extends Menu {

    /**
     * A set of entities which are ignored when the butcher item is used.
     */
    private static final Set<XEntityType> IGNORED_ENTITIES = Sets.newHashSet(
            XEntityType.ARMOR_STAND, XEntityType.END_CRYSTAL, XEntityType.ITEM_FRAME,
            XEntityType.FALLING_BLOCK, XEntityType.MINECART, XEntityType.CHEST_MINECART,
            XEntityType.COMMAND_BLOCK_MINECART, XEntityType.FURNACE_MINECART,
            XEntityType.HOPPER_MINECART, XEntityType.SPAWNER_MINECART, XEntityType.TNT_MINECART,
            XEntityType.PLAYER
    );

    private final BuildSystemPlugin plugin;
    private final PlayerServiceImpl playerManager;
    private final BuildWorld buildWorld;

    public EditInventory(BuildSystemPlugin plugin, BuildWorld buildWorld, Player player) {
        super(plugin.getMessages(), 54, plugin.getMessages().getString("worldeditor_title", player));
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerService();
        this.buildWorld = buildWorld;
    }

    @Override
    protected void populate(Player player) {
        Inventory inv = getInventory();
        WorldData worldData = buildWorld.getData();

        fillGuiWithGlass(player, inv);
        addBuildWorldInfoItem(player, inv);

        addSettingsItem(player, inv, 20, XMaterial.OAK_PLANKS,
                worldData.blockBreaking().get(), "worldeditor_blockbreaking_item", "worldeditor_blockbreaking_lore"
        );
        addSettingsItem(player, inv, 21, XMaterial.POLISHED_ANDESITE,
                worldData.blockPlacement().get(), "worldeditor_blockplacement_item", "worldeditor_blockplacement_lore"
        );
        addSettingsItem(player, inv, 22, XMaterial.SAND,
                worldData.physics().get(), "worldeditor_physics_item", "worldeditor_physics_lore"
        );
        addTimeItem(player, inv);
        addSettingsItem(player, inv, 24, XMaterial.TNT,
                worldData.explosions().get(), "worldeditor_explosions_item", "worldeditor_explosions_lore"
        );
        inv.setItem(29, InventoryUtils.createItem(XMaterial.DIAMOND_SWORD,
                messages.getString("worldeditor_butcher_item", player), messages.getStringList("worldeditor_butcher_lore", player)
        ));
        addBuildersItem(player, inv);
        addSettingsItem(player, inv, 31, XMaterial.ARMOR_STAND,
                worldData.mobAi().get(), "worldeditor_mobai_item", "worldeditor_mobai_lore"
        );
        addVisibilityItem(player, inv);
        addSettingsItem(player, inv, 33, XMaterial.TRIPWIRE_HOOK,
                worldData.blockInteractions().get(), "worldeditor_blockinteractions_item", "worldeditor_blockinteractions_lore"
        );
        inv.setItem(38, InventoryUtils.createItem(XMaterial.FILLED_MAP,
                messages.getString("worldeditor_gamerules_item", player), messages.getStringList("worldeditor_gamerules_lore", player)
        ));
        addDifficultyItem(player, inv);
        inv.setItem(40, InventoryUtils.createItem(plugin.getCustomizableIcons().getIcon(worldData.status().get()), messages.getString("worldeditor_status_item", player),
                messages.getStringList("worldeditor_status_lore", player,
                        Map.entry("%status%", messages.getString(Messages.getMessageKey(buildWorld.getData().status().get()), player))
                )
        ));
        inv.setItem(41, InventoryUtils.createItem(XMaterial.ANVIL, messages.getString("worldeditor_project_item", player),
                messages.getStringList("worldeditor_project_lore", player,
                        Map.entry("%project%", buildWorld.getData().project().get())
                )
        ));
        inv.setItem(42, InventoryUtils.createItem(XMaterial.PAPER, messages.getString("worldeditor_permission_item", player),
                messages.getStringList("worldeditor_permission_lore", player,
                        Map.entry("%permission%", buildWorld.getData().permission().get())
                )
        ));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        IntStream.range(0, inventory.getSize()).forEach(i -> InventoryUtils.addGlassPane(player, inventory, i));
    }

    private void addBuildWorldInfoItem(Player player, Inventory inventory) {
        String worldName = buildWorld.getName();
        String displayName = messages.getString("worldeditor_world_item", player, Map.entry("%world%", worldName));
        XMaterial material = buildWorld.getData().material().get();

        if (material == XMaterial.PLAYER_HEAD) {
            InventoryUtils.addWorldItem(inventory, 4, buildWorld, displayName, new ArrayList<>());
        } else {
            inventory.setItem(4, InventoryUtils.createItem(material, displayName));
        }
    }

    private void addSettingsItem(Player player, Inventory inventory, int position, XMaterial material, boolean isEnabled, String displayNameKey, String loreKey) {
        ItemStack itemStack = material.parseItem();
        ItemMeta itemMeta = itemStack.getItemMeta();

        itemMeta.setDisplayName(messages.getString(displayNameKey, player));
        itemMeta.setLore(messages.getStringList(loreKey, player));
        itemMeta.addItemFlags(ItemFlag.values());

        itemStack.setItemMeta(itemMeta);
        if (isEnabled) {
            itemStack.addUnsafeEnchantment(XEnchantment.UNBREAKING.get(), 1);
        }

        inventory.setItem(position, itemStack);
    }

    private void addTimeItem(Player player, Inventory inventory) {
        XMaterial xMaterial;
        String value;
        switch (getWorldTime()) {
            case NIGHT -> {
                xMaterial = XMaterial.BLUE_STAINED_GLASS;
                value = messages.getString("worldeditor_time_lore_night", player);
            }
            case NOON -> {
                xMaterial = XMaterial.YELLOW_STAINED_GLASS;
                value = messages.getString("worldeditor_time_lore_noon", player);
            }
            default -> {
                xMaterial = XMaterial.ORANGE_STAINED_GLASS;
                value = messages.getString("worldeditor_time_lore_sunrise", player);
            }
        }

        inventory.setItem(23, InventoryUtils.createItem(xMaterial, messages.getString("worldeditor_time_item", player),
                messages.getStringList("worldeditor_time_lore", player, Map.entry("%time%", value))
        ));
    }

    private Time getWorldTime() {
        int worldTime = (int) buildWorld.getWorld().getTime();
        int noonTime = plugin.getConfigService().current().world().defaults().time().noon();

        if (worldTime >= 0 && worldTime < noonTime) {
            return Time.SUNRISE;
        } else if (worldTime >= noonTime && worldTime < 13000) {
            return Time.NOON;
        } else {
            return Time.NIGHT;
        }
    }

    private void addBuildersItem(Player player, Inventory inventory) {
        if (buildWorld.getBuilders().isCreator(player) || player.hasPermission(BuildSystemPlugin.ADMIN_PERMISSION)) {
            addSettingsItem(player, inventory, 30, XMaterial.IRON_PICKAXE, buildWorld.getData().buildersEnabled().get(),
                    "worldeditor_builders_item", "worldeditor_builders_lore"
            );
        } else {
            inventory.setItem(30, InventoryUtils.createItem(XMaterial.BARRIER,
                    messages.getString("worldeditor_builders_not_creator_item", player),
                    messages.getStringList("worldeditor_builders_not_creator_lore", player)
            ));
        }
    }

    private void addVisibilityItem(Player player, Inventory inventory) {
        int slot = 32;
        String displayName = messages.getString("worldeditor_visibility_item", player);
        boolean isPrivate = buildWorld.getData().privateWorld().get();

        if (!playerManager.canCreateWorld(player, Visibility.matchVisibility(isPrivate))) {
            inventory.setItem(slot, InventoryUtils.createItem(XMaterial.BARRIER, "§c§m" + ChatColor.stripColor(displayName)));
            return;
        }

        XMaterial xMaterial = XMaterial.ENDER_EYE;
        List<String> lore = messages.getStringList("worldeditor_visibility_lore_public", player);

        if (isPrivate) {
            xMaterial = XMaterial.ENDER_PEARL;
            lore = messages.getStringList("worldeditor_visibility_lore_private", player);
        }

        inventory.setItem(slot, InventoryUtils.createItem(xMaterial, displayName, lore));
    }

    private void addDifficultyItem(Player player, Inventory inventory) {
        XMaterial material = switch (buildWorld.getData().difficulty().get()) {
            case EASY -> XMaterial.GOLDEN_HELMET;
            case NORMAL -> XMaterial.IRON_HELMET;
            case HARD -> XMaterial.DIAMOND_HELMET;
            default -> XMaterial.LEATHER_HELMET;
        };

        inventory.setItem(39, InventoryUtils.createItem(material,
                messages.getString("worldeditor_difficulty_item", player),
                messages.getStringList("worldeditor_difficulty_lore", player,
                        Map.entry("%difficulty%", getDifficultyName(buildWorld, player))
                )
        ));
    }

    private String getDifficultyName(BuildWorld buildWorld, Player player) {
        return switch (buildWorld.getData().difficulty().get()) {
            case PEACEFUL -> messages.getString("difficulty_peaceful", player);
            case EASY -> messages.getString("difficulty_easy", player);
            case NORMAL -> messages.getString("difficulty_normal", player);
            case HARD -> messages.getString("difficulty_hard", player);
        };
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        WorldData worldData = buildWorld.getData();
        switch (event.getSlot()) {
            case 20 -> {
                if (hasPermission(player, "buildsystem.edit.breaking")) {
                    worldData.blockBreaking().set(!worldData.blockBreaking().get());
                }
            }
            case 21 -> {
                if (hasPermission(player, "buildsystem.edit.placement")) {
                    worldData.blockPlacement().set(!worldData.blockPlacement().get());
                }
            }
            case 22 -> {
                if (hasPermission(player, "buildsystem.edit.physics")) {
                    worldData.physics().set(!worldData.physics().get());
                }
            }
            case 23 -> {
                if (hasPermission(player, "buildsystem.edit.time")) {
                    changeTime(player);
                }
            }
            case 24 -> {
                if (hasPermission(player, "buildsystem.edit.explosions")) {
                    worldData.explosions().set(!worldData.explosions().get());
                }
            }
            case 29 -> {
                if (hasPermission(player, "buildsystem.edit.entities")) {
                    removeEntities(player);
                }
            }
            case 30 -> {
                if (itemStack.getType() == XMaterial.BARRIER.get()) {
                    XSound.ENTITY_ITEM_BREAK.play(player);
                    return;
                }
                if (!hasPermission(player, "buildsystem.edit.builders")) {
                    return;
                }
                if (event.isRightClick()) {
                    XSound.BLOCK_CHEST_OPEN.play(player);
                    new BuilderInventory(plugin, buildWorld, player).open(player);
                    return;
                }
                worldData.buildersEnabled().set(!worldData.buildersEnabled().get());
            }
            case 31 -> {
                if (hasPermission(player, "buildsystem.edit.mobai")) {
                    worldData.mobAi().set(!worldData.mobAi().get());
                }
            }
            case 32 -> {
                if (itemStack.getType() == XMaterial.BARRIER.get()) {
                    XSound.ENTITY_ITEM_BREAK.play(player);
                    return;
                }
                if (hasPermission(player, "buildsystem.edit.visibility")) {
                    worldData.privateWorld().set(!worldData.privateWorld().get());
                }
            }
            case 33 -> {
                if (hasPermission(player, "buildsystem.edit.interactions")) {
                    worldData.blockInteractions().set(!worldData.blockInteractions().get());
                }
            }
            case 38 -> {
                if (hasPermission(player, "buildsystem.edit.gamerules")) {
                    XSound.BLOCK_CHEST_OPEN.play(player);
                    new GameRulesInventory(plugin, buildWorld, player).open(player);
                }
                return;
            }
            case 39 -> {
                if (hasPermission(player, "buildsystem.edit.difficulty")) {
                    Difficulty newDifficulty = buildWorld.cycleDifficulty();
                    buildWorld.getWorld().setDifficulty(newDifficulty);
                }
            }
            case 40 -> {
                if (hasPermission(player, "buildsystem.edit.status")) {
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    new StatusInventory(plugin, buildWorld, player).open(player);
                }
                return;
            }
            case 41 -> {
                if (hasPermission(player, "buildsystem.edit.project")) {
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    new SetProjectSubCommand(plugin).getProjectInput(player, buildWorld, false);
                }
                return;
            }
            case 42 -> {
                if (hasPermission(player, "buildsystem.edit.permission")) {
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    new SetPermissionSubCommand(plugin).getPermissionInput(player, buildWorld, false);
                }
                return;
            }
            default -> {
                return;
            }
        }

        XSound.ENTITY_CHICKEN_EGG.play(player);
        new EditInventory(plugin, buildWorld, player).open(player);
    }

    private boolean hasPermission(Player player, String permission) {
        if (player.hasPermission(permission)) {
            return true;
        }
        player.closeInventory();
        messages.sendPermissionError(player);
        XSound.ENTITY_ITEM_BREAK.play(player);
        return false;
    }

    private void changeTime(Player player) {
        int time = switch (getWorldTime()) {
            case SUNRISE -> plugin.getConfigService().current().world().defaults().time().noon();
            case NOON -> plugin.getConfigService().current().world().defaults().time().night();
            case NIGHT -> plugin.getConfigService().current().world().defaults().time().sunrise();
        };
        buildWorld.getWorld().setTime(time);
        new EditInventory(plugin, buildWorld, player).open(player);
    }

    private void removeEntities(Player player) {
        World bukkitWorld = Bukkit.getWorld(buildWorld.getName());
        if (bukkitWorld == null) {
            return;
        }

        AtomicInteger entitiesRemoved = new AtomicInteger();
        bukkitWorld.getEntities().stream()
                .filter(entity -> !IGNORED_ENTITIES.contains(XEntityType.of(entity)))
                .forEach(entity -> {
                    entity.remove();
                    entitiesRemoved.incrementAndGet();
                });

        player.closeInventory();
        messages.sendMessage(player, "worldeditor_butcher_removed", Map.entry("%amount%", entitiesRemoved.get()));
    }

    public enum Time {
        SUNRISE, NOON, NIGHT
    }
}
