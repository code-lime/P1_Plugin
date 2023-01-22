package org.lime.gp.item.settings.list;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.lime.system;
import org.lime.gp.chat.Apply;
import org.lime.gp.extension.Cooldown;
import org.lime.gp.item.Items;
import org.lime.gp.item.UseSetting;
import org.lime.gp.item.settings.*;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.sound.Sounds;

import com.google.gson.JsonObject;

import net.kyori.adventure.sound.Sound;

@Setting(name = "musical_instrument") public class MusicalInstrumentSetting extends ItemSetting<JsonObject> implements UseSetting.IUse {
    public final system.Action2<Location, Float> pitch;
    public final EquipmentSlot arm;
    public final double cooldown;
    public final String menu;
    public final HashMap<String, String> args = new HashMap<>();
    public MusicalInstrumentSetting(Items.ItemCreator creator, JsonObject json) {
        super(creator, json);
        this.pitch = json.has("note") ? new system.Action2<>() {
            final org.bukkit.Sound sound = org.bukkit.Sound.valueOf("BLOCK_NOTE_BLOCK_" + json.get("note").getAsString());
            @Override public void invoke(Location location, Float pitch) {
                location.getWorld().playSound(Sound.sound(sound, Sound.Source.PLAYER, 1.0f, pitch), location.getX(), location.getY(), location.getZ());
            }
        } : new system.Action2<>() {
            final String sound = json.get("sound").getAsString();
            @Override public void invoke(Location location, Float pitch) {
                Sounds.playSound(sound, location);
            }
        };
        this.cooldown = json.has("cooldown") ? json.get("cooldown").getAsDouble() : 0;
        this.menu = json.has("menu") ? json.get("menu").getAsString() : null;
        this.arm = EquipmentSlot.valueOf(json.get("arm").getAsString());
        if (json.has("args")) json.get("args").getAsJsonObject().entrySet().forEach(kv -> args.put(kv.getKey(), kv.getValue().getAsString()));
    }

    @Override public EquipmentSlot arm() { return arm; }
    @Override public boolean use(Player player, Player target, EquipmentSlot arm, boolean shift) {
        Location location = player.getLocation();
        if (cooldown > 0 && Cooldown.hasOrSetCooldown(player.getUniqueId(), creator().getKey(), cooldown)) return false;
        float pitch = (Math.min(30, Math.max(-20, player.getEyeLocation().getPitch())) + 20) / 50;
        this.pitch.invoke(location, pitch);
        if (this.menu != null) MenuCreator.show(player, this.menu, Apply.of().add(args));
        return false;
    }
}