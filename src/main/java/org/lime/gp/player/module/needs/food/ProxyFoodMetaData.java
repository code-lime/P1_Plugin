package org.lime.gp.player.module.needs.food;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.food.FoodMetaData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.lime.core;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.extension.JsonNBT;
import org.lime.gp.lime;
import org.lime.gp.player.module.needs.INeedEffect;
import org.lime.gp.town.ChurchManager;
import org.lime.system;

import java.util.Optional;
import java.util.stream.Stream;

public class ProxyFoodMetaData extends FoodMetaData {
    private final FoodMutate mutate;
    private ProxyFoodMetaData(EntityHuman human, system.Action1<NBTTagCompound> addSaveData) {
        super(human);
        NBTTagCompound tag = new NBTTagCompound();
        addSaveData.invoke(tag);
        this.mutate = new FoodMutate(human, tag, this::syncVariable);
        syncVariable();
    }

    public IFoodLevel food() { return mutate.food; }

    public void setDeltaMutate(float deltaMutate) { this.mutate.setDeltaMutate(deltaMutate); }
    public float getDeltaMutate() { return this.mutate.getDeltaMutate(); }

    private int oldFoodLevel = 20;
    private float oldSaturationLevel = 5.0f;
    private float oldExhaustionLevel = 0;
    private int oldSaturatedRegenRate = 10;
    private int oldUnsaturatedRegenRate = 80;
    private int oldStarvationRate = 80;

    public void syncVariable() {
        if (super.foodLevel != oldFoodLevel) setFoodLevel(oldFoodLevel = super.foodLevel);
        else super.foodLevel = oldFoodLevel = getFoodLevel();

        if (super.saturationLevel != oldSaturationLevel) setSaturation(oldSaturationLevel = super.saturationLevel);
        else oldSaturationLevel = super.saturationLevel = getSaturationLevel();

        if (super.exhaustionLevel != oldExhaustionLevel) setExhaustion(oldExhaustionLevel = super.exhaustionLevel);
        else oldExhaustionLevel = super.exhaustionLevel = getExhaustionLevel();

        if (super.saturatedRegenRate != oldSaturatedRegenRate) setSaturatedRegenRate(oldSaturatedRegenRate = super.saturatedRegenRate);
        else oldSaturatedRegenRate = super.saturatedRegenRate = getSaturatedRegenRate();

        if (super.unsaturatedRegenRate != oldUnsaturatedRegenRate) setUnsaturatedRegenRate(oldUnsaturatedRegenRate = super.unsaturatedRegenRate);
        else oldUnsaturatedRegenRate = super.unsaturatedRegenRate = getUnsaturatedRegenRate();

        if (super.starvationRate != oldStarvationRate) setStarvationRate(oldStarvationRate = super.starvationRate);
        else oldStarvationRate = super.starvationRate = getStarvationRate();
    }

    public void setSaturatedRegenRate(int saturatedRegenRate) { mutate.setSaturatedRegenRate(saturatedRegenRate); }
    public void setUnsaturatedRegenRate(int unsaturatedRegenRate) { mutate.setUnsaturatedRegenRate(unsaturatedRegenRate); }
    public void setStarvationRate(int starvationRate) { mutate.setStarvationRate(starvationRate); }

    public int getSaturatedRegenRate() { return mutate.getSaturatedRegenRate(); }
    public int getUnsaturatedRegenRate() { return mutate.getUnsaturatedRegenRate(); }
    public int getStarvationRate() { return mutate.getStarvationRate(); }

    public boolean modify(float food, float saturation) { return mutate.modify(food, saturation); }

    @Override public void eat(int food, float saturationModifier) { mutate.eatDelta(food, saturationModifier); }
    @Override public void eat(Item item, ItemStack stack) { mutate.eat(item, stack); }
    @Override public void tick(EntityHuman player) {
        mutate.tick(player);
        syncVariable();
    }
    @Override public void readAdditionalSaveData(NBTTagCompound nbt) { mutate.readAdditionalSaveData(nbt); }
    @Override public void addAdditionalSaveData(NBTTagCompound nbt) { mutate.addAdditionalSaveData(nbt); }
    @Override public int getLastFoodLevel() { return mutate.getLastFoodLevel(); }
    @Override public int getFoodLevel() { return mutate.getFoodLevel(); }
    @Override public boolean needsFood() { return mutate.needsFood(); }
    @Override public void addExhaustion(float exhaustion) { mutate.addExhaustion(exhaustion); }
    @Override public float getExhaustionLevel() { return mutate.getExhaustionLevel(); }
    @Override public float getSaturationLevel() { return mutate.getSaturationLevel(); }
    @Override public void setFoodLevel(int foodLevel) { mutate.setFoodLevel(foodLevel); }
    @Override public void setSaturation(float saturationLevel) { mutate.setSaturation(saturationLevel); }
    @Override public void setExhaustion(float exhaustion) { mutate.setExhaustion(exhaustion); }


    public static core.element create() {
        return core.element.create(ProxyFoodMetaData.class)
                .withInit(ProxyFoodMetaData::init);
    }
    private static void init() {
        lime.repeat(ProxyFoodMetaData::update, 0.2);
    }
    private static void update() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            ProxyFoodMetaData metaData = getProxyFood(((CraftPlayer)player).getHandle());
            metaData.setDeltaMutate(ChurchManager.hasAnyEffect(player, ChurchManager.EffectType.SATURATION) ? 0.5f : 1);
        });
    }
    private static ProxyFoodMetaData getProxyFood(EntityPlayer player) {
        FoodMetaData data = player.getFoodData();
        if (data instanceof ProxyFoodMetaData metaData && metaData.mutate.isVanilla() == FoodType.IsVanilla) return metaData;
        ProxyFoodMetaData proxy = new ProxyFoodMetaData(player, data::addAdditionalSaveData);
        ReflectionAccess.foodData_EntityHuman.set(player, proxy);
        return proxy;
    }
    public static Optional<ProxyFoodMetaData> ofPlayer(Player player) {
        return Optional.ofNullable(player)
                .map(v -> v instanceof CraftPlayer c ? c.getHandle() : null)
                .map(ProxyFoodMetaData::getProxyFood);
    }
    public static Stream<INeedEffect<?>> getFoodNeeds(Player player) {
        return ofPlayer(player)
                .map(v -> v.mutate.food.totalLevel())
                .stream()
                .flatMap(value -> FoodType.Needs.entrySet()
                        .stream()
                        .filter(v -> v.getKey().inRange(value, 20))
                        .flatMap(v -> v.getValue().stream()));
    }
}

















