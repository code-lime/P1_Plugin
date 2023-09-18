package org.lime.gp.extension;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.scoreboard.CraftScoreboard;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.*;

public class ScoreboardManager {
    public static Scoreboard scoreboard() {
        return _scoreboard == null ? (_scoreboard = Bukkit.getScoreboardManager().getMainScoreboard()) : _scoreboard;
    }

    private static Scoreboard _scoreboard = null;

    @SuppressWarnings("deprecation")
    public static Objective objective(String key, boolean create) {
        Scoreboard scoreboard = scoreboard();
        Objective objective;
        if ((objective = scoreboard.getObjective(key)) == null && create)
            objective = Bukkit.getScoreboardManager().getMainScoreboard().registerNewObjective(key, "dummy", key);
        return objective;
    }

    public static void set(String score, String key, int value) {
        objective(score, true).getScore(key).setScore(value);
    }
    public static void reset(String score, String key) {
        net.minecraft.world.scores.Scoreboard scoreboard = ((CraftScoreboard) scoreboard()).getHandle();
        scoreboard.getObjectives().forEach(obj -> {
            if (obj.getName().equals(score))
                scoreboard.resetPlayerScore(key, obj);
        });
    }

    public static HashMap<String, Integer> get(String score) {
        HashMap<String, Integer> hashMap = new HashMap<>();
        net.minecraft.world.scores.Scoreboard scoreboard = ((CraftScoreboard) scoreboard()).getHandle();
        scoreboard.getObjectives().forEach(obj -> {
            if (obj.getName().equals(score))
                scoreboard.getPlayerScores(obj).forEach(v -> hashMap.put(v.getOwner(), v.getScore()));
        });
        return hashMap;
    }
    public static Integer get(String score, String key, Integer _default) {
        Objective obj = objective(score, false);
        if (obj == null) return _default;
        Score _score = obj.getScore(key);
        if (!_score.isScoreSet()) return _default;
        return _score.getScore();
    }
    public static Integer get(String score, String key, Func0<Integer> _default) {
        Objective obj = objective(score, true);
        Score _score = obj.getScore(key);
        if (!_score.isScoreSet()) {
            int __default = _default.invoke();
            _score.setScore(__default);
        }
        return _score.getScore();
    }
    public static void move(String score, String fromKey, String toKey) {
        Objective obj = objective(score, false);
        if (obj == null) return;
        Score _score = obj.getScore(fromKey);
        if (!_score.isScoreSet()) return;
        int stamina = _score.getScore();
        obj.getScore(toKey).setScore(stamina);
        ScoreboardManager.reset(score, fromKey);
    }
    public static int add(String score, String key, int value, int _default) {
        int newValue = get(score, key, _default) + value;
        set(score, key, newValue);
        return newValue;
    }
    public static Integer edit(String score, String key, Func1<Integer, Integer> func, int _default) {
        return edit(score, key, func, () -> _default);
    }
    public static Integer edit(String score, String key, Func1<Integer, Integer> func, Func0<Integer> _default) {
        Integer value = func.invoke(get(score, key, _default));
        if (value == null) reset(score, key);
        else set(score, key, value);
        return value;
    }
}















