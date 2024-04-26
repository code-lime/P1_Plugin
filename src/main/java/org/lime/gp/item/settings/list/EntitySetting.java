package org.lime.gp.item.settings.list;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataHolder;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.extension.LimePersistentDataType;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.gp.lime;
import org.lime.system.json;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Setting(name = "entity") public class EntitySetting extends ItemSetting<JsonObject> {
    public static final NamespacedKey ENTITY_DATA_KEY = new NamespacedKey(lime._plugin, "entity_data");
    public final String entity;
    public final Map<String, JsonObject> entityArgs = new HashMap<>();

    public EntitySetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        entity = json.get("entity").getAsString();
        if (json.has("entity_args"))
            json.getAsJsonObject("entity_args")
                    .entrySet()
                    .forEach(kv -> entityArgs.put(kv.getKey(), kv.getValue().getAsJsonObject()));
    }

    @Override public void apply(ItemMeta meta, Apply apply) {
        JsonObject data = new JsonObject();
        apply.list().forEach((key, value) -> {
            if (key.startsWith("entity_data.")) data.addProperty(key.substring(12), value);
        });
        if (data.size() <= 0) return;
        meta.getPersistentDataContainer().set(ENTITY_DATA_KEY, LimePersistentDataType.JSON_OBJECT, data);
    }

    public Map<String, JsonObject> entityArgs(ItemStack item) {
        Apply apply = Apply.of();
        Optional.of(item)
                .map(ItemStack::getItemMeta)
                .map(PersistentDataHolder::getPersistentDataContainer)
                .map(v -> v.get(ENTITY_DATA_KEY, LimePersistentDataType.JSON_OBJECT))
                .ifPresent(v -> v.entrySet().forEach(kv -> apply.add(kv.getKey(), kv.getValue().getAsString())));
        return entityArgs.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, kv -> json.editStringToObject(kv.getValue().deepCopy(), text -> new JsonPrimitive(ChatHelper.formatText(text, apply)))));
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("entity"),
                        IJElement.raw("ENTITY"),
                        IComment.text("Энтити, на которое будет заменено")),
                JProperty.optional(IName.raw("entity_args"),
                        IJElement.anyObject(JProperty.require(IName.raw("COMPONENT_KEY"), IJElement.link(docs.formattedText()))),
                        IComment.empty()
                                .append(IComment.text("Устанавливает в компонент энтити "))
                                .append(IComment.raw("COMPONENT_KEY"))
                                .append(IComment.text(" данные из "))
                                .append(IComment.link(docs.formattedJson()))
                                .append(IComment.text(". В "))
                                .append(IComment.raw("args"))
                                .append(IComment.text(" передаются только "))
                                .append(IComment.raw("args"))
                                .append(IComment.text(" которые начинаются с "))
                                .append(IComment.raw("entity_data."))
                                .append(IComment.text(" Сам префикс "))
                                .append(IComment.raw("entity_data."))
                                .append(IComment.text(" удаляется. "))
                                .append(IComment.empty()
                                        .append(IComment.text("Пример: "))
                                        .append(IComment.raw("entity_data.any_color"))
                                        .append(IComment.text(" > "))
                                        .append(IComment.raw("any_color"))
                                        .italic()
                                ))
        ), IComment.text("Заменяет спавн энтити на энтити из `entities.json`"));
    }
}
