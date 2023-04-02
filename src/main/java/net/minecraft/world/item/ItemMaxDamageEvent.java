package net.minecraft.world.item;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.UnsafeInstance;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftMagicNumbers;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ItemMaxDamageEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    @Nullable private final Integer customModelData;
    private final Material type;
    private int maxDamage;

    private ItemMaxDamageEvent(ItemStack nms_item, boolean isAsync) {
        super(isAsync);
        NBTTagCompound tag = nms_item.getTag();
        maxDamage = nms_item.getItem().getMaxDamage();
        type = CraftMagicNumbers.getMaterial(nms_item.getItem());
        customModelData = tag != null && tag.contains("CustomModelData") ? tag.getInt("CustomModelData") : null;
    }

    public Optional<Integer> getCustomModelData() {
        return Optional.ofNullable(customModelData);
    }
    public Material getType() { return type; }
    public int getMaxDamage() {
        return maxDamage;
    }
    public void setMaxDamage(int stack) {
        maxDamage = stack;
    }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }

    public static int call_getMaxDamage(ItemStack nms_item) {
        Server server = Bukkit.getServer();
        ItemMaxDamageEvent event = new ItemMaxDamageEvent(nms_item, server == null || !server.isPrimaryThread());
        if (server == null) return event.getMaxDamage();
        server.getPluginManager().callEvent(event);
        return event.getMaxDamage();
    }

    private static final ConcurrentHashMap<Integer, Item> cache_items = new ConcurrentHashMap<>();
    private static Item ofDamage(int maxDamage) {
        return cache_items.computeIfAbsent(maxDamage, md -> {
            Item item = UnsafeInstance.createInstance(Item.class);
            try {
                Field field = Item.class.getDeclaredField(UnsafeInstance.ofMojang(Item.class, "maxDamage", Type.INT_TYPE, false));
                field.setAccessible(true);
                UnsafeInstance.nonFinal(field).set(item, md);
                return item;
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        });
    }

    public static Item call_getMaxDamageItem(ItemStack nms_item) {
        Server server = Bukkit.getServer();
        ItemMaxDamageEvent event = new ItemMaxDamageEvent(nms_item, server == null || !server.isPrimaryThread());
        if (server == null) return nms_item.getItem();
        int max = event.getMaxDamage();
        server.getPluginManager().callEvent(event);
        return max != event.getMaxDamage()
                ? ofDamage(event.getMaxDamage())
                : nms_item.getItem();
    }
}
