package org.lime.gp.item.settings.list;

import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IComment;
import org.lime.docs.json.IJElement;
import org.lime.docs.json.JsonGroup;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

@Setting(name = "dye_color") public class DyeColorSetting extends ItemSetting<JsonPrimitive> {
    public final boolean dyeColor;
    public DyeColorSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        dyeColor = json.getAsBoolean();
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.bool(), IComment.text("Разрешает или запрещает перекрашивать предмет"));
    }
}