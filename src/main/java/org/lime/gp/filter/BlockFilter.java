package org.lime.gp.filter;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftMagicNumbers;
import org.lime.gp.filter.data.IFilterData;
import org.lime.gp.filter.data.IFilterInfo;
import org.lime.gp.lime;
import org.lime.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class BlockFilter<T extends IFilterData<T>> implements IFilter<T> {
    private final system.Func1<IBlockData, Boolean> filter;
    public BlockFilter(IFilterInfo<T> filterInfo, String argLine) { filter = createBlockTest(argLine); }
    @Override public boolean isFilter(T loot) { return loot.blockData().map(filter).orElse(false); }

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
        net.minecraft.world.level.block.Block block = CraftMagicNumbers.getBlock(Material.valueOf(variable.remove("block")));
        boolean debug = variable.containsKey("debug") && ("true".equals(variable.remove("debug")));
        BlockStateList<Block, IBlockData> blockParams = block.getStateDefinition();
        HashMap<IBlockState<?>, String> props = new HashMap<>();

        variable.forEach((key, value) -> props.put(blockParams.getProperty(key), value));

        return data -> {
            if (debug) {
                BlockStateList<net.minecraft.world.level.block.Block, IBlockData> _blockParams = data.getBlock().getStateDefinition();
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
