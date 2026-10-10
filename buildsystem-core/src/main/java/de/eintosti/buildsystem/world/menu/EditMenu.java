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

import com.cryptomorin.xseries.XEntityType;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.google.common.collect.Sets;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.config.PluginConfig.World.Defaults.Time;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.util.Permissions;
import de.eintosti.buildsystem.world.WorldClock;
import de.eintosti.buildsystem.world.WorldNames;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class EditMenu extends ButtonMenu {

    private static final int SLOT_WORLD_INFO = 3;
    private static final int SLOT_PHYSICS = 22;
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

    private final PlayerServiceImpl playerManager;
    private final MenuItems menuItems;
    private final ConfigService configService;
    private final Prompts prompts;
    private final Menus menus;
    private final BuildWorld buildWorld;
    private final EditMenuRenderer renderer;

    public EditMenu(
            Messages messages,
            PlayerServiceImpl playerService,
            MenuItems menuItems,
            ConfigService configService,
            Prompts prompts,
            Menus menus,
            BuildWorld buildWorld,
            Player player) {
        super(messages, 54, messages.getString("worldeditor_title", player));
        this.playerManager = playerService;
        this.menuItems = menuItems;
        this.configService = configService;
        this.prompts = prompts;
        this.menus = menus;
        this.buildWorld = buildWorld;
        this.renderer = new EditMenuRenderer(messages, menuItems, configService, buildWorld);
        buildButtons();
    }

    private void buildButtons() {
        register(
                SLOT_WORLD_INFO,
                MenuButton.builder()
                        .permission(Permissions.EDIT_ICON)
                        .render((player, inventory, slot) -> renderer.renderWorldInfo(player, inventory, slot))
                        .onClick(this::onWorldInfoClick)
                        .build());

        EditMenuToggles.TOGGLES.forEach((toggleSlot, toggle) -> register(
                toggleSlot,
                MenuButton.builder()
                        .permission(toggle.permission())
                        .render((player, inventory, slot) ->
                                toggle.render(menuItems, buildWorld.getData(), player, inventory, slot))
                        .onClick((player, event) -> {
                            toggle.flip(buildWorld.getData());
                            reopen(player);
                        })
                        .build()));

        register(
                SLOT_TIME,
                MenuButton.builder()
                        .permission(Permissions.EDIT_TIME)
                        .render((player, inventory, slot) -> renderer.renderTime(player, inventory, slot))
                        .onClick((player, event) -> {
                            changeTime(player);
                            reopen(player);
                        })
                        .build());

        register(
                SLOT_BUTCHER,
                MenuButton.builder()
                        .permission(Permissions.EDIT_ENTITIES)
                        .render((player, inventory, slot) -> renderer.renderButcher(player, inventory, slot))
                        .onClick((player, event) -> removeEntities(player))
                        .build());

        register(
                SLOT_PHYSICS,
                MenuButton.builder()
                        .permission(Permissions.EDIT_PHYSICS)
                        .render((player, inventory, slot) -> menuItems.addToggleItem(
                                player,
                                inventory,
                                slot,
                                XMaterial.SAND,
                                buildWorld.getData().get(WorldDataKey.PHYSICS),
                                "worldeditor_physics_item",
                                "worldeditor_physics_lore"))
                        .onClick(this::onPhysicsClick)
                        .build());

        register(
                SLOT_BUILDERS,
                MenuButton.builder()
                        .permission(Permissions.EDIT_BUILDERS)
                        .render(this::renderBuilders)
                        .onClick(this::onBuildersClick)
                        .build());

        register(
                SLOT_VISIBILITY,
                MenuButton.builder()
                        .permission(Permissions.EDIT_VISIBILITY)
                        .render(this::renderVisibility)
                        .onClick(this::onVisibilityClick)
                        .build());

        register(
                SLOT_GAMERULES,
                MenuButton.builder()
                        .permission(Permissions.EDIT_GAMERULES)
                        .render((player, inventory, slot) -> renderer.renderGameRules(player, inventory, slot))
                        .onClick((player, event) -> {
                            XSound.BLOCK_CHEST_OPEN.play(player);
                            menus.openGameRules(buildWorld, player);
                        })
                        .build());

        register(
                SLOT_DIFFICULTY,
                MenuButton.builder()
                        .permission(Permissions.EDIT_DIFFICULTY)
                        .render((player, inventory, slot) -> renderer.renderDifficulty(player, inventory, slot))
                        .onClick((player, event) -> {
                            cycleDifficulty();
                            reopen(player);
                        })
                        .build());

        register(
                SLOT_STATUS,
                MenuButton.builder()
                        .permission(Permissions.EDIT_STATUS)
                        .render((player, inventory, slot) -> renderer.renderStatus(player, inventory, slot))
                        .onClick((player, event) -> {
                            XSound.ENTITY_CHICKEN_EGG.play(player);
                            menus.openStatus(buildWorld, player);
                        })
                        .build());

        register(
                SLOT_PROJECT,
                MenuButton.builder()
                        .permission(Permissions.EDIT_PROJECT)
                        .render((player, inventory, slot) -> renderer.renderProject(player, inventory, slot))
                        .onClick((player, event) -> {
                            XSound.ENTITY_CHICKEN_EGG.play(player);
                            menus.promptWorldProject(buildWorld, player);
                        })
                        .build());

        register(
                SLOT_PERMISSION,
                MenuButton.builder()
                        .permission(Permissions.EDIT_PERMISSION)
                        .render((player, inventory, slot) -> renderer.renderPermission(player, inventory, slot))
                        .onClick((player, event) -> {
                            XSound.ENTITY_CHICKEN_EGG.play(player);
                            menus.promptWorldPermission(buildWorld, player);
                        })
                        .build());
    }

    @Override
    protected void populate(Player player) {
        menuItems.fillAll(player, getInventory());
        renderButtons(player);
    }

    /**
     * The world-icon button mirrors the category icon control: left-click opens the item picker to choose the material,
     * and when that material is a player head a right-click prompts for the head texture (a texture, {@code viewer} for
     * the viewing player's head, or {@code none} to clear).
     */
    private void onWorldInfoClick(Player player, InventoryClickEvent event) {
        if (buildWorld.getIcon() == Material.PLAYER_HEAD && event.isRightClick()) {
            promptIconTexture(player);
            return;
        }

        XSound.BLOCK_CHEST_OPEN.play(player);
        menus.openMaterialPicker(
                player,
                material -> {
                    buildWorld.setIcon(material);
                    reopen(player);
                },
                () -> reopen(player));
    }

    private void promptIconTexture(Player player) {
        prompts.prompt(player)
                .title("worldeditor_world_skull_prompt")
                .onCancel(() -> reopen(player))
                .request(input -> {
                    applyIconTexture(input);
                    reopen(player);
                });
    }

    private void applyIconTexture(String rawInput) {
        String clean = rawInput.strip();
        if (clean.equalsIgnoreCase("none") || clean.equalsIgnoreCase("clear")) {
            buildWorld.setIconSkullTexture(null);
        } else if (clean.equalsIgnoreCase("viewer")) {
            buildWorld.setIconSkullTexture(ItemBuilder.VIEWER_HEAD);
        } else {
            buildWorld.setIconSkullTexture(clean);
        }
    }

    private boolean canManageBuilders(Player player) {
        return buildWorld.getBuilders().isCreator(player) || player.hasPermission(BuildSystemPlugin.ADMIN_PERMISSION);
    }

    private void renderBuilders(Player player, Inventory inventory, int slot) {
        if (canManageBuilders(player)) {
            menuItems.addToggleItem(
                    player,
                    inventory,
                    slot,
                    XMaterial.IRON_PICKAXE,
                    buildWorld.getData().get(WorldDataKey.BUILDERS_ENABLED),
                    "worldeditor_builders_item",
                    "worldeditor_builders_lore");
        } else {
            ItemBuilder.of(XMaterial.BARRIER)
                    .name(messages.getString("worldeditor_builders_not_creator_item", player))
                    .lore(messages.getStringList("worldeditor_builders_not_creator_lore", player))
                    .into(inventory, slot);
        }
    }

    private boolean canChangeVisibility(Player player, boolean isPrivate) {
        return playerManager.canCreateWorld(player, Visibility.matchVisibility(isPrivate));
    }

    private void renderVisibility(Player player, Inventory inventory, int slot) {
        String displayName = messages.getString("worldeditor_visibility_item", player);
        boolean isPrivate = buildWorld.getData().get(WorldDataKey.VISIBILITY).isPrivate();

        if (!canChangeVisibility(player, isPrivate)) {
            ItemBuilder.of(XMaterial.BARRIER)
                    .name("§c§m" + ChatColor.stripColor(displayName))
                    .into(inventory, slot);
            return;
        }

        XMaterial material = isPrivate ? XMaterial.ENDER_PEARL : XMaterial.ENDER_EYE;
        List<String> lore = messages.getStringList(
                isPrivate ? "worldeditor_visibility_lore_private" : "worldeditor_visibility_lore_public", player);

        ItemBuilder.of(material).name(displayName).lore(lore).into(inventory, slot);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return;
        }

        super.handleClick(event);
    }

    /**
     * The physics button is dual-action: a left-click flips the master physics flag and re-opens, a right-click opens
     * the {@link PhysicsMenu} with the per-category exceptions.
     */
    private void onPhysicsClick(Player player, InventoryClickEvent event) {
        if (event.isRightClick()) {
            XSound.BLOCK_CHEST_OPEN.play(player);
            menus.openPhysics(buildWorld, player);
            return;
        }

        WorldData worldData = buildWorld.getData();
        worldData.set(WorldDataKey.PHYSICS, !worldData.get(WorldDataKey.PHYSICS));
        reopen(player);
    }

    /**
     * The builders button mixes behaviors, so it cannot share the toggle closure: a player who cannot manage builders
     * only plays the deny sound, a right-click opens the {@link BuilderMenu}, and a left-click flips the builders flag
     * and re-opens. Re-opening happens only on the successful left-click path.
     */
    private void onBuildersClick(Player player, InventoryClickEvent event) {
        if (!canManageBuilders(player)) {
            XSound.ENTITY_ITEM_BREAK.play(player);
            return;
        }

        if (event.isRightClick()) {
            XSound.BLOCK_CHEST_OPEN.play(player);
            menus.openBuilder(buildWorld, player);
            return;
        }

        WorldData worldData = buildWorld.getData();
        worldData.set(WorldDataKey.BUILDERS_ENABLED, !worldData.get(WorldDataKey.BUILDERS_ENABLED));
        reopen(player);
    }

    /**
     * The visibility button guards against a player who cannot create the target visibility: such a click only plays
     * the deny sound and does not re-open. Every other click re-opens.
     */
    private void onVisibilityClick(Player player, InventoryClickEvent event) {
        boolean isPrivate = buildWorld.getData().get(WorldDataKey.VISIBILITY).isPrivate();
        if (!canChangeVisibility(player, isPrivate)) {
            XSound.ENTITY_ITEM_BREAK.play(player);
            return;
        }

        buildWorld.getData().set(WorldDataKey.VISIBILITY, isPrivate ? Visibility.EVERYONE : Visibility.ADDED_PLAYERS);
        reopen(player);
    }

    private void cycleDifficulty() {
        Difficulty difficulty = buildWorld.cycleDifficulty();
        buildWorld.getWorld().ifPresent(world -> world.setDifficulty(difficulty));
    }

    private void reopen(Player player) {
        XSound.ENTITY_CHICKEN_EGG.play(player);
        menus.reopenEdit(buildWorld, player);
    }

    private void changeTime(Player player) {
        Time defaultTime = configService.current().world().defaults().time();
        buildWorld.getWorld().ifPresent(world -> {
            int time =
                    switch (renderer.timeOfDay(world)) {
                        case SUNRISE -> defaultTime.noon();
                        case NOON -> defaultTime.night();
                        case NIGHT -> defaultTime.sunrise();
                    };
            if (!WorldClock.trySetTime(world, time)) {
                messages.sendMessage(player, "time_fixed", Placeholders.of("%world%", buildWorld.getName()));
            }
        });
    }

    private void removeEntities(Player player) {
        World bukkitWorld = WorldNames.bukkitWorld(buildWorld.getName());
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
        messages.sendMessage(player, "worldeditor_butcher_removed", Placeholders.of("%amount%", entitiesRemoved.get()));
    }
}
