package org.lime.gp.module;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.StructureGrowEvent;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.RandomUtils;

public class TreeGenerator implements Listener {
    public static CoreElement create() {
        return CoreElement.create(TreeGenerator.class)
                .withInstance();
    }

    @EventHandler public static void on(StructureGrowEvent e) {
        e.getBlocks().forEach(v -> {
            Material original = v.getType();
            Material material = original;
            switch (material) {
                case OAK_LEAVES:
                    if (RandomUtils.rand_is(0.10)) material = RandomUtils.rand_is(0.30) ? Material.FLOWERING_AZALEA_LEAVES : Material.AZALEA_LEAVES;
                    break;
                default:
                    break;
            }
            /*
            switch (material) {
                case ACACIA_LEAVES:
                case AZALEA_LEAVES:
                case BIRCH_LEAVES:
                case JUNGLE_LEAVES:
                case OAK_LEAVES:
                case SPRUCE_LEAVES:
                case DARK_OAK_LEAVES:
                case FLOWERING_AZALEA_LEAVES:
                    if (system.rand_is(0.001)) material = Material.COBWEB;
                    break;
                default:
                    break;
            }
            */
            if (original != material) v.setType(material);
        });
    }
}
