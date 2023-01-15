package org.lime.gp.block.component.display.partial;

import com.google.gson.JsonObject;

public class NonePartial extends Partial {
    public NonePartial(int distanceChunk, JsonObject json) { super(distanceChunk, json); }
    public NonePartial(int distanceChunk) { this(distanceChunk, new JsonObject()); }
    @Override public PartialEnum type() { return PartialEnum.None; }
}