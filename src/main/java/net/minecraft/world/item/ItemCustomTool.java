package net.minecraft.world.item;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;

public class ItemCustomTool extends ItemToolMaterial {
    private static ToolMaterial createTool(Item.Info settings) {
        int uses = settings.maxDamage;
        return new ToolMaterial() {
            @Override public int getUses() { return uses; }
            @Override public float getSpeed() { return 0; }
            @Override public float getAttackDamageBonus() { return 0; }
            @Override public int getLevel() { return 0; }
            @Override public int getEnchantmentValue() { return 0; }
            @Override public RecipeItemStack getRepairIngredient() { return RecipeItemStack.EMPTY; }
        };
    }

    public ItemCustomTool(Item.Info settings, EntityTypes<?> target, int damagePerUse) {
        super(createTool(settings), settings);
    }

    @Override public boolean hurtEnemy(ItemStack stack, EntityLiving target, EntityLiving attacker) {
        stack.hurtAndBreak(1, attacker, e2 -> e2.broadcastBreakEvent(EnumItemSlot.MAINHAND));
        return true;
    }
    @Override public boolean mineBlock(ItemStack stack, World world, IBlockData state, BlockPosition pos, EntityLiving miner) {
        if (!world.isClientSide) stack.hurtAndBreak(1, miner, e2 -> e2.broadcastBreakEvent(EnumItemSlot.MAINHAND));
        return true;
    }

    /*
    @Override public boolean isCorrectToolForDrops(IBlockData state) {
        int i2 = this.getTier().getLevel();
        if (i2 < 3 && state.is(TagsBlock.NEEDS_DIAMOND_TOOL)) {
            return false;
        }
        if (i2 < 2 && state.is(TagsBlock.NEEDS_IRON_TOOL)) {
            return false;
        }
        if (i2 < 1 && state.is(TagsBlock.NEEDS_STONE_TOOL)) {
            return false;
        }
        return state.is(this.blocks);
    }
    */
}