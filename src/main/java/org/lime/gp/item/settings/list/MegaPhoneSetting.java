package org.lime.gp.item.settings.list;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.meta.ItemMeta;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.gp.chat.Apply;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;
import org.lime.gp.player.voice.MegaPhoneData;

import com.google.gson.JsonObject;

@Setting(name = "megaphone") public class MegaPhoneSetting extends ItemSetting<JsonObject> {
    public final short def_distance;// 16
    public final short min_distance;// 0
    public final short max_distance;// 32
    public MegaPhoneSetting(ItemCreator creator, JsonObject json) {
        super(creator);
        def_distance = json.get("def_distance").getAsShort();
        min_distance = json.get("min_distance").getAsShort();
        max_distance = json.get("max_distance").getAsShort();
    }

    @Override public void apply(ItemMeta meta, Apply apply) {
        List<Action1<MegaPhoneData>> modify = new ArrayList<>();
        apply.get("distance").map(Short::parseShort).ifPresent(distance -> modify.add(data -> data.distance = distance));
        apply.get("volume").map(Integer::parseInt).ifPresent(volume -> modify.add(data -> data.volume = volume));
        if (!modify.isEmpty()) MegaPhoneData.modifyData(this, meta, data -> modify.forEach(action -> action.invoke(data)));
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("def_distance"), IJElement.raw(10), IComment.text("Начальная дальность слышимости")),
                JProperty.require(IName.raw("min_distance"), IJElement.raw(10), IComment.text("Минимальная дальность слышимости")),
                JProperty.require(IName.raw("max_distance"), IJElement.raw(10), IComment.text("Максимальная дальность слышимости"))
        ), IComment.text("Усиливает дальность звука"), IComment.text("Читаемые `args` в предмете: `distance` и `volume`"));
    }
}