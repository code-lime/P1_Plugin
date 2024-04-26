package org.lime.gp.player.voice;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.component.data.voice.RadioInstance;
import org.lime.gp.block.component.list.RadioComponent;
import org.lime.gp.chat.Apply;
import org.lime.gp.extension.JManager;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.IUpdate;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.data.UpdateType;
import org.lime.gp.item.settings.list.RadioSetting;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;
import org.lime.system.map;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.tritonus.share.TSettings;

import javax.annotation.Nullable;
import java.util.*;

public class RadioData {
    public enum RadioState {
        none(false, false),
        input(true, false),
        output(false, true),
        all(true, true);

        public final boolean isInput;
        public final boolean isOutput;

        RadioState(boolean input, boolean output) {
            this.isInput = input;
            this.isOutput = output;
        }
    }

    public int level;
    public boolean enable;
    public final RadioState state;
    public final int min_level;
    public final int max_level;
    public final int def_level;
    public final double total_distance;
    public final String category;
    public int volume = 100;

    public static int clampVolume(int volume) {
        return Math.min(Math.max(volume, 0), 100);
    }
    public int clampLevel(int level) {
        return Math.min(Math.max(level, min_level), max_level);
    }

    public RadioData(RadioSetting setting) {
        this(setting.min_level, setting.max_level, setting.def_level, setting.state, setting.total_distance, setting.is_on, setting.category);
    }
    public RadioData(RadioComponent component) {
        this(component.min_level, component.max_level, component.def_level, component.state, component.total_distance, false, component.category);
    }
    public RadioData(int min_level, int max_level, int def_level, RadioState state, double total_distance, boolean enable, @Nullable String category) {
        this.min_level = min_level;
        this.max_level = max_level;
        this.def_level = def_level;
        this.total_distance = total_distance;
        this.state = state;
        this.enable = enable;
        this.category = category;

        this.level = clampLevel(def_level);
    }

    public Map<String, String> map() {
        return map.<String, String>of()
                .add("level", String.valueOf(level))
                .add("state", enable ? "true" : "false")
                .add("action", String.valueOf(state))
                .add("min_level", String.valueOf(min_level))
                .add("max_level", String.valueOf(max_level))
                .add("volume", String.valueOf(volume))
                .add("category", category)
                .build();
    }
    /*public List<Component> createLore(ItemCreator itemCreator) {
        return itemCreator.createLore(Apply.of().add(map()));
    }*/

    public static Optional<RadioData> getData(ItemStack item) {
        return Items.getOptional(RadioSetting.class, item).map(setting -> {
            ItemMeta meta = item.getItemMeta();
            RadioData data = new RadioData(setting);
            Optional.ofNullable(JManager.get(JsonObject.class, meta.getPersistentDataContainer(), "radio.data", null))
                    .map(JsonObjectOptional::of)
                    .ifPresent(data::read);
            return data;
        });
    }
    public static void modifyData(ItemStack item, Action1<RadioData> modify) {
        Items.getOptional(RadioSetting.class, item).ifPresent(setting -> {
            ItemMeta meta = item.getItemMeta();
            modifyData(setting, meta, modify);
            item.setItemMeta(meta);
        });
    }
    public static void modifyData(RadioSetting setting, ItemMeta meta, Action1<RadioData> modify) {
        RadioData data = new RadioData(setting);
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Optional.ofNullable(JManager.get(JsonObject.class, container, "radio.data", null))
                .map(JsonObjectOptional::of)
                .ifPresent(data::read);
        modify.invoke(data);
        data.level = data.clampLevel(data.level);
        JManager.set(container, "radio.data", data.write().build());
        setting.creator().update(meta, Apply.of().add(data.map()), IUpdate.of(UpdateType.LORE));
        //meta.lore(data.createLore(setting.creator()));
        meta.setCustomModelData(data.enable ? setting.on : setting.off);
    }
    public static Optional<RadioData> getData(Block block) {
        return Blocks.of(block)
                .flatMap(Blocks::customOf)
                .flatMap(v -> v.list(RadioInstance.class).findFirst())
                .map(v -> v.radioData);
    }
    public static void modifyData(Block block, Action1<RadioData> modify) {
        Blocks.of(block)
                .flatMap(Blocks::customOf)
                .flatMap(v -> v.list(RadioInstance.class).findFirst())
                .ifPresent(instance -> {
                    modify.invoke(instance.radioData);
                    instance.radioData.level = instance.radioData.clampLevel(instance.radioData.level);
                    instance.radioData.volume = clampVolume(instance.radioData.volume);
                    instance.saveData();
                });
    }

    public static HashMap<Integer, Radio.LevelInfo> getOutput(Player player) {
        HashMap<Integer, Radio.LevelInfo> list = new HashMap<>();
        for (ItemStack item : player.getInventory().getContents()) {
            RadioData.getData(item).ifPresent(data -> {
                if (data.enable && data.state.isOutput) {
                    list.compute(data.level, (k,v) -> v == null
                            ? new Radio.LevelInfo(data.volume, data.category)
                            : v.volume() < data.volume
                            ? new Radio.LevelInfo(data.volume, data.category)
                            : v);
                }
            });
        }
        return list;
    }
    public static Optional<Toast2<Integer, Double>> getInput(Player player) {
        return RadioData.getData(player.getInventory().getItemInMainHand())
                .filter(data -> data.enable && data.state.isInput)
                .map(data -> Toast.of(data.level, data.total_distance));
    }

    public void read(JsonObjectOptional json) {
        level = clampLevel(json.getAsInt("level").orElse(def_level));
        enable = json.getAsBoolean("state").orElse(enable);
        volume = json.getAsInt("volume").orElse(100);
    }
    public json.builder.object write() {
        return json.object()
                .add("level", level)
                .add("state", enable)
                .add("volume", volume);
    }
}
