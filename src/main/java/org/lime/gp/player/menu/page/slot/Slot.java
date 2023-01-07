package org.lime.gp.player.menu.page.slot;

import com.google.gson.JsonObject;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.item.Items;
import org.lime.gp.player.menu.Logged;
import org.lime.system;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Slot extends Items.ItemCreator implements ISlot {
    @Override public boolean updateReplace() { return false; }
    public HashMap<ClickType, List<org.lime.gp.player.menu.Slot>> actions = new HashMap<>();

    public String isShow;
    public String isAction;

    public final String count;

    private final Logged.ILoggedDelete base;
    private final Logged.ChildLoggedDeleteHandle deleteHandle;

    @Override public String getLoggedKey() { return base.getLoggedKey(); }
    @Override public boolean isLogged() { return base.isLogged(); }
    @Override public boolean isDeleted() { return deleteHandle.isDeleted(); }
    @Override public void delete() { deleteHandle.delete(); }

    protected Slot(Logged.ILoggedDelete base, JsonObject json) {
        super("menu.slot", json);
        this.base = base;
        this.deleteHandle = new Logged.ChildLoggedDeleteHandle(base);
        this.count = json.has("count") ? json.get("count").getAsString() : "1";
        this.isShow = json.has("isShow") && !json.get("isShow").isJsonNull() ? json.get("isShow").getAsString() : null;
        this.isAction = json.has("isAction") && !json.get("isAction").isJsonNull() ? json.get("isAction").getAsString() : null;
        if (json.has("action")) json.get("action").getAsJsonObject().entrySet().forEach(kv -> {
            org.lime.gp.player.menu.Slot action = org.lime.gp.player.menu.Slot.parse(this, kv.getValue().getAsJsonObject());
            parseClickType(kv.getKey()).forEach(click -> {
                List<org.lime.gp.player.menu.Slot> actions = this.actions.getOrDefault(click, null);
                if (actions == null) this.actions.put(click, actions = new ArrayList<>());
                actions.add(action);
            });
        });
    }
    private static List<ClickType> parseClickType(String type) {
        return type.equals("ALL") ? Arrays.asList(ClickType.values()) : Arrays.stream(type.split("\\|")).map(ClickType::valueOf).distinct().collect(Collectors.toList());
    }

    public static Slot parse(Logged.ILoggedDelete base, JsonObject json) {
        return new Slot(base, json);
    }

    public boolean tryIsShow(Apply apply) {
        return isShow == null || ISlot.isTrue(isShow, ISlot.createArgs(this.args, apply));
    }
    public system.Toast2<HashMap<ClickType, List<org.lime.gp.player.menu.Slot>>, ItemStack> create(Apply apply) {
        return system.toast(actions, createItem(Integer.parseInt(ChatHelper.formatText(count, apply)), apply));
    }
}


















