package org.lime.gp.player.module.xaeros.packet;

import org.bukkit.entity.Player;
import org.lime.gp.player.module.xaeros.PluginChannel;

public interface IInPacket {
    void handle(PluginChannel channel, Player player);
}
