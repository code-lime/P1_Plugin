package org.lime.gp.player.menu.page;

import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.gp.chat.Apply;
import org.lime.gp.database.Rows;
import org.lime.gp.lime;
import org.lime.gp.player.menu.Slot;

import java.util.ArrayList;
import java.util.List;

public class Dialog extends Base {
    public List<Slot> dialog = new ArrayList<>();

    public Dialog(JsonObject json) {
        super(json);
        json.get("dialog").getAsJsonArray().forEach(kv -> dialog.add(Slot.parse(this, kv.getAsJsonObject())));
    }

    @Override protected void showGenerate(Rows.UserRow row, Player player, int page, Apply apply) {
        if (player == null) return;
        int waitTime = 0;
        for (Slot action : dialog) {
            lime.onceTicks(() -> action.invoke(player, apply, true), waitTime);
            waitTime += action.wait;
        }
    }
}
