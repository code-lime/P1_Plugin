package org.lime.gp.entity;

import org.lime.gp.entity.component.ComponentDynamic;

public abstract class EntityComponentInstance<T extends ComponentDynamic<?, ?>> extends EntityInstance {
    public EntityComponentInstance(T component, CustomEntityMetadata metadata) { super(component, metadata); }
    @SuppressWarnings("unchecked")
    @Override public T component() { return (T)super.component(); }
}






