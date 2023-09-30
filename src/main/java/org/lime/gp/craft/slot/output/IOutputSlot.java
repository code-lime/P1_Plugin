package org.lime.gp.craft.slot.output;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import org.lime.system.range.*;

import java.util.stream.Collectors;

public interface IOutputSlot {
    ItemStack modify(ItemStack item, boolean copy, IOutputVariable variable);
    ItemStack create(boolean isPreview, IOutputVariable variable);

    boolean test(ItemStack item);

    static IOutputSlot ofString(String str) {
        String[] args = str.split("\\*");
        return new RangeOutputSlot(args[0], args.length > 1 ? IRange.parse(args[1]) : new OnceRange(1));
    }
    static IOutputSlot of(JsonElement element) {
        if (element.isJsonPrimitive()) return ofString(element.getAsString());
        else if (element.isJsonArray()) return new OneOfOutputSlot(element.getAsJsonArray().asList().stream().map(IOutputSlot::of));
        else {
            JsonObject json = element.getAsJsonObject();

            if (json.has("preview")) return new PreviewOutputSlot(of(json.get("preview")), of(json.get("item")));
            else if (json.has("level")) return new LevelOutputSlot(json.getAsJsonObject("level")
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(kv -> Integer.parseInt(kv.getKey()), kv -> of(kv.getValue()))),
                    of(json.get("other")),
                    !json.has("work") || json.get("work").isJsonNull() ? null : json.get("work").getAsInt()
            );
            else if (json.has("items")) return new WeightOutputSlot(json.get("items").getAsJsonArray()
                    .asList().stream()
                    .map(JsonElement::getAsJsonObject)
                    .map(WeightOutputSlot.WeightData::of)
            );
            else {
                return new ModifyOutputSlot(of(json.get("item")),
                        json.has("durability")
                                ? IRange.parse(json.get("durability").getAsString())
                                : null,
                        json.has("name") ? json.get("name").getAsString() : null,
                        json.has("lore") ? json.get("lore").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList() : null,
                        json.has("id") ? json.get("id").getAsString() : null
                );
            }
        }
    }
}
