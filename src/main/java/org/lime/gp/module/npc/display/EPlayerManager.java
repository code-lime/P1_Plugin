package org.lime.gp.module.npc.display;

import org.lime.display.DisplayManager;
import org.lime.gp.module.DrawText;
import org.lime.gp.module.npc.EPlayerModule;
import org.lime.gp.module.npc.eplayer.IEPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class EPlayerManager extends DisplayManager<String, IEPlayer, EPlayerDisplay> implements DrawText.IShowGroup {
    @Override public boolean isFast() { return true; }
    @Override public boolean isAsync() { return true; }

    @Override public Map<String, IEPlayer> getData() { return EPlayerModule.createData(); }
    @Override public EPlayerDisplay create(String key, IEPlayer npc) { return new EPlayerDisplay(npc); }
    @Override public Stream<DrawText.IShow> list() { return getDisplays().values().stream().flatMap(EPlayerDisplay::nickList); }
}
