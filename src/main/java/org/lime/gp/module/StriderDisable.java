package org.lime.gp.module;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeItemStack;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.access.ReflectionAccess;

public class StriderDisable {
    public static CoreElement create() {
        return CoreElement.create(StriderDisable.class)
                .withInit(StriderDisable::init);
    }
    
    private static void init() {
        //ReflectionAccess.canInteractWith_ItemCarrotStick.set(Items.WARPED_FUNGUS_ON_A_STICK, null);
        ReflectionAccess.TEMPT_ITEMS_EntityStrider.set(null, RecipeItemStack.of(Items.WARPED_FUNGUS));
    }
}
