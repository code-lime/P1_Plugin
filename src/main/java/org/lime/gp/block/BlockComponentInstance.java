package org.lime.gp.block;

import org.lime.gp.block.component.ComponentDynamic;

public abstract class BlockComponentInstance<T extends ComponentDynamic<?, ?>> extends BlockInstance {
    public BlockComponentInstance(T component, CustomTileMetadata metadata) { super(component, metadata); }
    @SuppressWarnings("unchecked")
    @Override public T component() { return (T)super.component(); }
}






