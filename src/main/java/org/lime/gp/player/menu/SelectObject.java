package org.lime.gp.player.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.lime.gp.lime;
import org.lime.gp.extension.Cooldown;
import org.lime.system;

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
        public system.Action1<Player> owner;
        public system.Action1<Player> other;
        public system.Action1<Player> call;

        public InvokeAction(system.Action1<Player> owner, system.Action1<Player> other, system.Action1<Player> call) {
            this.other = other;
            this.owner = owner;
            this.call = call;
        }

        private void TryInvoke(UUID uuid, system.Action1<Player> callback) {
            Player player = uuid == null ? null : Bukkit.getPlayer(uuid);
            if (player == null) return;
            callback.invoke(player);
        }

        public void InvokeOwner() {
            this.call.invoke(SelectObject.this.owner_player);
            TryInvoke(SelectObject.this.owner, this.owner);
        }

        public void InvokeOther() {
            this.call.invoke(SelectObject.this.owner_player);
            TryInvoke(SelectObject.this.other, this.other);
        }

        public void InvokeAll() {
            this.call.invoke(SelectObject.this.owner_player);
            TryInvoke(SelectObject.this.owner, this.owner);
            TryInvoke(SelectObject.this.other, this.other);
        }
    }

    public InvokeAction invoke;

    public HashMap<String, InvokeAction> result = new HashMap<>();
    public InvokeAction timeout;
    public InvokeAction leave;

    public String state = null;

    public boolean isRemove() {
        if (owner == null || Bukkit.getPlayer(owner) == null) {
            Cooldown.resetCooldown(new UUID[]{owner, other}, key);
            if (MenuCreator.DEBUG)
                lime.logOP("SELECT: OTHER." + index + " " + key + " - " + (state == null ? "NULL" : state));
            lime.nextTick(leave::InvokeOther);
            //leave.InvokeOther();
            return true;
        }
        if (other == null || Bukkit.getPlayer(other) == null) {
            Cooldown.resetCooldown(new UUID[]{owner, other}, key);
            if (MenuCreator.DEBUG)
                lime.logOP("SELECT: OWNER." + index + " " + key + " - " + (state == null ? "NULL" : state));
            lime.nextTick(leave::InvokeOwner);
            return true;
        }
        if (System.currentTimeMillis() > endTime) {
            Cooldown.resetCooldown(new UUID[]{owner, other}, key);
            if (MenuCreator.DEBUG)
                lime.logOP("SELECT: TIMEOUT." + index + " " + key + " - " + (state == null ? "NULL" : state));
            lime.nextTick(timeout::InvokeAll);
            return true;
        }
        if (state == null) return false;
        Cooldown.resetCooldown(new UUID[]{owner, other}, key);
        InvokeAction action = result.getOrDefault(state, null);
        if (MenuCreator.DEBUG)
            lime.logOP("SELECT: ALL." + index + " " + key + " - " + (state == null ? "NULL" : state));
        if (action != null) lime.nextTick(action::InvokeAll);
        return true;
    }
}
