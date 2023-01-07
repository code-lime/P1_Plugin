package org.lime.gp.block.component.data;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.lime.gp.block.BlockComponentInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.list.VRComponent;
import org.lime.gp.lime;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class VRInstance extends BlockComponentInstance<VRComponent> {
    public VRInstance(VRComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
    }



    private static void sendToServer(Player player, String server) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            try {
                out.writeUTF("Connect");
                out.writeUTF(server);
            } catch (Exception e) {
                e.printStackTrace();
            }
            player.sendPluginMessage(lime._plugin, "BungeeCord", b.toByteArray());
        } catch (org.bukkit.plugin.messaging.ChannelNotRegisteredException e) {
            Bukkit.getLogger().warning(" ERROR - Usage of bungeecord connect effects is not possible. Your server is not having bungeecord support (Bungeecord channel is not registered in your minecraft server)!");
        }
    }

    @Override public void read(JsonObjectOptional json) {

    }
    @Override public system.json.builder.object write() {
        return system.json.object();
    }
}
