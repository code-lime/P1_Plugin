package org.lime.gp.craft.book;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.minecraft.network.chat.IChatBaseComponent;
import org.lime.gp.chat.ChatHelper;

class RecipesBookData {
    @SuppressWarnings("unused")
    public final String id;
    public final IChatBaseComponent title;
    public final Component adventure$title;

    public RecipesBookData(String id, JsonObject json) {
        this.id = id;
        this.title = ChatHelper.toNMS(adventure$title = ChatHelper.formatComponent(json.get("title").getAsString()));
    }

    public RecipesBookData(String id, Component component) {
        this.id = id;
        this.title = ChatHelper.toNMS(adventure$title = component);
    }
}
