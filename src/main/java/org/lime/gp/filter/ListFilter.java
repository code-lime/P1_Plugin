package org.lime.gp.filter;

import org.lime.gp.filter.data.IFilterData;
import org.lime.gp.filter.data.IFilterInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public abstract class ListFilter<T extends IFilterData<T>> implements IFilter<T> {
    public final List<IFilter<T>> filters = new ArrayList<>();

    public ListFilter(IFilterInfo<T> filterInfo, String argLine) {
        String[] argArray = argLine.split(Pattern.quote(";"));
        for (String argItem : argArray) {
            String[] kv = argItem.split(Pattern.quote("="), 2);
            String key = kv[0];
            String value = kv.length > 1 ? kv[1] : "";
            boolean negate = key.startsWith("!");
            if (negate) key = key.substring(1);
            filterInfo.getParamInfo(key)
                    .<IFilter<T>>map(info -> negate ? info::emptyKey : info.createVariableFilter(value))
                    .ifPresent(filters::add);

            /*switch (key) {
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
                case "killer.hand" -> filters.add(e -> {
                    Checker checker = Checker.createCheck(value);
                    return e.getOptional(LootContextParameters.KILLER_ENTITY).map(v -> {
                        if (v instanceof IProjectile projectile) {
                            return ArrowBow.getBowItem(projectile);
                        }
                        else if (v instanceof EntityLiving player) {
                            return player.getItemInHand(EnumHand.MAIN_HAND);
                        }
                        return null;
                    }).map(checker::check).orElse(false);
                });
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
                case "biome" -> filters.add(e -> e.getOptional(LootContextParameters.ORIGIN)
                        .map(v -> new BlockPosition((int) v.x, (int) v.y, (int) v.z))
                        .map(e.getWorld()::getBiome)
                        .flatMap(Holder::unwrapKey)
                        .map(ResourceKey::location)
                        .map(MinecraftKey::toString)
                        .orElse("NULL")
                        .equalsIgnoreCase(value));

                case "!this" -> filters.add(e -> !e.has(LootContextParameters.THIS_ENTITY));
                case "!damage.player" -> filters.add(e -> !e.has(LootContextParameters.LAST_DAMAGE_PLAYER));
                case "!damage" -> filters.add(e -> !e.has(LootContextParameters.DAMAGE_SOURCE));
                case "!block" -> filters.add(e -> !e.has(LootContextParameters.BLOCK_STATE));
                case "!tool" -> filters.add(e -> !e.has(LootContextParameters.TOOL));
                case "!explosion" -> filters.add(e -> !e.has(LootContextParameters.EXPLOSION_RADIUS));
                case "!looting" -> filters.add(e -> !e.has(LootContextParameters.LOOTING_MOD));
            }*/
        }
    }
}














