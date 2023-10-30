package org.lime.gp.entity.component.display.partial.list;

import com.google.gson.JsonObject;
import org.lime.gp.entity.component.display.partial.Partial;
import org.lime.gp.entity.component.display.partial.PartialEnum;

public class NonePartial extends Partial {
    public NonePartial(int distanceChunk, JsonObject json) { super(distanceChunk, json); }
    public NonePartial(int distanceChunk) { this(distanceChunk, new JsonObject()); }

    @Override public PartialEnum type() { return PartialEnum.None; }
}
