package org.lime.gp.block.component.display.partial.list;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.display.ItemParser;
import org.lime.docs.IIndexDocs;
import org.lime.docs.json.*;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.display.partial.PartialEnum;
import org.lime.gp.docs.IDocsLink;
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
            this.item = ItemParser.readItem(json.get("item"));
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

    public static JObject docs(IDocsLink docs, IIndexDocs variable, boolean isTop) {
        return BlockPartial.docs(docs, variable).addFirst(
                JProperty.property(isTop, IName.raw("item"), IJElement.link(docs.parseItem()), IComment.text("Отображаемый предмет в рамке")),
                JProperty.optional(IName.raw("rotation"), IJElement.link(docs.rotation()), IComment.text("Поворот модели блока"))
        );
    }
}