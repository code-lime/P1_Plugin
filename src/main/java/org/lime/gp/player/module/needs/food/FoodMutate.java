package org.lime.gp.player.module.needs.food;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayOutUpdateHealth;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import org.bukkit.craftbukkit.v1_20_R1.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.lime.gp.extension.JsonNBT;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.FoodSetting;
import org.lime.gp.lime;
import org.lime.gp.player.module.needs.NeedSystem;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FoodMutate {
    public final IFoodLevel food;
    private final EntityHuman human;
    
    private int lastFoodLevel = 20;

    private float saturationLevel = 5.0f;
    private float exhaustionLevel;
    private int tickTimer;
    private int saturatedRegenRate = 10;
    private int unsaturatedRegenRate = 80;
    private int starvationRate = 80;

    private float deltaMutate = 1;

    public boolean isVanilla() {
        return food.isVanilla();
    }

    public FoodMutate(EntityHuman human, NBTTagCompound tag, Action0 onChange) {
        this.food = FoodType.IsVanilla ? new VanillaFoodLevel(onChange) : new TypedFoodLevel(onChange);
        this.human = human;
        readAdditionalSaveData(tag);
    }

    public void eat(FoodType type, float food, float saturationModifier) {
        if (!this.food.addLevel(type, food)) return;
        this.saturationLevel = Math.min(this.saturationLevel + food * saturationModifier * 2.0f, this.food.totalLevel());
    }
    public void eatDelta(float food, float saturationModifier) {
        this.food.moveDelta(food);
        this.saturationLevel = Math.min(this.saturationLevel + food * saturationModifier * 2.0f, this.food.totalLevel());
    }
    public void eat(Item item, ItemStack stack) {
        if (!item.isEdible()) return;
        Items.getOptional(FoodSetting.class, stack)
                .<Map<FoodType, FoodSetting.Info>>map(v -> v.types)
                .orElseGet(() -> {
                    if (Items.hasIDByItem(stack)) return Collections.singletonMap(FoodType.Vanilla, FoodSetting.Info.of(item.getFoodProperties()));
                    HashMap<FoodType, FoodSetting.Info> types = new HashMap<>();
                    types.put(FoodType.Vanilla, FoodSetting.Info.of(item.getFoodProperties()));
                    Map<FoodType, FoodSetting.Info> _types = FoodType.MaterialFood.get(CraftMagicNumbers.getMaterial(stack.getItem()));
                    if (_types != null) types.putAll(_types);
                    return types;
                })
                .forEach((type, info) -> {
                    float oldValue = this.food.totalLevel();
                    float initValue = info.value() + oldValue;
                    int intValue = (int) initValue;
                    FoodLevelChangeEvent event = CraftEventFactory.callFoodLevelChangeEvent(this.human, intValue, stack);
                    if (!event.isCancelled())
                        this.eat(type, (event.getFoodLevel() != intValue ? event.getFoodLevel() : initValue) - oldValue, info.saturation());
                });
        ((EntityPlayer) this.human).getBukkitEntity().sendHealthUpdate();
    }
    public void tick(EntityHuman human) {
        EntityPlayer player = (EntityPlayer)human;
        EnumDifficulty enumdifficulty = player.level().getDifficulty();
        this.lastFoodLevel = (int) this.food.totalLevel();
        if (this.exhaustionLevel > 4.0f) {
            this.exhaustionLevel -= 4.0f;
            float deltaMutate = this.deltaMutate * (float) NeedSystem.getFoodMutate(player.getBukkitEntity());
            if (this.saturationLevel > 0.0f) {
                this.saturationLevel = Math.max(this.saturationLevel - FoodType.SaturationStep * deltaMutate, 0.0f);
            } else if (enumdifficulty != EnumDifficulty.PEACEFUL) {
                float oldValue = this.food.totalLevel();
                float initValue = Math.max(oldValue - FoodType.FoodStep * deltaMutate, 0);
                int intValue = (int) initValue;

                FoodLevelChangeEvent event = CraftEventFactory.callFoodLevelChangeEvent(player, intValue);
                if (!event.isCancelled())
                    this.food.moveTo(event.getFoodLevel() != intValue ? event.getFoodLevel() : initValue);
                player.connection.send(new PacketPlayOutUpdateHealth(player.getBukkitEntity().getScaledHealth(), (int) this.food.totalLevel(), this.saturationLevel));
            }
        }

        float totalFoodLevel = this.food.totalLevel();

        this.saturationLevel = Math.min(this.saturationLevel, totalFoodLevel);
        boolean naturalRegeneration = player.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION);
        if (naturalRegeneration && this.saturationLevel > 0.0f && player.isHurt() && totalFoodLevel >= 20) {
            ++this.tickTimer;
            if (this.tickTimer >= this.saturatedRegenRate) {
                float f2 = Math.min(this.saturationLevel, 6.0f);
                player.heal(f2 / 6.0f, EntityRegainHealthEvent.RegainReason.SATIATED, true);
                player.causeFoodExhaustion(f2, EntityExhaustionEvent.ExhaustionReason.REGEN);
                this.tickTimer = 0;
            }
        } else if (naturalRegeneration && totalFoodLevel >= 18 && player.isHurt()) {
            ++this.tickTimer;
            if (this.tickTimer >= this.unsaturatedRegenRate) {
                player.heal(1.0f, EntityRegainHealthEvent.RegainReason.SATIATED);
                player.causeFoodExhaustion(player.level().spigotConfig.regenExhaustion, EntityExhaustionEvent.ExhaustionReason.REGEN);
                this.tickTimer = 0;
            }
        } else if (totalFoodLevel <= 0) {
            ++this.tickTimer;
            if (this.tickTimer >= this.starvationRate) {
                if (player.getHealth() > 10.0f || enumdifficulty == EnumDifficulty.HARD || player.getHealth() > 1.0f && enumdifficulty == EnumDifficulty.NORMAL) {
                    player.hurt(player.damageSources().starve(), 1.0f);
                }
                this.tickTimer = 0;
            }
        } else {
            this.tickTimer = 0;
        }
    }
    public boolean modify(float food, float saturation) {
        boolean isModify = food != 0 && this.food.moveDelta(food);
        if (saturation == 0) return isModify;
        float oldSaturation = this.saturationLevel;
        this.saturationLevel = Math.min(this.saturationLevel + saturation, this.food.totalLevel());
        return isModify || oldSaturation != this.saturationLevel;
    }
    public void readAdditionalSaveData(NBTTagCompound nbt) {
        if (!nbt.contains("foodLevel", NBTBase.TAG_ANY_NUMERIC)) return;
        NBTTagCompound foodNative = nbt.getCompound("foodNative");
        this.food.readSaveData(foodNative);

        //this.foodLevel = nbt.getInt("foodLevel"); UNUSED
        this.tickTimer = nbt.getInt("foodTickTimer");
        this.saturationLevel = nbt.getFloat("foodSaturationLevel");
        this.exhaustionLevel = nbt.getFloat("foodExhaustionLevel");
    }
    public void addAdditionalSaveData(NBTTagCompound nbt) {
        NBTTagCompound foodNative = new NBTTagCompound();
        this.food.addSaveData(foodNative);
        nbt.put("foodNative", foodNative);

        nbt.putInt("foodLevel", (int) this.food.totalLevel());
        nbt.putInt("foodTickTimer", this.tickTimer);
        nbt.putFloat("foodSaturationLevel", this.saturationLevel);
        nbt.putFloat("foodExhaustionLevel", this.exhaustionLevel);
    }
    public int getFoodLevel() {
        return (int) this.food.totalLevel();
    }
    public int getLastFoodLevel() {
        return this.lastFoodLevel;
    }
    public boolean needsFood() {
        return this.food.needsFood();
    }
    public void addExhaustion(float exhaustion) {
        this.exhaustionLevel = Math.min(this.exhaustionLevel + exhaustion, 40.0f);
    }
    public float getExhaustionLevel() {
        return this.exhaustionLevel;
    }
    public float getSaturationLevel() {
        return this.saturationLevel;
    }
    public void setFoodLevel(int foodLevel) {
        this.food.moveTo(foodLevel);
    }
    public void setSaturation(float saturationLevel) {
        this.saturationLevel = saturationLevel;
    }
    public void setExhaustion(float exhaustion) {
        this.exhaustionLevel = exhaustion;
    }
    public void setSaturatedRegenRate(int saturatedRegenRate) {
        this.saturatedRegenRate = saturatedRegenRate;
    }
    public void setUnsaturatedRegenRate(int unsaturatedRegenRate) {
        this.unsaturatedRegenRate = unsaturatedRegenRate;
    }
    public void setStarvationRate(int starvationRate) {
        this.starvationRate = starvationRate;
    }
    public int getSaturatedRegenRate() {
        return this.saturatedRegenRate;
    }
    public int getUnsaturatedRegenRate() {
        return this.unsaturatedRegenRate;
    }
    public int getStarvationRate() { return this.starvationRate; }
    public void setDeltaMutate(float deltaMutate) { this.deltaMutate = deltaMutate; }
    public float getDeltaMutate() { return this.deltaMutate; }
}
