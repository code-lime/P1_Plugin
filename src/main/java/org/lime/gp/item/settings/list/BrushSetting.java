package org.lime.gp.item.settings.list;

import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IComment;
import org.lime.docs.json.IJElement;
import org.lime.docs.json.JsonGroup;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonNull;

@Setting(name = "brush") public class BrushSetting extends ItemSetting<JsonNull> {
    public BrushSetting(ItemCreator creator) { super(creator); }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, IJElement.nullable(), IComment.text("Предмет является кисточкой для покраски"));
    }
}