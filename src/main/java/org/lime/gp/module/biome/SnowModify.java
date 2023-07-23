package org.lime.gp.module.biome;

import net.minecraft.core.BlockPosition;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeTemperatureEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockSnow;
import net.minecraft.world.level.block.BlockSnowTickEvent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.craftbukkit.v1_19_R3.event.CraftEventFactory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.lime.core;

public class SnowModify implements Listener {
    public static core.element create() {
        return core.element.create(SnowModify.class)
                .withInstance();
    }

    private static boolean enableSnow;
    public static void setSnow(boolean enableSnow) {
        SnowModify.enableSnow = enableSnow;

        MinecraftServer.getServer().getAllLevels().forEach(world -> {
            if (enableSnow) {
                world.paperConfig().environment.frostedIce.enabled = false;
                world.paperConfig().environment.frostedIce.delay.min = 20000;
                world.paperConfig().environment.frostedIce.delay.max = 40000;
            } else {
                world.paperConfig().environment.frostedIce.enabled = true;
                world.paperConfig().environment.frostedIce.delay.min = 2000;
                world.paperConfig().environment.frostedIce.delay.max = 4000;
            }
        });
    }

    @EventHandler private static void on(BlockSnowTickEvent e) {
        if (SnowModify.enableSnow) return;
        BlockPosition position = e.getPos();
        WorldServer world = e.getWorld();
        BiomeBase biome = world.getBiome(position).value();
        if (biome.coldEnoughToSnow(position)) return;
        IBlockData oldBlock = e.getState();
        int layer = Math.max(oldBlock.getValue(BlockSnow.LAYERS) - 1, 0);
        IBlockData newBlock = layer == 0 ? Blocks.AIR.defaultBlockState() : oldBlock.setValue(BlockSnow.LAYERS, layer);
        if (CraftEventFactory.callBlockFadeEvent(world, position, newBlock).isCancelled()) return;
        BlockSnow.dropResources(oldBlock, world, position);
        if (newBlock.isAir()) world.removeBlock(position, false);
        else world.setBlock(position, newBlock, Block.UPDATE_ALL);
    }
    @EventHandler private static void on(BiomeTemperatureEvent e) {
        if (!SnowModify.enableSnow || !e.getBiome().hasPrecipitation()) return;
        e.setTemperature(0);
    }
}













