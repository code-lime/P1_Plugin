package org.lime.gp.player.module.needs.food;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import org.lime.gp.extension.JsonNBT;
import org.lime.gp.lime;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

public interface IFoodLevel {
    void addLevel(FoodType type, float level); //this.foodLevel = Math.min(food + this.foodLevel, 20);
    float totalLevel();
    boolean needsFood();
    void moveTo(float food);
    boolean moveDelta(float deltaFood);

    String type();
    void load(JsonObjectOptional json);
    system.json.builder.object save();
    boolean isVanilla();

    default void addSaveData(NBTTagCompound tag) {
        tag.putString("type", type());
        tag.putString("raw", save().build().toString());
    }
    default void readSaveData(NBTTagCompound tag) {
        if (!tag.contains("type", NBTBase.TAG_STRING) || !tag.getString("type").equals(type())) return;
        JsonObjectOptional raw = JsonObjectOptional.of(system.json.parse(tag.getString("raw")).getAsJsonObject());
        load(raw);
    }

    static float limit(FoodType type, float value) {
        return value + 0.0001 > type.maxCount ? type.maxCount : value - 0.0001 < 0 ? 0 : value;
    }
}








