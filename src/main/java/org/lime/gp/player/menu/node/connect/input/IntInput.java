package org.lime.gp.player.menu.node.connect.input;

import org.lime.gp.player.menu.node.connect.IInput;

public class IntInput extends IInput {
    public int value;
    public IntInput(String key, int value) {
        super(key);
        this.value = value;
    }
    @Override public Integer getDefault() { return value; }
}
