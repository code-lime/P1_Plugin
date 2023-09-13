package org.lime.gp.item.settings.list;

import com.google.gson.JsonObject;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.component.data.BaseAgeableInstance;
import org.lime.gp.block.component.list.CropsComponent;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.loot.ILoot;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.system;

@Setting(name = "crops") public class CropsSetting extends ItemSetting<JsonObject> implements BaseAgeableInstance.AgeableData {
    public final int ageCount;
    public final system.IRange ageStepTicks;
    public final ILoot loot;

    public CropsSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        JsonObject age = json.get("age").getAsJsonObject();
        ageCount = age.get("count").getAsInt();
        ageStepTicks = system.IRange.parse(age.get("step_ticks").getAsString());
        loot = ILoot.parse(json.get("loot"));
    }

    @Override public double tickAgeModify() { return 1 / ageStepTicks.getValue(100.0); }
    @Override public int limitAge() { return ageCount; }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("age"), JObject.of(
                        JProperty.require(IName.raw("count"), IJElement.raw(10), IComment.text("Количество этапов роста")),
                        JProperty.require(IName.raw("step_ticks"), IJElement.link(docs.range()), IComment.text("Количество тиков каждого этапа роста"))
                )),
                JProperty.require(IName.raw("loot"), IJElement.link(docs.loot()), IComment.text("Выходной лут"))
        ), "Семена для блока с " + docs.componentsLink(CropsComponent.class).link());
    }
}