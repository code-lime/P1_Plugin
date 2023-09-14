package org.lime.gp.player.menu.node.connect.output;

import org.lime.gp.player.menu.node.connect.IOutput;
import org.lime.system.toast.*;

import java.util.List;

public class IntOutput extends IOutput {
    public IntOutput(String key, List<Toast2<Integer, String>> target) {
        super(key, target);
    }
}
