package org.lime.gp.item.elemental.step.action;

import com.mojang.math.Transformation;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.module.mobs.IPopulateSpawn;
import org.lime.gp.module.mobs.spawn.ISpawn;
import org.lime.system.utils.MathUtils;

import java.util.Collections;

public record EntityStep(ISpawn spawn) implements IStep {
    @Override public void execute(Player player, DataContext context, Transformation position) {
        if (!(player.getWorld() instanceof CraftWorld craftWorld)) return;
        WorldServer worldServer = craftWorld.getHandle();
        Vector pos = MathUtils.convert(position.getTranslation());
        spawn.generateMob(IPopulateSpawn.of(worldServer, Collections.emptyList()))
                .ifPresent(creator -> creator.spawn(worldServer, new Vec3D(pos.getX(), pos.getY(), pos.getZ())));
    }
}
