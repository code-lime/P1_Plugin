package org.lime.gp.player.selector;

import org.bukkit.Particle;
import org.bukkit.event.player.PlayerInteractEvent;
import org.lime.Position;
import org.lime.gp.extension.Zone;

public class ZoneReadonly extends ISelector {
    public final static Particle ShowParticle = Particle.FLAME;
    public final Position pos1;
    public final Position pos2;
    public ZoneReadonly(Position pos1, Position pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;
    }
    @Override protected void init() { }
    @Override protected boolean onBlockClick(PlayerInteractEvent event) { return false; }
    @Override protected boolean isRemoveUpdate() {
        if (player.getWorld() != pos1.world) return false;
        Zone.showBox(player, pos1.toVector(), pos2.toVector(), ShowParticle);
        return false;
    }
    @Override protected void onRemove() { }
    @Override public SelectorType getType() { return SelectorType.ZoneReadonly; }
}
