package org.lime.gp.player.selector;

import org.lime.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

public abstract class ZoneMainSelector extends ZoneSelector {
    protected abstract void onCallback(Position pos1, Position pos2, Position mainPos, BlockFace mainFace);
    public static ZoneMainSelector create(Action4<Position, Position, Position, BlockFace> callback) {
        return new ZoneMainSelector() {
            @Override protected void onCallback(Position pos1, Position pos2, Position mainPos, BlockFace mainFace) {
                callback.invoke(pos1, pos2, mainPos, mainFace);
            }
        };
    }
    @Override public SelectorType getType() { return SelectorType.ZoneMain; }

    @Override protected boolean onBlockClick(PlayerInteractEvent event) {
        boolean isShift = event.getPlayer().isSneaking();
        if (!isSelected() || !isShift) return super.onBlockClick(event);
        if (event.getHand() != EquipmentSlot.HAND) return false;
        if (event.getItem() != null && event.getItem().getType() != Material.AIR) return false;
        Block block = event.getClickedBlock();
        if (block == null) return false;
        onCallback(pos1, pos2, new Position(block), event.getBlockFace());
        remove();
        return true;
    }
}
