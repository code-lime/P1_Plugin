package org.lime.gp.player.menu.node.connect.output;

import org.lime.gp.player.menu.node.connect.IOutput;
import org.lime.system;

import java.util.List;

public class IntOutput extends IOutput {
    public IntOutput(String key, List<system.Toast2<Integer, String>> target) {
        super(key, target);
    }
}
