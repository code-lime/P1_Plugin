package org.lime.gp.module;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.lime;
import org.lime.system;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class WorldGenModify {
    public static CoreElement create() {
        return CoreElement.create(WorldGenModify.class)
                .withInit(WorldGenModify::init);
    }
    private static final boolean DEBUG = false;
    private static final EnumDirection[] dirs = EnumDirection.values();
    private static final BlockPopulator BLOCK_POPULATOR = new BlockPopulator() {
        /*private static WorldServer of(LimitedRegion limitedRegion) { return ((CraftLimitedRegion)limitedRegion).getHandle().getMinecraftWorld(); }
        @Override public void populate(WorldInfo worldInfo, Random random, int chunk_x, int chunk_z, LimitedRegion limitedRegion) {
            populate(worldInfo, random, chunk_x, chunk_z, of(limitedRegion), (pos) -> !limitedRegion.isInRegion(pos.getX(), pos.getY(), pos.getZ()));
        }*/
        @Override public void populate(WorldInfo worldInfo, Random random, int chunk_x, int chunk_z, LimitedRegion limitedRegion) {
            int offset_x = chunk_x * 16;
            int offset_y = worldInfo.getMinHeight();
            int offset_z = chunk_z * 16;

            String offset_key = offset_x + " " + offset_z;
            if (DEBUG) lime.logOP("["+offset_key+"] Generate chunk...");

            Set<BlockPosition> positions = new HashSet<>();
            for (int y = 10; y <= 40; y += 10) {
                if (DEBUG)  lime.logOP("["+offset_key+"] Generate ore positions " + (y / 10) + " / " + 4 + "...");
                positions.clear();
                BlockPosition pos = new BlockPosition(offset_x + random.nextInt(0, 16), y + random.nextInt(-8, 8), offset_z + random.nextInt(0, 16));
                for (int i = 0; i < 10; i++) {
                    pos = pos.relative(system.rand(dirs));
                    positions.add(pos);
                }
            }

            positions.removeIf(v -> !limitedRegion.isInRegion(v.getX(), v.getY(), v.getZ()));
            if (DEBUG) lime.logOP("["+offset_key+"] Set ore blocks...");
            for (BlockPosition _pos : positions) {
                if (DEBUG)  lime.logOP("["+offset_key+"] Set ore block "+_pos+"...");
                if (limitedRegion.getType(_pos.getX(), _pos.getY(), _pos.getZ()) != Material.STONE) continue;
                limitedRegion.setType(_pos.getX(), _pos.getY(), _pos.getZ(), Material.NETHER_QUARTZ_ORE);
                if (DEBUG) lime.logOP("["+offset_key+"] Set ore block "+_pos+"...OK!");
            }

            positions.clear();
            if (DEBUG) lime.logOP("["+offset_key+"] Generate bedrock...");
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++)
                    for (int y = 0; y < 5; y++)
                        positions.add(new BlockPosition(offset_x+x, offset_y+y, offset_z+z));
            positions.removeIf(v -> !limitedRegion.isInRegion(v.getX(), v.getY(), v.getZ()));

            if (DEBUG)  lime.logOP("["+offset_key+"] Set bedrock...");
            positions.forEach(pos -> limitedRegion.setType(pos.getX(), pos.getY(), pos.getZ(), Material.BEDROCK));
            if (DEBUG)  lime.logOP("["+offset_key+"] Generate chunk...OK!");
        }

        @Override public String toString() { return "lime:BLOCK_POPULATOR#0"; }
    };
    public static void init() {
        lime.MainWorld.getPopulators().removeIf(v -> v.toString().equals(BLOCK_POPULATOR.toString()));
        lime.MainWorld.getPopulators().add(BLOCK_POPULATOR);
    }
}
