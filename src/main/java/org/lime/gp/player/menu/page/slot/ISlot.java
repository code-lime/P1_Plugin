package org.lime.gp.player.menu.page.slot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.item.Items;
import org.lime.gp.module.JavaScript;
import org.lime.gp.player.menu.Logged;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface ISlot extends Logged.ILoggedDelete {
    static ISlot parse(Logged.ILoggedDelete base, JsonElement json) {
        if (json.isJsonPrimitive()) return new ISlot() {
            private final Logged.ChildLoggedDeleteHandle deleteHandle = new Logged.ChildLoggedDeleteHandle(base);

            @Override public String getLoggedKey() { return base.getLoggedKey(); }
            @Override public boolean isLogged() { return base.isLogged(); }
            @Override public boolean isDeleted() { return deleteHandle.isDeleted(); }
            @Override public void delete() { deleteHandle.delete(); }

            @Override public boolean tryIsShow(Apply apply) { return true; }
            @Override public Toast3<List<Toast2<String, String>>, HashMap<ClickType, List<org.lime.gp.player.menu.ActionSlot>>, ItemStack> create(Apply apply) {
                return Toast.of(new ArrayList<>(), new HashMap<>(), Items.getItemCreator(ChatHelper.formatText(json.getAsString(), apply)).map(v -> v.createItem(1, apply)).orElseGet(() -> new ItemStack(Material.BARRIER)));
            }
        };
        JsonObject jsonObject = json.getAsJsonObject();
        if (jsonObject.has("item")) return Slot.parse(base, jsonObject);
        return SelectSlot.parse(base, jsonObject);
    }

    boolean tryIsShow(Apply apply);

    Toast3<List<Toast2<String, String>>, HashMap<ClickType, List<org.lime.gp.player.menu.ActionSlot>>, ItemStack> create(Apply apply);

    static Apply createArgs(List<Toast2<String, String>> args, Apply apply) {
        Apply _apply = apply.copy();
        for (Toast2<String, String> arg : args) {
            if (_apply.has(arg.val0)) continue;
            _apply.add(arg.val0, ChatHelper.formatText(arg.val1, _apply));
        }
        return _apply;
    }
    static boolean isTrue(String js, Apply apply) {
        return JavaScript.isJsTrue(ChatHelper.formatText(js, apply)).orElse(false);
    }
}






















