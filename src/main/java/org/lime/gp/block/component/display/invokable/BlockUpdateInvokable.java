package org.lime.gp.block.component.display.invokable;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.game.PacketPlayOutBlockChange;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import org.bukkit.entity.Player;
import org.lime.display.invokable.IWorldInvokable;

import java.util.Collection;
import java.util.UUID;

public class BlockUpdateInvokable extends IWorldInvokable {
    public final BlockPosition position;
    private final String key;

    public BlockUpdateInvokable(String key, Player player, UUID worldUUID, BlockPosition position, int waitTicks) {
        super(player, worldUUID, waitTicks);
        this.key = key;
        this.position = position;
    }
    public BlockUpdateInvokable(String key, Collection<? extends Player> players, UUID worldUUID, BlockPosition position, int waitTicks) {
        super(players, worldUUID, waitTicks);
        this.key = key;
        this.position = position;
    }

    @Override public void worldInvoke(PlayerConnection connection, WorldServer world) throws Throwable {
        BlockInvokableLogger.log(getClass().getSimpleName(), key, position);
        connection.send(new PacketPlayOutBlockChange(world, position));
    }
}
