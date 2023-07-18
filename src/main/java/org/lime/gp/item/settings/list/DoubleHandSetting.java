package org.lime.gp.item.settings.list;

import com.google.gson.JsonPrimitive;
import net.minecraft.world.entity.player.EntityHuman;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.lime.core;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.gp.lime;

@Setting(name = "double_hand") public class DoubleHandSetting extends ItemSetting<JsonPrimitive> {
    public final boolean isEnable;
    public DoubleHandSetting(ItemCreator creator, JsonPrimitive json) {
        super(creator, json);
        this.isEnable = json.getAsBoolean();
    }

    public static core.element create() {
        return core.element.create(DoubleHandSetting.class)
                .withInit(DoubleHandSetting::init);
    }

    private static void init() {
        lime.repeatTicks(() -> Bukkit.getOnlinePlayers().forEach(player -> {
            PlayerInventory inventory = player.getInventory();
            ItemStack offhand = inventory.getItemInOffHand();
            if (offhand.getType().isAir()) return;
            ItemStack mainhand = inventory.getItemInMainHand();
            if (mainhand.getType().isAir()) return;
            if (Items.getOptional(DoubleHandSetting.class, mainhand).map(v -> v.isEnable).orElse(false)
                    || Items.getOptional(DoubleHandSetting.class, offhand).map(v -> v.isEnable).orElse(false)) {
                inventory.setItemInOffHand(new ItemStack(Material.AIR));
                Items.dropGiveItem(player, offhand, false);
            }
        }), 1);
    }
}



