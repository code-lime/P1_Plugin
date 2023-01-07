package org.lime.gp.entity.collision;


import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class HeightReader {
    private final World world;
    private final HashMap<BlockPosition, Optional<Double>> heights = new HashMap<>();
    private final HashMap<BlockPosition, Optional<List<BoxSphereCollider.Box>>> boxes = new HashMap<>();
    private final HashMap<BlockPosition, Optional<List<AxisAlignedBB>>> aabbs = new HashMap<>();
    public HeightReader(World world) {
        this.world = world;
    }

    public World world() { return this.world; }
    public Optional<Double> height(BlockPosition pos) {
        return Optional.ofNullable(heights.get(pos))
                .orElseGet(() -> {
                    Optional<Double> val = Optional.ofNullable(world.getBlockStateIfLoaded(pos))
                            .map(v -> v.getCollisionShape(world, pos))
                            .map(_v -> Optional.of(_v)
                                    .filter(v -> !v.isEmpty())
                                    .map(v -> v.max(EnumDirection.EnumAxis.Y))
                                    .filter(v -> v != Double.NEGATIVE_INFINITY)
                                    .map(v -> Math.min(1, v))
                                    .orElse(0.0)
                            );
                    heights.put(pos, val);
                    return val;
                });
    }
    public Optional<List<BoxSphereCollider.Box>> boxes(BlockPosition pos) {
        return Optional.ofNullable(boxes.get(pos))
                .orElseGet(() -> {
                    Optional<List<BoxSphereCollider.Box>> val = Optional.ofNullable(world.getBlockStateIfLoaded(pos))
                            .map(v -> v.getCollisionShape(world, pos))
                            .map(VoxelShape::toAabbs)
                            .filter(v -> !v.isEmpty())
                            .map(aabbs -> aabbs.stream().map(aabb -> BoxSphereCollider.Box.of(pos, aabb)).toList());
                    boxes.put(pos, val);
                    return val;
                });
    }
    public Optional<List<AxisAlignedBB>> aabbs(BlockPosition pos) {
        return Optional.ofNullable(aabbs.get(pos))
                .orElseGet(() -> {
                    Optional<List<AxisAlignedBB>> val = Optional.ofNullable(world.getBlockStateIfLoaded(pos))
                            .map(v -> v.getCollisionShape(world, pos))
                            .map(VoxelShape::toAabbs)
                            .filter(v -> !v.isEmpty());
                    aabbs.put(pos, val);
                    return val;
                });
    }
}



















