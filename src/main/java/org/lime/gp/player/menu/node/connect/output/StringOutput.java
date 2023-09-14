package org.lime.gp.player.menu.node.connect.output;

import org.lime.gp.player.menu.node.connect.IOutput;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.List;

public class StringOutput extends IOutput {
    public StringOutput(String key, List<Toast2<Integer, String>> target) {
        super(key, target);
    }
}
