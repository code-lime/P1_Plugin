package org.lime.gp.entity.component.data.boat;

import com.mojang.datafixers.kinds.App;
import io.papermc.paper.util.CachedLists;
import io.papermc.paper.util.CollisionUtil;
import net.minecraft.core.BlockPosition;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLimeMarker;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.level.World;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Location;
import org.joml.Math;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lime.display.Displays;
import org.lime.display.Passenger;
import org.lime.display.models.display.BaseChildDisplay;
import org.lime.display.models.sitter.IExitLocation;
import org.lime.display.models.sitter.ISitter;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.EntityComponentInstance;
import org.lime.gp.entity.RotatedEntitySize;
import org.lime.gp.entity.component.display.display.EntityModelDisplay;
import org.lime.gp.entity.component.display.instance.DisplayInstance;
import org.lime.gp.entity.component.list.BoatComponent;
import org.lime.gp.entity.event.EntityMarkerEventInput;
import org.lime.gp.entity.event.EntityMarkerEventInteract;
import org.lime.gp.entity.event.EntityMarkerEventTick;
import org.lime.gp.module.DrawText;
import org.lime.gp.player.ui.CustomUI;
import org.lime.gp.player.ui.ImageBuilder;
import org.lime.json.JsonObjectOptional;
import org.lime.system.Time;
import org.lime.system.json;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;

import java.util.*;
import java.util.stream.Stream;

