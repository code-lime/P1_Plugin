package org.lime.gp.filter;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers;
import org.bukkit.entity.EntityType;
import org.lime.gp.filter.data.IFilterData;
import org.lime.gp.filter.data.IFilterInfo;
import org.lime.gp.lime;
import org.lime.system.Regex;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BlockFilter<T extends IFilterData<T>> implements IFilter<T> {
    private final Func1<IBlockData, Boolean> filter;
    public BlockFilter(IFilterInfo<T> filterInfo, String argLine) { filter = createBlockTest(argLine); }
    @Override public boolean isFilter(T data) { return data.blockData().map(filter).orElse(false); }

    private static <T extends Comparable<T>>Optional<String> getValueOf(IBlockData data, IBlockState<T> state) {
        return data.hasProperty(state) ? Optional.of(state.getName(data.getValue(state))) : Optional.empty();
    }
    private static <T extends Comparable<T>>Collection<String> getAllNames(@Nullable IBlockState<T> state) {
        return state == null ? Collections.emptySet() : state.getPossibleValues().stream().map(state::getName).collect(Collectors.toSet());
    }

    private static Set<net.minecraft.world.level.block.Block> blockSet(String regex) {
        Set<Block> blocks = Arrays.stream(Material.values())
                .filter(Regex.filterRegex(Material::name, regex)::invoke)
                .map(CraftMagicNumbers::getBlock)
                .collect(Collectors.toSet());
        if (blocks.isEmpty()) lime.logOP("Materials in '"+regex+"' is EMPTY! Maybe error...");
        return blocks;
    }
    private static HashMap<IBlockState<?>, String> blockSetProperties(String blockRegex, Set<net.minecraft.world.level.block.Block> blocks, HashMap<String, String> variable) {
        HashMap<IBlockState<?>, String> props = new HashMap<>();
        variable.forEach((key, regexValue) -> {
            HashMap<IBlockState<?>, String> varProps = new HashMap<>();
            blocks.forEach(block -> {
                BlockStateList<Block, IBlockData> blockParams = block.getStateDefinition();
                IBlockState<?> state = blockParams.getProperty(key);
                Regex.filterRegex(getAllNames(state), v -> v, regexValue)
                        .forEach(value -> varProps.put(state, value));
            });
            if (varProps.isEmpty()) lime.logOP("Block '"+blockRegex+"' property '"+key+"' in '"+regexValue+"' is EMPTY! Maybe error...");
            props.putAll(varProps);
        });
        return props;
        /*blocks.forEach(block -> {
            BlockStateList<Block, IBlockData> blockParams = block.getStateDefinition();
            variable.forEach((key, value) -> {
                props.put(blockParams.getProperty(key), value);
            });
        });
        /*Set<T> types = system.filterRegex(List.of(tClass.getEnumConstants()), Enum::name, regex)
                .collect(Collectors.toSet());
        if (types.isEmpty()) lime.logOP("EntityTypes in '"+regex+"' is EMPTY! Maybe error...");
        return types::contains;*/
    }
    /*private static <T extends Enum<T>>Func1<T, Boolean> regexChecker(T[] tClass, String regex) {
        Set<T> types = system.filterRegex(List.of(tClass.getEnumConstants()), Enum::name, regex)
                .collect(Collectors.toSet());
        if (types.isEmpty()) lime.logOP("EntityTypes in '"+regex+"' is EMPTY! Maybe error...");
        return types::contains;
    }*/
    public static Func1<IBlockData, Boolean> createBlockTest(String filterLine) {
        String[] argArray = filterLine.split(Pattern.quote(";"));
        HashMap<String, String> variable = new HashMap<>();
        for (String argItem : argArray) {
            String[] kv = argItem.split(Pattern.quote("="), 2);
            variable.put(kv[0], kv[1]);
        }
        String blockRegex = variable.remove("block");
        Set<net.minecraft.world.level.block.Block> blocks = blockSet(blockRegex);
        boolean debug = variable.containsKey("debug") && ("true".equals(variable.remove("debug")));
        HashMap<IBlockState<?>, String> props = blockSetProperties(blockRegex, blocks, variable);

        return data -> {
            if (debug) {
                BlockStateList<net.minecraft.world.level.block.Block, IBlockData> _blockParams = data.getBlock().getStateDefinition();
                List<String> log = new ArrayList<>();
                log.add("block=" + CraftMagicNumbers.getMaterial(data.getBlock()));
                _blockParams.getProperties()
                        .forEach(state -> log.add(state.getName() + "=" + getValueOf(data, state).orElse("NAN")));
                lime.logOP("block["+String.join("=",log)+"]");
            }
            if (!blocks.contains(data.getBlock())) return false;
            return props.entrySet()
                    .stream()
                    .allMatch(kv -> getValueOf(data, kv.getKey())
                            .map(v -> v.equals(kv.getValue()))
                            .orElse(true));
        };
    }
    public static String createBlockLine(IBlockData blockData) {
        Block block = blockData.getBlock();
        BlockStateList<Block, IBlockData> blockParams = block.getStateDefinition();
        String params = blockParams.getProperties()
                .stream()
                .map(v -> v.getName() + "=" + getValueOf(blockData, v).orElse("NAN"))
                .collect(Collectors.joining(";"));
        return CraftMagicNumbers.getMaterial(block).name() + (params.isEmpty() ? "" : ("[" + params + "]"));
    }
    public static String createBlockLine(org.bukkit.block.Block block) {
        return block instanceof CraftBlock cb ? createBlockLine(cb.getNMS()) : "NONE(" + block + ")";
    }
}
