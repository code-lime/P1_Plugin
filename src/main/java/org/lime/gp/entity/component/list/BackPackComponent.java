package org.lime.gp.entity.component.list;

import com.google.gson.JsonNull;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.EntityInfo;
import org.lime.gp.entity.component.ComponentDynamic;
import org.lime.gp.entity.component.InfoComponent;
import org.lime.gp.entity.component.data.BackPackInstance;

@InfoComponent.Component(name = "backpack")
public final class BackPackComponent extends ComponentDynamic<JsonNull, BackPackInstance> {
    public BackPackComponent(EntityInfo info) {
        super(info);
    }

    @Override
    public BackPackInstance createInstance(CustomEntityMetadata metadata) {
        return new BackPackInstance(this, metadata);
    }
}
