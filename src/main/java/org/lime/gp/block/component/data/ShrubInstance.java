package org.lime.gp.block.component.data;

import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.list.ShrubComponent;

public class ShrubInstance extends BaseAgeableInstance<ShrubComponent> implements CustomTileMetadata.Interactable {
    public ShrubInstance(ShrubComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
    }

    @Override public AgeableData ageableData() {
        return component();
    }

    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        ShrubComponent component = component();
        int age = age();
        if (age != component.limitAge()) return EnumInteractionResult.PASS;

        World world = event.world();
        BlockPosition pos = event.pos();
        component.loot.generate().forEach(item -> Block.popResource(world, pos, CraftItemStack.asNMSCopy(item)));
        world.playSound(null, pos, SoundEffects.SWEET_BERRY_BUSH_PICK_BERRIES, SoundCategory.BLOCKS, 1.0F, 0.8F + world.random.nextFloat() * 0.4F);
        age--;
        age(age);
        return EnumInteractionResult.sidedSuccess(world.isClientSide);
    }
}


















