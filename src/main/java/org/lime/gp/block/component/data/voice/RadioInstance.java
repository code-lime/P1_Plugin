package org.lime.gp.block.component.data.voice;

import de.maxhenkel.voicechat.voice.common.LocationSoundPacket;
import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import net.minecraft.world.level.block.entity.TileEntitySkullEventRemove;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.list.RadioComponent;
import org.lime.gp.chat.Apply;
import org.lime.gp.extension.Cooldown;
import org.lime.gp.extension.MapUUID;
import org.lime.gp.module.DrawText;
import org.lime.gp.module.TimeoutData;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.player.voice.*;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.util.UUID;

public class RadioInstance extends BlockInstance implements CustomTileMetadata.Tickable, CustomTileMetadata.Interactable, CustomTileMetadata.Removeable {
    @Override public RadioComponent component() { return (RadioComponent)super.component(); }
    public RadioData radioData;
    public DistanceData distanceData;
    public RadioInstance(RadioComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
        radioData = new RadioData(component);
        distanceData = new DistanceData(component);
    }

    @Override public void read(JsonObjectOptional json) {
        radioData.read(json);
        distanceData.read(json);
    }
    @Override public system.json.builder.object write() {
        return system.json.object()
                .add(radioData.write())
                .add(distanceData.write());
    }

    public static final class RadioVoiceData extends TimeoutData.ITimeout implements Radio.RadioElement {
        public final Location location;
        public final short distance;
        public final short max_distance;
        public final int level;
        public final int volume;
        public final UUID unique;
        public final RadioData.RadioState state;
        public final double total_distance;

        public RadioVoiceData(UUID unique, Location location, org.lime.gp.player.voice.RadioData radioData, DistanceData distanceData) {
            super(5);
            this.unique = unique;
            this.location = location;
            this.distance = distanceData.distance;
            this.max_distance = distanceData.max_distance;
            this.level = radioData.level;
            this.state = radioData.state;
            this.total_distance = radioData.total_distance;
            this.volume = radioData.volume;
        }

        @Override public boolean hasLevel(int level) {
            return this.level == level && !TimeoutData.has(unique, Radio.RadioLockTimeout.class);
        }
        @Override public UUID unique() { return this.unique; }
        @Override public boolean isDistance(Location location, double total_distance) {
            return Radio.RadioElement.isDistance(location, this.location, total_distance);
        }
        @Override public short distance() { return distance; }
        @Override public void play(Radio.SenderInfo sender, byte[] data, int level) {
            Cooldown.setCooldown(unique, "voice.active", 0.25);
            UUID packet_sender = MapUUID.of("radio.block", this.unique, sender.uuid());
            Voice.sendLocationPacket(location.getWorld(), new LocationSoundPacket(packet_sender, location, Voice.modifyVolume(sender, packet_sender, data, volume), Voice.nextSequence(packet_sender), distance, null), true);
        }
    }

    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        if (!radioData.enable) return;
        UUID unique = unique();
        Location location = metadata.location(0.5, 0.5, 0.5);
        if (Cooldown.hasCooldown(unique, "voice.active")) {
            DrawText.show(new DrawText.IShowTimed(0.25) {
                @Override public String getID() { return unique + ".Radio"; }
                @Override public boolean filter(Player player) { return true; }
                @Override public Component text(Player player) { return Component.text("\uE738"); }
                @Override public Location location() { return location; }
                @Override public double distance() { return distanceData.distance; }
            });
        }
        TimeoutData.put(unique, RadioVoiceData.class, new RadioVoiceData(unique, location, radioData, distanceData));
    }
    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        if (!(event.player().getBukkitEntity() instanceof Player player)) return EnumInteractionResult.PASS;
        showMenu(player);
        return EnumInteractionResult.CONSUME;
    }
    @Override public void onRemove(CustomTileMetadata metadata, TileEntitySkullEventRemove event) {
        TimeoutData.remove(unique(), RadioVoiceData.class);
    }
    public void showMenu(Player player) {
        BlockPosition pos = metadata().skull.getBlockPos();
        MenuCreator.show(player, "radio.block.menu", Apply.of()
                .add(radioData.map())
                .add(distanceData.map())
                .add("x", pos.getX() + "")
                .add("y", pos.getY() + "")
                .add("z", pos.getZ() + "")
        );
    }
}
