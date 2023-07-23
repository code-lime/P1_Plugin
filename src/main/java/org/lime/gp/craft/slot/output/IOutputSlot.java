package org.lime.gp.craft.slot.output;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.item.data.IItemCreator;
import org.lime.system;

import java.util.Optional;

public interface IOutputSlot {
    ItemStack create();
    ItemStack apply(ItemStack item, boolean copy);
    net.minecraft.world.item.ItemStack nms(boolean isPreview);

    static IOutputSlot ofString(String str) {
        String[] args = str.split("\\*");
        return new SingleOutputSlot(args[0], args.length > 1 ? Integer.parseUnsignedInt(args[1]) : 1);
    }
    static IOutputSlot of(JsonElement element) {
        if (element.isJsonPrimitive()) return ofString(element.getAsString());
        else if (element.isJsonArray()) return new OneOfOutputSlot(element.getAsJsonArray().asList().stream().map(IOutputSlot::of));
        else {
            JsonObject json = element.getAsJsonObject();
            if (json.has("preview")) return new PreviewOutputSlot(of(json.get("preview")), of(json.get("item")));
            else if (json.has("durability")) return new DurabilityOutputSlot(of(json.get("item")), system.IRange.parse(json.get("durability").getAsString()));
            else return new WeightOutputSlot(json.get("items").getAsJsonArray()
                    .asList().stream()
                    .map(JsonElement::getAsJsonObject)
                    .map(WeightOutputSlot.WeightData::of)
            );
        }
    }
}
