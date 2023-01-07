package org.lime.gp.block.component.data;

import net.minecraft.world.level.block.state.properties.BlockPropertyStairsShape;
import org.lime.gp.block.BlockComponentInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.display.DisplayInstance;
import org.lime.gp.block.component.list.StairComponent;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

public class StairInstance extends BlockComponentInstance<StairComponent> {
    public StairInstance(StairComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
    }

    public InfoComponent.Rotation.Value rotation() {
        return metadata().list(DisplayInstance.class).findAny().flatMap(DisplayInstance::getRotation).orElse(InfoComponent.Rotation.Value.ANGLE_0);
    }
    public BlockPropertyStairsShape shape() {
        return metadata().list(DisplayInstance.class).findAny().flatMap(v -> v.get("shape")).map(v -> switch (v) {
            default /*straight*/ -> BlockPropertyStairsShape.STRAIGHT;
            case "inner_left" -> BlockPropertyStairsShape.INNER_LEFT;
            case "inner_right" ->BlockPropertyStairsShape.INNER_RIGHT;
            case "outer_left" -> BlockPropertyStairsShape.OUTER_LEFT;
            case "outer_right" -> BlockPropertyStairsShape.OUTER_RIGHT;
        }).orElse(BlockPropertyStairsShape.STRAIGHT);
    }
    public void shape(BlockPropertyStairsShape shape) {
        metadata().list(DisplayInstance.class).findAny().ifPresent(display -> display.set("shape", shape.getSerializedName()));
    }