public class BoatInstance extends EntityComponentInstance<BoatComponent> implements
        CustomEntityMetadata.Tickable,
        CustomEntityMetadata.FirstTickable,
        CustomEntityMetadata.Interactable,
        CustomEntityMetadata.Inputable
{
    public BoatInstance(BoatComponent component, CustomEntityMetadata metadata) {
        super(component, metadata);
    }

    @Override public void read(JsonObjectOptional json) {

    }
    @Override public json.builder.object write() {
        return json.object();
    }

    private Vector3f lastDelta = new Vector3f();
    private static float normalFlat(float value, float limit) {
        float abs = Math.abs(value);
        if (abs < limit) return 0;
        if (abs < limit * 5) return value * 0.5f;
        return value;
    }

    public Vec3D clampToBounds(Vec3D point, WorldBorder border) {
        return new Vec3D(MathHelper.clamp(point.x, border.getMinX(), border.getMaxX()), point.y, MathHelper.clamp(point.z, border.getMinZ(), border.getMaxZ()));
    }

    private static boolean hasCollision(World world, Entity entity, AxisAlignedBB oldBox, AxisAlignedBB newBox) {
        List<AxisAlignedBB> collisions = CachedLists.getTempCollisionList();
        try {
            return CollisionUtil.getCollisions(world, entity, oldBox, collisions, false, true, false, true, null, null)
                    || CollisionUtil.getCollisions(world, entity, newBox, collisions, false, true, false, true, null, null);
        } finally {
            CachedLists.returnTempCollisionList(collisions);
        }
    }
    private static boolean tryRotateEntity(EntityLimeMarker marker, float deltaAngle) {
        World world = marker.level();
        AxisAlignedBB before = marker.getBoundingBox();
        Vec3D pos = marker.position();
        float lastYaw = marker.getYRot();
        marker.setYRot((lastYaw + deltaAngle) % 360F);
        AxisAlignedBB rotated = marker.getBoundingBoxAt(pos.x, pos.y, pos.z);
        boolean newCollision = hasCollision(world, marker, before, rotated);
        if (!newCollision) return true;
        marker.setYRot(lastYaw);
        return false;
    }
    private static Vector3f waterFrameDelta(World world, AxisAlignedBB aabb, Location location) {
        HashMap<Toast2<Integer, Integer>, Float> waterMap = new HashMap<>();

        WorldBorder border = world.getWorldBorder();
        BlockPosition.betweenClosedStream(aabb).forEach(block -> {
            Fluid fluid = world.getFluidState(block);
            float height = fluid.is(TagsFluid.WATER) ? (block.getY() + fluid.getHeight(world, block)) : Float.NEGATIVE_INFINITY;
            waterMap.compute(Toast.of(block.getX(), block.getZ()), (k,v) -> v == null ? height : Math.max(v, height));
        });

        Vector3f centerWaterPoint = new Vector3f();
        Vector3f centerPoint = new Vector3f();
        Vector3f maxWaterPoint = new Vector3f(0, Float.NEGATIVE_INFINITY, 0);
        Vector3f minWaterPoint = new Vector3f(0, Float.POSITIVE_INFINITY, 0);
        int centerWaterCount = 0;

        for (Map.Entry<Toast2<Integer, Integer>, Float> kv : waterMap.entrySet()) {
            float level = kv.getValue();
            Toast2<Integer, Integer> point = kv.getKey();
            centerPoint.add(point.val0, 0, point.val1);
            if (level == Float.NEGATIVE_INFINITY) continue;

            centerWaterCount++;
            centerWaterPoint.add(point.val0, level, point.val1);

            if (maxWaterPoint.y < level)
                maxWaterPoint.set(point.val0, level, point.val1);
            if (minWaterPoint.y > level)
                minWaterPoint.set(point.val0, level, point.val1);
        }

        if (waterMap.isEmpty()) centerPoint = null;
        else centerPoint.div(waterMap.size());

        if (centerWaterCount == 0) centerWaterPoint = null;
        else centerWaterPoint.div(centerWaterCount);

        float waterDelta = waterMap.isEmpty() ? 0 : (centerWaterCount / (float)waterMap.size());

        Vector3f position = new Vector3f((float) location.getX(), (float) location.getY(), (float) location.getZ());
        Vector3f blockPosition = new Vector3f(location.getBlockX(), location.getBlockY(), location.getBlockZ());

        Vector3f frameDelta;
        float percent;
        if (centerWaterPoint != null && centerPoint != null && centerWaterPoint.y >= blockPosition.y) {
            float scale = waterDelta * 0.8f;
            float value = Math.min(centerWaterPoint.y - position.y, 2) / 2;
            percent = (scale + 0.5f) * value - 0.5f;
            frameDelta = new Vector3f(centerWaterPoint).sub(centerPoint).mul(-scale, 0, -scale);
        } else {
            percent = -0.5f;
            frameDelta = new Vector3f();
        }
        frameDelta.y = percent;
        return frameDelta;
    }

    private final Vector2f input = new Vector2f();
    private Vector2f forceInput = new Vector2f();

    @Override public void onTick(CustomEntityMetadata metadata, EntityMarkerEventTick event) {
        BoatComponent component = component();

        Location location = metadata.location();
        EntityLimeMarker marker = metadata.marker;
        World world = marker.level();

        Vector3f delta = lastDelta.smoothStep(waterFrameDelta(world, marker.getBoundingBox(), location), 0.1f, new Vector3f()).mul(0.9f);

        if (marker.isInWater() && !marker.isUnderWater()) {
            metadata.list(DisplayInstance.class)
                    .flatMap(_v -> EntityModelDisplay.of(_v)
                            .flatMap(v -> v.model.ofKey("sit.main").stream())
                            .map(v -> v instanceof ISitter sitter ? sitter : null)
                            .filter(Objects::nonNull)
                            .flatMap(v -> v.sitter().map(__v -> Toast.of(_v, __v)).stream()))
                    .findAny()
                    .ifPresentOrElse(data -> data.invoke((display, player) ->
                                    CustomUI.TextUI.show(player, ImageBuilder.of(player, LangMessages.Message.Boat_Status.getSingleMessage(Apply.of().add(display.getAll()))), 15)),
                            () -> input.set(0, 0));

            forceInput = forceInput.lerp(input, 0.1f);

            float yaw = location.getYaw();
            float radYaw = Math.toRadians(yaw);

            float sinYaw = Math.sin(radYaw);
            float cosYaw = Math.cos(radYaw);

            Vector3f forward = new Vector3f(-sinYaw, 0, cosYaw);

            delta.add(forward.mul(0.05f).mul(forceInput.y).mul((forceInput.y > 0 ? component.speedForward : component.speedBackward) / 20));

            int deltaSign = forceInput.y < 0 ? -1 : 1;

            if (!tryRotateEntity(marker, deltaSign * delta.length() * forceInput.x * -component.speedAngle)) {
                Vector3f right = new Vector3f(cosYaw, 0, sinYaw);
                delta.add(right.mul(0.001f).mul(deltaSign));
            }
        }

        delta.x = normalFlat(delta.x, 0.0001f);
        delta.y = normalFlat(delta.y, 0.001f);
        delta.z = normalFlat(delta.z, 0.0001f);

        forceInput.x = normalFlat(forceInput.x, 0.00001f);
        forceInput.y = normalFlat(forceInput.y, 0.00001f);

        Vec3D pos = marker.position();
        marker.move(EnumMoveType.SELF, new Vec3D(delta.x, delta.y, delta.z)
                .add(clampToBounds(pos, world.getWorldBorder()).subtract(pos))
        );
        Vec3D newPos = marker.position();

        Vec3D moveDelta = newPos.subtract(pos);
        lastDelta = new Vector3f((float) moveDelta.x, (float) moveDelta.y, (float) moveDelta.z);
    }
    @Override public void onFirstTick(CustomEntityMetadata metadata, EntityMarkerEventTick event) {
        BoatComponent component = component();
        metadata.marker.setDimensions(new RotatedEntitySize(component.width, component.height, component.length, false));
        metadata.marker.noPhysics = false;
        metadata.marker.setNoGravity(false);
        metadata.marker.setTickable(true);
    }
    @Override public void onInput(CustomEntityMetadata metadata, EntityMarkerEventInput event) {
        event.getInputDisplay()
                .ofKey("sit")
                .stream()
                .flatMap(v -> v instanceof ISitter sitter && sitter.sitter().filter(event.getPlayer()::equals).isPresent()
                        ? Stream.of(sitter)
                        : Stream.empty())
                .findFirst()
                .ifPresent(sitter -> {
                    if (event.isSneaking()) {
                        sitter.unsit();
                        if (sitter.hasThisKey("sit.main")) {
                            input.x = 0;
                            input.y = 0;
                        }
                        return;
                    }
                    if (sitter.hasThisKey("sit.main")) {
                        input.x = event.getSidewaysSpeed();
                        input.y = event.getForwardSpeed();
                    }
                });
    }
    @Override public EnumInteractionResult onInteract(CustomEntityMetadata metadata, EntityMarkerEventInteract event) {
        return event.getClickDisplay()
                .ofKey("sit")
                .stream()
                .flatMap(v -> v instanceof ISitter sitter && !sitter.hasSitter() ? Stream.of(sitter) : Stream.empty())
                .findFirst()
                .filter(v -> v.sit(event.getPlayer(), IExitLocation.keyed("sit.exit")))
                .map(v -> EnumInteractionResult.CONSUME)
                .orElse(EnumInteractionResult.PASS);
    }
}

