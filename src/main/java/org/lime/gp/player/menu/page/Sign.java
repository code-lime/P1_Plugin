package org.lime.gp.player.menu.page;

import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.database.Rows;
import org.lime.gp.player.menu.ActionSlot;
import org.lime.gp.player.ui.EditorUI;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Sign extends Base {
    public List<ActionSlot> output = new ArrayList<>();
    public List<String> lines = new ArrayList<>();

    public Sign(JsonObject json) {
        super(json);
        if (json.has("output")) json.get("output").getAsJsonArray().forEach(kv -> output.add(ActionSlot.parse(this, kv.getAsJsonObject())));
        json.get("input").getAsJsonArray().forEach(line -> lines.add(line.getAsString()));
    }

    @Override protected void showGenerate(Rows.UserRow row, Player player, int page, Apply apply) {
        if (player == null) return;
        EditorUI.openSign(player, this.lines.stream().map(v -> ChatHelper.formatText(v, apply)).collect(Collectors.toList()), _lines -> {
            apply.add("line0", _lines.get(0)).add("line1", _lines.get(1)).add("line2", _lines.get(2)).add("line3", _lines.get(3));
            output.forEach(i -> i.invoke(player, apply, true));
        });
    }
}
