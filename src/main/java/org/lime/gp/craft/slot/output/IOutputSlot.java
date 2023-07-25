package org.lime.gp.craft.slot.output;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.lime.system;

import java.util.stream.Collectors;

public interface IOutputSlot {
    net.minecraft.world.item.ItemStack modify(net.minecraft.world.item.ItemStack item, boolean copy, IOutputVariable variable);
    net.minecraft.world.item.ItemStack create(boolean isPreview, IOutputVariable variable);

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
            else if (json.has("level")) return new LevelOutputSlot(json.getAsJsonObject("level")
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(kv -> Integer.parseInt(kv.getKey()), kv -> of(kv.getValue()))),
                    of(json.get("other")),
                    !json.has("work") || json.get("work").isJsonNull() ? null : json.get("work").getAsInt()
            );
            else return new WeightOutputSlot(json.get("items").getAsJsonArray()
                    .asList().stream()
                    .map(JsonElement::getAsJsonObject)
                    .map(WeightOutputSlot.WeightData::of)
            );
        }
    }
}
