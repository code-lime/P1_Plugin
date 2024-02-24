package org.lime.gp.block.component.display.display;

import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Vector2d;
import org.joml.Vector3f;
import org.lime.display.DisplayManager;
import org.lime.display.EditedDataWatcher;
import org.lime.display.ObjectDisplay;
import org.lime.gp.block.component.display.instance.DisplayMap;
import org.lime.gp.block.component.display.instance.list.ItemDisplayObject;
import org.lime.gp.module.TimeoutData;

import com.mojang.math.Transformation;

import org.lime.system.toast.*;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class BlockItemDisplay extends ObjectDisplay<ItemDisplayObject, Display.ItemDisplay> {
    private static final DataWatcherObject<ItemStack> DATA_ITEM_STACK_ID = EditedDataWatcher.getDataObject(Display.ItemDisplay.class, "DATA_ITEM_STACK_ID");

    @Override public double getDistance() { return Double.POSITIVE_INFINITY; }
    @Override public Location location() { return data.location(); }

    public final UUID block_uuid;
    public final UUID player_uuid;

    public ItemDisplayObject data;

    @Override public boolean isFilter(Player player) {
        if (!player_uuid.equals(player.getUniqueId())) return false;
        double back_angle = data.back_angle();
        if (back_angle <= 0) return true;
        if (player.getScoreboardTags().contains("back_angle.full")) return true;
        back_angle /= 2;
        Location location = player.getLocation();
        Vector point = location.toVector();
        double yaw = location.getYaw();

        Vector toPlayer = point.subtract(location().toVector());
        double angle = vectorAngle(
                new Vector2d(-Math.sin(Math.toRadians(yaw)), Math.cos(Math.toRadians(yaw))),
                new Vector2d(toPlayer.getX(), toPlayer.getZ())
        );
        double current_angle = Math.toDegrees(angle);
        return Math.abs(current_angle) >= back_angle;
    }
    // Return the angle between vector p11 --> p12 and p21 --> p22.
    // Angles less than zero are to the left. Angles greater than
    // zero are to the right.
    private static double vectorAngle(Vector2d v1, Vector2d v2) {
        // Calculate the vector lengths.
        double len1 = Math.sqrt(v1.x * v1.x + v1.y * v1.y);
        double len2 = Math.sqrt(v2.x * v2.x + v2.y * v2.y);

        // Use the dot product to get the cosine.
        double dot_product = v1.x * v2.x + v1.y * v2.y;
        double cos = dot_product / len1 / len2;

        // Use the cross product to get the sine.
        double cross_product = v1.x * v2.y - v1.y * v2.x;
        double sin = cross_product / len1 / len2;

        // Find the angle.
        double angle = Math.acos(cos);
        if (sin < 0) angle = -angle;
        return angle;
    }
    /*
    // Return the angle between vector p11 --> p12 and p21 --> p22.
    // Angles less than zero are to the left. Angles greater than
    // zero are to the right.
    private static double vectorAngle(Vec2F p11, Vec2F p12, Vec2F p21, Vec2F p22) {
        // Find the vectors.
        Vec2F v1 = new Vec2F(p12.x - p11.x, p12.y - p11.y);
        Vec2F v2 = new Vec2F(p22.x - p21.x, p22.y - p21.y);

        // Calculate the vector lengths.
        double len1 = Math.sqrt(v1.x * v1.x + v1.y * v1.y);
        double len2 = Math.sqrt(v2.x * v2.x + v2.y * v2.y);

        // Use the dot product to get the cosine.
        double dot_product = v1.x * v2.x + v1.y * v2.y;
        double cos = dot_product / len1 / len2;

        // Use the cross product to get the sine.
        double cross_product = v1.x * v2.y - v1.y * v2.x;
        double sin = cross_product / len1 / len2;

        // Find the angle.
        double angle = Math.acos(cos);
        if (sin < 0) angle = -angle;
        return angle;
    }
    */

    private BlockItemDisplay(UUID block_uuid, UUID player_uuid, ItemDisplayObject data) {
        super(data.location());

        this.block_uuid = block_uuid;
        this.player_uuid = player_uuid;
        this.data = data;
        postInit();
    }

    private static void updateData(Display.ItemDisplay itemFrame, ItemDisplayObject data) {
        Vector offset_translation = data.offset_translation();
        Vector offset_scale = data.offset_scale();
        itemFrame.setTransformation(
                new Transformation(
                        new Vector3f((float) offset_translation.getX(), (float)offset_translation.getY(), (float)offset_translation.getZ()),
                        null,
                        new Vector3f((float) offset_scale.getX(), (float)offset_scale.getY(), (float)offset_scale.getZ()),
                        null)
        );
        itemFrame.setYRot(data.rotation().angle + (float)data.offset_rotation());
    }
    @Override public void update(ItemDisplayObject data, double delta) {
        super.update(data, delta);
        if (this.data.index().equals(data.index())) {
            this.data = data;
            return;
        }
        this.data = data;
        updateData(entity, data);
        invokeAll(this::sendDataWatcher);
    }

    @Override protected void editDataWatcher(Player player, EditedDataWatcher dataWatcher) {
        super.editDataWatcher(player, dataWatcher);
        dataWatcher.setCustom(DATA_ITEM_STACK_ID, data.item());
    }
    @Override protected Display.ItemDisplay createEntity(Location location) {
        Display.ItemDisplay itemFrame = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, ((CraftWorld)location.getWorld()).getHandle());
        itemFrame.setPos(location.getBlockX() + 0.5, location.getBlockY() + 0.5, location.getBlockZ() + 0.5);
        itemFrame.setViewRange(1000);
        updateData(itemFrame, data);
        return itemFrame;
    }

    public static class BlockItemManager extends DisplayManager<Toast2<UUID, UUID>, ItemDisplayObject, BlockItemDisplay> {
        @Override public boolean isAsync() { return true; }
        @Override public boolean isFast() { return true; }

        @Override public Map<Toast2<UUID, UUID>, ItemDisplayObject> getData() {
            return TimeoutData.stream(DisplayMap.class)
                    .flatMap(kv -> kv.getValue().viewMap.entrySet().stream().map(v -> Toast.of(kv.getKey(), v.getKey(), v.getValue())))
                    .collect(Collectors.toMap(kv -> Toast.of(kv.val0, kv.val1), kv -> kv.val2));
        }
        @Override public BlockItemDisplay create(Toast2<UUID, UUID> uuid, ItemDisplayObject display) {
            return new BlockItemDisplay(uuid.val0, uuid.val1, display);
        }
    }

    public static BlockItemManager manager() { return new BlockItemManager(); }
}