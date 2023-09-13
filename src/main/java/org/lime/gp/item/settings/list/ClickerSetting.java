package org.lime.gp.item.settings.list;

import com.google.gson.JsonElement;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IJElement;
import org.lime.docs.json.JsonGroup;
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
    public ClickerSetting(ItemCreator creator, JsonElement json) {
        super(creator);
        if (json.isJsonPrimitive()) this.types.add(json.getAsString());
        else json.getAsJsonArray().forEach(type -> this.types.add(type.getAsString()));
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.raw("CLICKER_TYPE")
                .or(IJElement.anyList(IJElement.raw("CLICKER_TYPE"))),
                "Предмет является инструментом для взаимодействия с блоком " + docs.componentsLink(ClickerComponent.class).link());
    }
}