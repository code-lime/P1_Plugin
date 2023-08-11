package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import net.kyori.adventure.text.Component;
import org.lime.display.transform.LocalLocation;
import org.lime.display.transform.Transform;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.InventoryInstance;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.item.data.Checker;
import org.lime.gp.player.menu.page.Menu;
import org.lime.system;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@InfoComponent.Component(name = "inventory")
public final class InventoryComponent extends ComponentDynamic<JsonObject, InventoryInstance> {
    public final int rows;
    public final String type;
    public final Component title;
    public final Map<Integer, Checker> slots = new HashMap<>();
    public final Map<Integer, Transformation> display = new HashMap<>();

    public InventoryComponent(BlockInfo info, JsonObject json) {
        super(info, json);

        this.type = json.has("type") && !json.get("type").isJsonNull() ? json.get("type").getAsString() : null;
        this.rows = json.has("rows") ? json.get("rows").getAsInt() : 1;
        this.title = ChatHelper.formatComponent(json.get("title").getAsString());
        json.getAsJsonObject("slots").entrySet().forEach(kv -> {
            Checker checker = Checker.createCheck(kv.getValue().getAsString());
            Menu.rangeOf(kv.getKey()).forEach(slot -> this.slots.put(slot, checker));
        });
        json.getAsJsonObject("display").entrySet().forEach(kv -> {
            Menu.rangeOf(kv.getKey()).forEach(slot -> this.display.put(slot, system.transformation(kv.getValue())));
        });
    }

    public InventoryComponent(BlockInfo info, @Nullable String type, int rows, Component title, Map<Integer, Checker> slots, Map<Integer, Transformation> display) {
        super(info);

        this.type = type;
        this.rows = rows;
        this.title = title;
        this.slots.putAll(slots);
        this.display.putAll(display);
    }

    @Override
    public InventoryInstance createInstance(CustomTileMetadata metadata) {
        return new InventoryInstance(this, metadata);
    }
}
