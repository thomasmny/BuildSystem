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
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.util.color.ColorAPI;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;

/**
 * Renders {@link EditMenu}'s render-only, non-authorization-sensitive slots: the world-icon button, time, butcher,
 * game rules, difficulty, status, project, and permission items. Split out of {@link EditMenu} so the menu itself is
 * left holding the slot layout, click handling, and the builders/visibility slots whose render and click paths share
 * an authorization predicate.
 */
@NullMarked
final class EditMenuRenderer {

    private final Messages messages;
    private final MenuItems menuItems;
    private final ConfigService configService;
    private final BuildWorld buildWorld;

    EditMenuRenderer(Messages messages, MenuItems menuItems, ConfigService configService, BuildWorld buildWorld) {
        this.messages = messages;
        this.menuItems = menuItems;
        this.configService = configService;
        this.buildWorld = buildWorld;
    }

    void renderWorldInfo(Player player, Inventory inventory, int slot) {
        String displayName =
                messages.getString("worldeditor_world_item", player, Placeholders.of("%world%", buildWorld.getName()));
        boolean isHead = buildWorld.getIcon() == Material.PLAYER_HEAD;
        String loreKey = isHead ? "worldeditor_world_head_lore" : "worldeditor_world_lore";
        List<String> lore =
                messages.getStringList(loreKey, player, Placeholders.of("%texture%", iconTextureLabel(player)));

        menuItems.renderDisplayable(inventory, slot, buildWorld, player, displayName, lore);
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

    void renderTime(Player player, Inventory inventory, int slot) {
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
                .lore(messages.getStringList("worldeditor_time_lore", player, Placeholders.of("%time%", value)))
                .into(inventory, slot);
    }

    private TimeOfDay getWorldTime() {
        int worldTime = (int) buildWorld.getWorld().orElseThrow().getTime();
        int noonTime = configService.current().world().defaults().time().noon();
        return TimeOfDay.fromTicks(worldTime, noonTime);
    }

    void renderButcher(Player player, Inventory inventory, int slot) {
        ItemBuilder.of(XMaterial.DIAMOND_SWORD)
                .name(messages.getString("worldeditor_butcher_item", player))
                .lore(messages.getStringList("worldeditor_butcher_lore", player))
                .into(inventory, slot);
    }

    void renderGameRules(Player player, Inventory inventory, int slot) {
        ItemBuilder.of(XMaterial.FILLED_MAP)
                .name(messages.getString("worldeditor_gamerules_item", player))
                .lore(messages.getStringList("worldeditor_gamerules_lore", player))
                .into(inventory, slot);
    }

    void renderDifficulty(Player player, Inventory inventory, int slot) {
        XMaterial material =
                switch (buildWorld.getData().get(WorldDataKey.DIFFICULTY)) {
                    case EASY -> XMaterial.GOLDEN_HELMET;
                    case NORMAL -> XMaterial.IRON_HELMET;
                    case HARD -> XMaterial.DIAMOND_HELMET;
                    default -> XMaterial.LEATHER_HELMET;
                };

        ItemBuilder.of(material)
                .name(messages.getString("worldeditor_difficulty_item", player))
                .lore(messages.getStringList(
                        "worldeditor_difficulty_lore",
                        player,
                        Placeholders.of("%difficulty%", getDifficultyName(player))))
                .into(inventory, slot);
    }

    private String getDifficultyName(Player player) {
        return switch (buildWorld.getData().get(WorldDataKey.DIFFICULTY)) {
            case PEACEFUL -> messages.getString("difficulty_peaceful", player);
            case EASY -> messages.getString("difficulty_easy", player);
            case NORMAL -> messages.getString("difficulty_normal", player);
            case HARD -> messages.getString("difficulty_hard", player);
        };
    }

    void renderStatus(Player player, Inventory inventory, int slot) {
        BuildWorldStatus status = buildWorld.getData().get(WorldDataKey.STATUS);
        ItemBuilder.of(status.getIcon())
                .name(messages.getString("worldeditor_status_item", player))
                .lore(messages.getStringList(
                        "worldeditor_status_lore",
                        player,
                        Placeholders.of("%status%", ColorAPI.process(status.getStyledName()))))
                .into(inventory, slot);
    }

    void renderProject(Player player, Inventory inventory, int slot) {
        ItemBuilder.of(XMaterial.ANVIL)
                .name(messages.getString("worldeditor_project_item", player))
                .lore(messages.getStringList(
                        "worldeditor_project_lore",
                        player,
                        Placeholders.of("%project%", buildWorld.getData().get(WorldDataKey.PROJECT))))
                .into(inventory, slot);
    }

    void renderPermission(Player player, Inventory inventory, int slot) {
        ItemBuilder.of(XMaterial.PAPER)
                .name(messages.getString("worldeditor_permission_item", player))
                .lore(messages.getStringList(
                        "worldeditor_permission_lore",
                        player,
                        Placeholders.of("%permission%", buildWorld.getData().get(WorldDataKey.PERMISSION))))
                .into(inventory, slot);
    }
}
