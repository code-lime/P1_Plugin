package org.lime.gp.item.settings.list;

import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonPrimitive;

import net.minecraft.world.entity.EnumItemSlot;

@Setting(name = "equip") public class EquipSetting extends ItemSetting<JsonPrimitive> {
    public final EnumItemSlot slot;
    public EquipSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        this.slot = EnumItemSlot.byName(json.getAsString().toLowerCase());
    }
}