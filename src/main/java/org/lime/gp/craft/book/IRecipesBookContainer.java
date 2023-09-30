package org.lime.gp.craft.book;

import org.lime.gp.craft.recipe.IDisplayRecipe;

import java.util.Collection;
import java.util.stream.Stream;

public interface IRecipesBookContainer {
    Stream<? extends IDisplayRecipe> getRecipesCustom();
}
