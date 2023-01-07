package org.lime.gp.player.menu.node.connect.input;

import org.lime.gp.player.menu.node.connect.IInput;

public class ActionInput extends IInput {
    public ActionInput(String key) {
        super(key);
    }
    @Override public Object getDefault() { return false; }
}
