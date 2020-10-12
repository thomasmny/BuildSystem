package de.eintosti.buildsystem.version;

import net.minecraft.server.v1_16_R2.*;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;

/**
 * @author einTosti
 */
public class Sidebar_1_16_R2 extends Placeholders implements Sidebar {
    private final String title;
    private final List<String> body;

    private final Scoreboard scoreboard;
    private ScoreboardObjective objective;

    public Sidebar_1_16_R2(String title, List<String> body) {
        this.title = title;
        this.body = body;
        this.scoreboard = new Scoreboard();
    }

    @Override
    public void set(Player player, String... information) {
        if (scoreboard.getObjective("BuildSystemSB") != null) {
            objective = scoreboard.getObjective("BuildSystemSB");
        } else {
            objective = scoreboard.registerObjective("BuildSystemSB", IScoreboardCriteria.DUMMY,
                    IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + title + "\"}"), IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER);
        }
        update(player, false, information);
    }

    @Override
    public void update(Player player, boolean forceUpdate, String... information) {
        if (!forceUpdate) {
            try {
                HashSet<String> scores = new HashSet<>();
                for (ScoreboardScore score : scoreboard.getScoresForObjective(objective)) {
                    scores.add(score.getPlayerName());
                }
                if (scores.containsAll(replacedList(body, player, information))) return;
                forceUpdate(player, information);
            } catch (ConcurrentModificationException ignored) {
            }
            return;
        }
        forceUpdate(player, information);
    }

    private void forceUpdate(Player player, String... information) {
        if (objective == null) set(player, information);
        objective.setDisplayName(IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + title + "\"}"));

        PacketPlayOutScoreboardObjective removePacket = new PacketPlayOutScoreboardObjective(objective, 1);
        PacketPlayOutScoreboardObjective createPacket = new PacketPlayOutScoreboardObjective(objective, 0);
        PacketPlayOutScoreboardDisplayObjective display = new PacketPlayOutScoreboardDisplayObjective(1, objective);

        PlayerConnection playerConnection = ((CraftPlayer) player).getHandle().playerConnection;
        playerConnection.sendPacket(removePacket);
        playerConnection.sendPacket(createPacket);
        playerConnection.sendPacket(display);

        int scoreboardLines = body.size();
        for (int i = 0; i < scoreboardLines; i++) {
            String text = ChatColor.translateAlternateColorCodes('&', injectPlaceholders(body.get(i), player, information));
            ScoreboardScore scoreboardScore = scoreboard.getPlayerScoreForObjective(text, objective);
            scoreboardScore.setScore(scoreboardLines - i - 1);
            PacketPlayOutScoreboardScore packetPlayOutScoreboardScore = new PacketPlayOutScoreboardScore(ScoreboardServer.Action.CHANGE,
                    objective.getName(), text, scoreboardScore.getScore());
            playerConnection.sendPacket(packetPlayOutScoreboardScore);
        }
    }

    @Override
    public void remove(Player player) {
        if (objective == null) return;
        PacketPlayOutScoreboardObjective removePacket = new PacketPlayOutScoreboardObjective(objective, 1);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(removePacket);
        scoreboard.unregisterObjective(objective);
    }

    private List<String> replacedList(List<String> oldList, Player player, String... information) {
        List<String> list = new ArrayList<>();
        oldList.forEach(body -> list.add(ChatColor.translateAlternateColorCodes('&', injectPlaceholders(body, player, information))));
        return list;
    }
}

