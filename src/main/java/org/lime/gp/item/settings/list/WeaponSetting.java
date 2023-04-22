package org.lime.gp.item.settings.list;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.lime.system;
import org.lime.gp.lime;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;
import org.lime.gp.item.weapon.WeaponData;
import org.lime.gp.module.JavaScript;
import org.lime.gp.player.ui.ImageBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.kyori.adventure.text.format.TextColor;

@Setting(name = "weapon") public class WeaponSetting extends ItemSetting<JsonObject> {
    public static final NamespacedKey MAGAZINE_KEY = new NamespacedKey(lime._plugin, "magazine");

    public static class Pose {
        public final Vector offset;
        public final double range;
        public final double range_max;

        public double range(double recoil, double recoil_max) {
            return range + ((range_max - range) * (recoil / recoil_max));
        }

        public Pose(JsonObject json) {
            offset = system.getVector(json.get("offset").getAsString());
            range = json.get("range").getAsDouble();
            range_max = json.get("range_max").getAsDouble();
        }
    }
    public class UI {
        public final List<ImageBuilder> percent = new ArrayList<>();

        public ImageBuilder of(int ammo, double cooldown) {
            return ammo == 0
                    ? percent.get(percent.size() - 1)
                    : percent.get((int)Math.round(Math.max(Math.min(cooldown * 20.0 / tick_speed, 1), 0) * (percent.size() - 1)));
        }

        public UI(JsonArray json) {
            json.forEach(_item -> {
                JsonObject item = _item.getAsJsonObject();
                ImageBuilder builder = ImageBuilder.of(
                        ChatHelper.formatComponent(item.get("image").getAsString()),
                        item.get("size").getAsInt()
                );
                if (item.has("offset")) builder = builder.addOffset(item.get("offset").getAsInt());
                if (item.has("color")) builder = builder.withColor(TextColor.fromHexString("#" + item.get("color").getAsString()));
                percent.add(builder);
            });
        }
    }

    public final double bullet_speed;
    public final double bullet_down;

    public final String magazine_type;
    public final double recoil;
    public final double recoil_speed;
    public final double damage_scale;
    public final int tick_speed;
    public final double init_sec;

    public final List<WeaponData.State> states = new ArrayList<>();
    public final HashMap<WeaponData.Pose, WeaponSetting.Pose> poses = new HashMap<>();
    public final HashMap<WeaponData.State, UI> ui = new HashMap<>();

    public final String sound_shoot;
    public final String sound_load;
    public final String sound_unload;
    public final String sound_pose;
    public final String sound_state;

    public final String display;

    public WeaponSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        bullet_speed = json.get("bullet_speed").getAsDouble();
        bullet_down = json.get("bullet_down").getAsDouble();
        magazine_type = json.has("magazine_type") ? json.get("magazine_type").getAsString() : null;
        recoil = json.get("recoil").getAsDouble();
        recoil_speed = json.get("recoil_speed").getAsDouble();
        damage_scale = json.get("damage_scale").getAsDouble();
        tick_speed = json.get("tick_speed").getAsInt();
        init_sec = json.has("init_sec") ? json.get("init_sec").getAsDouble() : 2.5;

        json.getAsJsonArray("states").forEach(kv -> states.add(WeaponData.State.valueOf(kv.getAsString())));
        json.getAsJsonObject("poses").entrySet().forEach(kv -> poses.put(WeaponData.Pose.valueOf(kv.getKey()), new Pose(kv.getValue().getAsJsonObject())));
        if (json.has("ui")) json.getAsJsonObject("ui")
                .entrySet()
                .forEach(kv -> {
                    UI _ui = new UI(kv.getValue().getAsJsonArray());
                    if (_ui.percent.size() == 0) return;
                    ui.put(WeaponData.State.valueOf(kv.getKey()), _ui);
                });

        sound_shoot = json.has("sound_shoot") ? json.get("sound_shoot").getAsString() : null;
        sound_load = json.has("sound_load") ? json.get("sound_load").getAsString() : null;
        sound_unload = json.has("sound_unload") ? json.get("sound_unload").getAsString() : null;
        sound_pose = json.has("sound_pose") ? json.get("sound_pose").getAsString() : null;
        sound_state = json.has("sound_state") ? json.get("sound_state").getAsString() : null;

        display = json.has("display") ? json.get("display").getAsString() : ("" + creator.getID());
    }

    public static Optional<ItemStack> getMagazine(ItemStack weapon) {
        return Optional.of(weapon)
                .map(ItemStack::getItemMeta)
                .map(PersistentDataHolder::getPersistentDataContainer)
                .map(v -> v.get(MAGAZINE_KEY, PersistentDataType.STRING))
                .map(system::loadItem);
    }
    public static void setMagazine(ItemStack weapon, ItemStack magazine) {
        ItemMeta meta = weapon.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Optional.ofNullable(system.saveItem(magazine))
                .ifPresentOrElse(
                        _v -> container.set(MAGAZINE_KEY, PersistentDataType.STRING, _v),
                        () -> container.remove(MAGAZINE_KEY)
                );
        weapon.setItemMeta(meta);

        Items.getItemCreator(weapon)
                .map(v -> v instanceof ItemCreator c ? c : null)
                .ifPresent(v -> v.apply(weapon));
    }

    @Override public void appendArgs(ItemStack weapon, Apply apply) {
        apply.add("magazine_type", getMagazine(weapon)
                .flatMap(magazine -> Items.getOptional(MagazineSetting.class, magazine))
                .map(v -> v.magazine_type)
                .orElse("")
        );
    }

    public int weaponDisplay(WeaponData.Pose pose, Integer magazine_id, int bullet_count) {
        return JavaScript.getJsInt(display
                .replace("{pose}", pose.name())
                .replace("{magazine_id}", magazine_id == null ? "" : ("" + magazine_id))
                .replace("{bullet_count}", bullet_count + "")
        ).orElseGet(() -> creator().getID());
    }
}