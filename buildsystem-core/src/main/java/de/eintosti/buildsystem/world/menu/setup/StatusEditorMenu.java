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
package de.eintosti.buildsystem.world.menu.setup;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.util.color.ColorAPI;
import de.eintosti.buildsystem.world.data.WorldStatusImpl;
import de.eintosti.buildsystem.world.data.WorldStatusRegistryImpl;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Edits a single {@link BuildWorldStatus}: name, colour, icon, order, the building/navigator behaviour flags, and the
 * auto-progress target. Built-in statuses can be restyled here but never deleted (deletion lives in
 * {@link StatusManagementMenu}).
 */
@NullMarked
public class StatusEditorMenu extends RegistryEditorMenu {

    private static final int SLOT_RENAME = 10;
    private static final int SLOT_COLOR = 11;
    private static final int SLOT_ICON = 12;
    private static final int SLOT_ORDER = 13;
    private static final int SLOT_BUILDING = 14;
    private static final int SLOT_NAVIGATOR = 15;
    private static final int SLOT_PROGRESSES = 16;
    private static final int SLOT_BACK = 18;

    private final WorldStatusRegistryImpl registry;
    private final WorldStatusImpl status;

    public StatusEditorMenu(BuildSystemPlugin plugin, Player player, BuildWorldStatus status) {
        super(
                plugin,
                plugin.getMessages()
                        .getString(
                                "setup_status_editor_title",
                                player,
                                Map.entry("%status%", ColorAPI.process(status.getStyledName()))));
        this.registry = plugin.getWorldStatusRegistry();
        this.status = (WorldStatusImpl) status;

        register(
                SLOT_RENAME,
                renameButton("setup_status_rename", "setup_status_rename_prompt", this.status::setDisplayName));
        register(SLOT_COLOR, colorButton("setup_status_color", this.status::setColor));
        register(SLOT_ICON, iconButton("setup_status_icon", this.status::getIcon, this.status::setIcon));
        register(SLOT_ORDER, orderButton());
        register(
                SLOT_BUILDING,
                toggleButton(
                        "setup_status_building",
                        this.status::isBuildingAllowed,
                        () -> this.status.setBuildingAllowed(!this.status.isBuildingAllowed())));
        register(
                SLOT_NAVIGATOR,
                toggleButton(
                        "setup_status_navigator",
                        this.status::isVisibleInNavigator,
                        () -> this.status.setVisibleInNavigator(!this.status.isVisibleInNavigator())));
        register(SLOT_PROGRESSES, progressesButton());
        register(SLOT_BACK, backButton());
    }

    private MenuButton orderButton() {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(XMaterial.COMPARATOR)
                        .name(messages.getString(
                                "setup_status_order", player, Map.entry("%order%", String.valueOf(status.getOrder()))))
                        .lore(messages.getStringList("setup_order_lore", player))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    status.setOrder(Math.max(0, status.getOrder() + (event.isRightClick() ? -1 : 1)));
                    save(player);
                })
                .build();
    }

    private MenuButton progressesButton() {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(XMaterial.ARROW)
                        .name(messages.getString(
                                "setup_status_progresses", player, Map.entry("%target%", progressesLabel(player))))
                        .lore(messages.getStringList("setup_status_progresses_lore", player))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    status.setProgressesTo(event.isRightClick() ? null : nextProgressTarget());
                    save(player);
                })
                .build();
    }

    private String progressesLabel(Player player) {
        return status.getProgressesTo()
                .flatMap(registry::getStatus)
                .map(target -> ColorAPI.process(target.getStyledName()))
                .orElse(messages.getString("setup_status_progresses_none", player));
    }

    /**
     * Cycles the auto-progress target through the registered statuses (excluding this one, which never progresses to
     * itself). Only real registry values are cycled; clearing to "none" is a separate right-click on the button.
     */
    private @Nullable String nextProgressTarget() {
        List<String> candidates = registry.getStatuses().stream()
                .map(BuildWorldStatus::getId)
                .filter(id -> !id.equals(status.getId()))
                .toList();
        if (candidates.isEmpty()) {
            return null;
        }
        int index = candidates.indexOf(status.getProgressesTo().orElse(null));
        return candidates.get((index + 1) % candidates.size());
    }

    @Override
    protected void persist() {
        registry.persist(status);
    }

    @Override
    protected void reopen(Player player) {
        new StatusEditorMenu(plugin, player, status).open(player);
    }

    @Override
    protected void openManagement(Player player) {
        new StatusManagementMenu(plugin, player).open(player);
    }
}
