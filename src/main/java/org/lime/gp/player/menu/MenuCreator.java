package org.lime.gp.player.menu;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.lime.core;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.chat.Apply;
import org.lime.gp.lime;
import org.lime.gp.module.JavaScript;
import org.lime.gp.player.menu.page.Base;
import org.lime.gp.chat.ChatHelper;
import org.bukkit.entity.Player;
import org.lime.system;

import java.util.*;

import javax.annotation.Nullable;

public class MenuCreator {
    public static core.element create() {
        return core.element.create(MenuCreator.class)
                .withInit(MenuCreator::init)
                .<JsonObject>addConfig("menu", v -> v
                        .withInvoke(MenuCreator::config)
                        .withDefault(new JsonObject())
                );
    }

    public static boolean DEBUG = false;

    public static void init() {
        lime.repeat(MenuCreator::update, 1);

        AnyEvent.addEvent("open.menu", AnyEvent.type.owner, builder -> builder.createParam(v -> v, menuList::keySet), (player, menu) -> show(player, menu));
        AnyEvent.addEvent("select.state", AnyEvent.type.none, builder -> builder.createParam(Integer::parseUnsignedInt, "[select_id]").createParam("[state]"), (player, select_id, state) -> {
            SelectObject object = selects.getOrDefault(select_id, null);
            if (object == null) return;
            if (object.state != null) return;
            if (DEBUG) lime.logOP("CLICKER: " + player.getUniqueId() + "\nCAN CLICK: " + object.other);
            if (!player.getUniqueId().equals(object.other)) return;
            object.state = state;
        });
        AnyEvent.addEvent("select.state.key", AnyEvent.type.none, builder -> builder.createParam("[select_key]").createParam("[state]"), (player, select_key, state) -> {
            UUID uuid = player.getUniqueId();
            SelectObject object = selects.values().stream().filter(v -> v.key.equals(select_key) && uuid.equals(v.other)).findFirst().orElse(null);
            if (object == null) return;
            if (object.state != null) return;
            if (!uuid.equals(object.other)) return;
            object.state = state;
        });
    }
    public static void config(JsonObject json) {
        boolean debug = false;
        if (json.has("DEBUG")) {
            debug = json.get("DEBUG").getAsBoolean();
            json.remove("DEBUG");
        }
        menuList.values().forEach(Base::delete);
        menuList.clear();
        lime.combineParent(json, true, false).entrySet().forEach(kv -> menuList.put(kv.getKey(), Base.parse(kv.getKey(), kv.getValue().getAsJsonObject())));
        DEBUG = debug;
    }
    public static HashMap<String, Base> menuList = new HashMap<>();
    public static final HashMap<Integer, SelectObject> selects = new HashMap<>();
    public static void update() {
        selects.values().removeIf(SelectObject::isRemove);
    }

    public static boolean show(Player player, String menu) {
        return show(player, menu, 0, Apply.of());
    }
    public static boolean show(Player player, String menu, Apply apply) {
        return show(player, menu, 0, apply);
    }
    public static boolean show(Player player, String menu, int page, Apply apply) {
        return showOwner(player, menu, page, null, apply);
    }
    public static boolean showOwner(Player player, String menu, int page, @Nullable Logged.ILoggedDelete caller, Apply apply) {
        if (menu == null || menu.isEmpty()) return false;
        Base _menu = menuList.getOrDefault(menu, null);
        if (_menu == null) {
            if (player != null)
                player.sendMessage(ChatHelper.formatComponent("Страница '"+menu+"' не найдена!"));
            return false;
        }
        try {
            if (DEBUG) {
                String args_json = system.toFormat(system.json.object().add(apply.list()).build());
                lime.logOP(Component.text("MENU." + menu + "[" + page + "] ").append(Component.text("[args]").hoverEvent(HoverEvent.showText(Component.text("Click to copy:\n\n").append(Component.text(args_json)))).clickEvent(ClickEvent.copyToClipboard(args_json))));
            }
            return _menu.show(player, page, caller, apply);
        } catch (Exception e) {
            lime.logStackTrace(e);
            return false;
        }
    }
    public static boolean show(Player player, String menu, String page, Apply apply) {
        return show(player, menu, JavaScript.getJsInt(page).orElse(0), apply);
    }
    public static boolean showOwner(Player player, String menu, String page, @Nullable Logged.ILoggedDelete caller, Apply apply) {
        return showOwner(player, menu, JavaScript.getJsInt(page).orElse(0), caller, apply);
    }
    public static boolean showOwner(Player player, String menu, @Nullable Logged.ILoggedDelete caller, Apply apply) {
        return showOwner(player, menu, 0, caller, apply);
    }

    public static boolean show(String menu) {
        return show(menu, 0, Apply.of());
    }
    public static boolean show(String menu, Apply apply) {
        return show(menu, 0, apply);
    }
    public static boolean show(String menu, int page, Apply apply) {
        if (menu == null) return false;
        Base _menu = menuList.getOrDefault(menu, null);
        if (_menu == null) return false;
        try {
            if (DEBUG) {
                String args_json = system.toFormat(system.json.object().add(apply.list()).build());
                lime.logOP(Component.text("MENU." + menu + "[" + page + "] ").append(Component.text("[args]").hoverEvent(HoverEvent.showText(Component.text("Click to copy:\n\n").append(Component.text(args_json)))).clickEvent(ClickEvent.copyToClipboard(args_json))));
            }
            return _menu.show(null, page, null, apply);
        } catch (Exception e) {
            lime.logStackTrace(e);
            return false;
        }
    }
    public static boolean show(String menu, String page, Apply apply) {
        return show(menu, JavaScript.getJsInt(page).orElse(0), apply);
    }
}
























