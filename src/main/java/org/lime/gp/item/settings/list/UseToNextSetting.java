package org.lime.gp.item.settings.list;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.item.Items;
import org.lime.gp.item.UseSetting;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonObject;

@Setting(name = "use_to_next") public class UseToNextSetting extends ItemSetting<JsonObject> implements UseSetting.ITimeUse {
    public final int time;
    public final Boolean shift;
    public final EquipmentSlot arm;
    public UseToNextSetting(Items.ItemCreator creator, JsonObject json) {
        super(creator, json);
        this.time = json.get("time").getAsInt();
        this.shift = json.get("shift").isJsonNull() ? null : json.get("shift").getAsBoolean();
        this.arm = EquipmentSlot.valueOf(json.get("arm").getAsString());
    }
    @Override public EquipmentSlot arm() { return arm; }
    @Override public int getTime() { return time; }
    @Override public void timeUse(Player player, Player target, ItemStack item) { }
    @Override public boolean use(Player player, Player target, EquipmentSlot arm, boolean shift) {
        if (this.shift != null && this.shift != shift) return false;
        return UseSetting.ITimeUse.super.use(player, target, arm, shift);
    }
}