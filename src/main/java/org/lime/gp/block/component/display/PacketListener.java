package org.lime.gp.block.component.display;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import it.unimi.dsi.fastutil.shorts.Short2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.lime.display.invokable.PacketInvokable;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.display.block.IBlock;
import org.lime.gp.block.component.display.block.ITileBlock;
import org.lime.gp.block.component.display.event.BlockMarkerEventInteract;
import org.lime.gp.lime;
import org.lime.gp.module.TimeoutData;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast1;

import java.util.Optional;
import java.util.UUID;

public class PacketListener {
    private static boolean isDisable(Player player) {
        return player.getScoreboardTags().contains("blocks.disable");
    }

    private static Optional<IBlock> tryReplace(Player player, WorldServer world, BlockPosition position, IBlockData data) {
        return tryReplace(player, world, position, data, false);
    }
    private static Optional<IBlock> tryReplace(Player player, WorldServer world, BlockPosition position, IBlockData data, boolean debug) {
        return CacheBlockDisplay.getCacheBlock(position, world.uuid).map(v -> v.cache(player.getUniqueId()));
    }
    public static void onPacket(PacketPlayOutBlockChange packet, PacketEvent event) {
        if (isDisable(event.getPlayer())) return;
        Player player = event.getPlayer();
        WorldServer world = ((CraftWorld)player.getWorld()).getHandle();
        UUID worldUUID = world.uuid;
        tryReplace(player, world, packet.getPos(), packet.blockState)
                .ifPresent(block -> {
                    block.data().ifPresent(data -> event.setPacket(new PacketContainer(event.getPacketType(), new PacketPlayOutBlockChange(packet.getPos(), data))));
                    if (block instanceof ITileBlock tileBlock) tileBlock.packet()
                            .ifPresent(data -> lime.invokable(new PacketInvokable<>(player, worldUUID, data, 1)));
                });
    }
    public static void onPacket(PacketPlayOutMultiBlockChange packet, PacketEvent event) {
        if (isDisable(event.getPlayer())) return;
        Toast1<SectionPosition> section = Toast.of(null);
        Short2ObjectMap<IBlockData> shorts = new Short2ObjectArrayMap<>();
        packet.runUpdates((pos, state) -> {
            if (section.val0 == null) section.val0 = SectionPosition.of(pos);
            shorts.put(SectionPosition.sectionRelativePos(pos), state);
        });
        if (section.val0 == null || shorts.isEmpty()) return;
        long chunkID = section.val0.chunk().longKey;
        Player player = event.getPlayer();
        TimeoutData.values(new CustomTileMetadata.ChunkGroup(player.getWorld().getUID(), chunkID), CustomTileMetadata.ChunkBlockTimeout.class)
                .forEach(timeout -> {
                    short posID = SectionPosition.sectionRelativePos(timeout.pos);
                    if (shorts.remove(posID) != null) return;
                    timeout.sync(player);
                });
        event.setPacket(new PacketContainer(event.getPacketType(), new PacketPlayOutMultiBlockChange(section.val0, shorts)));
    }
    public static void onPacket(ClientboundLevelChunkWithLightPacket packet, PacketEvent event) {
        if (isDisable(event.getPlayer())) return;
        long chunk = ChunkCoordIntPair.asLong(packet.getX(), packet.getZ());
        Player player = event.getPlayer();
        TimeoutData.values(new CustomTileMetadata.ChunkGroup(player.getWorld().getUID(), chunk), CustomTileMetadata.ChunkBlockTimeout.class)
                .forEach(block -> block.sync(player));
    }
    public static void onPacket(PacketPlayOutTileEntityData packet, PacketEvent event) {
        if (isDisable(event.getPlayer())) return;
        if (Optional.ofNullable(packet.getTag()).filter(v -> v.contains("lime:sended") && v.getBoolean("lime:sended")).isEmpty()) return;
        Player player = event.getPlayer();
        WorldServer world = ((CraftWorld)player.getWorld()).getHandle();
        BlockPosition pos = packet.getPos();
        packet.getType()
                .validBlocks
                .stream()
                .findAny()
                .flatMap(state -> tryReplace(player, world, pos, state.defaultBlockState()))
                .map(v -> v instanceof ITileBlock b ? b : null)
                .flatMap(ITileBlock::packet)
                .ifPresent(v -> event.setPacket(new PacketContainer(event.getPacketType(), v)));
    }

    public static void onPacket(PacketPlayInUseEntity packet, PacketEvent event) {
        lime.invokeSync(() -> BlockMarkerEventInteract.execute(event.getPlayer(), packet));
    }
}
