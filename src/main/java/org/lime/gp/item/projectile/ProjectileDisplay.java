package org.lime.gp.item.projectile;

import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemDisplayContext;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.lime.display.ObjectDisplay;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.TableDisplaySetting;

import javax.annotation.Nullable;

public class ProjectileDisplay extends ObjectDisplay<ProjectileFrame, Display.ItemDisplay> {
    @Override public double getDistance() { return 128; }

    @Override public Location location() { return (lastFrame == null ? initFrame : lastFrame).location(); }

    private Vector offset;

    private final ProjectileFrame initFrame;
    private @Nullable ProjectileFrame lastFrame;
    public ProjectileDisplay(ProjectileFrame initFrame) {
        this.initFrame = initFrame;
        this.offset = initFrame.location().toVector();
        postInit();
    }

    private boolean syncPosition = false;
    @Override public void update(ProjectileFrame data, double delta) {
        super.update(data, delta);
        if (lastFrame == data) return;
        if (lastFrame != null) {
            if (lastFrame.isEquals(data)) {
                if (!syncPosition) {
                    offset = data.location().toVector();
                    entity.moveTo(offset.getX(), offset.getY(), offset.getZ());
                    hideAll();
                    super.update(data, 0);
                    syncPosition = true;
                }
            } else {
                syncPosition = false;
            }
        }
        lastFrame = data;
        entity.setInterpolationDelay(0);
        entity.setTransformation(data.withOffset(offset));
        invokeAll(this::sendDataWatcher);
    }

    @Override protected Display.ItemDisplay createEntity(Location location) {
        Display.ItemDisplay projectile = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, ((CraftWorld)location.getWorld()).getHandle());
        projectile.setPos(offset.getX(), offset.getY(), offset.getZ());
        projectile.setViewRange(Float.MAX_VALUE);
        projectile.setInterpolationDuration(ProjectileFrame.FRAME_DURATION);
        projectile.setTransformation(initFrame.transformation());
        projectile.setItemTransform(ItemDisplayContext.HEAD);
        ItemStack initItem = initFrame.item();
        projectile.setItemStack(Items.getOptional(TableDisplaySetting.class, initItem)
                .flatMap(v -> v.of(TableDisplaySetting.TableType.inventory, "projectile"))
                .map(v -> v.display(initItem))
                .orElseGet(() -> CraftItemStack.asNMSCopy(initItem)));
        return projectile;
    }
}
