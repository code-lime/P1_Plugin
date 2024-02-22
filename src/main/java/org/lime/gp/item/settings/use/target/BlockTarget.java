package org.lime.gp.item.settings.use.target;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import java.util.Optional;

public class BlockTarget implements ITarget {
    private final Location location;
    private final BlockState target;

    public BlockTarget(Block target) {
        this.location = target.getLocation();
        this.target = target.getState();
    }

    @Override public boolean isSelf() { return false; }
    @Override public boolean isActive() { return location.getBlock().getState().equals(target); }

    public Location getLocation() {
        return location;
    }
    public BlockState getState() {
        return target;
    }

    @Override public Optional<BlockTarget> castToBlock() { return Optional.of(this); }
    @Override public Optional<PlayerTarget> castToPlayer() { return Optional.empty(); }
}
