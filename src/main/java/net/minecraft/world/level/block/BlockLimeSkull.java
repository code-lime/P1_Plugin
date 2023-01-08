package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;

import javax.annotation.Nullable;
import java.util.Optional;

public class BlockLimeSkull extends BlockSkull implements IFluidContainer {
    protected BlockLimeSkull(a type, Info settings) {
        super(type, settings);
    }

    @Override public boolean hasDynamicShape() { return true; }
    @Override public TileEntity newBlockEntity(BlockPosition pos, IBlockData state) {
        return new TileEntityLimeSkull(pos, state);
    }

    private static @Nullable TileEntityLimeSkull of(IBlockAccess world, BlockPosition pos) {
        return world.getBlockEntity(pos) instanceof TileEntityLimeSkull skull ? skull : null;
    }

    //Async not supported!
    @Override public VoxelShape getShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        TileEntityLimeSkull skull = of(world, pos);
        if (skull == null) return super.getShape(state, world, pos, context);
        VoxelShape shape = skull.onShape(new BlockSkullShapeInfo(this, state, world, pos, context));
        return shape == null ? super.getShape(state, world, pos, context) : shape;
    }
    //Async not supported!
    @Override public VoxelShape getCollisionShape(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
        TileEntityLimeSkull skull = of(world, pos);
        if (skull == null) return super.getShape(state, world, pos, context);
        VoxelShape shape = skull.onShape(new BlockSkullShapeInfo(this, state, world, pos, context));
        return shape == null ? super.getShape(state, world, pos, context) : shape;
    }
    @Override public IBlockData getStateForPlacement(BlockActionContext ctx) {
        TileEntityLimeSkull skull = of(ctx.getLevel(), ctx.getClickedPos());
        IBlockData data = super.getStateForPlacement(ctx);
        if (skull == null) return data;
        IBlockData newData = skull.onPlace(new BlockSkullPlaceInfo(this, ctx.getLevel(), ctx.getClickedPos(), data, ctx.getPlayer(), ctx.getItemInHand()));
        return newData == null ? data : newData;
    }
    @Override public EnumInteractionResult use(IBlockData state, World world, BlockPosition pos, EntityHuman player, EnumHand hand, MovingObjectPositionBlock hit) {
        TileEntityLimeSkull skull = of(world, pos);
        if (skull == null) return EnumInteractionResult.PASS;
        EnumInteractionResult shape = skull.onInteract(new BlockSkullInteractInfo(this, state, world, pos, player, hand, hit));
        return shape == null ? EnumInteractionResult.PASS : shape;
    }
    @Override public <T extends TileEntity> BlockEntityTicker<T> getTicker(World world, IBlockData state, TileEntityTypes<T> type) {
        return type != TileEntityTypes.SKULL ? super.getTicker(world, state, type) : (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof TileEntityLimeSkull tileEntity)
                tileEntity.onTick(world1, pos, state1);
        };
    }
    @Override public float getDestroyProgress(IBlockData state, EntityHuman player, IBlockAccess world, BlockPosition pos) {
        TileEntityLimeSkull skull = of(world, pos);
        if (skull == null) return state.getDestroySpeed(world, pos);
        IBlockData newState = skull.onState(new BlockSkullStateInfo(this, world, pos, player));
        if (newState != null) state = newState;
        float f2 = state.getDestroySpeed(world, pos);
        if (f2 == -1.0f) return 0.0f;
        int i2 = player.hasCorrectToolForDrops(state) ? 30 : 100;
        return player.getDestroySpeed(state) / f2 / (float)i2;
    }

    @Override public void playerWillDestroy(World world, BlockPosition pos, IBlockData state, EntityHuman player) {
        TileEntityLimeSkull skull = of(world, pos);
        if (skull != null) skull.onDestroy(new BlockSkullDestroyInfo(this, state, world, pos, Optional.of(player)));
        super.playerWillDestroy(world, pos, state, player);
    }
    @Override @Deprecated public void onRemove(IBlockData state, World world, BlockPosition pos, IBlockData newState, boolean moved) {
        if (state.is(newState.getBlock())) return;
        TileEntityLimeSkull skull = of(world, pos);
        if (skull != null) skull.onDestroy(new BlockSkullDestroyInfo(this, state, world, pos, Optional.empty()));
        super.onRemove(state, world, pos, newState, moved);
    }
    @Override public void setPlacedBy(World world, BlockPosition pos, IBlockData state, @Nullable EntityLiving placer, ItemStack itemStack) {
        TileEntityLimeSkull skull = of(world, pos);
        if (skull != null) skull.onPlace(new BlockSkullPlaceInfo(this, world, pos, state, placer, itemStack));
        super.setPlacedBy(world, pos, state, placer, itemStack);
    }

    @Override public boolean canPlaceLiquid(IBlockAccess world, BlockPosition pos, IBlockData state, FluidType fluid) { return false; }
    @Override public boolean placeLiquid(GeneratorAccess world, BlockPosition pos, IBlockData state, Fluid fluidState) { return false; }
}










