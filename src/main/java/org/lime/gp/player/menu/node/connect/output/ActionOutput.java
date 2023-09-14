package org.lime.gp.player.menu.node.connect.output;

import org.bukkit.entity.Player;
import org.lime.gp.player.menu.node.BaseNode;
import org.lime.gp.player.menu.node.connect.IOutput;
import org.lime.system.toast.*;

import java.util.List;
import java.util.Map;

public class ActionOutput extends IOutput {
    public ActionOutput(String key, List<Toast2<Integer, String>> target) {
        super(key, target);
    }
    public void executeNext(Player player, Map<Integer, BaseNode> nodes, Map<Integer, Map<String, Object>> data) {
        target.forEach(kv -> {
            BaseNode node = nodes.get(kv.val0);
            if (node == null) return;
            node.invokeNode(player, nodes, data);
        });
    }
}
