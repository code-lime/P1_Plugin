package org.lime.gp.block.component.display.partial.list;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.display.partial.PartialEnum;
import org.lime.gp.extension.ItemNMS;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.kyori.adventure.text.format.TextColor;

public class FramePartial extends BlockPartial {
    private final ItemStack item;
    private final net.minecraft.world.item.ItemStack nms_item;
    private final InfoComponent.Rotation.Value rotation;
    private final boolean show;

    public InfoComponent.Rotation.Value rotation() { return rotation; }
    public boolean show() { return show; }

    public FramePartial(int distanceChunk, JsonObject json) {
        super(distanceChunk, json);
        if (json.has("item")) {
            JsonElement item = json.get("item");
            if (item.isJsonPrimitive()) {
                String[] args = item.getAsString().split("\\^");
                this.item = new ItemStack(Material.valueOf(args[0]));
                if (args.length >= 2) {
                    ItemMeta meta = this.item.getItemMeta();
                    meta.setCustomModelData(Integer.parseInt(args[1]));
                    this.item.setItemMeta(meta);
                }
            } else {
                JsonObject _item = item.getAsJsonObject();
                this.item = new ItemStack(Material.valueOf(_item.get("material").getAsString()));
                ItemMeta meta = this.item.getItemMeta();
                if (_item.has("id")) meta.setCustomModelData(_item.get("id").getAsInt());
                this.item.setItemMeta(meta);
            }
            rotation = json.has("rotation") ? InfoComponent.Rotation.Value.ofAngle(json.get("rotation").getAsInt()) : InfoComponent.Rotation.Value.ANGLE_0;
            nms_item = CraftItemStack.asNMSCopy(this.item);
            show = true;
        } else {
            item = new ItemStack(Material.AIR);
            nms_item = net.minecraft.world.item.ItemStack.EMPTY;
            rotation = InfoComponent.Rotation.Value.ANGLE_0;
            show = false;
        }
    }

    private final ConcurrentHashMap<String, net.minecraft.world.item.ItemStack> variableConvert = new ConcurrentHashMap<>();
    public net.minecraft.world.item.ItemStack nms(Map<String, String> variable) {
        String color = variable.getOrDefault("display.color", "default");
        String key = "display.color=" + color;
        return variableConvert.computeIfAbsent(key, (_key) -> {
            net.minecraft.world.item.ItemStack nms_item = this.nms_item.copy();
            if (color.equals("default")) return nms_item;
            ItemNMS.setColor(nms_item, TextColor.fromHexString("#" + color));
            return nms_item;
        });
    }

    @Override public PartialEnum type() { return PartialEnum.Frame; }
    @Override public String toString() {
        return super.toString()+ "^" + item + "R" + rotation.angle;
    }
}