package org.lime.gp.entity.component.list;

import com.google.gson.JsonPrimitive;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.EntityInfo;
import org.lime.gp.entity.component.ComponentDynamic;
import org.lime.gp.entity.component.InfoComponent;
import org.lime.gp.entity.component.data.OwnerInstance;

@InfoComponent.Component(name = "owner")
public class OwnerComponent extends ComponentDynamic<JsonPrimitive, OwnerInstance> {
    public final String entityType;
    public OwnerComponent(EntityInfo info, JsonPrimitive json) {
        super(info, json);
        this.entityType = json.getAsString();
    }

    @Override public OwnerInstance createInstance(CustomEntityMetadata metadata) { return new OwnerInstance(this, metadata); }
}
