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
import de.eintosti.buildsystem.menu.*;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.util.Permissions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class EditMenu extends ButtonMenu<EditMenu.EditButton> {

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

    /**
     * Classifies what a button does after a click, so the per-slot contract can be asserted as data. The actual action
     * lives in each button's {@link MenuButton#onClick}; this is the classification the golden test pins.
     */
    enum ClickOutcome {
        REOPEN,
        SUBMENU,
        INPUT,
        CLOSE,
        NONE
    }

    /**
     * A single editor slot: its required permission (or {@code null} for a render-only slot), its {@link ClickOutcome}
     * classification, and the render/click behavior. Declaring each slot once here replaces the old parallel
     * {@code populate} calls and {@code handleClick} switch.
     */
    record EditButton(
            @Nullable String permission,
            ClickOutcome outcome,
            BiConsumer<Player, Inventory> renderer,
            BiConsumer<Player, InventoryClickEvent> clickHandler)
            implements MenuButton {

        @Override
        public void render(Player player, Inventory inventory, int slot) {
            renderer.accept(player, inventory);
        }

        @Override
        public void onClick(Player player, InventoryClickEvent event) {
            clickHandler.accept(player, event);
        }

        static Builder builder() {
            return new Builder();
        }

        /**
         * Fluent builder for an {@link EditButton}. {@code outcome} defaults to {@link ClickOutcome#NONE}; the
         * {@code permission}, renderer, and click handler default to none/no-op until set. Editor slots render at fixed
         * positions, so the renderer is a plain {@code (player, inventory)} consumer rather than a slot-aware one.
         */
        static final class Builder {

            private @Nullable String permission;
            private ClickOutcome outcome = ClickOutcome.NONE;
            private BiConsumer<Player, Inventory> renderer = (player, inventory) -> {};
            private BiConsumer<Player, InventoryClickEvent> clickHandler = (player, event) -> {};

            private Builder() {}

            Builder permission(@Nullable String permission) {
                this.permission = permission;
                return this;
            }

            Builder outcome(ClickOutcome outcome) {
                this.outcome = outcome;
                return this;
            }

            Builder render(BiConsumer<Player, Inventory> renderer) {
                this.renderer = renderer;
                return this;
            }

            Builder onClick(BiConsumer<Player, InventoryClickEvent> clickHandler) {
                this.clickHandler = clickHandler;
                return this;
            }

            EditButton build() {
                return new EditButton(permission, outcome, renderer, clickHandler);
            }
        }
    }

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
                EditButton.builder()
                        .permission(Permissions.EDIT_ICON)
                        .outcome(ClickOutcome.SUBMENU)
                        .render((player, inventory) -> renderer.renderWorldInfo(player, inventory, SLOT_WORLD_INFO))
                        .onClick(this::onWorldInfoClick)
                        .build());

        EditMenuToggles.TOGGLES.forEach((slot, toggle) -> register(
                slot,
                EditButton.builder()
                        .permission(toggle.permission())
                        .outcome(ClickOutcome.REOPEN)
                        .render((player, inventory) ->
                                toggle.render(menuItems, buildWorld.getData(), player, inventory, slot))
                        .onClick((player, event) -> {
                            toggle.flip(buildWorld.getData());
                            reopen(player);
                        })
                        .build()));

        register(
                SLOT_TIME,
                EditButton.builder()
                        .permission(Permissions.EDIT_TIME)
                        .outcome(ClickOutcome.REOPEN)
                        .render((player, inventory) -> renderer.renderTime(player, inventory, SLOT_TIME))
                        .onClick((player, event) -> {
                            changeTime(player);
                            reopen(player);
                        })
                        .build());

        register(
                SLOT_BUTCHER,
                EditButton.builder()
                        .permission(Permissions.EDIT_ENTITIES)
                        .outcome(ClickOutcome.CLOSE)
                        .render((player, inventory) -> renderer.renderButcher(player, inventory, SLOT_BUTCHER))
                        .onClick((player, event) -> removeEntities(player))
                        .build());

        register(
                SLOT_PHYSICS,
                EditButton.builder()
                        .permission(Permissions.EDIT_PHYSICS)
                        .outcome(ClickOutcome.REOPEN)
                        .render((player, inventory) -> menuItems.addToggleItem(
                                player,
                                inventory,
                                SLOT_PHYSICS,
                                XMaterial.SAND,
                                buildWorld.getData().get(WorldDataKey.PHYSICS),
                                "worldeditor_physics_item",
                                "worldeditor_physics_lore"))
                        .onClick(this::onPhysicsClick)
                        .build());

        register(
                SLOT_BUILDERS,
                EditButton.builder()
                        .permission(Permissions.EDIT_BUILDERS)
                        .outcome(ClickOutcome.REOPEN)
                        .render(this::renderBuilders)
                        .onClick(this::onBuildersClick)
                        .build());

        register(
                SLOT_VISIBILITY,
                EditButton.builder()
                        .permission(Permissions.EDIT_VISIBILITY)
                        .outcome(ClickOutcome.REOPEN)
                        .render(this::renderVisibility)
                        .onClick(this::onVisibilityClick)
                        .build());

        register(
                SLOT_GAMERULES,
                EditButton.builder()
                        .permission(Permissions.EDIT_GAMERULES)
                        .outcome(ClickOutcome.SUBMENU)
                        .render((player, inventory) -> renderer.renderGameRules(player, inventory, SLOT_GAMERULES))
                        .onClick((player, event) -> {
                            XSound.BLOCK_CHEST_OPEN.play(player);
                            menus.openGameRules(buildWorld, player);
                        })
                        .build());

        register(
                SLOT_DIFFICULTY,
                EditButton.builder()
                        .permission(Permissions.EDIT_DIFFICULTY)
                        .outcome(ClickOutcome.REOPEN)
                        .render((player, inventory) -> renderer.renderDifficulty(player, inventory, SLOT_DIFFICULTY))
                        .onClick((player, event) -> {
                            cycleDifficulty();
                            reopen(player);
                        })
                        .build());

        register(
                SLOT_STATUS,
                EditButton.builder()
                        .permission(Permissions.EDIT_STATUS)
                        .outcome(ClickOutcome.SUBMENU)
                        .render((player, inventory) -> renderer.renderStatus(player, inventory, SLOT_STATUS))
                        .onClick((player, event) -> {
                            XSound.ENTITY_CHICKEN_EGG.play(player);
                            menus.openStatus(buildWorld, player);
                        })
                        .build());

        register(
                SLOT_PROJECT,
                EditButton.builder()
                        .permission(Permissions.EDIT_PROJECT)
                        .outcome(ClickOutcome.INPUT)
                        .render((player, inventory) -> renderer.renderProject(player, inventory, SLOT_PROJECT))
                        .onClick((player, event) -> {
                            XSound.ENTITY_CHICKEN_EGG.play(player);
                            menus.promptWorldProject(buildWorld, player);
                        })
                        .build());

        register(
                SLOT_PERMISSION,
                EditButton.builder()
                        .permission(Permissions.EDIT_PERMISSION)
                        .outcome(ClickOutcome.INPUT)
                        .render((player, inventory) -> renderer.renderPermission(player, inventory, SLOT_PERMISSION))
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

    private void renderBuilders(Player player, Inventory inventory) {
        if (canManageBuilders(player)) {
            menuItems.addToggleItem(
                    player,
                    inventory,
                    SLOT_BUILDERS,
                    XMaterial.IRON_PICKAXE,
                    buildWorld.getData().get(WorldDataKey.BUILDERS_ENABLED),
                    "worldeditor_builders_item",
                    "worldeditor_builders_lore");
        } else {
            ItemBuilder.of(XMaterial.BARRIER)
                    .name(messages.getString("worldeditor_builders_not_creator_item", player))
                    .lore(messages.getStringList("worldeditor_builders_not_creator_lore", player))
                    .into(inventory, SLOT_BUILDERS);
        }
    }

    private boolean canChangeVisibility(Player player, boolean isPrivate) {
        return playerManager.canCreateWorld(player, Visibility.matchVisibility(isPrivate));
    }

    private void renderVisibility(Player player, Inventory inventory) {
        String displayName = messages.getString("worldeditor_visibility_item", player);
        boolean isPrivate = buildWorld.getData().get(WorldDataKey.VISIBILITY).isPrivate();

        if (!canChangeVisibility(player, isPrivate)) {
            ItemBuilder.of(XMaterial.BARRIER)
                    .name("§c§m" + ChatColor.stripColor(displayName))
                    .into(inventory, SLOT_VISIBILITY);
            return;
        }

        XMaterial material = isPrivate ? XMaterial.ENDER_PEARL : XMaterial.ENDER_EYE;
        List<String> lore = messages.getStringList(
                isPrivate ? "worldeditor_visibility_lore_private" : "worldeditor_visibility_lore_public", player);

        ItemBuilder.of(material).name(displayName).lore(lore).into(inventory, SLOT_VISIBILITY);
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

    /**
     * The slot &rarr; required-permission mapping, derived from the button registry. Render-only slots (no permission)
     * are omitted. Exposed for the golden test that pins the per-slot contract.
     */
    Map<Integer, String> permissionBySlot() {
        Map<Integer, String> permissions = new LinkedHashMap<>();
        buttons().forEach((slot, button) -> {
            String permission = button.permission();
            if (permission != null) {
                permissions.put(slot, permission);
            }
        });
        return permissions;
    }

    /**
     * The slot &rarr; {@link ClickOutcome} classification, derived from the button registry. Exposed for the golden
     * test that pins the per-slot contract.
     */
    Map<Integer, ClickOutcome> outcomeBySlot() {
        Map<Integer, ClickOutcome> outcomes = new LinkedHashMap<>();
        buttons().forEach((slot, button) -> outcomes.put(slot, button.outcome()));
        return outcomes;
    }

    private void cycleDifficulty() {
        Difficulty difficulty = buildWorld.cycleDifficulty();
        buildWorld.getWorld().orElseThrow().setDifficulty(difficulty);
    }

    private void reopen(Player player) {
        XSound.ENTITY_CHICKEN_EGG.play(player);
        menus.openEdit(buildWorld, player);
    }

    private void changeTime(Player player) {
        Time defaultTime = configService.current().world().defaults().time();
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
        int noonTime = configService.current().world().defaults().time().noon();
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
        messages.sendMessage(player, "worldeditor_butcher_removed", Placeholders.of("%amount%", entitiesRemoved.get()));
    }
}
