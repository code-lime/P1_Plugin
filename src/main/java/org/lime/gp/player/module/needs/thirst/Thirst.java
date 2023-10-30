package org.lime.gp.player.module.needs.thirst;

import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.lime.plugin.CoreElement;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.extension.JManager;
import org.lime.gp.lime;
import org.lime.gp.player.module.needs.INeedEffect;
import org.lime.gp.player.ui.CustomUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Stream;

public class Thirst implements Listener {
    public static CoreElement create() {
        return CoreElement.create(Thirst.class)
                .withInstance()
                .withInit(Thirst::init)
                .<JsonObject>addConfig("thirst", v -> v.withInvoke(Thirst::config).withDefault(new JsonObject()));
    }

    public static void init() {
        AnyEvent.addEvent("thirst.value", AnyEvent.type.other, builder -> builder.createParam(Double::parseDouble, "[value:0-20]"), Thirst::thirstValue);
        AnyEvent.addEvent("thirst.state.add", AnyEvent.type.other, builder -> builder.createParam(StateData::getStateBy, StateData::getStateKeys), Thirst::thirstState);

        HashMap<UUID, Vector> actualData = new HashMap<>();
        lime.repeat(() -> {
            HashMap<UUID, Vector> lastData = new HashMap<>();
            Bukkit.getOnlinePlayers().forEach(player -> {
                switch (player.getGameMode()) {
                    case CREATIVE, SPECTATOR -> { return; }
                }
                UUID uuid = player.getUniqueId();

                Vector last = actualData.getOrDefault(uuid, new Vector());
                Vector now = player.getLocation().toVector();
                lastData.put(uuid, now);
                boolean moved = last.distance(now) > 0.1;

                ThirstData data = getThirst(player);
                data.update(player, moved, 0.15);
                setThirst(player, data);
            });
            actualData.clear();
            actualData.putAll(lastData);
        }, 0.15);
        lime.repeat(() -> Bukkit.getOnlinePlayers().forEach(player -> {
            switch (player.getGameMode()) {
                case CREATIVE, SPECTATOR -> { return; }
            }
            ThirstData data = getThirst(player);
            data.updateDesert(player, 5);
            setThirst(player, data);
        }), 5);

        CustomUI.addListener(new ThirstUI());
    }
    public static void config(JsonObject json) {
        StateData.parseAll(json);
    }
    public static void thirstValue(Player player, double value) {
        ThirstData data = getThirst(player);
        data.value = value;
        if (data.value < 0) data.value = 0;
        else if (data.value > 20) data.value = 20;
        setThirst(player, data);
    }
    public static void thirstValueCheck(Player player, double value, boolean forward) {
        ThirstData data = getThirst(player);
        if (forward == data.value > value) return;
        data.value = value;
        if (data.value < 0) data.value = 0;
        else if (data.value > 20) data.value = 20;
        setThirst(player, data);
    }
    public static void thirstState(Player player, StateData state) {
        ThirstData data = getThirst(player);
        data.times.put(state.key, 0.0);
        setThirst(player, data);
    }
    public static void thirstStateByKey(Player player, String stateKey) {
        ThirstData data = getThirst(player);
        data.times.put(stateKey, 0.0);
        setThirst(player, data);
    }
    public static void thirstStateReset(Player player) {
        ThirstData data = getThirst(player);
        data.times.clear();
        setThirst(player, data);
    }
    public static void thirstReset(Player player) {
        ThirstData data = getThirst(player);
        data.times.clear();
        data.value = 20;
        setThirst(player, data);
    }

    public static boolean isDesert(Location location) {
        return switch (location.getWorld().getBiome(location)) {
            case BADLANDS, ERODED_BADLANDS, WOODED_BADLANDS, DESERT, SAVANNA, SAVANNA_PLATEAU, WINDSWEPT_SAVANNA -> true;
            default -> false;
        };
    }

    public static Stream<INeedEffect<?>> getThirstNeeds(Player player) {
        return StateData.getThirstNeeds(getThirst(player).value);
    }

    public static ThirstData getThirst(Player player) {
        return ThirstData.parse(JManager.get(JsonObject.class, player.getPersistentDataContainer(), "thirst", null));
    }
    private static void setThirst(Player player, ThirstData value) {
        JManager.set(player.getPersistentDataContainer(), "thirst", value.toJson());
    }

    @EventHandler public static void onUse(PlayerItemConsumeEvent e) {
        ItemStack item = e.getItem();
        StateData state = StateData.includes.stream().filter(kv -> kv.val0.check(item)).map(kv -> kv.val1).findFirst().orElse(null);
        if (state == null) return;
        Player player = e.getPlayer();
        ThirstData data = getThirst(player);
        data.times.put(state.key, 0.0);
        setThirst(player, data);
    }
}















