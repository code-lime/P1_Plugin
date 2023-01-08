package org.lime.gp.player.selector;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.lime.Position;
import org.lime.gp.extension.Zone;

import java.util.Optional;

public abstract class ZoneSelector extends ISelector {
    public final static Particle ShowParticle = Particle.FLAME;
    public Position pos1 = null;
    public Position pos2 = null;

    public boolean isWorld(World world) {
        return Optional.ofNullable(pos1).map(v -> v.world).orElse(world) == world
                && Optional.ofNullable(pos2).map(v -> v.world).orElse(world) == world;
    }

    @Override protected void init() { }
    public boolean isSelected() {
        return pos1 != null && pos2 != null;
    }
    @Override protected boolean isRemoveUpdate() {
        if (!isSelected()) return false;
        if (player.getWorld() != pos1.world) return false;
        Zone.showBox(player, pos1.toVector(), pos2.toVector(), ShowParticle);
        return false;
    }
    @Override protected void onRemove() { }

    @Override protected boolean onBlockClick(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return false;
        if (event.getHand() != EquipmentSlot.HAND) return false;
        if (event.getItem() != null && event.getItem().getType() != Material.AIR) return false;
        if (!isWorld(block.getWorld())) return false;
        switch (event.getAction()) {
            case LEFT_CLICK_BLOCK:
                pos1 = new Position(block);
                if (pos2 == null) pos2 = pos1;
                return true;
            case RIGHT_CLICK_BLOCK:
                pos2 = new Position(block);
                if (pos1 == null) pos1 = pos2;
                return true;
            default:
                break;
        }
        return false;
    }
}
