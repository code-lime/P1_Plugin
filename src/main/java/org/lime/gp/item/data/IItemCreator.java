package org.lime.gp.item.data;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.lime.system;
import org.lime.gp.chat.Apply;

public abstract class IItemCreator {
    public boolean isDestroy = false;
    public abstract boolean updateReplace();
    public abstract String getKey();
    public abstract int getID();
    public abstract ItemStack createItem(int count, Apply apply);
    public abstract Stream<Material> getWhitelist();

    public abstract Optional<Integer> tryGetMaxStackSize();

    public final String stack;

    public IItemCreator() {
        stack = Stream.of(Thread.currentThread().getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n"));
    }

    public ItemStack createItem() { return createItem(1); }
    public ItemStack createItem(int count) { return createItem(count, Apply.of()); }
    public ItemStack createItem(Apply apply) { return createItem(1, apply); }
    public ItemStack createItem(system.Func1<Builder, Builder> builder) { return builder == null ? this.createItem(1) : builder.invoke(new Builder(this)).create(); }

    public static IItemCreator byMaterial(Material material) { return new MaterialCreator(material); }

    @Override public String toString() { return getClass().getSimpleName() + "^" + getKey(); }
}