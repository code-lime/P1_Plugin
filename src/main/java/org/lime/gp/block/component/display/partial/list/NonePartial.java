package org.lime.gp.block.component.display.partial.list;

import org.lime.docs.IIndexDocs;
import org.lime.docs.json.JObject;
import org.lime.gp.block.component.display.partial.Partial;
import org.lime.gp.block.component.display.partial.PartialEnum;

import com.google.gson.JsonObject;
import org.lime.gp.docs.IDocsLink;

public class NonePartial extends Partial {
    public NonePartial(int distanceChunk, JsonObject json) { super(distanceChunk, json); }
    public NonePartial(int distanceChunk) { this(distanceChunk, new JsonObject()); }
    @Override public PartialEnum type() { return PartialEnum.None; }

    public static JObject docs(IDocsLink docs, IIndexDocs variable) {
        return Partial.docs(docs, variable);
    }
}