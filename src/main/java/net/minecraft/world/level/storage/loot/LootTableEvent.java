package net.minecraft.world.level.storage.loot;

import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.function.Consumer;

public class LootTableEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final LootTable lootTable;
    private final LootTableInfo context;
    private final Consumer<ItemStack> lootConsumer;

    private boolean cancelled = false;

    private LootTableEvent(LootTable lootTable, LootTableInfo context, Consumer<ItemStack> lootConsumer) {
        this.lootTable = lootTable;
        this.context = context;
        this.lootConsumer = lootConsumer;
    }

    public static boolean execute(LootTable lootTable, LootTableInfo context, Consumer<ItemStack> lootConsumer) {
        LootTableEvent event = new LootTableEvent(lootTable, context, lootConsumer);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    public LootTable lootTable() { return lootTable; }
    public LootTableInfo context() { return context; }
    public Consumer<ItemStack> lootConsumer() { return lootConsumer; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
