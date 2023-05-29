package org.lime.gp.block.component.display.instance.list;

import java.util.UUID;

import org.bukkit.Location;
import org.lime.gp.block.component.InfoComponent;

import net.minecraft.world.item.ItemStack;

public record ItemFrameDisplayObject(Location location, ItemStack item, InfoComponent.Rotation.Value rotation, UUID index) {
    public static ItemFrameDisplayObject of(Location location, ItemStack item, InfoComponent.Rotation.Value rotation, UUID index) {
        return new ItemFrameDisplayObject(location, item, rotation, index);
    }
}