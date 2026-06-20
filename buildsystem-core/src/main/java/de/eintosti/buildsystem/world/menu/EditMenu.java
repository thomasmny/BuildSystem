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
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.PlayerChatInput;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.util.color.ColorAPI;
import de.eintosti.buildsystem.world.menu.setup.MaterialPickerMenu;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
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

    private final BuildSystemPlugin plugin;
    private final PlayerServiceImpl playerManager;
    private final BuildWorld buildWorld;

    public EditMenu(BuildSystemPlugin plugin, BuildWorld buildWorld, Player player) {
        super(plugin.getMessages(), 54, plugin.getMessages().getString("worldeditor_title", player));
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerService();
        this.buildWorld = buildWorld;
        buildButtons();
    }

    private void buildButtons() {
        register(
                SLOT_WORLD_INFO,
                EditButton.builder()
                        .permission("buildsystem.edit.icon")
                        .outcome(ClickOutcome.SUBMENU)
                        .render(this::renderWorldInfo)
                        .onClick(this::onWorldInfoClick)
                        .build());

        TOGGLES.forEach((slot, toggle) -> register(
                slot,
                EditButton.builder()
                        .permission(toggle.permission())
                        .outcome(ClickOutcome.REOPEN)
                        .render((player, inventory) -> renderToggle(player, inventory, slot, toggle))
                        .onClick((player, event) -> {
                            if (requirePermission(player, toggle.permission())) {
                                WorldData worldData = buildWorld.getData();
                                toggle.setter()
                                        .accept(worldData, !toggle.getter().test(worldData));
                                reopen(player);
                            }
                        })
                        .build()));

        register(
                SLOT_TIME,
                EditButton.builder()
                        .permission("buildsystem.edit.time")
                        .outcome(ClickOutcome.REOPEN)
                        .render(this::renderTime)
                        .onClick((player, event) -> {
                            if (requirePermission(player, "buildsystem.edit.time")) {
                                changeTime(player);
                            }
                            reopen(player);
                        })
                        .build());

        register(
                SLOT_BUTCHER,
                EditButton.builder()
                        .permission("buildsystem.edit.entities")
                        .outcome(ClickOutcome.CLOSE)
                        .render(this::renderButcher)
                        .onClick((player, event) -> {
                            if (requirePermission(player, "buildsystem.edit.entities")) {
                                removeEntities(player);
                            }
                        })
                        .build());

        register(
                SLOT_BUILDERS,
                EditButton.builder()
                        .permission("buildsystem.edit.builders")
                        .outcome(ClickOutcome.REOPEN)
                        .render(this::renderBuilders)
                        .onClick(this::onBuildersClick)
                        .build());

        register(
                SLOT_VISIBILITY,
                EditButton.builder()
                        .permission("buildsystem.edit.visibility")
                        .outcome(ClickOutcome.REOPEN)
                        .render(this::renderVisibility)
                        .onClick(this::onVisibilityClick)
                        .build());

        register(
                SLOT_GAMERULES,
                EditButton.builder()
                        .permission("buildsystem.edit.gamerules")
                        .outcome(ClickOutcome.SUBMENU)
                        .render(this::renderGameRules)
                        .onClick((player, event) -> {
                            if (requirePermission(player, "buildsystem.edit.gamerules")) {
                                XSound.BLOCK_CHEST_OPEN.play(player);
                                new GameRulesMenu(plugin, buildWorld, player).open(player);
                            }
                        })
                        .build());

        register(
                SLOT_DIFFICULTY,
                EditButton.builder()
                        .permission("buildsystem.edit.difficulty")
                        .outcome(ClickOutcome.REOPEN)
                        .render(this::renderDifficulty)
                        .onClick((player, event) -> {
                            if (requirePermission(player, "buildsystem.edit.difficulty")) {
                                cycleDifficulty();
                            }
                            reopen(player);
                        })
                        .build());

        register(
                SLOT_STATUS,
                EditButton.builder()
                        .permission("buildsystem.edit.status")
                        .outcome(ClickOutcome.SUBMENU)
                        .render(this::renderStatus)
                        .onClick((player, event) -> {
                            if (requirePermission(player, "buildsystem.edit.status")) {
                                XSound.ENTITY_CHICKEN_EGG.play(player);
                                new StatusMenu(plugin, buildWorld, player).open(player);
                            }
                        })
                        .build());

        register(
                SLOT_PROJECT,
                EditButton.builder()
                        .permission("buildsystem.edit.project")
                        .outcome(ClickOutcome.INPUT)
                        .render(this::renderProject)
                        .onClick((player, event) -> {
                            if (requirePermission(player, "buildsystem.edit.project")) {
                                XSound.ENTITY_CHICKEN_EGG.play(player);
                                new SetProjectSubCommand(plugin).getProjectInput(player, buildWorld, false);
                            }
                        })
                        .build());

        register(
                SLOT_PERMISSION,
                EditButton.builder()
                        .permission("buildsystem.edit.permission")
                        .outcome(ClickOutcome.INPUT)
                        .render(this::renderPermission)
                        .onClick((player, event) -> {
                            if (requirePermission(player, "buildsystem.edit.permission")) {
                                XSound.ENTITY_CHICKEN_EGG.play(player);
                                new SetPermissionSubCommand(plugin).getPermissionInput(player, buildWorld, false);
                            }
                        })
                        .build());
    }

    @Override
    protected void populate(Player player) {
        plugin.getMenuItems().fillAll(player, getInventory());
        renderButtons(player);
    }

    private void renderToggle(Player player, Inventory inventory, int slot, Toggle toggle) {
        WorldData worldData = buildWorld.getData();
        boolean enabled = toggle.getter().test(worldData);
        plugin.getMenuItems()
                .addToggleItem(
                        player, inventory, slot, toggle.iconFor(enabled), enabled, toggle.itemKey(), toggle.loreKey());
    }

    private void renderWorldInfo(Player player, Inventory inventory) {
        String displayName =
                messages.getString("worldeditor_world_item", player, Map.entry("%world%", buildWorld.getName()));
        boolean isHead = buildWorld.getIcon() == XMaterial.PLAYER_HEAD;
        String loreKey = isHead ? "worldeditor_world_head_lore" : "worldeditor_world_lore";
        ItemBuilder.icon(buildWorld, player)
                .name(displayName)
                .lore(messages.getStringList(loreKey, player, Map.entry("%texture%", iconTextureLabel(player))))
                .into(inventory, SLOT_WORLD_INFO);
    }

    /**
     * The world-icon button mirrors the category icon control: left-click opens the item picker to choose the material,
     * and when that material is a player head a right-click prompts for the head texture (a texture, {@code viewer} for
     * the viewing player's head, or {@code none} to clear).
     */
    private void onWorldInfoClick(Player player, InventoryClickEvent event) {
        if (!requirePermission(player, "buildsystem.edit.icon")) {
            return;
        }
        if (buildWorld.getIcon() == XMaterial.PLAYER_HEAD && event.isRightClick()) {
            promptIconTexture(player);
            return;
        }
        XSound.BLOCK_CHEST_OPEN.play(player);
        new MaterialPickerMenu(
                        plugin,
                        player,
                        material -> {
                            buildWorld.setIcon(material);
                            reopen(player);
                        },
                        () -> reopen(player))
                .open(player);
    }

    private void promptIconTexture(Player player) {
        new PlayerChatInput(
                plugin,
                player,
                "worldeditor_world_skull_prompt",
                input -> {
                    applyIconTexture(input);
                    reopen(player);
                },
                () -> reopen(player));
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

    private String iconTextureLabel(Player player) {
        String texture = buildWorld.getIconSkullTexture();
        if (texture == null || texture.isBlank()) {
            return messages.getString("worldeditor_world_skull_none", player);
        }
        if (ItemBuilder.VIEWER_HEAD.equals(texture)) {
            return messages.getString("worldeditor_world_skull_viewer", player);
        }
        return messages.getString("worldeditor_world_skull_custom", player);
    }

    private void renderTime(Player player, Inventory inventory) {
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

        ItemBuilder.of(material)
                .name(messages.getString("worldeditor_time_item", player))
                .lore(messages.getStringList("worldeditor_time_lore", player, Map.entry("%time%", value)))
                .into(inventory, SLOT_TIME);
    }

    private void renderButcher(Player player, Inventory inventory) {
        ItemBuilder.of(XMaterial.DIAMOND_SWORD)
                .name(messages.getString("worldeditor_butcher_item", player))
                .lore(messages.getStringList("worldeditor_butcher_lore", player))
                .into(inventory, SLOT_BUTCHER);
    }

    private void renderBuilders(Player player, Inventory inventory) {
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
            ItemBuilder.of(XMaterial.BARRIER)
                    .name(messages.getString("worldeditor_builders_not_creator_item", player))
                    .lore(messages.getStringList("worldeditor_builders_not_creator_lore", player))
                    .into(inventory, SLOT_BUILDERS);
        }
    }

    private void renderVisibility(Player player, Inventory inventory) {
        String displayName = messages.getString("worldeditor_visibility_item", player);
        boolean isPrivate = buildWorld.getData().getVisibility().isPrivate();

        if (!playerManager.canCreateWorld(player, Visibility.matchVisibility(isPrivate))) {
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

    private void renderGameRules(Player player, Inventory inventory) {
        ItemBuilder.of(XMaterial.FILLED_MAP)
                .name(messages.getString("worldeditor_gamerules_item", player))
                .lore(messages.getStringList("worldeditor_gamerules_lore", player))
                .into(inventory, SLOT_GAMERULES);
    }

    private void renderDifficulty(Player player, Inventory inventory) {
        XMaterial material =
                switch (buildWorld.getData().getDifficulty()) {
                    case EASY -> XMaterial.GOLDEN_HELMET;
                    case NORMAL -> XMaterial.IRON_HELMET;
                    case HARD -> XMaterial.DIAMOND_HELMET;
                    default -> XMaterial.LEATHER_HELMET;
                };

        ItemBuilder.of(material)
                .name(messages.getString("worldeditor_difficulty_item", player))
                .lore(messages.getStringList(
                        "worldeditor_difficulty_lore", player, Map.entry("%difficulty%", getDifficultyName(player))))
                .into(inventory, SLOT_DIFFICULTY);
    }

    private void renderStatus(Player player, Inventory inventory) {
        BuildWorldStatus status = buildWorld.getData().getStatus();
        ItemBuilder.of(status.getIcon())
                .name(messages.getString("worldeditor_status_item", player))
                .lore(messages.getStringList(
                        "worldeditor_status_lore",
                        player,
                        Map.entry("%status%", ColorAPI.process(status.getStyledName()))))
                .into(inventory, SLOT_STATUS);
    }

    private void renderProject(Player player, Inventory inventory) {
        ItemBuilder.of(XMaterial.ANVIL)
                .name(messages.getString("worldeditor_project_item", player))
                .lore(messages.getStringList(
                        "worldeditor_project_lore",
                        player,
                        Map.entry("%project%", buildWorld.getData().getProject())))
                .into(inventory, SLOT_PROJECT);
    }

    private void renderPermission(Player player, Inventory inventory) {
        ItemBuilder.of(XMaterial.PAPER)
                .name(messages.getString("worldeditor_permission_item", player))
                .lore(messages.getStringList(
                        "worldeditor_permission_lore",
                        player,
                        Map.entry("%permission%", buildWorld.getData().getPermission())))
                .into(inventory, SLOT_PERMISSION);
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

        super.handleClick(event);
    }

    /**
     * The builders button mixes behaviors, so it cannot share the toggle closure: a barrier icon (player is not the
     * creator) only plays the deny sound, a right-click opens the {@link BuilderMenu}, and a left-click flips the
     * builders flag and re-opens. Re-opening happens only on the successful left-click path.
     */
    private void onBuildersClick(Player player, InventoryClickEvent event) {
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack != null && itemStack.getType() == XMaterial.BARRIER.get()) {
            XSound.ENTITY_ITEM_BREAK.play(player);
            return;
        }
        if (!requirePermission(player, "buildsystem.edit.builders")) {
            return;
        }
        if (event.isRightClick()) {
            XSound.BLOCK_CHEST_OPEN.play(player);
            new BuilderMenu(plugin, buildWorld, player).open(player);
            return;
        }

        WorldData worldData = buildWorld.getData();
        worldData.setBuildersEnabled(!worldData.isBuildersEnabled());
        reopen(player);
    }

    /**
     * The visibility button guards against the barrier icon (player cannot create the target visibility): a barrier
     * click only plays the deny sound and does not re-open. Every other click re-opens, matching the original behavior
     * even when the permission check itself denies.
     */
    private void onVisibilityClick(Player player, InventoryClickEvent event) {
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack != null && itemStack.getType() == XMaterial.BARRIER.get()) {
            XSound.ENTITY_ITEM_BREAK.play(player);
            return;
        }
        if (requirePermission(player, "buildsystem.edit.visibility")) {
            WorldData worldData = buildWorld.getData();
            worldData.setVisibility(
                    worldData.getVisibility().isPrivate() ? Visibility.EVERYONE : Visibility.ADDED_PLAYERS);
        }
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
