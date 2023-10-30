package org.lime.gp.module.loot;

import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTableEvent;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.event.*;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.filter.data.IFilterParameter;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;
import org.lime.system.execute.Action0;

import java.util.*;
import java.util.function.Consumer;

public class PopulateLootEvent extends Event implements Cancellable, IPopulateLoot {
    public static CoreElement create() {
        return CoreElement.create(PopulateLootEvent.class)
                .withInstance(new Listener() {
                    @EventHandler public static void on(LootTableEvent e) {
                        LootTable lootTable = e.lootTable();
                        MinecraftKey key = ReflectionAccess.randomSequence_LootTable.get(lootTable);
                        executeLootTable(key, e.context(), e.lootConsumer(), () -> e.setCancelled(true));
                    }
                });
    }
    public static void executeLootTable(MinecraftKey key, LootTableInfo context, Consumer<ItemStack> lootConsumer, Action0 vanillaCancel) {
        PopulateLootEvent event = new PopulateLootEvent(key, context);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            vanillaCancel.invoke();
            return;
        }
        if (event.isReplaced()) {
            event.items.forEach(item -> lootConsumer.accept(CraftItemStack.asNMSCopy(item)));
            event.append_items.forEach(item -> lootConsumer.accept(CraftItemStack.asNMSCopy(item)));
            vanillaCancel.invoke();
            return;
        }
        event.append_items.forEach(item -> lootConsumer.accept(CraftItemStack.asNMSCopy(item)));
    }

    private final MinecraftKey key;
    private final LootTableInfo context;
    private List<org.bukkit.inventory.ItemStack> items;
    private List<org.bukkit.inventory.ItemStack> append_items = new ArrayList<>();
    public PopulateLootEvent(MinecraftKey key, LootTableInfo context) {
        this.key = key;
        this.context = context;
        this.items = null;
    }

    public MinecraftKey getKey() { return key; }
    public void setItems(Collection<org.bukkit.inventory.ItemStack> items) { this.items = new ArrayList<>(items); }
    public boolean isReplaced() { return this.items != null; }
    public void addItem(org.bukkit.inventory.ItemStack item) { this.append_items.add(item); }
    public void addItems(Collection<org.bukkit.inventory.ItemStack> items) { this.append_items.addAll(items); }

    public net.minecraft.world.level.World world() { return context.getLevel(); }
    public Optional<IBlockData> blockData() { return getOptional(LootContextParameters.BLOCK_STATE); }

    @Override public Optional<Collection<String>> tags() { return getOptional(LootContextParameters.THIS_ENTITY).map(Entity::getTags); }

    @Override public boolean has(IFilterParameter<IPopulateLoot, ?> parameter) {
        return LootParameter.of(parameter).map(this::has).orElse(false);
    }
    @Override public <TValue> TValue get(IFilterParameter<IPopulateLoot, TValue> parameter) {
        return LootParameter.of(parameter).map(this::get).orElseThrow(() -> new NoSuchElementException(parameter.name()));
    }
    @Override public <TValue> Optional<TValue> getOptional(IFilterParameter<IPopulateLoot, TValue> parameter) {
        return LootParameter.of(parameter).flatMap(this::getOptional);
    }
    @Override public <TValue> TValue getOrDefault(IFilterParameter<IPopulateLoot, TValue> parameter, TValue def) {
        return LootParameter.of(parameter).flatMap(this::getOptional).orElse(def);
    }

    public World getCraftWorld() { return context.getLevel().getWorld(); }

    public boolean has(LootContextParameter<?> parameter) { return context.hasParam(parameter); }
    public <T>T get(LootContextParameter<T> parameter) { return context.getParam(parameter); }
    public <T>Optional<T> getOptional(LootContextParameter<T> parameter) { return Optional.ofNullable(context.getParamOrNull(parameter)); }
    public <T>T getOrDefault(LootContextParameter<T> parameter, T def) { return has(parameter) ? get(parameter) : def; }

    private static final HandlerList handlers = new HandlerList();
    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }

    private boolean cancel = false;
    public boolean isCancelled() { return this.cancel; }
    public void setCancelled(boolean cancel) { this.cancel = cancel; }
}




























