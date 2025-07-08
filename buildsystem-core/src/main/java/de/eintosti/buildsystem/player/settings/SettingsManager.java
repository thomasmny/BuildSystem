/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
import org.jspecify.annotations.Nullable;

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
     * Only set a player's scoreboard if {@link SettingsImpl#isScoreboard} is equal to {@code true}.
     *
     * @param player   The player object
     * @param settings The player's settings
     */
    public void startScoreboard(Player player, Settings settings) {
        if (!settings.isScoreboard()) {
            stopScoreboard(player, settings);
            return;
        }

        startScoreboard(player);
    }

    /**
     * Only set a player's scoreboard if {@link SettingsImpl#isScoreboard} is equal to {@code true}.
     *
     * @param player The player object
     */
    public void startScoreboard(Player player) {
        if (!Config.Settings.scoreboard) {
            return;
        }

        Settings settings = getSettings(player);
        FastBoard board = new FastBoard(player);
        this.boards.put(player.getUniqueId(), board);

        if (!settings.isScoreboard()) {
            stopScoreboard(player, settings);
            return;
        }

        board.updateTitle(Messages.getString("title", player));
        BukkitTask scoreboardTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> updateScoreboard(player, board), 0L, 20L);
        settings.setScoreboardTask(scoreboardTask);
    }

    /**
     * Set each player's scoreboard if they have {@link SettingsImpl#isScoreboard} enabled.
     */
    public void startScoreboard() {
        if (!Config.Settings.scoreboard) {
            return;
        }

        Bukkit.getOnlinePlayers().forEach(this::startScoreboard);
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

        return new Map.Entry[]{
                Map.entry("%world%", worldName),
                Map.entry("%status%", parseWorldInformation(player, buildWorld, "%status%")),
                Map.entry("%permission%", parseWorldInformation(player, buildWorld, "%permission%")),
                Map.entry("%project%", parseWorldInformation(player, buildWorld, "%project%")),
                Map.entry("%creator%", parseWorldInformation(player, buildWorld, "%creator%")),
                Map.entry("%creation%", parseWorldInformation(player, buildWorld, "%creation%")),
                Map.entry("%lastedited%", parseWorldInformation(player, buildWorld, "%lastedited%")),
                Map.entry("%lastloaded%", parseWorldInformation(player, buildWorld, "%lastloaded%")),
                Map.entry("%lastunloaded%", parseWorldInformation(player, buildWorld, "%lastunloaded%"))
        };
    }

    // Is there an easier way of doing this?
    private String parseWorldInformation(Player player, @Nullable BuildWorld buildWorld, String input) {
        if (buildWorld == null) {
            return "§f-";
        }

        Builders builders = buildWorld.getBuilders();
        WorldData worldData = buildWorld.getData();
        return switch (input) {
            case "%status%" -> Messages.getString(Messages.getMessageKey(worldData.status().get()), player);
            case "%permission%" -> worldData.permission().get();
            case "%project%" -> worldData.project().get();
            case "%creator%" -> builders.hasCreator() ? builders.getCreator().getName() : "-";
            case "%creation%" -> Messages.formatDate(buildWorld.getCreation());
            case "%lastedited%" -> Messages.formatDate(worldData.lastEdited().get());
            case "%lastloaded%" -> Messages.formatDate(worldData.lastLoaded().get());
            case "%lastunloaded%" -> Messages.formatDate(worldData.lastUnloaded().get());
            default -> "§f-";
        };
    }

    private void stopScoreboard(Player player, Settings settings) {
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

    public void stopScoreboard(Player player) {
        stopScoreboard(player, getSettings(player));
    }

    public void stopScoreboard() {
        Bukkit.getOnlinePlayers().forEach(this::stopScoreboard);
    }
}