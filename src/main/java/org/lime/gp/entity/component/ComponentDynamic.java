package org.lime.gp.entity.component;

import com.google.gson.JsonElement;
import org.lime.gp.entity.EntityInfo;
import org.lime.gp.entity.EntityInstance;
import org.lime.gp.entity.CustomEntityMetadata;

public abstract class ComponentDynamic<T extends JsonElement, I extends EntityInstance> extends ComponentStatic<T> {
    public ComponentDynamic(EntityInfo info) { super(info); }
    public ComponentDynamic(EntityInfo info, T json) { super(info, json); }
    public abstract I createInstance(CustomEntityMetadata metadata);
}
