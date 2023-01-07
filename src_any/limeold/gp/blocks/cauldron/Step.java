package org.lime.gp.block.component.data.cauldron;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.item.Items;
import org.lime.system;

import java.util.Arrays;
import java.util.Objects;

public class Step {
    public static abstract class IItem {
        public abstract boolean compare(ItemStack item);
        public abstract boolean compare(IItem item);
        public abstract ItemStack create(int amount);
        public abstract JsonElement toJson();
        @Override public abstract int hashCode();
        @Override public String toString() { return toJson().toString(); }
        @Override public boolean equals(Object obj) { return obj instanceof IItem && compare((IItem) obj); }
        private static class NoneItem extends IItem {
            public static final IItem.NoneItem none = new IItem.NoneItem();
            private NoneItem() { if (none != null) throw new IllegalArgumentException("NotSupported"); }
            @Override public boolean compare(ItemStack item) { return false; }
            @Override public boolean compare(IItem item) { return item == this; }
            @Override public ItemStack create(int amount) { return new ItemStack(Material.STONE, 0); }
            @Override public JsonElement toJson() { return JsonNull.INSTANCE; }
            @Override public int hashCode() { return Objects.hash(1113); }
        }
        private static class MaterialItem extends IItem {
            private final Material material;
            private MaterialItem(Material material) { this.material = material; }
            @Override public boolean compare(ItemStack item) { return item != null && item.getType() == material; }
            @Override public boolean compare(IItem item) { return item instanceof IItem.MaterialItem && ((IItem.MaterialItem) item).material == material; }
            @Override public ItemStack create(int amount) { return new ItemStack(material, amount); }
            @Override public JsonElement toJson() { return new JsonPrimitive(Items.getMaterialKey(material)); }
            @Override public int hashCode() { return Objects.hash(1114, material); }
        }
        private static class CustomItem extends IItem {
            private final String regex;
            private CustomItem(String regex) { this.regex = regex; }
            @Override public boolean compare(ItemStack item) { return Items.getKeyByItem(item).filter(v -> system.compareRegex(regex, v)).isPresent(); }
            @Override public boolean compare(IItem item) { return item instanceof IItem.CustomItem && ((IItem.CustomItem) item).regex.equals(regex); }
            @Override public ItemStack create(int amount) { return Items.createItem(Items.getKeysRegex(regex).get(0), v -> v.setCount(amount)).orElseThrow(); }
            @Override public JsonElement toJson() { return new JsonPrimitive(regex); }
            @Override public int hashCode() { return Objects.hash(1115, regex); }
        }
        static IItem byNone() { return IItem.NoneItem.none; }
        static IItem byMaterial(Material material) { return material == null ? byNone() : new IItem.MaterialItem(material); }
        static IItem byCustom(String key) { return key == null ? byNone() : new IItem.CustomItem(key); }
        static IItem parse(JsonElement json) {
            if (json == null || json.isJsonNull()) return byNone();
            String text = json.getAsString();
            if (Items.isMaterialKey(text)) return byMaterial(Material.valueOf(text.substring(10)));
            if (Arrays.stream(Material.values()).map(Enum::name).toList().contains(text)) return byMaterial(Material.valueOf(text));
            return byCustom(text);
        }
    }
    public final IItem item;
    public final IItem output;
    public final int count;
    public Step(IItem item) { this(item, 1); }
    public Step(IItem item, int count) { this(item, IItem.byNone(), count); }
    public Step(IItem item, IItem output) { this(item, output, 1); }
    public Step(IItem item, IItem output, int count) {
        this.item = item;
        this.count = count;
        this.output = output;
    }
    public boolean test(ItemStack item) { return this.item.compare(item); }
    public static Step parse(JsonObject json) { return new Step(IItem.parse(json.get("item")), (json.has("output") ? IItem.parse(json.get("output")) : IItem.byNone()), json.get("count").getAsInt()); }
    @Override public String toString() { return "STEP:" + item.toJson() + "*" + count; }
    @Override public int hashCode() { return Objects.hash(1112, item, count); }
    @Override public boolean equals(Object obj) { return obj instanceof Step && this.equals((Step) obj); }
    public boolean equals(Step step) { return step.count == count && step.item.equals(this.item); }
}
