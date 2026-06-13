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

import static java.util.Map.entry;

import com.cryptomorin.xseries.XEntityType;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.google.common.collect.Sets;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.command.subcommand.worlds.SetPermissionSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetProjectSubCommand;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.InventoryUtils;
import de.eintosti.buildsystem.menu.Menu;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class EditMenu extends Menu {

    private static final int SLOT_WORLD_INFO = 3;
    private static final int SLOT_PIN = 5;
    private static final int SLOT_TIME = 23;
    private static final int SLOT_BUTCHER = 29;
    private static final int SLOT_BUILDERS = 30;
    private static final int SLOT_VISIBILITY = 32;
    private static final int SLOT_GAMERULES = 38;
    private static final int SLOT_DIFFICULTY = 39;
    private static final int SLOT_STATUS = 40;
    private static final int SLOT_PROJECT = 41;
    private static final int SLOT_PERMISSION = 42;

    /**
     * Entities which are ignored when the butcher item is used.
     */
    private static final Set<XEntityType> IGNORED_ENTITIES = Sets.newHashSet(
            XEntityType.ARMOR_STAND,
            XEntityType.END_CRYSTAL,
            XEntityType.ITEM_FRAME,
            XEntityType.FALLING_BLOCK,
            XEntityType.MINECART,
            XEntityType.CHEST_MINECART,
            XEntityType.COMMAND_BLOCK_MINECART,
            XEntityType.FURNACE_MINECART,
            XEntityType.HOPPER_MINECART,
            XEntityType.SPAWNER_MINECART,
            XEntityType.TNT_MINECART,
            XEntityType.PLAYER);

    /**
     * Slots whose only action is "check permission, flip a boolean world setting, re-open". They are rendered and
     * handled uniformly; heterogeneous slots (sub-menus, time, butcher, difficulty) are handled individually.
     */
    private static final Map<Integer, Toggle> TOGGLES = Map.ofEntries(
            entry(
                    SLOT_PIN,
                    new Toggle(
                            XMaterial.ITEM_FRAME,
                            XMaterial.GLOW_ITEM_FRAME,
                            "buildsystem.edit.pin",
                            "worldeditor_pin_item",
                            "worldeditor_pin_lore",
                            WorldData::isPinned,
                            WorldData::setPinned)),
            entry(
                    20,
                    new Toggle(
                            XMaterial.OAK_PLANKS,
                            "buildsystem.edit.breaking",
                            "worldeditor_blockbreaking_item",
                            "worldeditor_blockbreaking_lore",
                            WorldData::isBlockBreaking,
                            WorldData::setBlockBreaking)),
            entry(
                    21,
                    new Toggle(
                            XMaterial.POLISHED_ANDESITE,
                            "buildsystem.edit.placement",
                            "worldeditor_blockplacement_item",
                            "worldeditor_blockplacement_lore",
                            WorldData::isBlockPlacement,
                            WorldData::setBlockPlacement)),
            entry(
                    22,
                    new Toggle(
                            XMaterial.SAND,
                            "buildsystem.edit.physics",
                            "worldeditor_physics_item",
                            "worldeditor_physics_lore",
                            WorldData::isPhysics,
                            WorldData::setPhysics)),
            entry(
                    24,
                    new Toggle(
                            XMaterial.TNT,
                            "buildsystem.edit.explosions",
                            "worldeditor_explosions_item",
                            "worldeditor_explosions_lore",
                            WorldData::isExplosions,
                            WorldData::setExplosions)),
            entry(
                    31,
                    new Toggle(
                            XMaterial.ARMOR_STAND,
                            "buildsystem.edit.mobai",
                            "worldeditor_mobai_item",
                            "worldeditor_mobai_lore",
                            WorldData::isMobAi,
                            WorldData::setMobAi)),
            entry(
                    33,
                    new Toggle(
                            XMaterial.TRIPWIRE_HOOK,
                            "buildsystem.edit.interactions",
                            "worldeditor_blockinteractions_item",
                            "worldeditor_blockinteractions_lore",
                            WorldData::isBlockInteractions,
                            WorldData::setBlockInteractions)));

    private record Toggle(
            XMaterial material,
            XMaterial enabledMaterial,
            String permission,
            String itemKey,
            String loreKey,
            Predicate<WorldData> getter,
            BiConsumer<WorldData, Boolean> setter) {

        /**
         * Creates a toggle whose icon is the same whether the setting is enabled or not.
         */
        Toggle(
                XMaterial material,
                String permission,
                String itemKey,
                String loreKey,
                Predicate<WorldData> getter,
                BiConsumer<WorldData, Boolean> setter) {
            this(material, material, permission, itemKey, loreKey, getter, setter);
        }

        XMaterial iconFor(boolean enabled) {
            return enabled ? enabledMaterial : material;
        }
    }

    private final BuildSystemPlugin plugin;
    private final PlayerServiceImpl playerManager;
    private final BuildWorld buildWorld;

    public EditMenu(BuildSystemPlugin plugin, BuildWorld buildWorld, Player player) {
        super(plugin.getMessages(), 54, plugin.getMessages().getString("worldeditor_title", player));
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerService();
        this.buildWorld = buildWorld;
    }

    @Override
    protected void populate(Player player) {
        Inventory inv = getInventory();
        plugin.getMenuItems().fillAll(player, inv);

        addBuildWorldInfoItem(player, inv);
        addToggleItems(player, inv);
        addTimeItem(player, inv);
        addButcherItem(player, inv);
        addBuildersItem(player, inv);
        addVisibilityItem(player, inv);
        addGameRulesItem(player, inv);
        addDifficultyItem(player, inv);
        addStatusItem(player, inv);
        addProjectItem(player, inv);
        addPermissionItem(player, inv);
    }

    private void addToggleItems(Player player, Inventory inventory) {
        WorldData worldData = buildWorld.getData();
        TOGGLES.forEach((slot, toggle) -> {
            boolean enabled = toggle.getter().test(worldData);
            plugin.getMenuItems()
                    .addToggleItem(
                            player,
                            inventory,
                            slot,
                            toggle.iconFor(enabled),
                            enabled,
                            toggle.itemKey(),
                            toggle.loreKey());
        });
    }

    private void addBuildWorldInfoItem(Player player, Inventory inventory) {
        String displayName =
                messages.getString("worldeditor_world_item", player, Map.entry("%world%", buildWorld.getName()));
        XMaterial material = buildWorld.getData().getMaterial();

        if (material == XMaterial.PLAYER_HEAD) {
            plugin.getMenuItems().addWorldItem(inventory, SLOT_WORLD_INFO, buildWorld, displayName, new ArrayList<>());
        } else {
            inventory.setItem(SLOT_WORLD_INFO, InventoryUtils.createItem(material, displayName));
        }
    }

    private void addTimeItem(Player player, Inventory inventory) {
        XMaterial material;
        String value;
        switch (getWorldTime()) {
            case NIGHT -> {
                material = XMaterial.BLUE_STAINED_GLASS;
                value = messages.getString("worldeditor_time_lore_night", player);
            }
            case NOON -> {
                material = XMaterial.YELLOW_STAINED_GLASS;
                value = messages.getString("worldeditor_time_lore_noon", player);
            }
            default -> {
                material = XMaterial.ORANGE_STAINED_GLASS;
                value = messages.getString("worldeditor_time_lore_sunrise", player);
            }
        }

        inventory.setItem(
                SLOT_TIME,
                InventoryUtils.createItem(
                        material,
                        messages.getString("worldeditor_time_item", player),
                        messages.getStringList("worldeditor_time_lore", player, Map.entry("%time%", value))));
    }

    private void addButcherItem(Player player, Inventory inventory) {
        inventory.setItem(
                SLOT_BUTCHER,
                InventoryUtils.createItem(
                        XMaterial.DIAMOND_SWORD,
                        messages.getString("worldeditor_butcher_item", player),
                        messages.getStringList("worldeditor_butcher_lore", player)));
    }

    private void addBuildersItem(Player player, Inventory inventory) {
        if (buildWorld.getBuilders().isCreator(player) || player.hasPermission(BuildSystemPlugin.ADMIN_PERMISSION)) {
            plugin.getMenuItems()
                    .addToggleItem(
                            player,
                            inventory,
                            SLOT_BUILDERS,
                            XMaterial.IRON_PICKAXE,
                            buildWorld.getData().isBuildersEnabled(),
                            "worldeditor_builders_item",
                            "worldeditor_builders_lore");
        } else {
            inventory.setItem(
                    SLOT_BUILDERS,
                    InventoryUtils.createItem(
                            XMaterial.BARRIER,
                            messages.getString("worldeditor_builders_not_creator_item", player),
                            messages.getStringList("worldeditor_builders_not_creator_lore", player)));
        }
    }

    private void addVisibilityItem(Player player, Inventory inventory) {
        String displayName = messages.getString("worldeditor_visibility_item", player);
        boolean isPrivate = buildWorld.getData().isPrivateWorld();

        if (!playerManager.canCreateWorld(player, Visibility.matchVisibility(isPrivate))) {
            inventory.setItem(
                    SLOT_VISIBILITY,
                    InventoryUtils.createItem(XMaterial.BARRIER, "§c§m" + ChatColor.stripColor(displayName)));
            return;
        }

        XMaterial material = isPrivate ? XMaterial.ENDER_PEARL : XMaterial.ENDER_EYE;
        List<String> lore = messages.getStringList(
                isPrivate ? "worldeditor_visibility_lore_private" : "worldeditor_visibility_lore_public", player);

        inventory.setItem(SLOT_VISIBILITY, InventoryUtils.createItem(material, displayName, lore));
    }

    private void addGameRulesItem(Player player, Inventory inventory) {
        inventory.setItem(
                SLOT_GAMERULES,
                InventoryUtils.createItem(
                        XMaterial.FILLED_MAP,
                        messages.getString("worldeditor_gamerules_item", player),
                        messages.getStringList("worldeditor_gamerules_lore", player)));
    }

    private void addDifficultyItem(Player player, Inventory inventory) {
        XMaterial material =
                switch (buildWorld.getData().getDifficulty()) {
                    case EASY -> XMaterial.GOLDEN_HELMET;
                    case NORMAL -> XMaterial.IRON_HELMET;
                    case HARD -> XMaterial.DIAMOND_HELMET;
                    default -> XMaterial.LEATHER_HELMET;
                };

        inventory.setItem(
                SLOT_DIFFICULTY,
                InventoryUtils.createItem(
                        material,
                        messages.getString("worldeditor_difficulty_item", player),
                        messages.getStringList(
                                "worldeditor_difficulty_lore",
                                player,
                                Map.entry("%difficulty%", getDifficultyName(player)))));
    }

    private void addStatusItem(Player player, Inventory inventory) {
        BuildWorldStatus status = buildWorld.getData().getStatus();
        inventory.setItem(
                SLOT_STATUS,
                InventoryUtils.createItem(
                        plugin.getCustomizableIcons().getIcon(status),
                        messages.getString("worldeditor_status_item", player),
                        messages.getStringList(
                                "worldeditor_status_lore",
                                player,
                                Map.entry("%status%", messages.getString(Messages.getMessageKey(status), player)))));
    }

    private void addProjectItem(Player player, Inventory inventory) {
        inventory.setItem(
                SLOT_PROJECT,
                InventoryUtils.createItem(
                        XMaterial.ANVIL,
                        messages.getString("worldeditor_project_item", player),
                        messages.getStringList(
                                "worldeditor_project_lore",
                                player,
                                Map.entry("%project%", buildWorld.getData().getProject()))));
    }

    private void addPermissionItem(Player player, Inventory inventory) {
        inventory.setItem(
                SLOT_PERMISSION,
                InventoryUtils.createItem(
                        XMaterial.PAPER,
                        messages.getString("worldeditor_permission_item", player),
                        messages.getStringList(
                                "worldeditor_permission_lore",
                                player,
                                Map.entry("%permission%", buildWorld.getData().getPermission()))));
    }

    private String getDifficultyName(Player player) {
        return switch (buildWorld.getData().getDifficulty()) {
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

        if (handleToggleClick(event.getSlot(), player)) {
            return;
        }

        switch (event.getSlot()) {
            case SLOT_TIME -> {
                if (requirePermission(player, "buildsystem.edit.time")) {
                    changeTime(player);
                }
            }
            case SLOT_BUTCHER -> {
                if (requirePermission(player, "buildsystem.edit.entities")) {
                    removeEntities(player);
                }
                return;
            }
            case SLOT_BUILDERS -> {
                if (handleBuildersClick(player, itemStack, event.isRightClick())) {
                    return;
                }
            }
            case SLOT_VISIBILITY -> {
                if (handleVisibilityClick(player, itemStack)) {
                    return;
                }
            }
            case SLOT_GAMERULES -> {
                if (requirePermission(player, "buildsystem.edit.gamerules")) {
                    XSound.BLOCK_CHEST_OPEN.play(player);
                    new GameRulesMenu(plugin, buildWorld, player).open(player);
                }
                return;
            }
            case SLOT_DIFFICULTY -> {
                if (requirePermission(player, "buildsystem.edit.difficulty")) {
                    cycleDifficulty();
                }
            }
            case SLOT_STATUS -> {
                if (requirePermission(player, "buildsystem.edit.status")) {
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    new StatusMenu(plugin, buildWorld, player).open(player);
                }
                return;
            }
            case SLOT_PROJECT -> {
                if (requirePermission(player, "buildsystem.edit.project")) {
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    new SetProjectSubCommand(plugin).getProjectInput(player, buildWorld, false);
                }
                return;
            }
            case SLOT_PERMISSION -> {
                if (requirePermission(player, "buildsystem.edit.permission")) {
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    new SetPermissionSubCommand(plugin).getPermissionInput(player, buildWorld, false);
                }
                return;
            }
            default -> {
                return;
            }
        }

        reopen(player);
    }

    private boolean handleToggleClick(int slot, Player player) {
        Toggle toggle = TOGGLES.get(slot);
        if (toggle == null) {
            return false;
        }

        if (requirePermission(player, toggle.permission())) {
            WorldData worldData = buildWorld.getData();
            toggle.setter().accept(worldData, !toggle.getter().test(worldData));
            reopen(player);
        }
        return true;
    }

    private boolean handleBuildersClick(Player player, ItemStack itemStack, boolean rightClick) {
        if (itemStack.getType() == XMaterial.BARRIER.get()) {
            XSound.ENTITY_ITEM_BREAK.play(player);
            return true;
        }
        if (!requirePermission(player, "buildsystem.edit.builders")) {
            return true;
        }
        if (rightClick) {
            XSound.BLOCK_CHEST_OPEN.play(player);
            new BuilderMenu(plugin, buildWorld, player).open(player);
            return true;
        }

        WorldData worldData = buildWorld.getData();
        worldData.setBuildersEnabled(!worldData.isBuildersEnabled());
        return false;
    }

    private boolean handleVisibilityClick(Player player, ItemStack itemStack) {
        if (itemStack.getType() == XMaterial.BARRIER.get()) {
            XSound.ENTITY_ITEM_BREAK.play(player);
            return true;
        }
        if (requirePermission(player, "buildsystem.edit.visibility")) {
            WorldData worldData = buildWorld.getData();
            worldData.setPrivateWorld(!worldData.isPrivateWorld());
        }
        return false;
    }

    private void cycleDifficulty() {
        Difficulty difficulty = buildWorld.cycleDifficulty();
        buildWorld.getWorld().orElseThrow().setDifficulty(difficulty);
    }

    private void reopen(Player player) {
        XSound.ENTITY_CHICKEN_EGG.play(player);
        new EditMenu(plugin, buildWorld, player).open(player);
    }

    private void changeTime(Player player) {
        var defaultTime = plugin.getConfigService().current().world().defaults().time();
        int time =
                switch (getWorldTime()) {
                    case SUNRISE -> defaultTime.noon();
                    case NOON -> defaultTime.night();
                    case NIGHT -> defaultTime.sunrise();
                };
        buildWorld.getWorld().orElseThrow().setTime(time);
    }

    private TimeOfDay getWorldTime() {
        int worldTime = (int) buildWorld.getWorld().orElseThrow().getTime();
        int noonTime =
                plugin.getConfigService().current().world().defaults().time().noon();
        return TimeOfDay.fromTicks(worldTime, noonTime);
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

    /**
     * Which third of the Minecraft day the world clock currently sits in. Drives the editor's time button.
     */
    public enum TimeOfDay {
        SUNRISE,
        NOON,
        NIGHT;

        /**
         * Minecraft tick at which night begins (the day is 24000 ticks).
         */
        static final int NIGHT_START_TICKS = 13000;

        /**
         * Buckets a raw world tick into a {@link TimeOfDay}.
         *
         * @param worldTicks The world time in ticks (0–24000)
         * @param noonStart The configured tick at which noon begins
         * @return The matching time-of-day bucket
         */
        static TimeOfDay fromTicks(int worldTicks, int noonStart) {
            if (worldTicks >= 0 && worldTicks < noonStart) {
                return SUNRISE;
            } else if (worldTicks >= noonStart && worldTicks < NIGHT_START_TICKS) {
                return NOON;
            } else {
                return NIGHT;
            }
        }
    }
}
