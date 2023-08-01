package org.lime.gp.block.component.display.instance.list;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.lime.gp.block.component.InfoComponent;

import net.minecraft.world.item.ItemStack;
import org.lime.gp.block.component.display.partial.list.ViewPartial;

public record ItemDisplayObject(
        Location location,
        ItemStack item,
        double back_angle,
        double offset_rotation,
        Vector offset_translation,
        Vector offset_scale,
        InfoComponent.Rotation.Value rotation,
        UUID index
) {
    public static ItemDisplayObject of(Location location, ItemStack item, ViewPartial partial) {
        return new ItemDisplayObject(location, item, partial.back_angle(), partial.offset_rotation(), partial.offset_translation(), partial.offset_scale(), partial.rotation(), partial.uuid());
    }
}