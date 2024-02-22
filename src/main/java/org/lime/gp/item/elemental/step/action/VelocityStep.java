package org.lime.gp.item.elemental.step.action;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import net.minecraft.network.protocol.game.PacketPlayOutEntityVelocity;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.system.utils.MathUtils;

import java.util.stream.Stream;

@Step(name = "velocity")
public record VelocityStep(Transformation point, float power, Vector radius, boolean self) implements IStep<VelocityStep> {
    @Override public void execute(Player player, DataContext context, Transformation location) {
        World world = player.getWorld();
        Location from = MathUtils.convert(location.getTranslation()).toLocation(world);
        Vector to = MathUtils.convert(MathUtils.transform(point, location).getTranslation());
        if (radius.isZero()) {
            if (!self) return;
            sendForce(Stream.of(player), to, power);
            return;
        }
        sendForce(from.getNearbyPlayers(radius.getX(), radius.getY(), radius.getZ())
                .stream()
                .filter(other -> self || player != other), to, power);
    }
    private static void sendForce(Stream<Player> players, Vector point, float power) {
        Vec3D toPoint = new Vec3D(point.getX(), point.getY(), point.getZ());
        players.forEach(player -> {
            if (!(player instanceof CraftPlayer craftPlayer)) return;
            EntityPlayer handle = craftPlayer.getHandle();
            PlayerConnection connection = handle.connection;
            connection.send(new PacketPlayOutEntityVelocity(craftPlayer.getEntityId(), toPoint.subtract(handle.position()).normalize().multiply(power, power, power)));
        });
    }

    public VelocityStep parse(JsonObject json) {
        return new VelocityStep(
                MathUtils.transformation(json.get("point")),
                json.get("power").getAsFloat(),
                MathUtils.getVector(json.get("radius").getAsString()),
                json.get("self").getAsBoolean()
        );
    }
}











