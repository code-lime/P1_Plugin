package org.lime.gp.player.menu.page.slot;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.mojang.datafixers.kinds.App;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.lime;
import org.lime.gp.player.menu.Logged;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Slot extends ItemCreator implements ISlot {
    @Override public boolean updateReplace() { return false; }
    public HashMap<ClickType, List<org.lime.gp.player.menu.ActionSlot>> actions = new HashMap<>();

    public String isShow;
    public String isAction;

    public final String count;

    private final String display;

    private final Logged.ILoggedDelete base;
    private final Logged.ChildLoggedDeleteHandle deleteHandle;

    @Override public String getLoggedKey() { return base.getLoggedKey(); }
    @Override public boolean isLogged() { return base.isLogged(); }
    @Override public boolean isDeleted() { return deleteHandle.isDeleted(); }
    @Override public void delete() { deleteHandle.delete(); }

    @Override public List<Toast2<String, String>> args(Apply apply) {
        List<Toast2<String, String>> defaultArgs = super.args(apply);
        if (display == null) return defaultArgs;
        String key = apply.apply(display);
        return Items.getItemCreator(key)
                .map(v -> v instanceof ItemCreator c ? c : null)
                .map(item -> {
                    List<Toast2<String, String>> args = new ArrayList<>(defaultArgs);
                    args.add(Toast.of("display_name", item.name));
                    args.add(Toast.of("display_item", item.item));
                    args.add(Toast.of("display_id", item.id));
                    return args;
                })
                .orElse(defaultArgs);
    }

    protected Slot(Logged.ILoggedDelete base, JsonObject json) {
        super("menu.slot", json);

        display = json.has("display") ? json.get("display").getAsString() : null;

        this.base = base;
        this.deleteHandle = new Logged.ChildLoggedDeleteHandle(base);
        this.count = json.has("count") ? json.get("count").getAsString() : "1";
        this.isShow = json.has("isShow") && !json.get("isShow").isJsonNull() ? json.get("isShow").getAsString() : null;
        this.isAction = json.has("isAction") && !json.get("isAction").isJsonNull() ? json.get("isAction").getAsString() : null;
        if (json.has("action")) json.get("action").getAsJsonObject().entrySet().forEach(kv -> {
            org.lime.gp.player.menu.ActionSlot action = org.lime.gp.player.menu.ActionSlot.parse(this, kv.getValue().getAsJsonObject());
            parseClickType(kv.getKey()).forEach(click -> {
                List<org.lime.gp.player.menu.ActionSlot> actions = this.actions.getOrDefault(click, null);
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
        return isShow == null || ISlot.isTrue(isShow, ISlot.createArgs(args(apply), apply));
    }
    public Toast3<List<Toast2<String, String>>, HashMap<ClickType, List<org.lime.gp.player.menu.ActionSlot>>, ItemStack> create(Apply apply) {
        return Toast.of(this.args(apply), actions, createItem(Integer.parseInt(ChatHelper.formatText(count, apply)), apply));
    }
}


















