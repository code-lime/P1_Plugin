package org.lime.gp.item.settings.list;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.lime.system;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.*;
import org.lime.gp.sound.SoundMaterial;
import org.lime.gp.sound.Sounds;

import com.google.gson.JsonObject;

import net.minecraft.world.level.block.state.IBlockData;

@Setting(name = "bullet") public class BulletSetting extends ItemSetting<JsonObject> {
    public enum BulletAction {
        NONE,
        TASER,
        TRAUMATIC
    }

    public final String bullet_type;
    public final BulletSetting.BulletAction bullet_action;
    public final int count;
    public final double damage;
    public final double time_sec;
    public final double time_damage_scale;

    public final HashMap<SoundMaterial, String> sound_hit = new HashMap<>();

    public BulletSetting(Items.ItemCreator creator, JsonObject json) {
        super(creator, json);
        bullet_type = json.get("bullet_type").getAsString();
        count = json.has("count") ? json.get("count").getAsInt() : 1;
        damage = json.get("damage").getAsDouble();
        if (json.has("time")) {
            JsonObject time = json.getAsJsonObject("time");
            time_sec = time.get("sec").getAsDouble();
            time_damage_scale = time.get("damage_scale").getAsDouble();
        } else {
            time_sec = Double.POSITIVE_INFINITY;
            time_damage_scale = 0;
        }

        bullet_action = json.has("bullet_action")
                ? system.tryParse(BulletSetting.BulletAction.class, json.get("bullet_action").getAsString())
                .orElseThrow(() -> new IllegalArgumentException("bullet_action can't be '" + json.get("bullet_action").getAsString() + "'. Only: " + Arrays.stream(BulletAction.values())
                        .map(Enum::name)
                        .collect(Collectors.joining(", ")))
                )
                : BulletAction.NONE;

        if (json.has("sound_hit")) json.getAsJsonObject("sound_hit").entrySet().forEach(kv -> sound_hit.put(SoundMaterial.valueOf(kv.getKey()), kv.getValue().getAsString()));
    }

    public void playSound(Material material, Location location) {
        Sounds.playSound(sound_hit.get(SoundMaterial.of(material)), location);
    }
    public void playSound(IBlockData state, Location location) {
        Sounds.playSound(sound_hit.get(SoundMaterial.of(state)), location);
    }
}