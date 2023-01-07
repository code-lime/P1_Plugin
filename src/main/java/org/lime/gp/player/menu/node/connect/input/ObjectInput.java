package org.lime.gp.player.menu.node.connect.input;

import org.lime.gp.player.menu.node.connect.IInput;

public class ObjectInput extends IInput {
    public Object value;
    public ObjectInput(String key, Object value) {
        super(key);
        this.value = value;
    }
    @Override public Object getDefault() { return value; }
}
