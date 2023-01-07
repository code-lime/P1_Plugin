package org.lime.gp.player.menu.node.connect;

public abstract class IInput {
    public final String key;
    public IInput(String key) {
        this.key = key;
    }
    public Object orDefault(Object value) {
        return value == null ? this.getDefault() : value;
    }
    public abstract Object getDefault();
}
