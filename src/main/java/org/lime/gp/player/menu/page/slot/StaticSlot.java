package org.lime.gp.player.menu.page.slot;

import org.lime.gp.item.data.ItemCreator;

import com.google.gson.JsonObject;

public class StaticSlot extends ItemCreator {
    @Override public boolean updateReplace() { return false; }
    protected StaticSlot(JsonObject json) { super("menu.static_slot", json); }
    public static StaticSlot parse(JsonObject json) { return new StaticSlot(json); }
}
