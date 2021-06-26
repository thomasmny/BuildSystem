package de.eintosti.buildsystem.version;

import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardDisplayObjective;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardObjective;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardScore;
import net.minecraft.server.ScoreboardServer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardObjective;
import net.minecraft.world.scores.ScoreboardScore;
import net.minecraft.world.scores.criteria.IScoreboardCriteria;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;

public class Sidebar_1_17_R1 extends Placeholders implements Sidebar {
    private final static String SCOREBOARD_NAME = "BuildSystemSB";

    private final String title;
    private final List<String> body;

    private final Scoreboard scoreboard;
    private ScoreboardObjective objective;

    public Sidebar_1_17_R1(String title, List<String> body) {
        this.title = title;
        this.body = body;
        this.scoreboard = new Scoreboard();
    }

    @Override
    public void set(Player player, String... information) {
        if (scoreboard.getObjective(SCOREBOARD_NAME) != null) {
            objective = scoreboard.getObjective(SCOREBOARD_NAME);
        } else {
            objective = scoreboard.registerObjective(SCOREBOARD_NAME, IScoreboardCriteria.a,
                    IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + title + "\"}"), IScoreboardCriteria.EnumScoreboardHealthDisplay.a);
        }

        update(player, false, information);
    }

    @Override
    public void update(Player player, boolean forceUpdate, String... information) {
        if (!forceUpdate) {
            HashSet<String> scores = new HashSet<>();
            for (ScoreboardScore score : scoreboard.getScoresForObjective(objective)) {
                scores.add(score.getPlayerName());
            }

            if (scores.containsAll(replacedList(body, player, information))) {
                return;
            }
        }

        forceUpdate(player);
    }

    private void forceUpdate(Player player, String... information) {
        if (objective == null) {
            set(player, information);
        }
        objective.setDisplayName(IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + title + "\"}"));

        PacketPlayOutScoreboardObjective removePacket = new PacketPlayOutScoreboardObjective(objective, 1);
        PacketPlayOutScoreboardObjective createPacket = new PacketPlayOutScoreboardObjective(objective, 0);
        PacketPlayOutScoreboardDisplayObjective displayPacket = new PacketPlayOutScoreboardDisplayObjective(1, objective);

        PlayerConnection playerConnection = ((CraftPlayer) player).getHandle().b;
        playerConnection.sendPacket(removePacket);
        playerConnection.sendPacket(createPacket);
        playerConnection.sendPacket(displayPacket);

        int scoreboardLines = body.size();
        for (int i = 0; i < scoreboardLines; i++) {
            String text = ChatColor.translateAlternateColorCodes('&', injectPlaceholders(body.get(i), player, information));
            ScoreboardScore scoreboardScore = scoreboard.getPlayerScoreForObjective(text, objective);
            scoreboardScore.setScore(scoreboardLines - i - 1);

            PacketPlayOutScoreboardScore packetPlayOutScoreboardScore = new PacketPlayOutScoreboardScore(ScoreboardServer.Action.a,
                    objective.getName(), text, scoreboardScore.getScore());
            playerConnection.sendPacket(packetPlayOutScoreboardScore);
        }
    }

    @Override
    public void remove(Player player) {
        if (objective == null) {
            return;
        }

        PacketPlayOutScoreboardObjective removePacket = new PacketPlayOutScoreboardObjective(objective, 1);
        ((CraftPlayer) player).getHandle().b.sendPacket(removePacket);
        scoreboard.unregisterObjective(objective);
    }
}
