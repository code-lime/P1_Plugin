package org.lime.gp.block.component.display.invokable;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.game.PacketPlayOutBlockChange;
import net.minecraft.server.level.ChunkProviderServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.level.ChunkCoordIntPair;
import org.lime.invokable.IInvokable;

public class BlockDirtyInvokable extends IInvokable {
    public final WorldServer world;
    public final BlockPosition position;
    private final String key;

    public BlockDirtyInvokable(String key, WorldServer world, BlockPosition position, int waitTicks) {
        super(waitTicks);
        this.key = key;
        this.world = world;
        this.position = position;
    }

    @Override public void invoke() throws Throwable {
        ChunkProviderServer cps = world.getChunkSource();
        PacketPlayOutBlockChange packet = new PacketPlayOutBlockChange(world, position);
        BlockInvokableLogger.log(getClass().getSimpleName(), key, position);
        cps.chunkMap.getPlayers(new ChunkCoordIntPair(position), false).forEach(player -> {
            PlayerConnection connection = player.connection;
            if (connection.isDisconnected()) return;
            connection.send(packet);
        });
    }
}