    @Override public void read(JsonObjectOptional json) {

    }
    @Override public system.json.builder.object write() {
        return null;
    }
/*
    private static VoxelShape[] makeShapes(VoxelShape base, VoxelShape northWest, VoxelShape northEast, VoxelShape southWest, VoxelShape southEast) {
        return (VoxelShape[])IntStream.range(0, 16).mapToObj(i2 -> BlockStairs.makeStairShape(i2, base, northWest, northEast, southWest, southEast)).toArray(VoxelShape[]::new);
    }
    private static VoxelShape makeStairShape(int i2, VoxelShape base, VoxelShape northWest, VoxelShape northEast, VoxelShape southWest, VoxelShape southEast) {
        VoxelShape voxelShape = base;
        if ((i2 & 1) != 0) {
            voxelShape = VoxelShapes.or(voxelShape, northWest);
        }
        if ((i2 & 2) != 0) {
            voxelShape = VoxelShapes.or(voxelShape, northEast);
        }
        if ((i2 & 4) != 0) {
            voxelShape = VoxelShapes.or(voxelShape, southWest);
        }
        if ((i2 & 8) != 0) {
            voxelShape = VoxelShapes.or(voxelShape, southEast);
        }
        return voxelShape;
    }

    private int getShapeIndex(IBlockData state) {
        return state.getValue(SHAPE).ordinal() * 4 + state.getValue(FACING).get2DDataValue();
    }

    public IBlockData getStateForPlacement(BlockActionContext ctx) {
        EnumDirection direction = ctx.getClickedFace();
        BlockPosition blockPos = ctx.getClickedPos();
        Fluid fluidState = ctx.getLevel().getFluidState(blockPos);
        IBlockData blockState = (IBlockData)((IBlockData)((IBlockData)this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection())).setValue(HALF, direction == EnumDirection.DOWN || direction != EnumDirection.UP && ctx.getClickLocation().y - (double)blockPos.getY() > 0.5 ? BlockPropertyHalf.TOP : BlockPropertyHalf.BOTTOM)).setValue(WATERLOGGED, fluidState.getType() == FluidTypes.WATER);
        return (IBlockData)blockState.setValue(SHAPE, getStairsShape(blockState, ctx.getLevel(), blockPos));
    }

    public IBlockData updateShape(IBlockData state, EnumDirection direction, IBlockData neighborState, GeneratorAccess world, BlockPosition pos, BlockPosition neighborPos) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            world.scheduleTick(pos, FluidTypes.WATER, FluidTypes.WATER.getTickDelay(world));
        }
        if (direction.getAxis().isHorizontal()) {
            return (IBlockData)state.setValue(SHAPE, BlockStairs.getStairsShape(state, world, pos));
        }
        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    private static BlockPropertyStairsShape getStairsShape(StairInstance state, IBlockAccess world, BlockPosition pos) {
        EnumDirection direction3;
        EnumDirection direction2;
        EnumDirection direction = state.getValue(FACING);
        IBlockData blockState = world.getBlockState(pos.relative(direction));
        if (BlockStairs.isStairs(blockState) && state.getValue(HALF) == blockState.getValue(HALF) && (direction2 = blockState.getValue(FACING)).getAxis() != state.getValue(FACING).getAxis() && BlockStairs.canTakeShape(state, world, pos, direction2.getOpposite())) {
            if (direction2 == direction.getCounterClockWise()) {
                return BlockPropertyStairsShape.OUTER_LEFT;
            }
            return BlockPropertyStairsShape.OUTER_RIGHT;
        }
        IBlockData blockState2 = world.getBlockState(pos.relative(direction.getOpposite()));
        if (BlockStairs.isStairs(blockState2) && state.getValue(HALF) == blockState2.getValue(HALF) && (direction3 = blockState2.getValue(FACING)).getAxis() != state.getValue(FACING).getAxis() && BlockStairs.canTakeShape(state, world, pos, direction3)) {
            if (direction3 == direction.getCounterClockWise()) {
                return BlockPropertyStairsShape.INNER_LEFT;
            }
            return BlockPropertyStairsShape.INNER_RIGHT;
        }
        return BlockPropertyStairsShape.STRAIGHT;
    }

    private static boolean canTakeShape(IBlockData state, IBlockAccess world, BlockPosition pos, EnumDirection dir) {
        IBlockData blockState = world.getBlockState(pos.relative(dir));
        return !BlockStairs.isStairs(blockState) || blockState.getValue(FACING) != state.getValue(FACING) || blockState.getValue(HALF) != state.getValue(HALF);
    }

    public static boolean isStairs(IBlockData state) {
        return state.getBlock() instanceof BlockStairs;
    }

    public IBlockData rotate(IBlockData state, EnumBlockRotation rotation) {
        return (IBlockData)state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    public IBlockData mirror(IBlockData state, EnumBlockMirror mirror) {
        EnumDirection direction = state.getValue(FACING);
        BlockPropertyStairsShape stairsShape = state.getValue(SHAPE);
        switch (mirror) {
            case LEFT_RIGHT: {
                if (direction.getAxis() != EnumDirection.EnumAxis.Z) break;
                switch (stairsShape) {
                    case INNER_LEFT: {
                        return (IBlockData)state.rotate(EnumBlockRotation.CLOCKWISE_180).setValue(SHAPE, BlockPropertyStairsShape.INNER_RIGHT);
                    }
                    case INNER_RIGHT: {
                        return (IBlockData)state.rotate(EnumBlockRotation.CLOCKWISE_180).setValue(SHAPE, BlockPropertyStairsShape.INNER_LEFT);
                    }
                    case OUTER_LEFT: {
                        return (IBlockData)state.rotate(EnumBlockRotation.CLOCKWISE_180).setValue(SHAPE, BlockPropertyStairsShape.OUTER_RIGHT);
                    }
                    case OUTER_RIGHT: {
                        return (IBlockData)state.rotate(EnumBlockRotation.CLOCKWISE_180).setValue(SHAPE, BlockPropertyStairsShape.OUTER_LEFT);
                    }
                }
                return state.rotate(EnumBlockRotation.CLOCKWISE_180);
            }
            case FRONT_BACK: {
                if (direction.getAxis() != EnumDirection.EnumAxis.X) break;
                switch (stairsShape) {
                    case INNER_LEFT: {
                        return (IBlockData)state.rotate(EnumBlockRotation.CLOCKWISE_180).setValue(SHAPE, BlockPropertyStairsShape.INNER_LEFT);
                    }
                    case INNER_RIGHT: {
                        return (IBlockData)state.rotate(EnumBlockRotation.CLOCKWISE_180).setValue(SHAPE, BlockPropertyStairsShape.INNER_RIGHT);
                    }
                    case OUTER_LEFT: {
                        return (IBlockData)state.rotate(EnumBlockRotation.CLOCKWISE_180).setValue(SHAPE, BlockPropertyStairsShape.OUTER_RIGHT);
                    }
                    case OUTER_RIGHT: {
                        return (IBlockData)state.rotate(EnumBlockRotation.CLOCKWISE_180).setValue(SHAPE, BlockPropertyStairsShape.OUTER_LEFT);
                    }
                    case STRAIGHT: {
                        return state.rotate(EnumBlockRotation.CLOCKWISE_180);
                    }
                }
                break;
            }
        }
        return super.mirror(state, mirror);
    }

    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> builder) {
        builder.add(FACING, HALF, SHAPE, WATERLOGGED);
    }

    public Fluid getFluidState(IBlockData state) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            return FluidTypes.WATER.getSource(false);
        }
        return super.getFluidState(state);
    }

    public boolean isPathfindable(IBlockData state, IBlockAccess world, BlockPosition pos, PathMode type) {
        return false;
    }*/
}
