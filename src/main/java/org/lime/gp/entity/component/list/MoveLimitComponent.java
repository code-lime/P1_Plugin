package org.lime.gp.entity.component.list;

import com.google.gson.JsonObject;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.EntityInfo;
import org.lime.gp.entity.component.ComponentDynamic;
import org.lime.gp.entity.component.InfoComponent;
import org.lime.gp.entity.component.data.MoveLimitInstance;
import org.lime.gp.item.data.Checker;
import org.lime.system.range.IRange;

import java.util.HashMap;
import java.util.Map;

@InfoComponent.Component(name = "move_limit")
public class MoveLimitComponent extends ComponentDynamic<JsonObject, MoveLimitInstance> {
    public final double total;
    public final Map<Checker, IRange> repair = new HashMap<>();
    public MoveLimitComponent(EntityInfo info, JsonObject json) {
        super(info, json);
        total = json.get("total").getAsDouble();
        json.get("repair")
                .getAsJsonObject()
                .entrySet()
                .forEach(kv -> repair.put(Checker.createCheck(kv.getKey()), IRange.parse(kv.getValue().getAsString())));
    }

    @Override public MoveLimitInstance createInstance(CustomEntityMetadata metadata) { return new MoveLimitInstance(this, metadata); }
}
