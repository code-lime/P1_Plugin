package org.lime.gp.module;

import net.minecraft.network.protocol.game.PacketPlayOutEntityEquipment;
import org.lime.core;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.lime;

public class FixEquipment {
    public static core.element create() {
        return core.element.create(FixEquipment.class)
                .withInit(FixEquipment::init);
    }
    private static boolean DEBUG = false;
    private static int index = 0;
    public static void init() {
        AnyEvent.addEvent("debug.equipment", AnyEvent.type.owner_console, p -> {
            DEBUG = !DEBUG;
            lime.logOP("[FixEquipment] Debug: " + (DEBUG ? "Enable" : "Disable"));
        });
        PacketManager.adapter()
                .add(PacketPlayOutEntityEquipment.class, (packet, sender) -> {
                    if (DEBUG) {
                        int id = index++;
                        lime.logOP("[Packet:"+id+"] Sending... " + packet.getSlots().isEmpty());
                        lime.once(() -> lime.logOP("[Packet:"+id+"] Sended: " + packet.getSlots().isEmpty()), 0.5);
                    }
                    if (packet.getSlots().isEmpty()) {
                        sender.setCancelled(true);
                    }
                })
                .listen();
    }
}
