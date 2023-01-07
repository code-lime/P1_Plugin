package org.lime.gp.sound;

import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.lime.core;
import org.lime.gp.lime;
import org.lime.system;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Sounds {
    public static core.element create() {
        return core.element.create(Sounds.class)
                .<JsonObject>addConfig("sounds.json", v -> v.withInvoke(Sounds::config).withDefault(new JsonObject()));
    }

    private static system.Func0<Double> parseDouble(String value) {
        String[] args = value.split("\\.\\.");
        double from = Double.parseDouble(args[0]);
        double to = args.length == 1 ? from : Double.parseDouble(args[1]);
        return () -> system.rand(from, to);
    }
    public static abstract class ISound {
        public static ISound parse(HashMap<String, ISound> sounds, JsonElement value) {
            if (value.isJsonPrimitive()) {
                String[] args = value.getAsString().split(" ");
                if (args.length == 1) return sounds.get(args[0]);
                if (args.length == 3) return new SingleSound(args[0], Double.parseDouble(args[1]), Sound.Source.NAMES.value(args[2]));
                if (args.length == 4) return new SingleSound(args[0], Double.parseDouble(args[1]), Sound.Source.NAMES.value(args[2]), parseDouble(args[3]));
                if (args.length == 5) return new SingleSound(args[0], Double.parseDouble(args[1]), Sound.Source.NAMES.value(args[2]), parseDouble(args[3]), parseDouble(args[4]));
                throw new IllegalArgumentException("Exception in single format '"+value+"'");
            } else if (value.isJsonObject()) {
                JsonObject json = value.getAsJsonObject();
                switch (json.get("type").getAsString()) {
                    case "single": return new SingleSound(json);
                    default: throw new IllegalArgumentException("Type '"+json.get("type").getAsString()+"' not founded!");
                }
            } else if (value.isJsonArray())
                return new RandomlySound(Streams.stream(value.getAsJsonArray().iterator()).map(v -> parse(sounds, v)).toList());
            throw new IllegalArgumentException("Exception in format '"+value+"'");
        }

        public abstract void playSound(Player player);
        public abstract void playSound(Location location);
    }
    public static class SingleSound extends ISound {
        public final String key;
        public final double distance;
        public final Sound.Source source;
        public final system.Func0<Double> volume;
        public final system.Func0<Double> pitch;
        public SingleSound(String key, double distance, Sound.Source source) {
            this(key, distance, source, 1);
        }
        public SingleSound(String key, double distance, Sound.Source source, double volume) {
            this(key, distance, source, volume, 1);
        }
        public SingleSound(JsonObject json) {
            this(
                    json.get("key").getAsString(),
                    json.has("distance") ? json.get("distance").getAsDouble() : 5.0,
                    json.has("source") ? Sound.Source.NAMES.value(json.get("source").getAsString()) : Sound.Source.MASTER,
                    json.has("volume") ? json.get("volume").getAsDouble() : 1.0,
                    json.has("pitch") ? json.get("pitch").getAsDouble() : 1.0
            );
        }
        public SingleSound(String key, double distance, Sound.Source source, double volume, double pitch) {
            this(key, distance, source, () -> volume, () -> pitch);
        }
        public SingleSound(String key, double distance, Sound.Source source, system.Func0<Double> volume) {
            this(key, distance, source, volume, () -> 1.0);
        }
        public SingleSound(String key, double distance, Sound.Source source, system.Func0<Double> volume, system.Func0<Double> pitch) {
            this.key = key;
            this.distance = distance;
            this.source = source;
            this.volume = volume;
            this.pitch = pitch;
        }

        @Override public void playSound(Player player) {
            player.playSound(Sound.sound(Key.key(key), source, (float)(double) volume.invoke(), (float)(double) pitch.invoke()));
        }
        @Override public void playSound(Location location) {
            double x = location.getX();
            double y = location.getY();
            double z = location.getZ();
            location.getNearbyPlayers(distance)
                    .forEach(player -> player.playSound(Sound.sound(Key.key(key), source, (float)(double) volume.invoke(), (float)(double) pitch.invoke()), x, y, z));
        }
    }
    public static class RandomlySound extends ISound {
        public final List<ISound> list;
        public RandomlySound(List<ISound> list) {
            this.list = list;
        }

        @Override public void playSound(Player player) {
            system.rand(list).playSound(player);
        }
        @Override public void playSound(Location location) {
            system.rand(list).playSound(location);
        }
    }

    public static ConcurrentHashMap<String, ISound> sounds = new ConcurrentHashMap<>();
    public static void playSound(String key, Player player) {
        if (key == null) return;
        Optional.ofNullable(sounds.get(key))
                .ifPresentOrElse(sound -> sound.playSound(player), () -> lime.logOP("Sound '"+key+"' not founded!"));
    }
    public static void playSound(String key, Location location) {
        if (key == null) return;
        String[] args = key.split(" ");
        Optional.ofNullable(sounds.get(args[0]))
                .ifPresentOrElse(sound -> sound.playSound(args.length == 4
                        ? new Location(location.getWorld(), Integer.parseInt(args[1]) + 0.5, Integer.parseInt(args[2]) + 0.5, Integer.parseInt(args[3]) + 0.5)
                        : location
                ), () -> lime.logOP("Sound '"+key+"' not founded!"));
    }

    public static void config(JsonObject json) {
        HashMap<String, ISound> sounds = new HashMap<>();
        json.entrySet()
                .stream()
                .flatMap(v -> v.getValue().getAsJsonObject().entrySet().stream())
                .flatMap(v -> v.getValue().getAsJsonObject().entrySet().stream())
                .filter(system.distinctBy(Map.Entry::getKey))
                .forEach(kv -> {
                    try {
                        sounds.put(kv.getKey(), ISound.parse(sounds, kv.getValue()));
                    } catch (Exception e) {
                        lime.logOP("Error load sound '"+kv.getKey()+"' with value '"+kv.getValue()+"' with error '"+e.toString()+"'");
                        throw new IllegalArgumentException(e);
                    }
                });
        Sounds.sounds.clear();
        Sounds.sounds.putAll(sounds);
    }
}
