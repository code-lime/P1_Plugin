package org.lime.gp.player.menu;

import com.mojang.datafixers.kinds.App;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.lime.gp.chat.Apply;
import org.lime.gp.lime;
import org.lime.gp.extension.Cooldown;
import org.lime.system.execute.*;
import org.lime.system.toast.Toast2;

import java.util.HashMap;
import java.util.UUID;

public class SelectObject {
    public final int index;

    private static int lastIndex = 0;

    public String key;
    private final Player owner_player;
    public UUID owner;
    public UUID other;
    public long endTime;

    public SelectObject(String key, UUID owner, UUID other, int timeout) {
        this.index = lastIndex++;

        this.owner_player = Bukkit.getPlayer(owner);
        this.key = key;
        this.owner = owner;
        this.other = other;
        this.endTime = System.currentTimeMillis() + timeout * 1000;

        if (MenuCreator.DEBUG)
            lime.logOP("CREATE_SELECT: " + key + "." + owner + "." + other + " with timeout " + timeout + " sec");
    }

    public class InvokeAction {
        public Action2<Apply, Player> owner;
        public Action2<Apply, Player> other;
        public Action2<Apply, Player> call;

        public InvokeAction(Action2<Apply, Player> owner, Action2<Apply, Player> other, Action2<Apply, Player> call) {
            this.other = other;
            this.owner = owner;
            this.call = call;
        }

        private void tryInvoke(UUID uuid, Apply apply, Action2<Apply, Player> callback) {
            Player player = uuid == null ? null : Bukkit.getPlayer(uuid);
            if (player == null) return;
            callback.invoke(apply, player);
        }

        public void invokeOwner(Apply apply) {
            this.call.invoke(apply, SelectObject.this.owner_player);
            tryInvoke(SelectObject.this.owner, apply, this.owner);
        }

        public void invokeOther(Apply apply) {
            this.call.invoke(apply,SelectObject.this.owner_player);
            tryInvoke(SelectObject.this.other, apply, this.other);
        }

        public void invokeAll(Apply apply) {
            this.call.invoke(apply,SelectObject.this.owner_player);
            tryInvoke(SelectObject.this.owner, apply, this.owner);
            tryInvoke(SelectObject.this.other, apply, this.other);
        }
    }

    public InvokeAction invoke;

    public HashMap<String, InvokeAction> result = new HashMap<>();
    public InvokeAction timeout;
    public InvokeAction leave;

    public Toast2<String, Apply> state = null;

    public boolean isRemove() {
        if (owner == null || Bukkit.getPlayer(owner) == null) {
            Cooldown.resetCooldown(new UUID[]{owner, other}, key);
            if (MenuCreator.DEBUG)
                lime.logOP("SELECT: OTHER." + index + " " + key + " - " + (state == null ? "NULL" : state));
            lime.nextTick(() -> leave.invokeOther(state == null ? Apply.of() : state.val1));
            //leave.InvokeOther();
            return true;
        }
        if (other == null || Bukkit.getPlayer(other) == null) {
            Cooldown.resetCooldown(new UUID[]{owner, other}, key);
            if (MenuCreator.DEBUG)
                lime.logOP("SELECT: OWNER." + index + " " + key + " - " + (state == null ? "NULL" : state));
            lime.nextTick(() -> leave.invokeOwner(state == null ? Apply.of() : state.val1));
            return true;
        }
        if (System.currentTimeMillis() > endTime) {
            Cooldown.resetCooldown(new UUID[]{owner, other}, key);
            if (MenuCreator.DEBUG)
                lime.logOP("SELECT: TIMEOUT." + index + " " + key + " - " + (state == null ? "NULL" : state));
            lime.nextTick(() -> timeout.invokeAll(state == null ? Apply.of() : state.val1));
            return true;
        }
        if (state == null) return false;
        Cooldown.resetCooldown(new UUID[]{owner, other}, key);
        InvokeAction action = result.getOrDefault(state.val0, null);
        if (MenuCreator.DEBUG)
            lime.logOP("SELECT: ALL." + index + " " + key + " - " + (state == null ? "NULL" : state));
        if (action != null) lime.nextTick(() -> action.invokeAll(state == null ? Apply.of() : state.val1));
        return true;
    }
}
