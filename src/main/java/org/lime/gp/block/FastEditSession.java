package org.lime.gp.block;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.util.BlockStateListPopulator;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers;
import org.lime.gp.lime;

import java.util.*;

public class FastEditSession {
    private final World world;
    private final HashMap<BlockPosition, IBlockData> modified = new HashMap<>();

    public FastEditSession(World world) { this.world = world; }
    public static FastEditSession of(World world) { return new FastEditSession(world); }
    public static FastEditSession of(org.bukkit.World world) { return new FastEditSession(((CraftWorld)world).getHandle()); }

    public FastEditSession set(BlockPosition position, IBlockData data) { modified.put(position, data); return this; }
    public FastEditSession set(BlockPosition position, Block block) { return set(position, block.defaultBlockState()); }
    public FastEditSession set(BlockPosition position, Material material) { return set(position, CraftMagicNumbers.getBlock(material).defaultBlockState()); }

    public FastEditSession set(List<BlockPosition> position, IBlockData data) { position.forEach(pos -> modified.put(pos, data)); return this; }
    public FastEditSession set(List<BlockPosition> position, Block block) { return set(position, block.defaultBlockState()); }
    public FastEditSession set(List<BlockPosition> position, Material material) { return set(position, CraftMagicNumbers.getBlock(material).defaultBlockState()); }

    public FastEditSession update(int ticks) {
        List<Map.Entry<BlockPosition, IBlockData>> data = new ArrayList<>(modified.entrySet());
        modified.clear();

        int _ticks = Math.max(ticks, 1);
        lime.invokeAsync(() -> {
            List<List<Map.Entry<BlockPosition, IBlockData>>> parts = Lists.partition(data, data.size() / _ticks);

            int frames = parts.size();

            for (int i = 0; i < frames; i++) {
                List<Map.Entry<BlockPosition, IBlockData>> map = parts.get(i);
                BlockStateListPopulator populator = new BlockStateListPopulator(world);
                map.forEach(kv -> populator.setBlock(kv.getKey(), kv.getValue(), Block.UPDATE_ALL));
                lime.onceTicks(populator::updateList, i + 1);
            }
        }, () -> {});

        return this;
    }
}




















