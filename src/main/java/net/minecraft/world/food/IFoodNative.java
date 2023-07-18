package net.minecraft.world.food;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IFoodNative {
    static void readNativeSaveData(IFoodNative food, NBTTagCompound nbt) {
        food.nativeData(nbt.contains("foodNative", NBTBase.TAG_COMPOUND)
                ? nbt.getCompound("foodNative")
                : new NBTTagCompound());
    }
    static void addNativeSaveData(IFoodNative food, NBTTagCompound nbt) {
        nbt.put("foodNative", food.nativeData());
    }

    @Nullable NBTTagCompound nullableNativeData();
    default @Nonnull NBTTagCompound nativeData() {
        NBTTagCompound nativeData = nullableNativeData();
        if (nativeData == null) nativeData(nativeData = new NBTTagCompound());
        return nativeData;
    }
    void nativeData(@Nonnull NBTTagCompound nbt);

    class Example implements IFoodNative {
        @Nullable private NBTTagCompound nativeData;

        @Override public @Nullable NBTTagCompound nullableNativeData() { return nativeData; }
        @Override public void nativeData(@Nonnull NBTTagCompound nbt) { nativeData = nbt; }

        public void readAdditionalSaveData(NBTTagCompound nbt) { readNativeSaveData(this, nbt); }
        public void addAdditionalSaveData(NBTTagCompound nbt) { addNativeSaveData(this, nbt); }
    }
}
