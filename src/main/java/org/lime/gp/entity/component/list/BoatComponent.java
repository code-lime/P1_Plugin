package org.lime.gp.entity.component.list;

import com.google.gson.JsonObject;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.EntityInfo;
import org.lime.gp.entity.component.ComponentDynamic;
import org.lime.gp.entity.component.InfoComponent;
import org.lime.gp.entity.component.data.boat.BoatInstance;

@InfoComponent.Component(name = "boat")
public class BoatComponent extends ComponentDynamic<JsonObject, BoatInstance> {
    public final float width;
    public final float height;
    public final float length;

    public final float speedAngle;
    public final float speedForward;
    public final float speedBackward;

    public BoatComponent(EntityInfo info, JsonObject json) {
        super(info, json);
        this.width = json.get("width").getAsFloat();
        this.height = json.get("height").getAsFloat();
        this.length = json.get("length").getAsFloat();

        JsonObject speed = json.getAsJsonObject("speed");
        speedAngle = speed.get("angle").getAsFloat();
        speedForward = speed.get("forward").getAsFloat();
        speedBackward = speed.get("backward").getAsFloat();
    }

    @Override public BoatInstance createInstance(CustomEntityMetadata metadata) { return new BoatInstance(this, metadata); }
}
