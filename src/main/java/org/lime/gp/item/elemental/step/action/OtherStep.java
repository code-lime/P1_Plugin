package org.lime.gp.item.elemental.step.action;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.bukkit.entity.Player;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Elemental;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.elemental.step.IStep;

@Step(name = "other")
public record OtherStep(String step) implements IStep<OtherStep> {
    @Override public void execute(Player player, DataContext context, Transformation location) { Elemental.execute(player, context, step, location); }

    public OtherStep parse(JsonObject json) {
        return new OtherStep(json.get("other").getAsString());
    }

    /*@Override public JObject docs(IDocsLink docs) {
        return JObject.of(
                JProperty.require(IName.raw("other"), IJElement.link(docs.elementalName()), IComment.text("Вызываемый элементаль"))
        );
    }*/
}
