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
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.config.Config;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import fr.mrmicky.fastboard.FastBoard;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SettingsManager {

    private final BuildSystemPlugin plugin;
    private final WorldServiceImpl worldService;

    private final Map<UUID, FastBoard> boards;

    public SettingsManager(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.worldService = plugin.getWorldService();

        this.boards = new HashMap<>();
    }

    public Settings getSettings(Player player) {
        return plugin.getPlayerService().getPlayerStorage().getBuildPlayer(player).getSettings();
    }

    /**
     * Displays the player's scoreboard if enabled in the config and in their {@link Settings}.
     *
     * @param player The player object
     */
    public void displayScoreboard(Player player) {
        if (!Config.Settings.scoreboard) {
            return;
        }

        Settings settings = getSettings(player);
        FastBoard board = new FastBoard(player);
        this.boards.put(player.getUniqueId(), board);

        if (!settings.isScoreboard()) {
            hideScoreboard(player);
            return;
        }

        board.updateTitle(Messages.getString("title", player));
        BukkitTask scoreboardTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> updateScoreboard(player, board), 0L, 20L);
        settings.setScoreboardTask(scoreboardTask);
    }

    /**
     * Displays scoreboards for all online players who have it enabled and if enabled in the config.
     */
    public void displayScoreboard() {
        if (!Config.Settings.scoreboard) {
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
        List<String> body = Messages.getStringList("body", player, (line) -> getPlaceholders(line, player));
        board.updateLines(body);
    }

    @Contract("_, _ -> new")
    @SuppressWarnings("unchecked")
    private Map.Entry<String, Object>[] getPlaceholders(String originalString, Player player) {
        if (!originalString.matches(".*%*%.*")) {
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

            status = Messages.getString(Messages.getMessageKey(worldData.status().get()), player);
            permission = worldData.permission().get();
            project = worldData.project().get();
            creator = builders.hasCreator() ? builders.getCreator().getName() : "-";
            creation = Messages.formatDate(buildWorld.getCreation());
            lastEdited = Messages.formatDate(worldData.lastEdited().get());
            lastLoaded = Messages.formatDate(worldData.lastLoaded().get());
            lastUnloaded = Messages.formatDate(worldData.lastUnloaded().get());
        }

        return new Map.Entry[]{
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
        Settings settings = getSettings(player);
        BukkitTask scoreboardTask = settings.getScoreboardTask();
        if (scoreboardTask != null) {
            scoreboardTask.cancel();
            settings.setScoreboardTask(null);
        }

        FastBoard board = this.boards.remove(player.getUniqueId());
        if (board != null) {
            board.delete();
        }
    }

    public void hideScoreboards() {
        Bukkit.getOnlinePlayers().forEach(this::hideScoreboard);
    }
}