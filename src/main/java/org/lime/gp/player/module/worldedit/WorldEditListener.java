package org.lime.gp.player.module.worldedit;

import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.lime.gp.lime;

public class WorldEditListener {
    @Subscribe
    public void onEditSessionEvent(EditSessionEvent event) {
        Actor actor = event.getActor();
        if (actor == null) return;
        if (!(Bukkit.getPlayer(actor.getUniqueId()) instanceof CraftPlayer player)) return;

        World _world = event.getWorld();
        if (_world == null) return;
        if (!(Bukkit.getWorld(_world.getName()) instanceof CraftWorld world)) return;

        event.setExtent(new WorldEditHandler(world, player, event.getExtent()));
    }
}
