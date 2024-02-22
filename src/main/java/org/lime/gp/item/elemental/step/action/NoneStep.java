package org.lime.gp.item.elemental.step.action;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import org.bukkit.entity.Player;
import org.lime.docs.json.JObject;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.elemental.step.IStep;

@Step(name = "none")
public final class NoneStep implements IStep<NoneStep> {
    public static final NoneStep Instance = new NoneStep();
    public static NoneStep instance() { return Instance; }

    private NoneStep() {}
    @Override public void execute(Player player, DataContext context, Transformation location) { }
    @Override public NoneStep parse(JsonObject json) { return Instance; }

    /*@Override public JObject docs(IDocsLink docs) { return JObject.of(); }*/
}
