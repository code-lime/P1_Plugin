package org.lime.gp.item.loot.filter;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.Holder;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.state.IBlockDataHolder;
import org.lime.system;
import org.lime.gp.lime;
import org.lime.gp.item.Items;
import org.lime.gp.module.ArrowBow;
import org.lime.gp.module.PopulateLootEvent;

import net.minecraft.world.EnumHand;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;

public class DebugLootFilter implements ILootFilter {
    public final String prefix;
    public DebugLootFilter(String prefix) {
        this.prefix = prefix;
    }
    
    private static <T>void addParam(List<String> params, PopulateLootEvent loot, String name, LootContextParameter<T> type, system.Func1<T, String> convert) {
        loot.getOptional(type)
            .ifPresentOrElse(
                v -> {
                    String value = convert.invoke(v);
                    if (value == null || value.isEmpty()) params.add(name);
                    else params.add(name+"="+value);
                },
                () -> params.add("!"+name)
            );
    }

    public boolean isFilter(PopulateLootEvent e) {
        List<String> params = new ArrayList<>();
        params.add("List '"+prefix+"' of loot filter:");
        addParam(params, e, "this", LootContextParameters.THIS_ENTITY, v -> v.getBukkitEntity().getType().name());
        addParam(params, e, "direct.killer", LootContextParameters.DIRECT_KILLER_ENTITY, v -> v.getBukkitEntity().getType().name());
        addParam(params, e, "killer", LootContextParameters.KILLER_ENTITY, v -> v.getBukkitEntity().getType().name());
        addParam(params, e, "killer.hand", LootContextParameters.KILLER_ENTITY, v -> {
            if (v instanceof IProjectile projectile) {
                return Items.getGlobalKeyByItem(ArrowBow.getBowItem(projectile)).orElse("NULL");
            }
            else if (v instanceof EntityLiving player) {
                return Items.getGlobalKeyByItem(player.getItemInHand(EnumHand.MAIN_HAND)).orElse("NULL");
            }
            return null;
        });
        addParam(params, e, "damage.player", LootContextParameters.LAST_DAMAGE_PLAYER, v -> null);
        addParam(params, e, "damage", LootContextParameters.DAMAGE_SOURCE, v -> v.type().msgId());
        addParam(params, e, "block", LootContextParameters.BLOCK_STATE, IBlockDataHolder::toString);
        addParam(params, e, "tool", LootContextParameters.TOOL, v -> Items.getGlobalKeyByItem(v).orElse("NULL"));
        addParam(params, e, "explosion", LootContextParameters.EXPLOSION_RADIUS, system::getDouble);
        addParam(params, e, "looting", LootContextParameters.LOOTING_MOD, system::getDouble);
        addParam(params, e, "biome", LootContextParameters.ORIGIN, v -> e.getWorld().getBiome(new BlockPosition((int)v.x, (int)v.y, (int)v.z)).unwrapKey().map(ResourceKey::location).map(MinecraftKey::toString).orElse("NULL"));
        addParam(params, e, "position", LootContextParameters.ORIGIN, v -> system.getDouble(v.x) + " " + system.getDouble(v.y) + " " + system.getDouble(v.z));
        lime.logOP(String.join("\n - ", params));
        return true;
    }
}