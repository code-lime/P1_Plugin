package org.lime.gp.sound;

import com.comphenix.protocol.events.PacketEvent;
import com.google.gson.JsonObject;
import net.minecraft.network.protocol.game.PacketPlayOutEntitySound;
import net.minecraft.network.protocol.game.PacketPlayOutNamedSoundEffect;
import net.minecraft.sounds.SoundEffect;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.lime;
import org.lime.system;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Sounds {
    public static CoreElement create() {
        return CoreElement.create(Sounds.class)
                .withInit(Sounds::init)
                .<JsonObject>addConfig("sounds.json", v -> v.withInvoke(Sounds::config).withDefault(new JsonObject()));
    }

    public static ConcurrentHashMap<String, ISound> sounds = new ConcurrentHashMap<>();
    public static void playSound(String key, Player player) {
        if (key == null) return;
        Optional.ofNullable(sounds.get(key))
                .ifPresentOrElse(sound -> sound.playSound(player), () -> lime.logOP("Sound '"+key+"' not founded!"));
    }
    public static void playSound(String key, Player player, Vector position) {
        if (key == null) return;
        Optional.ofNullable(sounds.get(key))
                .ifPresentOrElse(sound -> sound.playSound(player, position), () -> lime.logOP("Sound '"+key+"' not founded!"));
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
    private static final List<system.Func1<Integer, Optional<IReplaceInfo>>> mapper = new ArrayList<>();
    public static void registryReplaceEntity(system.Func1<Integer, Optional<IReplaceInfo>> mapper) {
        Sounds.mapper.add(mapper);
    }
    private static Optional<IReplaceInfo> getMappedInfo(int id) {
        for (system.Func1<Integer, Optional<IReplaceInfo>> item : mapper) {
            Optional<IReplaceInfo> result = item.invoke(id);
            if (result.isEmpty()) continue;
            return result;
        }
        return Optional.empty();
    }

    private static void init() {
        PacketManager.adapter()
                .add(PacketPlayOutEntitySound.class, Sounds::onPacket)
                .add(PacketPlayOutNamedSoundEffect .class, Sounds::onPacket)
                .listen();
    }
    private static void config(JsonObject json) {
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

    private static final system.LockToast1<Integer> ignoringEntitySound = system.toast(0).lock();
    private static void onPacket(PacketPlayOutEntitySound packet, PacketEvent event) {
        if (ignoringEntitySound.get0() > 0) return;
        replaceSound(packet.getSound().value())
                .ifPresent(sound -> {
                    event.setCancelled(true);
                    try {
                        ignoringEntitySound.edit0(v -> v + 1);
                        int id = packet.getId();
                        IReplaceInfo info = getMappedInfo(id)
                                .orElseGet(() -> IReplaceInfo.ofID(id));
                        info.playSound(sound, event.getPlayer(), info.tags());
                    } finally {
                        ignoringEntitySound.edit0(v -> v - 1);
                    }
                });
    }

    private static final system.LockToast1<Integer> ignoringNamedSound = system.toast(0).lock();
    private static void onPacket(PacketPlayOutNamedSoundEffect packet, PacketEvent event) {
        if (ignoringNamedSound.get0() > 0) return;
        replaceSound(packet.getSound().value())
                .ifPresent(sound -> {
                    event.setCancelled(true);
                    try {
                        ignoringNamedSound.edit0(v -> v + 1);
                        sound.playSound(event.getPlayer(), new Vector(packet.getX(), packet.getY(), packet.getZ()));
                    } finally {
                        ignoringNamedSound.edit0(v -> v - 1);
                    }
                });
    }
    private static Optional<ISound> replaceSound(SoundEffect sound) {
        return Optional.ofNullable(Sounds.sounds.get(sound.getLocation().getPath() + "#replace"));
    }
}
