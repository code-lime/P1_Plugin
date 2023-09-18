package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.event.CraftEventFactory;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BlockSnowTickEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private boolean cancel;

    private final BlockSnow snow;
    private final IBlockData state;
    private final WorldServer world;
    private final BlockPosition pos;
    private final RandomSource random;

    public BlockSnowTickEvent(BlockSnow snow, IBlockData state, WorldServer world, BlockPosition pos, RandomSource random) {
        this.snow = snow;
        this.state = state;
        this.world = world;
        this.pos = pos;
        this.random = random;
    }

    public static void execute(BlockSnow snow, IBlockData state, WorldServer world, BlockPosition pos, RandomSource random) {
        BlockSnowTickEvent event = new BlockSnowTickEvent(snow, state, world, pos, random);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        if (world.getBrightness(EnumSkyBlock.BLOCK, pos) > 11) {
            if (CraftEventFactory.callBlockFadeEvent(world, pos, Blocks.AIR.defaultBlockState()).isCancelled()) {
                return;
            }
            BlockSnow.dropResources(state, world, pos);
            world.removeBlock(pos, false);
        }
    }

    @Override public boolean isCancelled() { return this.cancel; }
    @Override public void setCancelled(boolean cancel) { this.cancel = cancel; }

    public BlockSnow getSnow() { return snow; }
    public IBlockData getState() { return state; }
    public WorldServer getWorld() { return world; }
    public BlockPosition getPos() { return pos; }
    public RandomSource getRandom() { return random; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
