package org.lime.gp.item.settings.list;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.lime.system;
import org.lime.gp.item.Items;
import org.lime.gp.item.UseSetting;
import org.lime.gp.item.settings.*;
import org.lime.gp.player.module.Death;

import com.google.gson.JsonObject;

@Setting(name = "heal") public class HealSetting extends ItemSetting<JsonObject> implements UseSetting.ITimeUse {
    public final system.IRange heal;
    public final Integer total;
    public final int time;
    public final boolean up;

    public HealSetting(Items.ItemCreator creator, JsonObject json) {
        super(creator, json);
        this.heal = json.has("heal") ? system.IRange.parse(json.get("heal").getAsString()) : null;
        this.total = json.has("total") ? json.get("total").getAsInt() : null;
        this.time = json.has("time") ? json.get("time").getAsInt() : 0;
        this.up = json.get("up").getAsBoolean();
    }

    @Override public EquipmentSlot arm() { return EquipmentSlot.HAND; }
    @Override public int getTime() { return time; }
    @Override public void timeUse(Player player, Player target, ItemStack item) {
        if (up) Death.up(target);
        double total = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double health = heal.getValue(total);
        target.setHealth(Math.max(0, Math.min(this.total == null ? total : Math.min(this.total, total), target.getHealth() + health)));
    }
}