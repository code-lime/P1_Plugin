package org.lime.gp.craft.book;

import org.lime.gp.craft.recipe.IDisplayRecipe;

import java.util.Collection;

public interface IRecipesBookContainer {
    Collection<? extends IDisplayRecipe> getRecipesCustom();
}
