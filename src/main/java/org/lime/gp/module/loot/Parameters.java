package org.lime.gp.module.loot;

import com.google.common.collect.ImmutableMap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.Vec3D;

import java.util.Map;

public class Parameters {
    public static final LootContextParameter<Entity> ThisEntity = LootContextParameters.THIS_ENTITY;
    public static final LootContextParameter<EntityHuman> LastDamagePlayer = LootContextParameters.LAST_DAMAGE_PLAYER;
    public static final LootContextParameter<net.minecraft.world.damagesource.DamageSource> DamageSource = LootContextParameters.DAMAGE_SOURCE;
    public static final LootContextParameter<Entity> KillerEntity = LootContextParameters.KILLER_ENTITY;
    public static final LootContextParameter<Entity> DirectKillerEntity = LootContextParameters.DIRECT_KILLER_ENTITY;
    public static final LootContextParameter<Vec3D> Origin = LootContextParameters.ORIGIN;
    public static final LootContextParameter<IBlockData> BlockState = LootContextParameters.BLOCK_STATE;
    public static final LootContextParameter<TileEntity> BlockEntity = LootContextParameters.BLOCK_ENTITY;
    public static final LootContextParameter<ItemStack> Tool = LootContextParameters.TOOL;
    public static final LootContextParameter<Float> ExplosionRadius = LootContextParameters.EXPLOSION_RADIUS;
    public static final LootContextParameter<Integer> LootingMod = LootContextParameters.LOOTING_MOD;

    public static Map<String, LootContextParameter<?>> all() {
        return ImmutableMap.<String, LootContextParameter<?>>builder()
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
    }

    public static <T> void appendTo(LootContextParameter<T> param, LootTableInfo context, LootTableInfo.Builder builder) {
        builder.withParameter(param, context.getParam(param));
    }
}
