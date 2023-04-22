package org.lime.gp.item.settings.list;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataHolder;
import org.lime.system;
import org.lime.gp.lime;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.extension.LimePersistentDataType;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

@Setting(name = "block") public class BlockSetting extends ItemSetting<JsonObject> {
    public static final NamespacedKey BLOCK_DATA_KEY = new NamespacedKey(lime._plugin, "block_data");
    public final Map<InfoComponent.Rotation.Value, String> rotation = new LinkedHashMap<>();
    public final Map<String, JsonObject> block_args = new HashMap<>();

    public BlockSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        json.getAsJsonObject("rotation").entrySet().forEach(kv -> {
            String value = kv.getValue().getAsString();
            for (String key : kv.getKey().split("\\|")) rotation.put(InfoComponent.Rotation.Value.ofAngle(Integer.parseInt(key)), value);
        });
        if (json.has("block_args")) json.getAsJsonObject("block_args").entrySet().forEach(kv -> block_args.put(kv.getKey(), kv.getValue().getAsJsonObject()));
    }

    @Override public void apply(ItemStack item, ItemMeta meta, Apply apply) {
        JsonObject data = new JsonObject();
        apply.list().forEach((key, value) -> {
            if (key.startsWith("block_data.")) data.addProperty(key.substring(11), value);
        });
        if (data.size() <= 0) return;
        meta.getPersistentDataContainer().set(BLOCK_DATA_KEY, LimePersistentDataType.JSON_OBJECT, data);
    }

    public Map<String, JsonObject> blockArgs(ItemStack item) {
        Apply apply = Apply.of();
        Optional.of(item)
                .map(ItemStack::getItemMeta)
                .map(PersistentDataHolder::getPersistentDataContainer)
                .map(v -> v.get(BLOCK_DATA_KEY, LimePersistentDataType.JSON_OBJECT))
                .ifPresent(v -> v.entrySet().forEach(kv -> apply.add(kv.getKey(), kv.getValue().getAsString())));
        return block_args.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, kv -> system.EditStringToObject(kv.getValue().deepCopy(), text -> new JsonPrimitive(ChatHelper.formatText(text, apply)))));
    }
}