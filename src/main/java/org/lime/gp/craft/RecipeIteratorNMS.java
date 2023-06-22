package org.lime.gp.craft;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.IRecipe;
import net.minecraft.world.item.crafting.Recipes;

import java.util.Iterator;
import java.util.Map;

public class RecipeIteratorNMS implements Iterator<IRecipe<?>> {
    private final Iterator<Map.Entry<Recipes<?>, Object2ObjectLinkedOpenHashMap<MinecraftKey, IRecipe<?>>>> recipes;
    private Iterator<IRecipe<?>> current;
    private IRecipe<?> currentRecipe;

    public RecipeIteratorNMS() {
        this.recipes = MinecraftServer.getServer().getRecipeManager().recipes.entrySet().iterator();
    }

    @Override
    public boolean hasNext() {
        if (this.current != null && this.current.hasNext()) return true;
        if (this.recipes.hasNext()) {
            this.current = this.recipes.next().getValue().values().iterator();
            return this.hasNext();
        }
        return false;
    }

    @Override
    public IRecipe<?> next() {
        if (this.current == null || !this.current.hasNext()) {
            this.current = this.recipes.next().getValue().values().iterator();
            this.currentRecipe = this.next();
            return this.currentRecipe;
        }
        this.currentRecipe = this.current.next();
        return this.currentRecipe;
    }

    @Override
    public void remove() {
        if (this.current == null) throw new IllegalStateException("next() not yet called");
        MinecraftServer.getServer().getRecipeManager().byName.remove(this.currentRecipe.getId());
        this.current.remove();
    }
}

