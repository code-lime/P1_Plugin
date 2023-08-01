package org.lime.gp.sound;

import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.system;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

public abstract class ISound {
    public static ISound parse(HashMap<String, ISound> sounds, JsonElement value) {
        if (value.isJsonPrimitive()) {
            String[] args = value.getAsString().split(" ");
            return switch (args.length) {
                case 1 -> sounds.get(args[0]);
                case 3 -> new SingleSound(args[0], Double.parseDouble(args[1]), Sound.Source.NAMES.value(args[2]));
                case 4 -> new SingleSound(args[0], Double.parseDouble(args[1]), Sound.Source.NAMES.value(args[2]), parseDouble(args[3]));
                case 5 -> new SingleSound(args[0], Double.parseDouble(args[1]), Sound.Source.NAMES.value(args[2]), parseDouble(args[3]), parseDouble(args[4]));
                default -> throw new IllegalArgumentException("Exception in single format '" + value + "'");
            };
        } else if (value.isJsonObject()) {
            JsonObject json = value.getAsJsonObject();
            return switch (json.get("type").getAsString()) {
                case "single" -> new SingleSound(json);
                case "tags" -> new TagSound(parse(sounds, json.get("none")), json.getAsJsonObject("values")
                        .entrySet()
                        .stream()
                        .map(kv -> system.toast(kv.getKey(), parse(sounds, kv.getValue())))
                        .toList()
                );
                default -> throw new IllegalArgumentException("Type '" + json.get("type").getAsString() + "' not founded!");
            };
        } else if (value.isJsonArray()) return new RandomlySound(Streams.stream(value.getAsJsonArray().iterator()).map(v -> parse(sounds, v)).toList());
        throw new IllegalArgumentException("Exception in format '" + value + "'");
    }
    private static system.Func0<Double> parseDouble(String value) {
        String[] args = value.split("\\.\\.");
        double from = Double.parseDouble(args[0]);
        double to = args.length == 1 ? from : Double.parseDouble(args[1]);
        return () -> system.rand(from, to);
    }

    public abstract void playSound(Player player, Collection<String> tags);
    public abstract void playSound(Player player, Vector position, Collection<String> tags);
    public abstract void playSound(Player player, Entity target, Collection<String> tags);
    public abstract void playSound(Player player, int targetId, Collection<String> tags);
    public abstract void playSound(Location location, Collection<String> tags);


    public final void playSound(Player player) { playSound(player, Collections.emptyList()); }
    public final void playSound(Player player, Vector position) { playSound(player, position, Collections.emptyList()); }
    public final void playSound(Player player, Entity target) { playSound(player, target, Collections.emptyList()); }
    public final void playSound(Player player, int targetId) { playSound(player, targetId, Collections.emptyList()); }
    public final void playSound(Location location) { playSound(location, Collections.emptyList()); }
}
