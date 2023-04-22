package org.lime.gp.module;

import com.google.common.collect.ImmutableMap;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.IInventory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.lime.core;
import org.lime.gp.access.ReflectionAccess;
import org.lime.reflection;

import java.util.*;
import java.util.function.Consumer;

public class PopulateLootEvent extends Event implements Cancellable {
    private static class LootTableProxy extends LootTable {
        public final LootTable base_of_proxy;
        public final MinecraftKey key;
        public LootTableProxy(MinecraftKey key, LootTable base) {
            super(LootContextParameterSets.EMPTY, new LootSelector[0], new LootItemFunction[0]);
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
        @Override public void validate(LootCollector reporter) { base_of_proxy.validate(reporter); }
        @Override public void fill(IInventory inventory, LootTableInfo context) { base_of_proxy.fill(inventory, context); }
        @Override public LootContextParameterSet getParamSet() { return base_of_proxy.getParamSet(); }
    }
    public static core.element create() {
        return core.element.create(PopulateLootEvent.class)
                .withInit(PopulateLootEvent::init);
    }
    
    private static void init() {
        LootTableRegistry registry = MinecraftServer.getServer().getLootTables();
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
    }

    public static class Parameters {
        public static final LootContextParameter<Entity> ThisEntity = LootContextParameters.THIS_ENTITY;
        public static final LootContextParameter<EntityHuman> LastDamagePlayer = LootContextParameters.LAST_DAMAGE_PLAYER;
        public static final LootContextParameter<DamageSource> DamageSource = LootContextParameters.DAMAGE_SOURCE;
        public static final LootContextParameter<Entity> KillerEntity = LootContextParameters.KILLER_ENTITY;
        public static final LootContextParameter<Entity> DirectKillerEntity = LootContextParameters.DIRECT_KILLER_ENTITY;
        public static final LootContextParameter<Vec3D> Origin = LootContextParameters.ORIGIN;
        public static final LootContextParameter<IBlockData> BlockState = LootContextParameters.BLOCK_STATE;
        public static final LootContextParameter<TileEntity> BlockEntity = LootContextParameters.BLOCK_ENTITY;
        public static final LootContextParameter<ItemStack> Tool = LootContextParameters.TOOL;
        public static final LootContextParameter<Float> ExplosionRadius = LootContextParameters.EXPLOSION_RADIUS;
        public static final LootContextParameter<Integer> LootingMod = LootContextParameters.LOOTING_MOD;

        public static Map<String, LootContextParameter<?>> all() {
            return ImmutableMap.<String, LootContextParameter<?>>builder()
                    .put("ThisEntity", ThisEntity)
                    .put("LastDamagePlayer", LastDamagePlayer)
                    .put("DamageSource", DamageSource)
                    .put("KillerEntity", KillerEntity)
                    .put("DirectKillerEntity", DirectKillerEntity)
                    .put("Origin", Origin)
                    .put("BlockState", BlockState)
                    .put("BlockEntity", BlockEntity)
                    .put("Tool", Tool)
                    .put("ExplosionRadius", ExplosionRadius)
                    .put("LootingMod", LootingMod)
                    .build();
        }

        private static <T>void appendTo(LootContextParameter<T> param, LootTableInfo context, LootTableInfo.Builder builder) { builder.withParameter(param, context.getParam(param)); }
    }

    private final MinecraftKey key;
    private final LootTableProxy proxy;
    private final LootTableInfo context;
    private HashMap<LootContextParameter<?>, Object> parameters = null;
    private List<org.bukkit.inventory.ItemStack> items;
    private List<org.bukkit.inventory.ItemStack> append_items = new ArrayList<>();
    public PopulateLootEvent(MinecraftKey key, LootTableProxy proxy, LootTableInfo context) {
        this.key = key;
        this.proxy = proxy;
        this.context = context;
        this.items = null;
    }

    public MinecraftKey getKey() { return key; }
    public LootTableInfo getContext(boolean copy) {
        if (!copy) return context;
        LootTableInfo.Builder builder = new LootTableInfo.Builder(context.getLevel())
                .withRandom(context.getRandom())
                .withLuck(context.getLuck());
        LootContextParameterSet.Builder set = LootContextParameterSet.builder();
        Parameters.all().values().forEach(param -> {
            if (context.hasParam(param)) {
                Parameters.appendTo(param, context, builder);
                set.required(param);
            }
        });
        ReflectionAccess.dynamicDrops_LootTableInfo.get(context).forEach(builder::withDynamicDrop);
        return builder.create(set.build());
    }
    public void setItems(Collection<org.bukkit.inventory.ItemStack> items) { this.items = new ArrayList<>(items); }
    public List<ItemStack> getVanillaItems() { return proxy.base_of_proxy.getRandomItems(context); }
    public boolean isReplaced() { return this.items != null; }
    public void addItem(org.bukkit.inventory.ItemStack item) { this.append_items.add(item); }
    public void addItems(Collection<org.bukkit.inventory.ItemStack> items) { this.append_items.addAll(items); }

    public net.minecraft.world.level.World getWorld() { return context.getLevel(); }
    public World getCraftWorld() { return context.getLevel().getWorld(); }

    public boolean has(LootContextParameter<?> parameter) { return context.hasParam(parameter); }
    public <T>T get(LootContextParameter<T> parameter) { return context.getParam(parameter); }
    public <T>T getOrDefault(LootContextParameter<T> parameter, T def) { return has(parameter) ? get(parameter) : def; }
    private static final reflection.field<Map<LootContextParameter<?>, Object>> params_LootTableInfo = reflection.field
            .ofMojang(LootTableInfo.class, "params");
    public <T>void set(LootContextParameter<T> parameter, T value) {
        if (parameters == null) {
            parameters = new HashMap<>();
            parameters.putAll(params_LootTableInfo.get(context));
        }
        if (value == null) parameters.remove(parameter);
        else parameters.put(parameter, value);
    }

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




























