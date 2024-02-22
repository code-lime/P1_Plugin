package org.lime.gp.block.component.display.partial.list;

import com.google.gson.JsonObject;
import org.lime.display.models.shadow.IBuilder;
import org.lime.docs.IIndexDocs;
import org.lime.docs.json.*;
import org.lime.gp.block.component.display.partial.PartialEnum;
import org.lime.gp.docs.IDocsLink;

public class ModelPartial extends FramePartial implements IModelPartial {
    private final String model;
    private final double modelDistance;
    private IBuilder generic = null;

    public ModelPartial(int distanceChunk, JsonObject json) {
        super(distanceChunk, json);
        this.model = parseModel(json.get("model"));
        this.modelDistance = json.has("model_distance") ? json.get("model_distance").getAsDouble() : Double.POSITIVE_INFINITY;
    }

    @Override public void generic(IBuilder generic) { this.generic = generic; }
    @Override public IBuilder generic() { return this.generic; }
    @Override public double modelDistance() { return this.modelDistance; }
    @Override public String modelKey() { return this.model; }

    @Override public PartialEnum type() { return PartialEnum.Model; }
    @Override public String toString() { return super.toString()+ "^" + model; }

    public static JObject docs(IDocsLink docs, IIndexDocs variable) {
        return FramePartial.docs(docs, variable, false).addFirst(
                JProperty.optional(IName.raw("model"), IJElement.link(docs.model()), IComment.text("Отображемая модель")),
                JProperty.optional(IName.raw("model_distance"), IJElement.raw(10.0), IComment.text("Максимальная дальность отображения модели"))
        );
    }
}