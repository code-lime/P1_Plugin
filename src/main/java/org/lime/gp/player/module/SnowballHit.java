package org.lime.gp.player.module;

import net.minecraft.world.item.Items;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_20_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftSnowball;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.lime.gp.module.biome.holiday.Snowy;
import org.lime.plugin.CoreElement;
import org.lime.system.utils.RandomUtils;

public class SnowballHit implements Listener {
    public static CoreElement create() {
        return CoreElement.create(SnowballHit.class)
                .withInstance();
    }
    @EventHandler public static void on(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof CraftSnowball snowball) || !snowball.getHandle().getItem().is(Items.SNOWBALL)) return;
        Block hit_block = e.getHitBlock();
        if (e.getHitEntity() instanceof Player hit_player) {
            hit_player.setFreezeTicks(Math.min(hit_player.getMaxFreezeTicks(), hit_player.getFreezeTicks() + RandomUtils.rand(0, 30)));
        }
        if (hit_block == null) return;
        if (!RandomUtils.rand_is(0.2)) return;
        if (hit_block.getType() == Material.SNOW) addSnow(hit_block, e.getEntity());
        else if (e.getHitBlockFace() == BlockFace.UP) addSnow(hit_block.getRelative(BlockFace.UP), e.getEntity());
    }
    private static void addSnow(Block block, Projectile projectile) {
        if (!(block instanceof CraftBlock cblock)) return;
        Snowy.throwSnow(cblock.getCraftWorld().getHandle(), cblock.getPosition(), projectile.getShooter() instanceof CraftEntity c ? c.getHandle() : null);
    }
}



















