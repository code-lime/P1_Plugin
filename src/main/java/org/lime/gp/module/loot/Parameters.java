package org.lime.gp.module.loot;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPosition;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.EnumHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.IBlockDataHolder;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.Vec3D;
import org.lime.gp.filter.IFilter;
import org.lime.gp.filter.data.FilterParameterInfo;
import org.lime.gp.filter.data.IFilterInfo;
import org.lime.gp.filter.data.IFilterParameter;
import org.lime.gp.filter.data.IFilterParameterInfo;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.Checker;
import org.lime.gp.module.ArrowBow;
import org.lime.gp.module.biome.weather.BiomeChecker;
import org.lime.system;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Parameters {
    public static final LootParameter<Entity> ThisEntity = LootParameter.of(LootContextParameters.THIS_ENTITY);
    public static final LootParameter<EntityHuman> LastDamagePlayer = LootParameter.of(LootContextParameters.LAST_DAMAGE_PLAYER);
    public static final LootParameter<net.minecraft.world.damagesource.DamageSource> DamageSource = LootParameter.of(LootContextParameters.DAMAGE_SOURCE);
    public static final LootParameter<Entity> KillerEntity = LootParameter.of(LootContextParameters.KILLER_ENTITY);
    public static final LootParameter<Entity> DirectKillerEntity = LootParameter.of(LootContextParameters.DIRECT_KILLER_ENTITY);
    public static final LootParameter<Vec3D> Origin = LootParameter.of(LootContextParameters.ORIGIN);
    public static final LootParameter<IBlockData> BlockState = LootParameter.of(LootContextParameters.BLOCK_STATE);
    public static final LootParameter<TileEntity> BlockEntity = LootParameter.of(LootContextParameters.BLOCK_ENTITY);
    public static final LootParameter<ItemStack> Tool = LootParameter.of(LootContextParameters.TOOL);
    public static final LootParameter<Float> ExplosionRadius = LootParameter.of(LootContextParameters.EXPLOSION_RADIUS);
    public static final LootParameter<Integer> LootingMod = LootParameter.of(LootContextParameters.LOOTING_MOD);

    public static IFilterInfo<IPopulateLoot> filterInfo() {
        return new IFilterInfo<IPopulateLoot>() {
            @Override public Optional<IFilterParameterInfo<IPopulateLoot, ?>> getParamInfo(String key) { return Parameters.paramInfo(key); }
            @Override public Collection<IFilterParameterInfo<IPopulateLoot, ?>> getAllParams() { return Parameters.allInfo(); }
        };
    }

    private static final Map<String, LootParameter<?>> all = ImmutableMap.<String, LootParameter<?>>builder()
            .put("ThisEntity", ThisEntity)
            .put("LastDamagePlayer", LastDamagePlayer)
            .put("DamageSource", DamageSource)
            .put("KillerEntity", KillerEntity)
            .put("DirectKillerEntity", DirectKillerEntity)
            .put("Origin", Origin)
            .put("BlockState", BlockState)
            .put("BlockEntity", BlockEntity)
            .put("Tool", Tool)
            .put("ExplosionRadius", ExplosionRadius)
            .put("LootingMod", LootingMod)
            .build();
    private static final Map<String, IFilterParameterInfo<IPopulateLoot, ?>> allInfo = Stream.<IFilterParameterInfo<IPopulateLoot, ?>>builder()
            .add(ThisEntity.createInfoEqualsIgnoreCase("this", v -> v.getBukkitEntity().getType().name()))
            .add(DirectKillerEntity.createInfoEqualsIgnoreCase("direct.killer", v -> v.getBukkitEntity().getType().name()))
            .add(KillerEntity.createInfoEqualsIgnoreCase("killer", v -> v.getBukkitEntity().getType().name()))
            .add(KillerEntity.createInfo("killer.hand", new FilterParameterInfo.IAction<>() {
                @Override public String convert(Entity entity, World world) {
                    if (entity instanceof IProjectile projectile)
                        return Items.getGlobalKeyByItem(ArrowBow.getBowItem(projectile)).orElse("NULL");
                    else if (entity instanceof EntityLiving player)
                        return Items.getGlobalKeyByItem(player.getItemInHand(EnumHand.MAIN_HAND)).orElse("NULL");
                    return null;
                }
                @Override public IFilter<IPopulateLoot> createFilter(String rawValue, FilterParameterInfo<IPopulateLoot, Entity> info) {
                    Checker checker = Checker.createCheck(rawValue);
                    return loot -> loot.getOptional(info.type).map(v -> {
                        if (v instanceof IProjectile projectile)
                            return ArrowBow.getBowItem(projectile);
                        else if (v instanceof EntityLiving player)
                            return player.getItemInHand(EnumHand.MAIN_HAND);
                        return null;
                    }).map(checker::check).orElse(false);
                }
            }))
            .add(LastDamagePlayer.createInfoEqualsIgnoreCase("damage.player", v -> ""))
            .add(DamageSource.createInfoEqualsIgnoreCase("damage", v -> v.type().msgId()))
            .add(BlockState.createInfoEqualsIgnoreCase("block", IBlockDataHolder::toString))
            .add(Tool.createInfoFilter("tool", v -> Items.getGlobalKeyByItem(v).orElse("NULL"), Checker::createCheck, Checker::check))
            .add(ExplosionRadius.createInfoFilter("explosion", system::getDouble, system.IRange::parse, (range, value) -> range.inRange(value, 16)))
            .add(LootingMod.createInfoFilter("looting", system::getDouble, system.IRange::parse, (range, value) -> range.inRange(value, 3)))
            .add(Origin.createInfoWorldFilter("biome",
                    (v, world) -> world.getBiome(new BlockPosition((int)v.x, (int)v.y, (int)v.z))
                            .unwrapKey()
                            .map(ResourceKey::location)
                            .map(MinecraftKey::toString)
                            .orElse("NULL"),
                    BiomeChecker::createCheck,
                    (checker, v, world) -> checker.check(world.getBiome(new BlockPosition((int)v.x, (int)v.y, (int)v.z)).value())
            ))
            .add(Origin.createInfoFilter("position", v -> system.getDouble(v.x) + " " + system.getDouble(v.y) + " " + system.getDouble(v.z), s -> {
                String[] args = s.split(" ");
                return system.toast(system.IRange.parse(args[0]), system.IRange.parse(args[1]), system.IRange.parse(args[2]));
            }, (range, position) -> range.invokeGet((x, y, z) -> x.inRange(position.x, 16) && y.inRange(position.y, 16) && z.inRange(position.z, 16))))
            .add(ThisEntity.createInfoFilter("tags",
                    v -> system.json.by(v.getTags()).build().toString(),
                    s -> List.of(s.split(",")),
                    (tags, entity) -> entity.getTags().containsAll(tags)))
            .build()
            .collect(Collectors.toMap(IFilterParameterInfo::name, v -> v));

    public static Map<String, LootParameter<?>> all() { return all; }
    public static Optional<IFilterParameterInfo<IPopulateLoot, ?>> paramInfo(String key) { return Optional.ofNullable(allInfo.get(key)); }
    public static Collection<IFilterParameterInfo<IPopulateLoot, ?>> allInfo() { return allInfo.values(); }

    public static <T>Optional<LootContextParameter<T>> raw(IFilterParameter<IPopulateLoot, T> param) {
        return param instanceof LootParameter<T> parameter ? Optional.of(parameter.nms()) : Optional.empty();
    }

    public static <T> void appendTo(LootContextParameter<T> param, LootTableInfo context, LootTableInfo.Builder builder) {
        builder.withParameter(param, context.getParam(param));
    }
}

















