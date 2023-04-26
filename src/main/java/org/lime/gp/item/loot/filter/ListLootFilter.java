package org.lime.gp.item.loot.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.lime.system;
import org.lime.gp.item.data.Checker;

import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;

public abstract class ListLootFilter implements ILootFilter {
    public final List<ILootFilter> filters = new ArrayList<>();

    /*
        public static final LootContextParameter<Entity> ThisEntity = LootContextParameters.THIS_ENTITY;
        public static final LootContextParameter<EntityHuman> LastDamagePlayer = LootContextParameters.LAST_DAMAGE_PLAYER;
        public static final LootContextParameter<DamageSource> DamageSource = LootContextParameters.DAMAGE_SOURCE;
        public static final LootContextParameter<Entity> KillerEntity = LootContextParameters.KILLER_ENTITY;
        public static final LootContextParameter<Entity> DirectKillerEntity = LootContextParameters.DIRECT_KILLER_ENTITY;
        public static final LootContextParameter<Vec3D> Origin = LootContextParameters.ORIGIN;
        public static final LootContextParameter<IBlockData> BlockState = LootContextParameters.BLOCK_STATE;
        public static final LootContextParameter<TileEntity> BlockEntity = LootContextParameters.BLOCK_ENTITY;
        public static final LootContextParameter<ItemStack> Tool = LootContextParameters.TOOL;
        public static final LootContextParameter<Float> ExplosionRadius = LootContextParameters.EXPLOSION_RADIUS;
        public static final LootContextParameter<Integer> LootingMod = LootContextParameters.LOOTING_MOD;
    */
    public ListLootFilter(String argLine) {
        String[] argArray = argLine.split(Pattern.quote(";"));
        for (String argItem : argArray) {
            String[] kv = argItem.split(Pattern.quote("="), 2);
            String key = kv[0];
            String value = kv.length > 1 ? kv[1] : "";
            switch (key) {
                case "this" -> filters.add(e -> e.getOptional(LootContextParameters.THIS_ENTITY)
                    .map(v -> v.getBukkitEntity().getType().name().equalsIgnoreCase(value))
                    .orElse(false)
                );
                case "direct.killer" -> filters.add(e -> e.getOptional(LootContextParameters.DIRECT_KILLER_ENTITY)
                    .map(v -> v.getBukkitEntity().getType().name().equalsIgnoreCase(value))
                    .orElse(false)
                );
                case "killer" -> filters.add(e -> e.getOptional(LootContextParameters.KILLER_ENTITY)
                    .map(v -> v.getBukkitEntity().getType().name().equalsIgnoreCase(value))
                    .orElse(false)
                );
                case "damage.player" -> filters.add(e -> e.getOptional(LootContextParameters.LAST_DAMAGE_PLAYER).isPresent());
                case "damage" -> filters.add(e -> e.getOptional(LootContextParameters.DAMAGE_SOURCE).map(v -> v.type().msgId().equalsIgnoreCase(value)).orElse(false));
                case "block" -> filters.add(e -> e.getOptional(LootContextParameters.BLOCK_STATE).map(v -> v.toString().equalsIgnoreCase(value)).orElse(false));
                case "tool" -> {
                    Checker checker = Checker.createCheck(value);
                    filters.add(e -> e.getOptional(LootContextParameters.TOOL).map(checker::check).orElse(false));
                }
                case "explosion" -> {
                    system.IRange range = system.IRange.parse(value);
                    filters.add(e -> e.getOptional(LootContextParameters.EXPLOSION_RADIUS).map(v -> range.inRange(v, 16)).orElse(false));
                }
                case "looting" -> {
                    system.IRange range = system.IRange.parse(value);
                    filters.add(e -> e.getOptional(LootContextParameters.LOOTING_MOD).map(v -> range.inRange(v, 3)).orElse(false));
                }

                case "!this" -> filters.add(e -> !e.has(LootContextParameters.THIS_ENTITY));
                case "!damage.player" -> filters.add(e -> !e.has(LootContextParameters.LAST_DAMAGE_PLAYER));
                case "!damage" -> filters.add(e -> !e.has(LootContextParameters.DAMAGE_SOURCE));
                case "!block" -> filters.add(e -> !e.has(LootContextParameters.BLOCK_STATE));
                case "!tool" -> filters.add(e -> !e.has(LootContextParameters.TOOL));
                case "!explosion" -> filters.add(e -> !e.has(LootContextParameters.EXPLOSION_RADIUS));
                case "!looting" -> filters.add(e -> !e.has(LootContextParameters.LOOTING_MOD));
            }
        }
    }
}
