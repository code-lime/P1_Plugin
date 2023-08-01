package org.lime.gp.player.module.needs.thirst;

import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.lime.gp.lime;
import org.lime.system;

import java.util.*;

public class ThirstData {
    private static final int THIRST_VERSION = 1;

    private ThirstData() {}
    public static ThirstData getEmpty() { return new ThirstData(); }

    public double value = 20;
    public double desertTime = 0;
    public boolean desert = false;

    public static final int MAX_DESERT_TIME = 10 * 60 * 60;

    public HashMap<String, Double> times = new HashMap<>();

    public StateColor getStateColor() {
        String state = times.entrySet()
                .stream()
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        StateData data = state == null ? null : StateData.getStateBy(state);
        if (data == null) data = StateData.getDefault(desert, desertTime / MAX_DESERT_TIME);
        return data.color;
    }

    public static ThirstData parse(JsonObject json) {
        try {
            ThirstData dat = ThirstData.getEmpty();
            if (json == null) return dat;
            if (json.get("v").getAsInt() != THIRST_VERSION) return getEmpty();
            dat.value = json.get("value").getAsDouble();
            JsonObject desert = json.get("desert").getAsJsonObject();
            dat.desertTime = desert.get("time").getAsDouble();
            dat.desert = desert.get("desert").getAsBoolean();
            json.get("times").getAsJsonObject().entrySet().forEach(kv -> dat.times.put(kv.getKey(), kv.getValue().getAsDouble()));
            return dat;
        } catch (Exception e) {
            lime.logStackTrace(e);
            return getEmpty();
        }
    }

    public JsonObject toJson() {
        return system.json
                .object()
                .add("v", THIRST_VERSION)
                .add("value", value)
                .addObject("desert", v -> v
                        .add("time", desertTime)
                        .add("desert", desert)
                )
                .add("times", times).build();
    }

    public void updateDesert(Player player, double delta_time) {
        desert = Thirst.isDesert(player.getLocation());
        desertTime += delta_time * (desert ? 1 : -1);
        if (desertTime > MAX_DESERT_TIME) desertTime = MAX_DESERT_TIME;
        else if (desertTime < 0) desertTime = 0;
    }

    public void update(Player player, boolean moving, double delta_time) {
        Set<String> nextList = new HashSet<>();
        times.entrySet().removeIf(v -> {
            String key = v.getKey();
            if (key.equals("default")) return true;
            StateData data = StateData.getStateBy(key);
            if (data == null) return true;
            double time = v.getValue() + delta_time;
            v.setValue(time);
            String next = data.update(player, moving, this, time);
            if (next == null) return false;
            if (!next.equals("default")) nextList.add(next);
            return true;
        });
        nextList.forEach(k -> times.put(k, 0.0));
        if (times.size() == 0)
            StateData.getDefault(desert, desertTime / MAX_DESERT_TIME).update(player, moving, this, 0);
    }
}