/*
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
        }
 */


/*
        +addParam(function, "this", LootContextParameters.THIS_ENTITY, v -> v.getBukkitEntity().getType().name());
        +addParam(function, "direct.killer", LootContextParameters.DIRECT_KILLER_ENTITY, v -> v.getBukkitEntity().getType().name());
        +addParam(function, "killer", LootContextParameters.KILLER_ENTITY, v -> v.getBukkitEntity().getType().name());
        +addParam(function, "killer.hand", LootContextParameters.KILLER_ENTITY, v -> {
            if (v instanceof IProjectile projectile) {
                return Items.getGlobalKeyByItem(ArrowBow.getBowItem(projectile)).orElse("NULL");
            }
            else if (v instanceof EntityLiving player) {
                return Items.getGlobalKeyByItem(player.getItemInHand(EnumHand.MAIN_HAND)).orElse("NULL");
            }
            return null;
        });
        addParam(function, "damage.player", LootContextParameters.LAST_DAMAGE_PLAYER, v -> null);
        addParam(function, "damage", LootContextParameters.DAMAGE_SOURCE, v -> v.type().msgId());
        addParam(function, "block", LootContextParameters.BLOCK_STATE, IBlockDataHolder::toString);
        addParam(function, "tool", LootContextParameters.TOOL, v -> Items.getGlobalKeyByItem(v).orElse("NULL"));
        addParam(function, "explosion", LootContextParameters.EXPLOSION_RADIUS, system::getDouble);
        addParam(function, "looting", LootContextParameters.LOOTING_MOD, system::getDouble);
        addParam(function, "biome", LootContextParameters.ORIGIN, v -> e.getWorld().getBiome(new BlockPosition((int)v.x, (int)v.y, (int)v.z)).unwrapKey().map(ResourceKey::location).map(MinecraftKey::toString).orElse("NULL"));
        addParam(function, "position", LootContextParameters.ORIGIN, v -> system.getDouble(v.x) + " " + system.getDouble(v.y) + " " + system.getDouble(v.z));
        addParam(function, "tags", LootContextParameters.ORIGIN, v -> e.getOptional(LootContextParameters.THIS_ENTITY).map(Entity::getTags).orElseGet(Collections::emptySet).toString());

*/
