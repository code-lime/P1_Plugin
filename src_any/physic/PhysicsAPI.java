package org.lime.gp.physic;

import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;

public final class PhysicsAPI {
    private final Plugin owner;
    private final PhysicsModule plugin;

    public void setExplosionTracking(boolean track) {
        this.plugin.setTrackExplosions(track);
    }

    public PhysicsBlock spawnBlock(Location loc, BlockState block) {
        PBlock bl = new PBlock(this.plugin, loc, block);
        this.plugin.getBlocks().add(bl);
        return new PhysicsBlock(bl, this);
    }

    public PhysicsBlock spawnBlock(Location loc, MaterialData data) {
        PBlock bl = new PBlock(this.plugin, loc, data.toItemStack());
        this.plugin.getBlocks().add(bl);
        return new PhysicsBlock(bl, this);
    }

    public PhysicsBlock spawnBlock(Location loc, MaterialData data, Collection<ItemStack> drops) {
        PBlock bl = new PBlock(this.plugin, loc, data.toItemStack(), drops);
        this.plugin.getBlocks().add(bl);
        return new PhysicsBlock(bl, this);
    }

    public Plugin getOwner() {
        return this.owner;
    }

    public PhysicsAPI(Plugin owner, PhysicsModule plugin) {
        this.owner = owner;
        this.plugin = plugin;
    }
}