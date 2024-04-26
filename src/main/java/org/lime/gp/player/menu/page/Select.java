package org.lime.gp.player.menu.page;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.lime.gp.chat.Apply;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.lime;
import org.lime.gp.player.menu.Logged;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.player.menu.SelectObject;
import org.lime.gp.player.menu.ActionSlot;
import org.lime.gp.extension.Cooldown;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.UUID;

public class Select extends Base {
    public static class InvokeAction implements Logged.ILoggedDelete {
        public static final InvokeAction NONE = new InvokeAction(Logged.ILoggedDelete.NONE, null, null, null);
        public final ActionSlot owner;
        public final ActionSlot other;
        public final ActionSlot call;

        public static InvokeAction parse(Logged.ILoggedDelete base, JsonObject json) {
            return new InvokeAction(
                    base,
                    json.has("owner") ? json.get("owner").getAsJsonObject() : null,
                    json.has("other") ? json.get("other").getAsJsonObject() : null,
                    json.has("call") ? json.get("call").getAsJsonObject() : null
            );
        }

        private final Logged.ILoggedDelete base;
        private final Logged.ChildLoggedDeleteHandle deleteHandle;

        @Override public String getLoggedKey() { return base.getLoggedKey(); }
        @Override public boolean isLogged() { return base.isLogged(); }
        @Override public boolean isDeleted() { return deleteHandle.isDeleted(); }
        @Override public void delete() { deleteHandle.delete(); }

        public InvokeAction(Logged.ILoggedDelete base, JsonObject owner, JsonObject other, JsonObject call) {
            this.base = base;
            this.deleteHandle = new Logged.ChildLoggedDeleteHandle(base);
            this.owner = owner == null ? ActionSlot.NONE : ActionSlot.parse(this.deleteHandle, owner);
            this.other = other == null ? ActionSlot.NONE : ActionSlot.parse(this.deleteHandle, other);
            this.call = call == null ? ActionSlot.NONE : ActionSlot.parse(this.deleteHandle, call);
        }

        public SelectObject.InvokeAction Apply(Apply apply, SelectObject object) {
            return object.new InvokeAction(
                    (data, player) -> owner.invoke(player, apply.join(data), true),
                    (data, player) -> other.invoke(player, apply.join(data), true),
                    (data, player) -> call.invoke(player, apply.join(data), true));
        }
    }

    public Select(JsonObject json) {
        super(json);
        timeoutTime = json.get("timeout").getAsInt();
        cooldownTime = json.has("cooldown") ? json.get("cooldown").getAsInt() : 0;
        dynamic = json.has("dynamic") && json.get("dynamic").getAsBoolean();
        JsonObject select = json.get("select").getAsJsonObject();
        invoke = select.has("invoke") ? InvokeAction.parse(this, select.get("invoke").getAsJsonObject()) : InvokeAction.NONE;
        if (select.has("result")) select.get("result").getAsJsonObject().entrySet().forEach(kv -> result.put(kv.getKey(), InvokeAction.parse(this, kv.getValue().getAsJsonObject())));
        timeout = select.has("timeout") ? InvokeAction.parse(this, select.get("timeout").getAsJsonObject()) : InvokeAction.NONE;
        leave = select.has("leave") ? InvokeAction.parse(this, select.get("leave").getAsJsonObject()) : InvokeAction.NONE;
        cooldown = select.has("cooldown") ? ActionSlot.parse(this, select.get("cooldown").getAsJsonObject()) : ActionSlot.NONE;
        contains = select.has("contains") ? ActionSlot.parse(this, select.get("contains").getAsJsonObject()) : ActionSlot.NONE;
    }

    public boolean dynamic;

    public int timeoutTime;
    public int cooldownTime;

    public InvokeAction invoke;

    public HashMap<String, InvokeAction> result = new HashMap<>();

    public InvokeAction timeout;
    public InvokeAction leave;
    public ActionSlot cooldown;
    public ActionSlot contains;

    public String getDynamicKey() {
        return getKey() + (dynamic ? UUID.randomUUID().toString() : "");
    }

    @Override protected void showGenerate(UserRow row, @Nullable Player player, int page, Apply apply) {
        UUID other_uuid;
        try {
            other_uuid = UUID.fromString(apply.getOrDefault("other_uuid", null));
        } catch (Exception e) {
            other_uuid = null;
        }

        UUID select_uuid;
        try {
            select_uuid = UUID.fromString(apply.getOrDefault("select_uuid", null));
        } catch (Exception e) {
            if (player == null) {
                lime.logOP("Menu '"+getKey()+"' not called! User is NULL and arg `select_uuid` is NULL");
                return;
            }
            select_uuid = player.getUniqueId();
        }

        Player select_player = Bukkit.getPlayer(select_uuid);
        if (select_player == null) return;

        int cd = (int)Cooldown.getCooldown(select_uuid, getKey());
        if (cd > 0) {
            cooldown.invoke(select_player, apply.add("cooldown", String.valueOf(cd)), true);
            return;
        }
        if (Cooldown.hasCooldown(new UUID[]{select_uuid, other_uuid}, getKey())) {
            contains.invoke(select_player, apply, true);
            return;
        }

        String dynamicKey = getDynamicKey();

        SelectObject selectObject = new SelectObject(dynamicKey, select_uuid, other_uuid, timeoutTime);
        apply.add("select_index", String.valueOf(selectObject.index));

        selectObject.invoke = invoke.Apply(apply, selectObject);

        result.forEach((k, v) -> selectObject.result.put(k, v.Apply(apply, selectObject)));

        selectObject.timeout = timeout.Apply(apply, selectObject);
        selectObject.leave = leave.Apply(apply, selectObject);

        MenuCreator.selects.put(selectObject.index, selectObject);

        Cooldown.setCooldown(new UUID[]{select_uuid, other_uuid}, dynamicKey, timeoutTime);
        Cooldown.setCooldown(select_uuid, dynamicKey, cooldownTime);
        selectObject.invoke.invokeAll(Apply.of());
    }
}

















