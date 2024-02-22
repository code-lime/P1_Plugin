package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.UsestationInstance;
import org.lime.gp.docs.IDocsLink;

import java.util.stream.Stream;

@InfoComponent.Component(name = "medstation")
public final class UsestationComponent extends ComponentDynamic<JsonObject, UsestationInstance> {
    public final int distance;
    public UsestationComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        this.distance = json.get("distance").getAsInt();
    }

    @Override public UsestationInstance createInstance(CustomTileMetadata metadata) { return new UsestationInstance(this, metadata); }
    @Override public Class<UsestationInstance> classInstance() { return UsestationInstance.class; }
    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        IIndexGroup group = IIndexGroup.raw("JavaScript object", null, v -> Stream.of(
                "```js",
                "usestation.inZone(\"UUID\", \"BLOCK_KEY\"); //Проверка нахождения игрока в зоне типа блока. Возвраващет true/false",
                "usestation.hasUse(\"UUID\", \"ITEM_REGEX\"); //Проверка нахождения предмета в руке. Возвраващет true/false",
                "usestation.use(\"UUID\", \"ITEM_REGEX\"); //Использования педмета в руке. Возвраващет true/false",
                "```"
        ));
        return JsonGroup.of(index, JObject.of(
                JProperty.require(IName.raw("distance"), IJElement.raw(10), IComment.text("Дальность детекта использования"))
        )).withChilds(group);
    }
}
