package org.lime.gp.player.level;

import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_19_R3.block.CraftBlock;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.lime.system;
import org.lime.gp.item.data.Checker;
import org.lime.gp.item.loot.filter.BlockLootFilter;

import net.minecraft.world.level.block.state.IBlockData;

public class ExperienceAction<TValue, TCompare> {
    public static final ExperienceAction<Entity, EntityCompare> KILL = of((e1, e2) -> e2.isCompare(e1), v -> EntityCompare.create(v));
    public static final ExperienceAction<Block, system.Func1<IBlockData, Boolean>> BREAK = of((e1, e2) -> e1 instanceof CraftBlock b && e2.invoke(b.getNMS()), v -> BlockLootFilter.createBlockTest("block="+v));
    public static final ExperienceAction<ItemStack, Checker> CRAFT = of((e1, e2) -> e2.check(e1), v -> Checker.createCheck(v));
    public static final ExperienceAction<Entity, EntityCompare> FARM = of((e1, e2) -> e2.isCompare(e1), v -> EntityCompare.create(v));

    private final system.Func2<TValue, TCompare, Boolean> action;
    private final system.Func1<String, TCompare> parse;

    private ExperienceAction(system.Func2<TValue, TCompare, Boolean> action, system.Func1<String, TCompare> parse) {
        this.action = action;
        this.parse = parse;
    }

    public boolean compare(TValue value, TCompare compare) {
        return this.action.invoke(value, compare);
    }
    public TCompare parse(String data) {
        return this.parse.invoke(data);
    }

    private static <TValue, TCompare>ExperienceAction<TValue, TCompare> of(system.Func2<TValue, TCompare, Boolean> action, system.Func1<String, TCompare> parse) {
        return new ExperienceAction<>(action, parse);
    }

    public static ExperienceAction<?, ?> getByName(String name) {
        return switch (name.toLowerCase()) {
            case "kill" -> KILL;
            case "break" -> BREAK;
            case "craft" -> CRAFT;
            case "farm" -> FARM;
            default -> throw new IllegalArgumentException("Type '"+name+"' not supported!");
        };
    }
}
