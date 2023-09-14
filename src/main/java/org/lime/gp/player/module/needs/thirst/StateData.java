package org.lime.gp.player.module.needs.thirst;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.Checker;
import org.lime.gp.player.module.needs.INeedEffect;
import org.lime.gp.player.module.needs.NeedSystem;
import org.lime.gp.town.ChurchManager;
import org.lime.system.range.IRange;
import org.lime.system.toast.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

public class StateData {
    private static final HashMap<String, StateData> dats = new HashMap<>();
    public static final List<Toast2<Checker, StateData>> includes = new ArrayList<>();
    public static final Map<IRange, List<INeedEffect<?>>> needs = new HashMap<>();

    public static @Nullable StateData getStateBy(String key) { return dats.get(key); }
    public static Collection<String> getStateKeys() { return dats.keySet(); }

    private static double getDelta(double des, double def, double perc) {
        return ((des - def) * (1 - perc)) + def;
    }

    public static StateData getDefault(boolean desert, double desertPerc) {
        StateData _default = dats.get("default");
        if (!desert) return _default;
        StateData _desert = dats.get("desert");
        double delta = getDelta(_desert.delta, _default.delta, desertPerc);
        return new StateData("default", delta, 0, _desert.color, "default", _desert.effects, _desert.commands);
    }
    public static Stream<INeedEffect<?>> getThirstNeeds(double value) {
        return needs.entrySet()
                .stream()
                .filter(v -> v.getKey().inRange(value, 20))
                .flatMap(v -> v.getValue().stream());
    }

    public final String key;

    public final double delta;
    public final double wait;
    public final StateColor color;
    public final String next;
    public final List<PotionEffect> effects = new ArrayList<>();
    public final List<String> commands = new ArrayList<>();

    private StateData(String key, double delta, double wait, StateColor color, String next, List<PotionEffect> effects, List<String> commands) {
        this.key = key;
        this.delta = delta;
        this.wait = wait;
        this.color = color;
        this.next = next;
        this.effects.addAll(effects);
        this.commands.addAll(commands);
    }

    private StateData(String key, JsonObject json, HashMap<String, StateColor> colors, String next) {
        this.key = key;
        delta = json.get("delta").getAsDouble();
        wait = json.get("wait").getAsDouble();
        color = colors.get(json.get("color").getAsString());
        this.next = next != null ? next : json.has("next") && json.get("next").isJsonPrimitive() ? json.get("next").getAsString() : "default";
        if (json.has("effects"))
            json.get("effects").getAsJsonArray().forEach(obj -> effects.add(Items.parseEffect(obj.getAsJsonObject())));
        if (json.has("commands"))
            json.get("commands").getAsJsonArray().forEach(obj -> commands.add(obj.getAsString()));
    }

    private StateData drink(double single) {
        return new StateData(key + ".drink", single, 0, color, key, effects, commands);
    }

    private double getDelta(Player player, boolean moving) {
        double delta = this.delta;
        if (delta >= 0) return delta;
        if (player.isSprinting()) delta *= 5;
        else if (moving) delta *= 2;
        return delta;
    }

    public String update(Player player, boolean moving, ThirstData data, double time) {
        double delta = getDelta(player, moving);
        if (delta < 0 && ChurchManager.hasAnyEffect(player, ChurchManager.EffectType.SATURATION)) delta *= 0.5;
        if (delta < 0) delta *= NeedSystem.getThirstMutate(player);
        else delta /= NeedSystem.getThirstMutate(player);
        data.value += delta;
        if (data.value < 0) data.value = 0;
        else if (data.value > 20) data.value = 20;
        effects.forEach(player::addPotionEffect);
        commands.forEach(cmd -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{name}", player.getName()).replace("{uuid}", player.getUniqueId().toString())));
        if (key.equals("default")) return "default";
        if (time >= wait) return next;
        return null;
    }

    private static List<StateData> parse(String key, JsonObject value, HashMap<String, StateColor> colors, String next) {
        List<StateData> list = new ArrayList<>();
        StateData data = new StateData(key, value, colors, next);
        list.add(data);

        if (value.has("single")) {
            JsonElement single = value.get("single");
            if (single.isJsonObject()) list.addAll(parse(key + ".drink", single.getAsJsonObject(), colors, key));
            else list.add(data.drink(single.getAsDouble()));
        }

        return list;
    }

    public static void parseAll(JsonObject json) {
        HashMap<String, StateColor> colors = new HashMap<>();
        json.get("colors").getAsJsonObject().entrySet().forEach(kv -> colors.put(kv.getKey(), StateColor.parse(kv.getValue().getAsJsonObject())));
        HashMap<String, StateData> dats = new HashMap<>();
        JsonObject effects = json.get("effects").getAsJsonObject();
        effects.entrySet().forEach(kv -> parse(kv.getKey(), kv.getValue().getAsJsonObject(), colors, null).forEach(dat -> dats.put(dat.key, dat)));
        if (!dats.containsKey("default")) throw new IllegalArgumentException("StateData.default not founded!");

        List<Toast2<Checker, StateData>> includes = new ArrayList<>();
        json.get("invoke").getAsJsonObject().entrySet().forEach(kv -> {
            String _data = kv.getValue().getAsString();
            StateData data = dats.getOrDefault(_data, null);
            if (data == null) throw new IllegalArgumentException("StateData." + _data + " not founded!");
            includes.add(Toast.of(Checker.createCheck(kv.getKey()), data));
        });

        Map<IRange, List<INeedEffect<?>>> needs = new HashMap<>();
        if (json.has("needs")) json.get("needs").getAsJsonObject().entrySet().forEach(kv -> {
            List<INeedEffect<?>> values = new ArrayList<>();
            kv.getValue().getAsJsonArray().forEach(item -> values.add(INeedEffect.parse(item.getAsJsonObject())));
            needs.put(IRange.parse(kv.getKey()), values);
        });

        StateData.dats.clear();
        StateData.dats.putAll(dats);

        StateData.includes.clear();
        StateData.includes.addAll(includes);

        StateData.needs.clear();
        StateData.needs.putAll(needs);
    }
}








