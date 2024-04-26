package org.lime.gp.player.module.worldedit;

import com.sk89q.worldedit.WorldEdit;
import org.lime.plugin.CoreElement;

public class WorldEditService {
    private static final WorldEditListener listener = new WorldEditListener();

    public static CoreElement create() {
        return CoreElement.create(WorldEditService.class)
                .withInit(WorldEditService::init)
                .withUninit(WorldEditService::uninit);
    }

    private static void init() {
        WorldEdit.getInstance().getEventBus().register(listener);
    }
    private static void uninit() {
        WorldEdit.getInstance().getEventBus().unregister(listener);
    }
}
