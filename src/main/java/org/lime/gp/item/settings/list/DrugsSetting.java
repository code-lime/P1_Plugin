package org.lime.gp.item.settings.list;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.gp.player.module.drugs.EffectType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

@Setting(name = "drugs") public class DrugsSetting extends ItemSetting<JsonObject> {
    public final HashSet<EffectType> first_effects = new HashSet<>();
    public final HashSet<EffectType> last_effects = new HashSet<>();

    public double addiction;
    public double first;
    public double last;

    public DrugsSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        json.getAsJsonArray("first_effects")
                .forEach(item -> first_effects.add(EffectType.valueOf(item.getAsString())));
        json.getAsJsonArray("last_effects")
                .forEach(item -> last_effects.add(EffectType.valueOf(item.getAsString())));

        this.addiction = json.get("addiction").getAsDouble();
        this.first = json.get("first").getAsDouble();
        this.last = json.get("last").getAsDouble();
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        IIndexGroup drugs_effect_type = JsonEnumInfo.of("DRUGS_EFFECT_TYPE", EffectType.class);
        return JsonGroup.of(index, index, JObject.of(
                        JProperty.require(IName.raw("first_effects"),
                                IJElement.anyList(IJElement.link(drugs_effect_type)),
                                IComment.text("Список эфектов выдаваемых на первой страдии")),
                        JProperty.require(IName.raw("last_effects"),
                                IJElement.anyList(IJElement.link(drugs_effect_type)),
                                IComment.text("Список эфектов выдаваемых на последней страдии")),
                        JProperty.require(IName.raw("addiction"),
                                IJElement.raw(1.0),
                                IComment.text("Добавление значения зависимости")),
                        JProperty.require(IName.raw("first"),
                                IJElement.raw(1.0),
                                IComment.text("Время первой стадии в минутах")),
                        JProperty.require(IName.raw("last"),
                                IJElement.raw(1.0),
                                IComment.text("Время последней стадии в минутах"))),
                IComment.text("Предмет при съедении выдает последовательность определенных эффектов")
        ).withChilds(drugs_effect_type);
    }
}














