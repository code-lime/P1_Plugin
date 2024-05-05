package org.lime.gp.item.elemental.step.action;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import net.minecraft.network.protocol.game.PacketPlayOutEntityVelocity;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.item.settings.use.target.ILocationTarget;
import org.lime.gp.item.settings.use.target.PlayerTarget;
import org.lime.system.utils.MathUtils;

import javax.annotation.Nullable;
import java.util.stream.Stream;

@Step(name = "velocity")
public record VelocityStep(Transformation point, float power, Vector radius, boolean self) implements IStep<VelocityStep> {
    @Override public void execute(ILocationTarget target, DataContext context, Transformation location) {
        @Nullable Player player = target.castToPlayer().map(PlayerTarget::getPlayer).orElse(null);

        Location from = MathUtils.convert(location.getTranslation()).toLocation(target.getWorld());
        Vector to = MathUtils.convert(MathUtils.transform(point, location).getTranslation());
        if (radius.isZero()) {
            if (!self || player == null) return;
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
    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, JObject.of(
                JProperty.require(IName.raw("point"), IJElement.link(docs.transform()), IComment.text("Координата в сторону которой будут ускоряться игроки")),
                JProperty.require(IName.raw("power"), IJElement.raw(1.0), IComment.text("Скорость с которой будут двигаться игроки. Может быть отрицательной")),
                JProperty.require(IName.raw("radius"), IJElement.link(docs.vector()), IComment.join(
                        IComment.text("Игроки, находящиеся в данном радиусе ускорятся. Если радиус равен "),
                        IComment.raw("0 0 0"),
                        IComment.text(" то ускорится только текущий игрок")
                )),
                JProperty.require(IName.raw("self"), IJElement.bool(), IComment.text("Придает ли ускорение текущему игроку"))
        ), IComment.text("Придает ускорение игрокам к точке ").append(IComment.field("point")).append(IComment.text(" с силой ").append(IComment.field("power"))));
    }
}











