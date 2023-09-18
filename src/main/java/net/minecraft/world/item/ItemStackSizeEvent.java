package net.minecraft.world.item;

import net.minecraft.nbt.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.Optional;

public class ItemStackSizeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    @Nullable private final Integer customModelData;
    private final Material type;
    private int maxItemStack;

    private ItemStackSizeEvent(org.bukkit.inventory.ItemStack bukkit_item, boolean isAsync) {
        super(isAsync);
        ItemMeta meta = bukkit_item.getItemMeta();
        customModelData = meta.hasCustomModelData() ? meta.getCustomModelData() : null;
        if (bukkit_item instanceof CraftItemStack craft_item) {
            ItemStack handle = craft_item.handle;
            type = craft_item.getType();
            maxItemStack = handle == null ? Material.AIR.getMaxStackSize() : handle.getItem().getMaxStackSize();
        } else {
            type = bukkit_item.getType();
            maxItemStack = type == null ? -1 : type.getMaxStackSize();
        }
    }
    private ItemStackSizeEvent(net.minecraft.world.item.ItemStack nms_item, boolean isAsync) {
        super(isAsync);
        NBTTagCompound tag = nms_item.getTag();
        maxItemStack = nms_item.getItem().getMaxStackSize();
        type = CraftMagicNumbers.getMaterial(nms_item.getItem());
        customModelData = tag != null && tag.contains("CustomModelData") ? tag.getInt("CustomModelData") : null;
    }

    public Optional<Integer> getCustomModelData() {
        return Optional.ofNullable(customModelData);
    }
    public Material getType() { return type; }
    public int getMaxItemStack() {
        return maxItemStack;
    }
    public void setMaxItemStack(int stack) {
        maxItemStack = stack;
    }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }

    public static int call_getMaxStackSizeBukkit(org.bukkit.inventory.ItemStack bukkit_item) {
        Server server = Bukkit.getServer();
        ItemStackSizeEvent event = new ItemStackSizeEvent(bukkit_item, server == null || !server.isPrimaryThread());
        if (server == null) return event.getMaxItemStack();
        server.getPluginManager().callEvent(event);
        return event.getMaxItemStack();
    }
    public static int call_getMaxStackSizeNMS(net.minecraft.world.item.ItemStack nms_item) {
        Server server = Bukkit.getServer();
        ItemStackSizeEvent event = new ItemStackSizeEvent(nms_item, server == null || !server.isPrimaryThread());
        if (server == null) return event.getMaxItemStack();
        server.getPluginManager().callEvent(event);
        return event.getMaxItemStack();
    }
}
