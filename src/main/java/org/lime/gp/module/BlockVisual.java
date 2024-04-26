package org.lime.gp.module;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.game.PacketDebug;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.lime.gp.admin.AnyEvent;
import org.lime.plugin.CoreElement;

public class BlockVisual {
    public static CoreElement create() {
        return CoreElement.create(BlockVisual.class)
                .withInit(BlockVisual::init);
    }

    private static void init() {
        AnyEvent.addEvent(
                "block.visual",
                AnyEvent.type.owner_console,
                v -> v
                        .createParam(Integer::parseInt, "[x]")
                        .createParam(Integer::parseInt, "[y]")
                        .createParam(Integer::parseInt, "[z]")
                        .createParam(Double::parseDouble, "[time]")
                        .createParam("8000FF00", "{color}")
                        .createParam("{message}"),
                (p,x,y,z,time,color,message) -> Bukkit.getWorlds().forEach(world -> {
                    BlockPosition pos = new BlockPosition(x,y,z);
                    if (world instanceof CraftWorld handle)
                        PacketDebug.sendGameTestAddMarker(handle.getHandle(), pos, message, Integer.parseUnsignedInt(color, 16), (int)(time * 1000));
                }));
        AnyEvent.addEvent(
                "block.visual",
                AnyEvent.type.owner_console,
                v -> v
                        .createParam(Integer::parseInt, "[x]")
                        .createParam(Integer::parseInt, "[y]")
                        .createParam(Integer::parseInt, "[z]")
                        .createParam(Double::parseDouble, "[time]")
                        .createParam("8000FF00", "{color}"),
                (p,x,y,z,time,color) -> Bukkit.getWorlds().forEach(world -> {
                    BlockPosition pos = new BlockPosition(x,y,z);
                    if (world instanceof CraftWorld handle)
                        PacketDebug.sendGameTestAddMarker(handle.getHandle(), pos, "", Integer.parseUnsignedInt(color, 16), (int)(time * 1000));
                }));
        AnyEvent.addEvent(
                "block.visual",
                AnyEvent.type.owner_console,
                v -> v
                        .createParam(Integer::parseInt, "[x]")
                        .createParam(Integer::parseInt, "[y]")
                        .createParam(Integer::parseInt, "[z]")
                        .createParam(Double::parseDouble, "[time]"),
                (p,x,y,z,time) -> Bukkit.getWorlds().forEach(world -> {
                    BlockPosition pos = new BlockPosition(x,y,z);
                    if (world instanceof CraftWorld handle)
                        PacketDebug.sendGameTestAddMarker(handle.getHandle(), pos, "", 0x8000FF00, (int)(time * 1000));
                }));
    }
}
