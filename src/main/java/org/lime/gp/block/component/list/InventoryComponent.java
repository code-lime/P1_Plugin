package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import org.lime.display.transform.LocalLocation;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.InventoryInstance;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.item.Items;
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
    public final Map<Integer, Items.Checker> slots = new HashMap<>();
    public final Map<Integer, LocalLocation> display = new HashMap<>();

    public InventoryComponent(BlockInfo info, JsonObject json) {
        super(info, json);

        this.type = json.has("type") && !json.get("type").isJsonNull() ? json.get("type").getAsString() : null;
        this.rows = json.has("rows") ? json.get("rows").getAsInt() : 1;
        this.title = ChatHelper.formatComponent(json.get("title").getAsString());
        json.getAsJsonObject("slots").entrySet().forEach(kv -> {
            Items.Checker checker = Items.createCheck(kv.getValue().getAsString());
            Menu.rangeOf(kv.getKey()).forEach(slot -> this.slots.put(slot, checker));
        });
        json.getAsJsonObject("display").entrySet().forEach(kv -> {
            LocalLocation location = new LocalLocation(system.getLocation(null, kv.getValue().getAsString()));
            Menu.rangeOf(kv.getKey()).forEach(slot -> this.display.put(slot, location));
        });
    }

    public InventoryComponent(BlockInfo info, @Nullable String type, int rows, Component title, Map<Integer, Items.Checker> slots, Map<Integer, LocalLocation> display) {
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
