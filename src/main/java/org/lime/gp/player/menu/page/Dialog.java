package org.lime.gp.player.menu.page;

import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.gp.chat.Apply;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.lime;
import org.lime.gp.player.menu.ActionSlot;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class Dialog extends Base {
    public List<ActionSlot> dialog = new ArrayList<>();

    public Dialog(JsonObject json) {
        super(json);
        json.get("dialog").getAsJsonArray().forEach(kv -> dialog.add(ActionSlot.parse(this, kv.getAsJsonObject())));
    }

    @Override protected void showGenerate(UserRow row, @Nullable Player player, int page, Apply apply) {
        int waitTime = 0;
        for (ActionSlot action : dialog) {
            lime.onceTicks(() -> action.invoke(player, apply, true), waitTime);
            waitTime += action.wait;
        }
    }
}
