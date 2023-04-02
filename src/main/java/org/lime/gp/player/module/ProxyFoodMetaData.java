package org.lime.gp.player.module;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayOutUpdateHealth;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.food.FoodMetaData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.event.CraftEventFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.lime.core;
import org.lime.gp.lime;
import org.lime.gp.town.ChurchManager;
import org.lime.gp.access.ReflectionAccess;

import java.util.Optional;

public class ProxyFoodMetaData extends FoodMetaData {
    public static core.element create() {
        return core.element.create(ProxyFoodMetaData.class)
                .withInit(ProxyFoodMetaData::init);
    }
    private static void init() {
        lime.repeat(ProxyFoodMetaData::update, 0.2);
    }
    private static void update() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            EntityPlayer eplayer = ((CraftPlayer)player).getHandle();
            FoodMetaData data = eplayer.getFoodData();
            if (data instanceof ProxyFoodMetaData metaData) {
                if (ChurchManager.hasEffect(player, ChurchManager.EffectType.SATURATION)) {
                    metaData.saturationStep = 0.5f;
                    metaData.foodStep = 0.5f;
                } else {
                    metaData.saturationStep = 1;
                    metaData.foodStep = 1;
                }
                return;
            }
            if (data.getClass() != FoodMetaData.class) data = org.lime.reflection.getField(data.getClass(), "base", data);
            ReflectionAccess.foodData_EntityHuman.set(eplayer, new ProxyFoodMetaData(data));
        });
    }

    private final FoodMetaData base;

    private float doubleFood;

    public float foodStep = 1;
    public float saturationStep = 1;

    public ProxyFoodMetaData(FoodMetaData base) {
        super(ReflectionAccess.entityhuman_FoodMetaData.get(base));
        this.base = base;
        this.doubleFood = base.getFoodLevel();
    }
    private void onFoodEdit() {
        doubleFood = this.base.foodLevel;
        foodLevel = this.base.foodLevel;
    }
    private void sync() {
        foodLevel = base.foodLevel;
        saturationLevel = base.saturationLevel;
        exhaustionLevel = base.exhaustionLevel;
        saturatedRegenRate = base.saturatedRegenRate;
        unsaturatedRegenRate = base.unsaturatedRegenRate;
        starvationRate = base.starvationRate;
    }

    @Override public void eat(int food, float saturationModifier) {
        base.eat(food, saturationModifier);
        onFoodEdit();
    }
    @Override public void eat(Item item, ItemStack stack) { base.eat(item, stack); }

    @Override public void tick(EntityHuman player) {
        if (base.foodLevel != this.foodLevel) onFoodEdit();

        EnumDifficulty enumdifficulty = player.level.getDifficulty();
        if (base.exhaustionLevel > 4.0f) {
            base.exhaustionLevel -= 4.0f;
            if (base.saturationLevel > 0.0f) base.saturationLevel = Math.max(base.saturationLevel - saturationStep, 0.0f);
            else if (enumdifficulty != EnumDifficulty.PEACEFUL) {
                float food = Math.max(doubleFood - foodStep, 0);
                int _food = (int) food;
                FoodLevelChangeEvent event = CraftEventFactory.callFoodLevelChangeEvent(player, _food);
                if (!event.isCancelled()) {
                    int __food = event.getFoodLevel();
                    if (__food == _food) doubleFood = food;
                    else doubleFood = __food;
                    base.foodLevel = (int) doubleFood;
                }
                ((EntityPlayer) player).connection.send(new PacketPlayOutUpdateHealth(((EntityPlayer) player).getBukkitEntity().getScaledHealth(), base.foodLevel, base.saturationLevel));
            }
        }
        float buff = base.exhaustionLevel;
        base.exhaustionLevel = 0;
        base.tick(player);
        base.exhaustionLevel = buff;
        sync();
    }
    @Override public void readAdditionalSaveData(NBTTagCompound nbt) {
        base.readAdditionalSaveData(nbt);
        onFoodEdit();
    }
    @Override public void addAdditionalSaveData(NBTTagCompound nbt) { base.addAdditionalSaveData(nbt); }
    @Override public int getLastFoodLevel() { return base.getLastFoodLevel(); }
    @Override public int getFoodLevel() { return base.getFoodLevel(); }
    @Override public boolean needsFood() { return base.needsFood(); }
    @Override public void addExhaustion(float exhaustion) { base.addExhaustion(exhaustion); }
    @Override public float getExhaustionLevel() { return base.getExhaustionLevel(); }
    @Override public float getSaturationLevel() { return base.getSaturationLevel(); }
    @Override public void setFoodLevel(int foodLevel) { base.setFoodLevel(foodLevel); }
    @Override public void setSaturation(float saturationLevel) { base.setSaturation(saturationLevel); }
    @Override public void setExhaustion(float exhaustion) { base.setExhaustion(exhaustion); }

    public static Optional<FoodMetaData> ofPlayer(Player player) {
        return Optional.ofNullable(player)
                .map(v -> v instanceof CraftPlayer c ? c.getHandle() : null)
                .map(EntityHuman::getFoodData);
    }
}

















