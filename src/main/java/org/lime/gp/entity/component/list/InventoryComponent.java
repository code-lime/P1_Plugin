package org.lime.gp.entity.component.list;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.EntityInfo;
import org.lime.gp.entity.component.ComponentDynamic;
import org.lime.gp.entity.component.InfoComponent;
import org.lime.gp.entity.component.data.InventoryInstance;
import org.lime.gp.item.data.Checker;
import org.lime.gp.item.settings.list.TableDisplaySetting;
import org.lime.system.range.IRange;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@InfoComponent.Component(name = "inventory")
public final class InventoryComponent extends ComponentDynamic<JsonObject, InventoryInstance> {
    public record Data(int rows, Component title, Map<Integer, Checker> slots, boolean variable) {
        public static Data parse(JsonObject json) {
            boolean variable = json.has("variable") && json.get("variable").getAsBoolean();
            int rows = json.has("rows") ? json.get("rows").getAsInt() : 1;
            Component title = ChatHelper.formatComponent(json.get("title").getAsString());
            Map<Integer, Checker> slots = new HashMap<>();
            json.getAsJsonObject("slots").entrySet().forEach(kv -> {
                Checker checker = Checker.createCheck(kv.getValue().getAsString());
                IRange.parse(kv.getKey()).getAllInts(rows * 9)
                        .forEach(slot -> slots.put(slot, checker));
            });
            return new Data(rows, title, slots, variable);
        }
    }
    public final Map<String, Data> containers = new HashMap<>();

    public InventoryComponent(EntityInfo info, JsonObject json) {
        super(info, json);
        json.getAsJsonObject().entrySet().forEach(kv -> containers.put(kv.getKey(), Data.parse(kv.getValue().getAsJsonObject())));
    }

    public InventoryComponent(EntityInfo info, Map<String, Data> containers) {
        super(info);
        this.containers.putAll(containers);
    }

    @Override public InventoryInstance createInstance(CustomEntityMetadata metadata) { return new InventoryInstance(this, metadata); }
    public Class<InventoryInstance> classInstance() { return InventoryInstance.class; }

    public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, IJElement.anyObject(
                JProperty.optional(IName.raw("index_name"), JObject.of(
                        JProperty.optional(IName.raw("type"), IJElement.raw("TYPE"), IComment.empty()
                                .append(IComment.text("Пользовательский тип испольуемый в связке с "))
                                .append(IComment.link(docs.settingsLink(TableDisplaySetting.class)))),
                        JProperty.optional(IName.raw("rows"), IJElement.range(1, 6), IComment.text("Количество строк в инвентаре. По умолчанию - ").append(IComment.raw(1))),
                        JProperty.require(IName.raw("slots"), IJElement.anyObject(
                                JProperty.require(IName.link(docs.range()), IJElement.link(docs.regexItem()))
                        ), IComment.text("Список слотов и предметов которые в него можно положить. Пропущенные слоты будут автоматически заблокированы для взаимодействия"))
                ))
        ), IComment.text("Хранит предметы"));
    }
}
