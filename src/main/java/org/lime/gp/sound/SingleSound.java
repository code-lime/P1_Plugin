package org.lime.gp.sound;

import com.google.gson.JsonObject;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.PacketPlayOutEntitySound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.world.entity.Marker;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.unsafe;

import java.util.Collection;
import java.util.Optional;

public class SingleSound extends ISound {
    public final String key;
    public final double distance;
    public final Sound.Source source;
    public final Func0<Double> volume;
    public final Func0<Double> pitch;

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

    public SingleSound(String key, double distance, Sound.Source source, Func0<Double> volume) {
        this(key, distance, source, volume, () -> 1.0);
    }

    public SingleSound(String key, double distance, Sound.Source source, Func0<Double> volume, Func0<Double> pitch) {
        this.key = key;
        this.distance = distance;
        this.source = source;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override public void playSound(Player player, Collection<String> tags) {
        player.playSound(Sound.sound(Key.key(key), source, (float) (double) volume.invoke(), (float) (double) pitch.invoke()));
    }

    @Override public void playSound(Player player, Vector position, Collection<String> tags) {
        player.playSound(Sound.sound(Key.key(key), source, (float) (double) volume.invoke(), (float) (double) pitch.invoke()), position.getX(), position.getY(), position.getZ());
    }

    @Override public void playSound(Player player, Entity target, Collection<String> tags) {
        player.playSound(Sound.sound(Key.key(key), source, (float) (double) volume.invoke(), (float) (double) pitch.invoke()), target);
    }

    @Override public void playSound(Player player, int targetId, Collection<String> tags) {
        if (!(player instanceof CraftPlayer cplayer)) return;
        Sound sound = Sound.sound(Key.key(key), source, (float) (double) volume.invoke(), (float) (double) pitch.invoke());

        long seed = sound.seed().orElseGet(cplayer.getHandle().getRandom()::nextLong);
        MinecraftKey name = PaperAdventure.asVanilla(sound.name());
        Optional<SoundEffect> event = BuiltInRegistries.SOUND_EVENT.getOptional(name);
        Holder<SoundEffect> soundHolder = event.map(BuiltInRegistries.SOUND_EVENT::wrapAsHolder).orElseGet(() -> Holder.direct(SoundEffect.createVariableRangeEvent(name)));
        Marker target = unsafe.createInstance(Marker.class);
        target.setId(targetId);
        cplayer.getHandle().connection.send(new PacketPlayOutEntitySound(soundHolder, PaperAdventure.asVanilla(sound.source()), target, sound.volume(), sound.pitch(), seed));
    }

    @Override public void playSound(Location location, Collection<String> tags) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        location.getNearbyPlayers(distance)
                .forEach(player -> player.playSound(Sound.sound(Key.key(key), source, (float) (double) volume.invoke(), (float) (double) pitch.invoke()), x, y, z));
    }
}
