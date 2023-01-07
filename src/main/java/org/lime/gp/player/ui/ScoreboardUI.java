package org.lime.gp.player.ui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.lime.core;

import java.util.*;

public class ScoreboardUI implements Listener {
    public static core.element create() {
        return core.element.create(ScoreboardUI.class)
                .withInstance();
    }
    private static final Map<UUID, FastBoard> boards = new HashMap<>();
    @EventHandler public static void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        FastBoard board = boards.remove(player.getUniqueId());
        if (board != null) board.delete();
    }
    public static void SendFakeScoreboard(Collection<? extends Player> players, String name, String... lines) {
        SendFakeScoreboard(players, name, Arrays.asList(lines));
    }
    public static void SendFakeScoreboard(Collection<? extends Player> players, String name, List<String> lines) {
        players.forEach(player -> SendFakeScoreboard(player, name, lines));
    }
    public static void SendFakeScoreboard(Player player, String name, String... lines) {
        SendFakeScoreboard(player, name, Arrays.asList(lines));
    }
    public static void SendFakeScoreboard(Player player, String name, List<String> lines) {
        UUID uuid = player.getUniqueId();
        FastBoard board = boards.getOrDefault(uuid, null);
        if (board == null) board = new FastBoard(player);
        board.updateTitle(name);
        board.updateLines(lines);
    }
}
