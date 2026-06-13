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
package de.eintosti.buildsystem;

import de.eintosti.buildsystem.api.player.settings.NavigatorType;
import java.util.Map;
import java.util.stream.Collectors;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.jspecify.annotations.NullMarked;

/**
 * Registers the plugin's bStats charts.
 */
@NullMarked
final class BuildSystemMetrics {

    private final BuildSystemPlugin plugin;

    BuildSystemMetrics(BuildSystemPlugin plugin) {
        this.plugin = plugin;
    }

    void register() {
        Metrics metrics = new Metrics(plugin, BuildSystemPlugin.METRICS_ID);
        metrics.addCustomChart(new SimplePie(
                "archive_vanish",
                () -> String.valueOf(
                        plugin.getConfigService().current().settings().archive().vanish())));
        metrics.addCustomChart(new SimplePie(
                "block_world_edit",
                () -> String.valueOf(
                        plugin.getConfigService().current().settings().builder().blockWorldEditNonBuilder())));
        metrics.addCustomChart(new SimplePie(
                "join_quit_messages",
                () -> String.valueOf(
                        plugin.getConfigService().current().settings().joinQuitMessages())));
        metrics.addCustomChart(new SimplePie(
                "lock_weather",
                () -> String.valueOf(plugin.getConfigService().current().world().lockWeather())));
        metrics.addCustomChart(new SimplePie(
                "scoreboard",
                () -> String.valueOf(
                        plugin.getConfigService().current().settings().scoreboard())));
        metrics.addCustomChart(new SimplePie(
                "update_checker",
                () -> String.valueOf(
                        plugin.getConfigService().current().settings().updateChecker())));
        metrics.addCustomChart(new SimplePie(
                "unload_worlds",
                () -> String.valueOf(
                        plugin.getConfigService().current().world().unload().enabled())));
        metrics.addCustomChart(new AdvancedPie("navigator_type", () -> {
            Map<NavigatorType, Long> countsByType =
                    plugin.getPlayerService().getPlayerStorage().getBuildPlayers().stream()
                            .collect(Collectors.groupingBy(
                                    buildPlayer -> buildPlayer.getSettings().getNavigatorType(),
                                    Collectors.counting()));
            int oldCount = countsByType.getOrDefault(NavigatorType.OLD, 0L).intValue();
            int newCount = countsByType.getOrDefault(NavigatorType.NEW, 0L).intValue();
            return Map.of("Old", oldCount, "New", newCount);
        }));
        metrics.addCustomChart(new SimplePie(
                "folder_override_permissions",
                () -> String.valueOf(
                        plugin.getConfigService().current().folder().overridePermissions())));
        metrics.addCustomChart(new SimplePie(
                "folder_override_projects",
                () -> String.valueOf(
                        plugin.getConfigService().current().folder().overrideProjects())));
    }
}
