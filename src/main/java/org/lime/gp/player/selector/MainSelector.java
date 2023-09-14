package org.lime.gp.player.selector;

import org.lime.Position;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public abstract class MainSelector extends ISelector {
    public abstract void onCallback(Position mainPos, BlockFace mainFace);
    protected abstract boolean isFilter(Block block);

    public static MainSelector create(Func1<Block, Boolean> filter, Action2<Position, BlockFace> callback) {
        return new MainSelector() {
            @Override public void onCallback(Position mainPos, BlockFace mainFace) { callback.invoke(mainPos, mainFace); }
            @Override protected boolean isFilter(Block block) { return filter.invoke(block); }
        };
    }

    @Override protected void init() { }
    @Override protected boolean isRemoveUpdate() { return false; }
    @Override protected void onRemove() { }
    @Override public SelectorType getType() { return SelectorType.Main; }

    @Override protected boolean onBlockClick(PlayerInteractEvent event) {
        boolean isShift = event.getPlayer().isSneaking();
        if (!isShift) return false;
        if (event.getHand() != EquipmentSlot.HAND) return false;
        if (event.getItem() != null && event.getItem().getType() != Material.AIR) return false;
        Block block = event.getClickedBlock();
        if (block == null || !isFilter(block)) return false;
        onCallback(new Position(block), event.getBlockFace());
        remove();
        return true;
    }
}
