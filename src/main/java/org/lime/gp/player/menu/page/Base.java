package org.lime.gp.player.menu.page;

import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.database.rows.BaseRow;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.database.tables.ITable;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.*;
import org.lime.gp.player.menu.Logged;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.player.menu.ActionSlot;
import org.lime.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Base implements Logged.ILoggedDelete {
    private String key;

    public List<ActionSlot> action = new ArrayList<>();
    public List<Integer> roleWhite = new ArrayList<>();
    public List<system.Toast2<String, String>> args = new ArrayList<>();
    public List<system.Toast2<String, String>> sqlArgs = new ArrayList<>();
    public boolean isLogged;
    public boolean isLockMenu;

    private final system.DeleteHandle deleteHandle = new system.DeleteHandle();

    public Base(JsonObject json) {
        if (json.has("action")) json.get("action").getAsJsonArray().forEach(kv -> action.add(ActionSlot.parse(this, kv.getAsJsonObject())));
        if (json.has("args")) json.get("args").getAsJsonObject().entrySet().forEach(arg -> args.add(system.toast(arg.getKey(), arg.getValue().getAsString())));
        if (json.has("role_white")) json.get("role_white").getAsJsonArray().forEach(arg -> roleWhite.add(arg.getAsInt()));
        if (json.has("sql_args")) json.get("sql_args").getAsJsonObject().entrySet().forEach(arg -> sqlArgs.add(system.toast(arg.getKey(), arg.getValue().getAsString())));
        isLogged = !json.has("is_logged") || json.get("is_logged").getAsBoolean();
        isLockMenu = json.has("is_lock_menu") && json.get("is_lock_menu").getAsBoolean();
    }

    public String getKey() { return key; }
    @Override public String getLoggedKey() { return key; }
    @Override public boolean isLogged() { return isLogged; }

    @Override public boolean isDeleted() { return deleteHandle.isDeleted(); }
    @Override public void delete() { deleteHandle.delete(); }

    public boolean show(Player player, int page, Apply apply) {
        if (isDeleted()) {
            if (player != null) player.closeInventory();
            return false;
        }
        Apply send_apply = Apply.of();
        UserRow row;
        if (player != null) {
            PlayerInventory playerInventory = player.getInventory();

            if (isLockMenu && Items.getOptional(LockMenuSetting.class, playerInventory.getItemInMainHand())
                    .filter(v -> v.isLock)
                    .or(() -> Items.getOptional(LockMenuSetting.class, playerInventory.getItemInOffHand()).filter(v -> v.isLock))
                    .isPresent()
            ) {
                LangMessages.Message.Menu_Error_Weapon.sendMessage(player);
                return false;
            }

            row = UserRow.getBy(player).orElse(null);
            if (roleWhite.size() > 0 && (row == null || !roleWhite.contains(row.role))) return false;
            if (row != null) send_apply.add(row);
            else send_apply.add("uuid", player.getUniqueId().toString()).add("user_name", player.getName());
            send_apply.add("page", String.valueOf(page));
        } else row = null;
        send_apply.join(apply);
        for (system.Toast2<String, String> arg : this.args) send_apply.add(arg.val0, ChatHelper.formatText(arg.val1, send_apply));

        system.waitAllAnyAsyns(
                sqlArgs.stream()
                .map(v -> system.toast("!sql " + ChatHelper.formatText(v.val1, send_apply), v.val0))
                .collect(Collectors.toList()),
                (String table, system.Action1<ITable<? extends BaseRow>> callback) -> Tables
                .getTable(table, callback)
                .withSQL(isLogged ? (sql) -> Logged.log(player, sql, this) : null),
                argsTableData -> {
                    Apply table_apply = send_apply.copy();
                    HashMap<String, ITable<? extends BaseRow>> customTables = new HashMap<>();
                    for (system.Toast3<String, String, ITable<? extends BaseRow>> dat : argsTableData) {
                        if (dat.val1.startsWith("!")) customTables.put(dat.val1.substring(1), dat.val2);
                        else dat.val2.getFirstRow().ifPresent(v -> table_apply.add(dat.val1 + ".", v));
                    }
                    table_apply.with(customTables);
                    showGenerate(row, player, page, table_apply.copy());
                });
        return true;
    }

    protected abstract void showGenerate(UserRow row, Player player, int page, Apply apply);

    public static Base parse(String key, JsonObject json) {
        Base menu;
        if (json.has("slots")) menu = new Menu(json);
        else if (json.has("pages")) menu = new Book(json);
        else if (json.has("input")) menu = new Sign(json);
        else if (json.has("select")) menu = new Select(json);
        else if (json.has("called")) menu = new SQL(json);
        else if (json.has("dialog")) menu = new Dialog(json);
        else if (json.has("selector")) menu = new Selector(json);
        else if (json.has("type")) menu = switch (json.get("type").getAsString()) {
            case "insert" -> new Insert(json);
            default -> throw new IllegalArgumentException("TYPE '" + json.get("type") + "' IN MENU");
        };
        else throw new IllegalArgumentException("ERROR PARSE '" + key + "' IN MENU");
        menu.key = key;
        if (json.has("childs")) json.get("childs").getAsJsonObject().entrySet().forEach(kv -> {
            String child = key + "." + kv.getKey();
            MenuCreator.menuList.put(child, parse(child, kv.getValue().getAsJsonObject()));
        });
        return menu;
    }
}














