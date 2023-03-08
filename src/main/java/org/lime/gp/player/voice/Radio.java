package org.lime.gp.player.voice;

import de.maxhenkel.voicechat.voice.common.GroupSoundPacket;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.Position;
import org.lime.core;
import org.lime.gp.extension.MapUUID;
import org.lime.gp.lime;
import org.lime.gp.module.TimeoutData;
import org.lime.system;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Radio {
    public static core.element create() {
        return core.element.create(Radio.class)
                .withInit(Radio::init);
    }
    public static void init() {
        lime.repeat(() -> radioLogs.entrySet().removeIf(kv -> {
            kv.getKey().invoke((key, file) -> lime.logToFile("radio_" + file, "[{time}] " + key.replace("{count}", String.valueOf(kv.getValue()))));
            return true;
        }), 30);
        addListener(() -> Bukkit.getOnlinePlayers().stream().map(player -> {
            HashMap<Integer, Integer> levels = RadioData.getOutput(player);
            UUID uuid = player.getUniqueId();
            Location pos = player.getLocation();
            return levels.size() == 0 ? (RadioElement)null : new PlayerRadioElement() {
                @Override public Player player() { return player; }
                @Override public Collection<Integer> levels() { return levels.keySet(); }
                @Override public boolean hasLevel(int level) { return levels.containsKey(level); }
                @Override public UUID unique() { return uuid; }
                @Override public boolean isDistance(Location location, double total_distance) { return RadioElement.isDistance(location, pos, total_distance); }
                @Override public short distance() { return 8; }
                @Override public void play(SenderInfo info, byte[] data, int level, double total_distance) {
                    UUID sender = info.uuid();
                    if (uuid.equals(sender)) return;
                    UUID packet_sender = MapUUID.of("radio.self", uuid, sender);
                    double noise = 0;
                    if (info.noise()) noise = info.local().distance(pos.toVector()) / total_distance;
                    if (noise > 1) noise = 1;
                    else if (noise < 0) noise = 0;
                    Voice.sendPacket(uuid, new GroupSoundPacket(packet_sender, Voice.modifyVolume(info, packet_sender, data, levels.getOrDefault(level, 100), noise), Voice.nextSequence(packet_sender), ""));
                }
            };
        }).filter(Objects::nonNull));
        lime.repeat(() -> {
            Map<UUID, RadioElement> bufferElements = listeners.stream()
                    .flatMap(RadioListener::elements)
                    .collect(Collectors.toMap(RadioElement::unique, v -> v));
            Radio.bufferElements.putAll(bufferElements);
            Radio.bufferElements.keySet().removeIf(uuid -> !bufferElements.containsKey(uuid));
        }, 2.5);
    }
    private static final ConcurrentHashMap<system.Toast2<String, String>, Integer> radioLogs = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, RadioElement> bufferElements = new ConcurrentHashMap<>();

    public static List<RadioListener> listeners = new ArrayList<>();
    public static void addListener(RadioListener listener) {
        listeners.add(listener);
    }

    public static class RadioLockTimeout extends TimeoutData.ITimeout { }
    public interface RadioListener {
        Stream<? extends RadioElement> elements();
    }
    public interface PlayerRadioElement extends RadioElement {
        Player player();
        Collection<Integer> levels();
    }
    public interface RadioElement {
        boolean hasLevel(int level);
        UUID unique();
        boolean isDistance(Location location, double total_distance);
        short distance();
        void play(SenderInfo info, byte[] data, int level, double total_distance);

        static boolean isDistance(Location location1, Location location2, double total_distance) {
            return location1.getWorld() == location2.getWorld() && location1.distanceSquared(location2) <= total_distance * total_distance;
        }
    }

    public static Stream<RadioElement> elements() {
        return bufferElements.values().stream();
    }

    public interface SenderInfo {
        UUID uuid();
        Vector local();
        boolean noise();
        String prefix();

        static SenderInfo player(UUID uuid, Vector local, boolean noise) {
            return new SenderInfo() {
                @Override public UUID uuid() { return uuid; }
                @Override public boolean noise() { return noise; }
                @Override public Vector local() { return local; }
                @Override public String prefix() { return uuid + ""; }
            };
        }
        static SenderInfo block(UUID uuid, Position position) {
            return block(uuid, position.x, position.y, position.z);
        }
        static SenderInfo block(UUID uuid, int x, int y, int z) {
            return new SenderInfo() {
                @Override public UUID uuid() { return uuid; }
                @Override public boolean noise() { return false; }
                @Override public Vector local() { return new Vector(x+0.5,y+0.5,z+0.5); }
                @Override public String prefix() { return uuid + ":" + x + "," + y + "," + z; }
            };
        }
    }

    public static void logRadioError(Radio.SenderInfo info, Throwable throwable) {
        radioLogs.compute(system.toast("["+info.prefix()+"]: " + throwable.getMessage(), "error"), (k, v) -> (v == null ? 0 : v) + 1);
    }

    public static void playRadio(SenderInfo info, Location location, double total_distance, int level, byte[] data) {
        radioLogs.compute(system.toast("["+info.prefix()+"]: " + level + "*{count}", "log"), (k, v) -> (v == null ? 0 : v) + 1);
        bufferElements.values().stream()
                .filter(v -> v.hasLevel(level))
                .filter(v -> v.isDistance(location, total_distance))
                .forEach(v -> v.play(info, data, level, total_distance));
                //.forEach(element -> Voice.playWithDistance(location.getWorld(), new LocationalSoundPacketImpl(new LocationSoundPacket(element.playUUID(), location, data, nextSequence())), element.distance()));
    }
}






