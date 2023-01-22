package org.lime.gp.player.voice;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.lime.gp.chat.Apply;
import org.lime.gp.extension.JManager;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.MegaPhoneSetting;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.util.*;

public class MegaPhoneData extends DistanceData {
    public int volume = 100;

    public static int clampVolume(int volume) {
        return Math.min(Math.max(volume, 0), 100);
    }

    public MegaPhoneData(MegaPhoneSetting setting) {
        this(setting.min_distance, setting.max_distance, setting.def_distance);
    }
    public MegaPhoneData(short min_distance, short max_distance, short def_distance) {
        super(min_distance, max_distance, def_distance);
    }

    public List<Component> createLore(Items.ItemCreator itemCreator) {
        return itemCreator.createLore(Apply.of().add(map()));
    }
    public static Optional<MegaPhoneData> getData(ItemStack item) {
        return Items.getOptional(MegaPhoneSetting.class, item).map(setting -> {
            ItemMeta meta = item.getItemMeta();
            MegaPhoneData data = new MegaPhoneData(setting);
            Optional.ofNullable(JManager.get(JsonObject.class, meta.getPersistentDataContainer(), "megaphone.data", null))
                    .map(JsonObjectOptional::of)
                    .ifPresent(data::read);
            return data;
        });
    }
    public static void modifyData(ItemStack item, system.Action1<MegaPhoneData> modify) {
        Items.getOptional(MegaPhoneSetting.class, item).ifPresent(setting -> {
            ItemMeta meta = item.getItemMeta();
            modifyData(setting, meta, modify);
            item.setItemMeta(meta);
        });
    }
    public static void modifyData(MegaPhoneSetting setting, ItemMeta meta, system.Action1<MegaPhoneData> modify) {
        MegaPhoneData data = new MegaPhoneData(setting);
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Optional.ofNullable(JManager.get(JsonObject.class, container, "megaphone.data", null))
                .map(JsonObjectOptional::of)
                .ifPresent(data::read);
        modify.invoke(data);
        JManager.set(container, "megaphone.data", data.write().build());
        meta.lore(data.createLore(setting.creator()));
    }

    @Override public HashMap<String, String> map() {
        HashMap<String, String> map = super.map();
        map.put("volume", String.valueOf(volume));
        return map;
    }

    public void read(JsonObjectOptional json) {
        super.read(json);
        volume = clampVolume(json.getAsInt("volume").orElse(100));
    }
    public system.json.builder.object write() {
        return super.write()
                .add("volume", volume);
    }
}
