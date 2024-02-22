package org.lime.gp.item.settings.list;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.component.list.ClickerComponent;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Setting(name = "clicker") public class ClickerSetting extends ItemSetting<JsonElement> {
    public final List<String> types = new ArrayList<>();
    public final int clicks;
    public ClickerSetting(ItemCreator creator, JsonElement json) {
        super(creator);
        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            loadTypes(obj.get("types"));
            clicks = obj.has("clicks") ? obj.get("clicks").getAsInt() : 1;
        } else {
            loadTypes(json);
            clicks = 1;
        }
    }
    private void loadTypes(JsonElement json) {
        if (json.isJsonPrimitive()) this.types.add(json.getAsString());
        else json.getAsJsonArray().forEach(type -> this.types.add(type.getAsString()));
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.raw("CLICKER_TYPE")
                .or(IJElement.anyList(IJElement.raw("CLICKER_TYPE")))
                .or(JObject.of(
                        JProperty.require(IName.raw("types"), IJElement.raw("CLICKER_TYPE")
                                .or(IJElement.anyList(IJElement.raw("CLICKER_TYPE")))),
                        JProperty.optional(IName.raw("clicks"), IJElement.raw(2), IComment.text("Количество кликов которое будет регистрироваться за 1 удар"))
                )),
                IComment.text("Предмет является инструментом для взаимодействия с блоком ").append(IComment.link(docs.componentsLink(ClickerComponent.class))));
    }
}