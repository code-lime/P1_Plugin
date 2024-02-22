package org.lime.gp.module;

import net.minecraft.tags.TagsBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.block.CraftBlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.StructureGrowEvent;
import org.lime.plugin.CoreElement;
import org.lime.system.utils.RandomUtils;

import java.util.List;

public class TreeGenerator implements Listener {
    public static CoreElement create() {
        return CoreElement.create(TreeGenerator.class)
                .withInstance();
    }

    private static final Material[] RANDOM_REPLACE_BLOCKS = new Material[] {
            Material.ROOTED_DIRT,
            Material.COARSE_DIRT
    };
    private static void addRandomReplaceBlocks(CraftWorld world, int x, int y, int z, List<BlockState> states) {
        for (int _x = -2; _x <= 2; _x++) {
            for (int _y = -1; _y <= 0; _y++)
                for (int _z = -2; _z <= 2; _z++) {
                    int __x = x + _x;
                    int __y = y + _y;
                    int __z = z + _z;

                    Block block = world.getBlockAt(__x, __y, __z);
                    if (switch (block.getType()) {
                        case DIRT, GRASS_BLOCK, PODZOL -> false;
                        default -> true;
                    }) continue;
                    if ((_x != 0 || _y != 0 || _z != 0) && RandomUtils.rand())
                        continue;

                    states.removeIf(v -> v.getX() == __x && v.getY() == __y && v.getZ() == __z);
                    BlockState state = block.getState();
                    state.setType(RandomUtils.rand(RANDOM_REPLACE_BLOCKS));
                    states.add(state);
                }
        }
    }

    @EventHandler public static void on(StructureGrowEvent e) {
        Location location = e.getLocation();
        int x = location.getBlockX();
        int y = location.getBlockY() - 1;
        int z = location.getBlockZ();
        CraftWorld world = (CraftWorld) location.getWorld();
        Block block = world.getBlockAt(x, y, z);
        switch (block.getType()) {
            case DIRT:
            case GRASS_BLOCK:
            case PODZOL: break;
            default:
                e.setCancelled(true);
                return;
        }
        List<BlockState> states = e.getBlocks();
        if (states.isEmpty()) return;
        boolean empty = true;
        for (BlockState state : states) {
            if (state instanceof CraftBlockState handleState && !handleState.getHandle().is(TagsBlock.SAPLINGS)) {
                empty = false;
                break;
            }
        }
        if (empty) return;
        addRandomReplaceBlocks(world, x, y, z, states);
        states.forEach(state -> {
            if (state.getType() == Material.OAK_LEAVES && RandomUtils.rand_is(0.10))
                state.setType(RandomUtils.rand_is(0.30) ? Material.FLOWERING_AZALEA_LEAVES : Material.AZALEA_LEAVES);
        });
    }
}
