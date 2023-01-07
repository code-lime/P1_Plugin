package org.lime.gp.module;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Door;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.lime.core;

public class DoorSound implements Listener {
    public static core.element create() {
        return core.element.create(DoorSound.class)
                .withInstance();
    }

    @EventHandler public void on(BlockDamageEvent e) {
        Block block = e.getBlock();
        if (!(block.getBlockData() instanceof Door)) return;
        block.getLocation().getWorld().playSound(
                block.getLocation(),
                block.getType() == Material.IRON_DOOR ? Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR : Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR,
                SoundCategory.BLOCKS,
                0.3F,
                2
        );
    }
}
