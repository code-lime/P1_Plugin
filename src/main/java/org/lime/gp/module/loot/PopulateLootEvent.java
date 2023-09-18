package org.lime.gp.module.loot;

import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.filter.data.IFilterParameter;
import org.lime.reflection;

import java.util.*;
import java.util.function.Consumer;

public class PopulateLootEvent extends Event implements Cancellable, IPopulateLoot {
    private static class LootTableProxy extends LootTable {
        public final LootTable base_of_proxy;
        public final MinecraftKey key;
        public LootTableProxy(MinecraftKey key, LootTable base) {
            super(LootContextParameterSets.EMPTY, key, new LootSelector[0], new LootItemFunction[0]);
            this.base_of_proxy = base;
            this.key = key;
        }

        @Override public void getRandomItemsRaw(LootTableInfo context, Consumer<ItemStack> lootConsumer) {
            PopulateLootEvent event = new PopulateLootEvent(key, this, context);
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) return;
            if (event.items != null) {
                event.items.forEach(item -> lootConsumer.accept(CraftItemStack.asNMSCopy(item)));
                event.append_items.forEach(item -> lootConsumer.accept(CraftItemStack.asNMSCopy(item)));
                return;
            }
            base_of_proxy.getRandomItemsRaw(context, lootConsumer);
            event.append_items.forEach(item -> lootConsumer.accept(CraftItemStack.asNMSCopy(item)));
        }
        /*
        @Override public void getRandomItemsRaw(LootTableInfo context, Consumer<ItemStack> lootConsumer) {
            LootTableInfo.c<LootTable> loottableinfo_c = LootTableInfo.createVisitedEntry(this);
            if (context.pushVisitedElement(loottableinfo_c)) {
                Consumer<ItemStack> consumer1 = LootItemFunction.decorate(this.compositeFunction, lootConsumer, context);
                for (LootSelector lootselector : this.pools) {
                    lootselector.addRandomItems(consumer1, context);
                }
                context.popVisitedElement(loottableinfo_c);
            } else {
                LOGGER.warn("Detected infinite loop in loot tables");
            }
        }
        */
        @Override public void validate(LootCollector reporter) { base_of_proxy.validate(reporter); }
        @Override public LootContextParameterSet getParamSet() { return base_of_proxy.getParamSet(); }
    }
    public static CoreElement create() {
        return CoreElement.create(PopulateLootEvent.class)
                .withInit(PopulateLootEvent::init);
    }

    private static void init() {
        LootDataManager lootDataManager = MinecraftServer.getServer().getLootData();
        Map<LootDataId<?>, ?> elements = reflection.field.<Map<LootDataId<?>, ?>>ofMojang(LootDataManager.class, "elements").get(lootDataManager);
        /*Map<LootDataId<?>, ?> elements = reflection.dynamic.ofValue(lootDataManager)
                .<Map<LootDataId<?>, ?>>get("elements")
                .value;*/
        HashMap<LootTable, MinecraftKey> lootTableToKey = new HashMap<>();
        HashMap<LootDataId<?>, Object> idToAny = new HashMap<>();
        elements.entrySet().forEach(kv -> {
            LootDataId<?> id = kv.getKey();
            if (id.type() != LootDataType.TABLE) {
                idToAny.put(id, kv.getValue());
                return;
            }
            MinecraftKey key = id.location();
            LootTable lootTable = (LootTable)kv.getValue();
            while (org.lime.reflection.hasField(lootTable.getClass(), "base_of_proxy"))
                lootTable = org.lime.reflection.getField(lootTable.getClass(), "base_of_proxy", lootTable);
            LootTableProxy proxy = new LootTableProxy(key, lootTable);
            lootTableToKey.put(proxy, key);
            idToAny.put(id, proxy);
        });
        reflection.field.ofMojang(LootDataManager.class, "lootTableToKey").set(lootDataManager, lootTableToKey);
        reflection.field.ofMojang(LootDataManager.class, "elements").set(lootDataManager, idToAny);

        /*
        LootTableRegistry registry = MinecraftServer.getServer().getLootData().getLootTables();
        HashMap<MinecraftKey, LootTable> lootTables = new HashMap<>();
        HashMap<LootTable, MinecraftKey> lootTableToKey = new HashMap<>();
        registry.getIds().forEach(key -> {
            LootTable lootTable = registry.get(key);
            while (org.lime.reflection.hasField(lootTable.getClass(), "base_of_proxy")) lootTable = org.lime.reflection.getField(lootTable.getClass(), "base_of_proxy", lootTable);
            LootTableProxy proxy = new LootTableProxy(key, lootTable);
            lootTables.put(key, proxy);
            lootTableToKey.put(proxy, key);
        });
        reflection.field.ofMojang(LootTableRegistry.class, "tables").set(registry, lootTables);
        registry.lootTableToKey = lootTableToKey;
        */
    }

    private final MinecraftKey key;
    private final LootTableProxy proxy;
    private final LootTableInfo context;
    private final LootParams params;
    //private HashMap<LootContextParameter<?>, Object> parameters = null;
    private List<org.bukkit.inventory.ItemStack> items;
    private List<org.bukkit.inventory.ItemStack> append_items = new ArrayList<>();
    public PopulateLootEvent(MinecraftKey key, LootTableProxy proxy, LootTableInfo context) {
        this.key = key;
        this.proxy = proxy;
        this.context = context;
        this.params = ReflectionAccess.params_LootTableInfo.get(context);
        this.items = null;
    }

    public MinecraftKey getKey() { return key; }
    //public LootTableInfo getContext(boolean copy) {
        //if (!copy) return context;
        /*Map<LootContextParameter<?>, Object> params = new HashMap<>();
        Parameters.all().values().forEach(param -> {
            if (context.hasParam(param.nms())) {
                params.put(param.nms(), context.getParam(param.nms()));
                //Parameters.appendTo(param.nms(), context, builder);
            }
        });
        //WorldServer world, Map<LootContextParameter<?>, Object> parameters, Map<MinecraftKey, b> dynamicDrops, float luck
        //LootParams params = new LootParams(context.getLevel(), );

        /*Map<MinecraftKey, LootParams.b> dynamicDrops = reflection.dynamic.ofValue(context)
                .get("params")
                .<Map<MinecraftKey, LootParams.b>>get("dynamicDrops")
                .value;*/

        //LootTableInfo.Builder builder = new LootTableInfo.Builder(new LootParams(context.getLevel(), params, dynamicDrops, context.getLuck()));
                //.withRandom(context.getRandom())
                //.withLuck(context.getLuck());
        //ReflectionAccess.dynamicDrops_LootTableInfo.get(context).forEach(builder::withDynamicDrop);
        //return new LootTableInfo.Builder(this.params).create(key);
    //}
    public void setItems(Collection<org.bukkit.inventory.ItemStack> items) { this.items = new ArrayList<>(items); }
    public List<ItemStack> getVanillaItems() { return proxy.base_of_proxy.getRandomItems(params); }
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
    /*public <T>void set(LootContextParameter<T> parameter, T value) {
        if (parameters == null) {
            parameters = new HashMap<>();
            parameters.putAll(ReflectionAccess.params_LootParams.get(params));
        }
        if (value == null) parameters.remove(parameter);
        else parameters.put(parameter, value);
    }*/

    private static final HandlerList handlers = new HandlerList();
    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }

    private boolean cancel = false;
    public boolean isCancelled() { return this.cancel; }
    public void setCancelled(boolean cancel) { this.cancel = cancel; }

    public PopulateLootEvent copy() {
        return new PopulateLootEvent(key, proxy, context);
    }
}




























