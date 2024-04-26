package org.lime.gp.item.settings.list;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.bukkit.inventory.meta.ItemMeta;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.chat.Apply;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;
import org.lime.gp.player.voice.RadioData;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import com.google.gson.JsonObject;

import javax.annotation.Nullable;

@Setting(name = "radio") public class RadioSetting extends ItemSetting<JsonObject> {
    public final int min_level;// 1490,
    public final int def_level;
    public final int max_level;// 1740,
    public final int on;// 19,
    public final int off;// 20
    public final short total_distance;
    public final boolean noise;
    public final boolean is_on;
    public final RadioData.RadioState state;
    public final @Nullable String category;
    public RadioSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        min_level = json.get("min_level").getAsInt();
        def_level = json.get("def_level").getAsInt();
        max_level = json.get("max_level").getAsInt();
        total_distance = json.get("total_distance").getAsShort();
        category = json.has("category") ? json.get("category").getAsString() : null;
        on = json.get("on").getAsInt();
        off = json.get("off").getAsInt();
        is_on = !json.has("is_on") || json.get("is_on").getAsBoolean();
        noise = json.has("noise") && json.get("noise").getAsBoolean();
        state = json.has("state") ? RadioData.RadioState.valueOf(json.get("state").getAsString()) : RadioData.RadioState.all;
    }

    @Override public void apply(ItemMeta meta, Apply apply) {
        List<Action1<RadioData>> modifyRadio = new ArrayList<>();
        apply.get("level").map(Integer::parseInt).ifPresent(level -> modifyRadio.add(data -> data.level = level));
        apply.get("state").map(Boolean::parseBoolean).ifPresent(state -> modifyRadio.add(data -> data.enable = state));
        if (!modifyRadio.isEmpty()) RadioData.modifyData(this, meta, data -> modifyRadio.forEach(action -> action.invoke(data)));
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("min_level"), IJElement.raw(10), IComment.text("Начальная волна")),
                JProperty.require(IName.raw("def_level"), IJElement.raw(10), IComment.text("Минимальная волна")),
                JProperty.require(IName.raw("max_level"), IJElement.raw(10), IComment.text("Максимальная волна")),
                JProperty.require(IName.raw("total_distance"), IJElement.raw(10), IComment.text("Максимальная дистанция связи")),
                JProperty.require(IName.raw("on"), IJElement.raw(10), IComment.text("ID включенного состояния")),
                JProperty.require(IName.raw("off"), IJElement.raw(10), IComment.text("ID выключенного состояния")),
                JProperty.optional(IName.raw("is_on"), IJElement.bool(), IComment.text("Состояние включения")),
                JProperty.optional(IName.raw("category"), IJElement.bool(), IComment.text("Категория, в которую будет проигрываться звук")),
                JProperty.optional(IName.raw("noise"), IJElement.bool(), IComment.text("Есть ли шум")),
                JProperty.optional(IName.raw("state"), IJElement.raw("STATE"), IComment.empty()
                        .append(IComment.text("Возможные состояния: "))
                        .append(IComment.or(Stream.of(RadioData.RadioState.values()).map(IComment::raw).toList())))
        ), IComment.text("Проигрывает или записывает звук на выбранной волне"), IComment.text("Читаемые `args` в предмете: `level` и `state`"));
    }
}



