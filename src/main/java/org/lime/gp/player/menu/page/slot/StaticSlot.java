package org.lime.gp.player.menu.page.slot;

import com.google.gson.JsonObject;
import org.lime.gp.item.Items;
import org.lime.gp.player.menu.page.Menu;

public class StaticSlot extends Items.ItemCreator {
    @Override public boolean updateReplace() { return false; }
    protected StaticSlot(JsonObject json) { super("menu.static_slot", json); }
    public static StaticSlot parse(JsonObject json) { return new StaticSlot(json); }
}
