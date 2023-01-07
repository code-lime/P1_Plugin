package org.lime.gp.player.menu.node.connect.input;

import org.lime.gp.player.menu.node.connect.IInput;

public class StringInput extends IInput {
    public String text;
    public StringInput(String key, String text) {
        super(key);
        this.text = text;
    }
    @Override public Object getDefault() { return text; }
}
