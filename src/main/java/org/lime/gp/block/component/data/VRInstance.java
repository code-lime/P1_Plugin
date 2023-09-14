package org.lime.gp.block.component.data;

import org.lime.gp.block.BlockComponentInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.list.VRComponent;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;

public class VRInstance extends BlockComponentInstance<VRComponent> {
    public VRInstance(VRComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
    }


/* 
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
    }*/

    @Override public void read(JsonObjectOptional json) {

    }
    @Override public json.builder.object write() {
        return json.object();
    }
}
