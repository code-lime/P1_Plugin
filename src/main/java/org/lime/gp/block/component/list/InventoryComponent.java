package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import net.kyori.adventure.text.Component;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.InventoryInstance;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.data.Checker;
import org.lime.gp.item.settings.list.TableDisplaySetting;
import org.lime.system.range.IRange;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.MathUtils;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@InfoComponent.Component(name = "inventory")
public final class InventoryComponent extends ComponentDynamic<JsonObject, InventoryInstance> {
    public final int rows;
    public final String type;
    public final Component title;
    public final Map<Integer, Checker> slots = new HashMap<>();
    public final Map<Integer, Transformation> display = new HashMap<>();

    public InventoryComponent(BlockInfo info, JsonObject json) {
        super(info, json);

        this.type = json.has("type") && !json.get("type").isJsonNull() ? json.get("type").getAsString() : null;
        this.rows = json.has("rows") ? json.get("rows").getAsInt() : 1;
        this.title = ChatHelper.formatComponent(json.get("title").getAsString());
        json.getAsJsonObject("slots").entrySet().forEach(kv -> {
            Checker checker = Checker.createCheck(kv.getValue().getAsString());
            //Menu.rangeOf(kv.getKey())
            IRange.parse(kv.getKey()).getAllInts(this.rows * 9)
                    .forEach(slot -> this.slots.put(slot, checker));
        });
        json.getAsJsonObject("display").entrySet().forEach(kv -> {
            Transformation transformation = MathUtils.transformation(kv.getValue());
            //Menu.rangeOf(kv.getKey())
            IRange.parse(kv.getKey()).getAllInts(this.rows * 9)
                    .forEach(slot -> this.display.put(slot, transformation));
        });
    }

    public InventoryComponent(BlockInfo info, @Nullable String type, int rows, Component title, Map<Integer, Checker> slots, Map<Integer, Transformation> display) {
        super(info);

        this.type = type;
        this.rows = rows;
        this.title = title;
        this.slots.putAll(slots);
        this.display.putAll(display);
    }

    @Override public InventoryInstance createInstance(CustomTileMetadata metadata) { return new InventoryInstance(this, metadata); }
    @Override public Class<InventoryInstance> classInstance() { return InventoryInstance.class; }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, JObject.of(
                JProperty.optional(IName.raw("type"), IJElement.raw("TYPE"), IComment.empty()
                        .append(IComment.text("Пользовательский тип испольуемый в связке с "))
                        .append(IComment.link(docs.settingsLink(TableDisplaySetting.class)))),
                JProperty.optional(IName.raw("rows"), IJElement.range(1, 6), IComment.text("Количество строк в инвентаре. По умолчанию - ").append(IComment.raw(1))),
                JProperty.require(IName.raw("slots"), IJElement.anyObject(
                        JProperty.require(IName.link(docs.range()), IJElement.link(docs.regexItem()))
                ), IComment.text("Список слотов и предметов которые в него можно положить. Пропущенные слоты будут автоматически заблокированы для взаимодействия")),
                JProperty.require(IName.raw("display"), IJElement.anyObject(
                        JProperty.require(IName.link(docs.range()), IJElement.link(docs.transform()))
                ), IComment.text("Список слотов и трансформация которые будут отображены. Пропущенные слоты не будут отображаться"))
        ), IComment.text("Хранит и отображает предметы"));
    }
}
