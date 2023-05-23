package org.lime.gp.item.loot.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftMagicNumbers;
import org.lime.system;
import org.lime.gp.lime;
import org.lime.gp.module.PopulateLootEvent;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;

public class BlockLootFilter implements ILootFilter {
    private final system.Func1<IBlockData, Boolean> filter;
    public BlockLootFilter(String argLine) {
        filter = createBlockTest(argLine);
    }
    @Override public boolean isFilter(PopulateLootEvent loot) {
        return loot.getOptional(PopulateLootEvent.Parameters.BlockState)
            .map(v -> filter.invoke(v))
            .orElse(false);
    }

    private static <T extends Comparable<T>>String getValueOf(IBlockData data, IBlockState<T> state) {
        return state.getName(data.getValue(state));
    }

    public static system.Func1<IBlockData, Boolean> createBlockTest(String filterLine) {
        String[] argArray = filterLine.split(Pattern.quote(";"));
        HashMap<String, String> variable = new HashMap<>();
        for (String argItem : argArray) {
            String[] kv = argItem.split(Pattern.quote("="), 2);
            variable.put(kv[0], kv[1]);
        }
        Block block = CraftMagicNumbers.getBlock(Material.valueOf(variable.remove("block")));
        boolean debug = variable.containsKey("debug") && (variable.remove("debug") == "true");
        BlockStateList<Block, IBlockData> blockParams = block.getStateDefinition();
        HashMap<IBlockState<?>, String> props = new HashMap<>();

        variable.forEach((key, value) -> props.put(blockParams.getProperty(key), value));

        return data -> {
            if (debug) {
                BlockStateList<Block, IBlockData> _blockParams = data.getBlock().getStateDefinition();
                List<String> log = new ArrayList<>();
                log.add("block=" + CraftMagicNumbers.getMaterial(data.getBlock()));
                _blockParams.getProperties()
                    .forEach(state -> log.add(state.getName() + "=" + getValueOf(data, state)));
                lime.logOP("block["+String.join("=",log)+"]");
            }
            if (!data.getBlock().equals(block)) return false;
            for (Map.Entry<IBlockState<?>, String> kv : props.entrySet()) {
                if (!getValueOf(data, kv.getKey()).equals(kv.getValue())) return false;
            }
            return true;
        };
    }
}
