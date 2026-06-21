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
package de.eintosti.buildsystem.player.settings;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.util.color.ColorAPI;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import fr.mrmicky.fastboard.FastBoard;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SettingsService {

    private final BuildSystemPlugin plugin;
    private final ConfigService configService;
    private final Messages messages;
    private final PlayerServiceImpl playerService;
    private final WorldServiceImpl worldService;

    private final Map<UUID, FastBoard> boards;
    private final Map<UUID, BukkitTask> scoreboardTasks;

    public SettingsService(
            BuildSystemPlugin plugin,
            ConfigService configService,
            Messages messages,
            PlayerServiceImpl playerService,
            WorldServiceImpl worldService) {
        this.plugin = plugin;
        this.configService = configService;
        this.messages = messages;
        this.playerService = playerService;
        this.worldService = worldService;

        this.boards = new HashMap<>();
        this.scoreboardTasks = new HashMap<>();
    }

    public Settings getSettings(Player player) {
        return playerService.getPlayerStorage().getBuildPlayer(player).getSettings();
    }

    /**
     * Displays the player's scoreboard if enabled in the config and in their {@link Settings}.
     *
     * @param player The player object
     */
    public void displayScoreboard(Player player) {
        if (!configService.current().settings().scoreboard()) {
            return;
        }

        Settings settings = getSettings(player);
        FastBoard board = new FastBoard(player);
        this.boards.put(player.getUniqueId(), board);

        if (!settings.isScoreboard()) {
            hideScoreboard(player);
            return;
        }

        board.updateTitle(messages.getString("title", player));
        BukkitTask scoreboardTask = Bukkit.getScheduler()
                .runTaskTimerAsynchronously(plugin, () -> updateScoreboard(player, board), 0L, 20L);
        this.scoreboardTasks.put(player.getUniqueId(), scoreboardTask);
    }

    /**
     * Displays scoreboards for all online players who have it enabled and if enabled in the config.
     */
    public void displayScoreboard() {
        if (!configService.current().settings().scoreboard()) {
            return;
        }

        Bukkit.getOnlinePlayers().forEach(this::displayScoreboard);
    }

    public void updateScoreboard(Player player) {
        FastBoard board = this.boards.get(player.getUniqueId());
        if (board != null) {
            updateScoreboard(player, board);
        }
    }

    private void updateScoreboard(Player player, FastBoard board) {
        List<String> body = messages.getStringList("body", player, (line) -> getPlaceholders(line, player));
        board.updateLines(body);
    }

    @Contract("_, _ -> new")
    @SuppressWarnings("unchecked")
    private Map.Entry<String, Object>[] getPlaceholders(String originalString, Player player) {
        if (!originalString.contains("%")) {
            return new Map.Entry[0]; // Don't replace anything
        }

        String worldName = player.getWorld().getName();
        BuildWorld buildWorld = worldService.getWorldStorage().getBuildWorld(worldName);

        final String defaultVal = "§f-";
        String status = defaultVal;
        String permission = defaultVal;
        String project = defaultVal;
        String creator = defaultVal;
        String creation = defaultVal;
        String lastEdited = defaultVal;
        String lastLoaded = defaultVal;
        String lastUnloaded = defaultVal;

        if (buildWorld != null) {
            WorldData worldData = buildWorld.getData();
            Builders builders = buildWorld.getBuilders();

            status = ColorAPI.process(worldData.getStatus().getStyledName());
            permission = worldData.getPermission();
            project = worldData.getProject();
            creator = builders.hasCreator() ? builders.getCreator().getName() : "-";
            creation = messages.formatDate(buildWorld.getCreation());
            lastEdited = messages.formatDate(worldData.getLastEdited());
            lastLoaded = messages.formatDate(worldData.getLastLoaded());
            lastUnloaded = messages.formatDate(worldData.getLastUnloaded());
        }

        return new Map.Entry[] {
            Map.entry("%world%", worldName),
            Map.entry("%status%", status),
            Map.entry("%permission%", permission),
            Map.entry("%project%", project),
            Map.entry("%creator%", creator),
            Map.entry("%creation%", creation),
            Map.entry("%lastedited%", lastEdited),
            Map.entry("%lastloaded%", lastLoaded),
            Map.entry("%lastunloaded%", lastUnloaded)
        };
    }

    public void hideScoreboard(Player player) {
        BukkitTask scoreboardTask = this.scoreboardTasks.remove(player.getUniqueId());
        if (scoreboardTask != null) {
            scoreboardTask.cancel();
        }

        FastBoard board = this.boards.remove(player.getUniqueId());
        if (board != null) {
            board.delete();
        }
    }

    public void hideScoreboards() {
        Bukkit.getOnlinePlayers().forEach(this::hideScoreboard);
    }

    public void forceUpdateSidebar(BuildWorld buildWorld) {
        if (!configService.current().settings().scoreboard()) {
            return;
        }
        World bukkitWorld = Bukkit.getWorld(buildWorld.getName());
        if (bukkitWorld == null) {
            return;
        }
        bukkitWorld.getPlayers().forEach(this::forceUpdateSidebar);
    }

    public void forceUpdateSidebar(Player player) {
        if (!configService.current().settings().scoreboard()
                || !getSettings(player).isScoreboard()) {
            return;
        }
        updateScoreboard(player);
    }
}
