package org.lime.gp.block.component.data;

import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.list.ShrubComponent;
import org.lime.gp.module.loot.IPopulateLoot;
import org.lime.gp.module.loot.ModifyLootTable;
import org.lime.gp.module.loot.Parameters;
import org.lime.gp.player.level.LevelModule;

import java.util.List;
import java.util.UUID;

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

        EnumHand hand = event.hand();
        EntityHuman player = event.player();
        UUID uuid = player.getUUID();
        World world = event.world();
        BlockPosition pos = event.pos();

        net.minecraft.world.item.ItemStack itemStack = player.getItemInHand(hand);
        IPopulateLoot loot = IPopulateLoot.of(world, List.of(
                IPopulateLoot.var(Parameters.ThisEntity, player),
                IPopulateLoot.var(Parameters.BlockEntity, metadata.skull),
                IPopulateLoot.var(Parameters.BlockState, event.state()),
                IPopulateLoot.var(Parameters.Origin, event.pos().getCenter()),
                IPopulateLoot.var(Parameters.Tool, itemStack)
        ));
        String key = "shrub/" + component.info().getKey().toLowerCase();
        LevelModule.onHarvest(uuid, key);
        ModifyLootTable.getLoot(uuid, key, component.loot, loot)
                .generateLoot(loot)
                .forEach(item -> Block.popResource(world, pos, CraftItemStack.asNMSCopy(item)));

        world.playSound(null, pos, SoundEffects.SWEET_BERRY_BUSH_PICK_BERRIES, SoundCategory.BLOCKS, 1.0F, 0.8F + world.random.nextFloat() * 0.4F);
        age -= component.ageRemove;
        age(age);
        return EnumInteractionResult.sidedSuccess(world.isClientSide);
    }
}


















