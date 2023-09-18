package org.lime.gp.item.elemental.step.action;

import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.system.map;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.HashMap;
import java.util.Map;

public abstract class IBlockStep implements IStep {
    protected final IBlockData block;

    public IBlockStep(IBlockData block) { this.block = block; }
    public IBlockStep(Material material, Map<String, String> states) { this(setup(material, states)); }

    private static IBlockData setup(Material material, Map<String, String> setup) {
        IBlockData blockData = CraftMagicNumbers.getBlock(material).defaultBlockState();
        HashMap<String, IBlockState<?>> states = map.<String, IBlockState<?>>of()
                .add(blockData.getProperties(), IBlockState::getName, v -> v)
                .build();
        for (Map.Entry<String, String> kv : setup.entrySet()) {
            IBlockState<?> state = states.getOrDefault(kv.getKey(), null);
            if (state == null) continue;
            blockData = BlockInfo.setValue(blockData, state, kv.getValue());
        }
        return blockData;
    }
}
