package org.lime.gp.player.level;

import com.google.gson.JsonElement;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_19_R3.block.CraftBlock;
import org.bukkit.entity.Entity;
import org.lime.gp.filter.BlockFilter;
import org.lime.system;

import net.minecraft.world.level.block.state.IBlockData;

import java.util.*;

public class ExperienceAction<TValue, TCompare> {
    private static final HashMap<String, ExperienceAction<?,?>> actions = new HashMap<>();

    public static final ExperienceAction<Entity, EntityCompare> KILL = of("kill", (e1, e2) -> e2.isCompare(e1), EntityCompare::create, v -> v.getType().name());
    public static final ExperienceAction<Block, system.Func1<IBlockData, Boolean>> BREAK = of("break", (e1, e2) -> e1 instanceof CraftBlock b && e2.invoke(b.getNMS()), v -> BlockFilter.createBlockTest("block="+v), BlockFilter::createBlockLine);
    public static final ExperienceAction<String, String> CRAFT = of("craft", system::compareRegex, v -> v, v -> v);
    public static final ExperienceAction<Entity, EntityCompare> FARM = of("farm", (e1, e2) -> e2.isCompare(e1), EntityCompare::create, v -> v.getType().name());
    public static final ExperienceAction<String, String> HARVEST = of("harvest", system::compareRegex, v -> v, v -> v);
    public enum SingleValue {
        DIE;

        public String key() { return this.toString().toLowerCase(); }
        public static SingleValue ofKey(String key) { return SingleValue.valueOf(key.toUpperCase()); }
    }
    public static final ExperienceAction<SingleValue, SingleValue> SINGLE = of("single", Objects::equals, SingleValue::ofKey, SingleValue::key);

    private final String key;
    private final system.Func2<TValue, TCompare, Boolean> action;
    private final system.Func1<String, TCompare> parse;
    private final system.Func1<TValue, String> debug;
    private final boolean empty;

    private ExperienceAction(String key, system.Func2<TValue, TCompare, Boolean> action, system.Func1<String, TCompare> parse, system.Func1<TValue, String> debug, boolean empty) {
        this.key = key;
        this.action = action;
        this.parse = parse;
        this.debug = debug;
        this.empty = empty;

        actions.put(key, this);
    }

    public boolean compare(TValue value, TCompare compare) { return this.action.invoke(value, compare); }
    public TCompare parse(String data) { return this.parse.invoke(data); }
    public String debug(TValue value) { return this.debug.invoke(value); }
    public String key() { return this.key; }

    private static <TValue, TCompare>ExperienceAction<TValue, TCompare> of(String key, system.Func2<TValue, TCompare, Boolean> action, system.Func1<String, TCompare> parse, system.Func1<TValue, String> debug) {
        return new ExperienceAction<>(key, action, parse, debug, false);
    }
    public static ExperienceAction<?, ?> getByName(String name) {
        ExperienceAction<?, ?> action = actions.get(name);
        if (action == null) throw new IllegalArgumentException("Type '"+name+"' not supported!");
        return action;
    }

    public List<ExperienceGetter<?, ?>> createVariable(JsonElement json) {
        return empty
                ? Collections.singletonList(ExperienceGetter.parse(this, null, json))
                : json.getAsJsonObject()
                .entrySet()
                .stream()
                .<ExperienceGetter<?, ?>>map(kv -> ExperienceGetter.parse(this, kv.getKey(), kv.getValue()))
                .toList();
    }
}
