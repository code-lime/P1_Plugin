package org.lime.gp.entity.component.list;

import com.google.gson.JsonNull;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.EntityInfo;
import org.lime.gp.entity.component.ComponentDynamic;
import org.lime.gp.entity.component.InfoComponent;
import org.lime.gp.entity.component.data.ActionInstance;

@InfoComponent.Component(name = "action")
public class ActionComponent extends ComponentDynamic<JsonNull, ActionInstance> {
    public ActionComponent(EntityInfo info) { super(info); }
    @Override public ActionInstance createInstance(CustomEntityMetadata metadata) { return new ActionInstance(this, metadata); }
}
