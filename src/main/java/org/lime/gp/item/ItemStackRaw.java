package org.lime.gp.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.extension.ItemNMS;
import org.lime.gp.item.data.IItemCreator;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.lime;
import org.lime.system.utils.ItemUtils;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class ItemStackRaw {
    public static final ItemStackRaw EMPTY = new ItemStackRaw();

    private final @Nullable String raw;
    private final @Nullable ItemStack item;
    private final @Nullable net.minecraft.world.item.ItemStack nms;

    private ItemStackRaw() { this((String)null); }
    public ItemStackRaw(@Nullable String raw) {
        this.raw = raw;
        ItemStack item = ItemUtils.loadItem(this.raw);
        if (item == null || item.isEmpty()) {
            this.item = null;
            this.nms = null;
        } else {
            this.item = item;
            this.nms = CraftItemStack.asNMSCopy(item);
        }
    }
    public ItemStackRaw(ItemStack item) { this(ItemUtils.saveItem(item)); }
    public ItemStackRaw(net.minecraft.world.item.ItemStack item) { this(item.asBukkitMirror()); }

    public Optional<String> raw() { return Optional.ofNullable(raw); }
    public Optional<ItemStack> item() { return Optional.ofNullable(item).map(ItemStack::clone); }
    public Optional<net.minecraft.world.item.ItemStack> nms() { return Optional.ofNullable(this.nms).map(net.minecraft.world.item.ItemStack::copy); }

    public Optional<Material> type() { return Optional.ofNullable(item).map(ItemStack::getType); }
    public Optional<Integer> customModelData() { return Optional.ofNullable(nms).flatMap(ItemNMS::getCustomModelData); }
    public Optional<Integer> count() { return Optional.ofNullable(nms).map(net.minecraft.world.item.ItemStack::getCount); }
    public boolean isEmpty() { return Optional.ofNullable(nms).map(net.minecraft.world.item.ItemStack::isEmpty).orElse(true); }

    public JsonElement save() { return raw == null ? JsonNull.INSTANCE : new JsonPrimitive(raw); }
    public static ItemStackRaw load(JsonElement json) { return json.isJsonNull() ? EMPTY : new ItemStackRaw(json.getAsString()); }

    public boolean isSameItemSameTags(net.minecraft.world.item.ItemStack item) {
        return this.nms != null && net.minecraft.world.item.ItemStack.isSameItemSameTags(this.nms, item);
    }
    public boolean isEquals(net.minecraft.world.item.ItemStack item) {
        if (item == null) return this.nms == null;
        if (this.nms == null) return false;
        return this.nms.getCount() == item.getCount() && net.minecraft.world.item.ItemStack.isSameItemSameTags(this.nms, item);
    }
    public boolean isEquals(ItemStack item) {
        if (item == null) return this.item == null;
        if (this.item == null) return false;
        return this.item.getAmount() == item.getAmount() && item.isSimilar(this.item);
    }

    public boolean isEquals(ItemStackRaw item) { return item != null && Objects.equals(this.raw, item.raw); }
    @Override public boolean equals(Object obj) { return obj instanceof ItemStackRaw _obj && isEquals(_obj); }

    public ItemStackRaw grow(int amount) {
        net.minecraft.world.item.ItemStack raw = nms().orElseThrow();
        int count = raw.getCount() + amount;
        if (count > 127) throw new IllegalArgumentException("ITEM COUNT LIMIT: " + count + " > 127");
        raw.setCount(count);
        return new ItemStackRaw(raw);
    }
    public Optional<IItemCreator> creator() {
        return item == null || nms == null
                ? Optional.empty()
                : Items.getItemCreator(item.getType(), ItemNMS.getCustomModelData(nms).orElse(null));
    }
}









