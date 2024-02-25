package org.lime.gp.item.settings.use.target;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

import java.util.Optional;

public class BlockTarget implements ITarget {
    private final Location location;
    private final BlockState state;
    private final BlockData data;

    public BlockTarget(Block block) {
        this.location = block.getLocation();
        this.data = block.getBlockData();
        this.state = block.getState();
    }

    @Override public boolean isSelf() { return false; }
    @Override public boolean isActive() { return location.getBlock().getState().equals(state); }

    public Location getLocation() {
        return location;
    }
    public BlockState getState() {
        return state;
    }
    public BlockData getData() {
        return data;
    }

    @Override public Optional<BlockTarget> castToBlock() { return Optional.of(this); }
    @Override public Optional<PlayerTarget> castToPlayer() { return Optional.empty(); }
}
