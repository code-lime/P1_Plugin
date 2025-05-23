package org.lime.gp.block.component.display;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.lime.Position;
import org.lime.gp.block.component.display.display.BlockItemFrameManager;
import org.lime.plugin.CoreElement;
import org.lime.display.Displays;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.display.block.IModelBlock;
import org.lime.gp.block.component.display.display.BlockItemDisplay;
import org.lime.gp.block.component.display.display.BlockItemFrameDisplay;
import org.lime.gp.block.component.display.display.BlockModelDisplay;
import org.lime.gp.block.component.display.event.BlockMarkerEventInteract;
import org.lime.gp.block.component.display.event.PlayerChunkMoveEvent;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.extension.PacketManager;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class BlockDisplay implements Listener {
    public static final int CHUNK_SIZE = 8;
    public static int getChunkSize(double coord) {
        return (int)Math.ceil((coord + 1) / BlockDisplay.CHUNK_SIZE);
    }

    public static CoreElement create() {
        return CoreElement.create(BlockDisplay.class)
                .withInit(BlockDisplay::init)
                .withInstance();
    };
    public static final BlockModelDisplay.EntityModelManager MODEL_MANAGER = BlockModelDisplay.manager();
    public static final BlockItemFrameManager ITEM_FRAME_MANAGER = BlockItemFrameDisplay.manager();
    public static final BlockItemDisplay.BlockItemManager ITEM_MANAGER = BlockItemDisplay.manager();
    public static void init() {
        PacketManager.adapter()
                .add(PacketPlayOutBlockChange.class, PacketListener::onPacket)
                .add(PacketPlayOutMultiBlockChange.class, PacketListener::onPacket)
                .add(ClientboundLevelChunkWithLightPacket.class, PacketListener::onPacket)
                .add(PacketPlayOutTileEntityData.class, PacketListener::onPacket)
                .add(PacketPlayInUseEntity.class, PacketListener::onPacket)
                .listen();
        AnyEvent.addEvent("resync.chunk", AnyEvent.type.owner, v -> v.createParam("[chunk_x]", "~").createParam("[chunk_z]", "~"), (p,cx,cz) -> {
            Chunk chunk = p.getChunk();
            int chunk_x = Objects.equals(cx, "~") ? chunk.getX() : Integer.parseInt(cx);
            int chunk_z = Objects.equals(cz, "~") ? chunk.getZ() : Integer.parseInt(cz);
            chunk.getWorld().refreshChunk(chunk_x, chunk_z);
        });
        AnyEvent.addEvent("block.display.variable", AnyEvent.type.other, v -> v
                .createParam(UUID::fromString, "[block_uuid:uuid]")
                .createParam(Integer::parseInt, "[x:int]")
                .createParam(Integer::parseInt, "[y:int]")
                .createParam(Integer::parseInt, "[z:int]")
                .createParam("[key:text]")
                .createParam("[value:text]"),
                (p, block_uuid, x, y, z, key, value) -> org.lime.gp.block.Blocks.of(p.getWorld().getBlockAt(x,y,z))
                        .flatMap(org.lime.gp.block.Blocks::customOf)
                        .filter(v -> v.key.uuid().equals(block_uuid))
                        .flatMap(v -> v.list(DisplayInstance.class).findAny())
                        .ifPresent(instance -> instance.set(key, value))
        );
    }
    public static void resetDisplay() {
        Displays.uninitDisplay(MODEL_MANAGER, ITEM_FRAME_MANAGER, ITEM_MANAGER);
        Displays.initDisplay(MODEL_MANAGER, ITEM_FRAME_MANAGER, ITEM_MANAGER);
    }

    public interface Displayable extends CustomTileMetadata.Uniqueable {
        Optional<IModelBlock> onDisplayAsync(Player player, World world, BlockPosition position, IBlockData data);
    }
    public interface Interactable extends CustomTileMetadata.Element {
        void onInteract(CustomTileMetadata metadata, BlockMarkerEventInteract event);
    }
    public static boolean inDebugZone(BlockPosition position) {
        if (position.getY() <= 62) return false;
        if (-517 > position.getX() || position.getX() > -513) return false;
        if (-146 > position.getZ() || position.getZ() > -142) return false;
        return true;
    }
    public static boolean inDebugZone(Position position) {
        if (position.y <= 62) return false;
        if (-517 > position.x || position.x > -513) return false;
        if (-146 > position.z || position.z > -142) return false;
        return true;
    }

    @EventHandler public static void interact(BlockMarkerEventInteract e) {
        org.lime.gp.block.Blocks.customOf(e.getSkull())
                .ifPresent(v -> v.list(Interactable.class).forEach(_v -> _v.onInteract(v, e)));
    }
    @EventHandler public static void move(PlayerChunkMoveEvent e) {
        DisplayInstance.appendDirtyQueue(e.getPlayer().getUniqueId());
    }
}


































