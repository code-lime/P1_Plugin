package org.lime.gp.module;

import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Player;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.block.component.data.SafeBoxInstance;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.list.SafeBoxComponent;
import org.lime.gp.coreprotect.CoreProtectHandle;
import org.lime.plugin.CoreElement;
import org.lime.system.utils.RandomUtils;

import javax.annotation.Nullable;
import java.util.UUID;

public class LockPicking {
    public static CoreElement create() {
        return CoreElement.create(LockPicking.class)
                .withInit(LockPicking::init);
    }

    private static void init() {
        AnyEvent.addEvent("open.block", AnyEvent.type.other, v -> v
                .createParam(Integer::parseInt, "[x]")
                .createParam(Integer::parseInt, "[y]")
                .createParam(Integer::parseInt, "[z]"), LockPicking::executeOpen);
        AnyEvent.addEvent("open.block", AnyEvent.type.other, v -> v
                .createParam(Integer::parseInt, "[x]")
                .createParam(Integer::parseInt, "[y]")
                .createParam(Integer::parseInt, "[z]")
                .createParam("[sound]"), LockPicking::executeOpenSound);
        AnyEvent.addEvent("open.safebox", AnyEvent.type.other, v -> v
                        .createParam(UUID::fromString, "[block_uuid:uuid]")
                        .createParam(Integer::parseInt, "[x:int]")
                        .createParam(Integer::parseInt, "[y:int]")
                        .createParam(Integer::parseInt, "[z:int]"),
                (p, block_uuid, x, y, z) -> org.lime.gp.block.Blocks.of(p.getWorld().getBlockAt(x,y,z))
                        .flatMap(org.lime.gp.block.Blocks::customOf)
                        .filter(v -> v.key.uuid().equals(block_uuid))
                        .flatMap(v -> v.list(SafeBoxInstance.class).findAny())
                        .ifPresent(SafeBoxInstance::executeOpen)
        );
    }
    private static void executeOpen(Player player, int x, int y, int z) { executeOpenSound(player, x, y, z, null); }
    private static void executeOpenSound(Player player, int x, int y, int z, @Nullable String sound) {
        World world = player.getWorld();
        Block block = world.getBlockAt(x, y, z);
        BlockData data = block.getBlockData();
        if (!(data instanceof Openable openable)) return;
        openable.setOpen(!openable.isOpen());
        block.setBlockData(data);
        if (sound != null) world.playSound(block.getLocation().add(0.5f,0.5f,0.5f), sound, SoundCategory.BLOCKS, 1.0f, (float) RandomUtils.rand(0.0, 1.0) * 0.1f + 0.9f);
        CoreProtectHandle.logInteract(block, player);
    }
}
