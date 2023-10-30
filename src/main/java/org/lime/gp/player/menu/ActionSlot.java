package org.lime.gp.player.menu;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.monster.EntityZombie;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.lime;
import org.lime.gp.module.JavaScript;
import org.lime.gp.sound.Sounds;
import org.lime.system.Regex;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ActionSlot implements Logged.ILoggedDelete {
    /*public interface PlaySound extends Action1<Player> {
        void play(Player player);
        @Override default void invoke(Player player) { play(player); }
        static PlaySound parse(JsonObject json) {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(json.get("sound").getAsString());
            float volume = json.get("volume").getAsFloat();
            float pitch = json.get("pitch").getAsFloat();
            return player -> player.playSound(Sound.sound(sound, Sound.Source.MASTER, volume, pitch));
        }
    }*/

    public static final ActionSlot NONE = new ActionSlot(Logged.ILoggedDelete.NONE);

    @Override public String getLoggedKey() { return base.getLoggedKey(); }
    @Override public boolean isLogged() { return base.isLogged(); }

    private final Logged.ILoggedDelete base;
    private final Logged.ChildLoggedDeleteHandle deleteHandle;

    private ActionSlot(Logged.ILoggedDelete base) {
        this.base = base;
        this.deleteHandle = new Logged.ChildLoggedDeleteHandle(base);
    }

    public List<String> commands = new ArrayList<>();
    public List<String> sql = new ArrayList<>();
    public List<String> messages = new ArrayList<>();
    public List<String> logs = new ArrayList<>();
    public String page = null;
    public int wait = 0;
    public List<Toast2<String, String>> pageArgs = new ArrayList<>();
    public boolean close = false;
    public boolean online = false;
    public List<ActionSlot> offlineActions = new ArrayList<>();
    public List<ActionSlot> actions = new ArrayList<>();
    public List<Toast2<String, String>> args = new ArrayList<>();
    public List<Toast2<String, ActionSlot>> checkActions = new ArrayList<>();
    public List<String> sounds = new ArrayList<>();
    public List<String> globalSounds = new ArrayList<>();

    public static ActionSlot parse(Logged.ILoggedDelete base, JsonObject json) {
        ActionSlot action = new ActionSlot(base);
        if (json.has("command")) {
            JsonElement cmd = json.get("command");
            if (cmd.isJsonArray()) cmd.getAsJsonArray().forEach(_cmd -> action.commands.add(_cmd.getAsString()));
            else action.commands.add(cmd.getAsString());
        }
        action.close = json.has("close") && json.get("close").getAsBoolean();
        action.online = json.has("online") && json.get("online").getAsBoolean();
        if (json.has("messages")) json.get("messages").getAsJsonArray().forEach(msg -> action.messages.add(msg.getAsString()));
        if (json.has("logs")) json.get("logs").getAsJsonArray().forEach(msg -> action.logs.add(msg.getAsString()));
        if (json.has("sql")) json.get("sql").getAsJsonArray().forEach(msg -> action.sql.add(msg.getAsString()));
        action.page = json.has("page") ? json.get("page").getAsString() : null;
        if (json.has("args")) json.get("args").getAsJsonObject().entrySet().forEach(arg -> action.args.add(Toast.of(arg.getKey(), arg.getValue().getAsString())));
        action.wait = json.has("wait") ? json.get("wait").getAsInt() : 0;
        if (json.has("page_args")) json.get("page_args").getAsJsonObject().entrySet().forEach(kv -> action.pageArgs.add(Toast.of(kv.getKey(), kv.getValue().getAsString())));
        if (json.has("actions")) json.get("actions").getAsJsonArray().forEach(kv -> action.actions.add(ActionSlot.parse(action, kv.getAsJsonObject())));
        if (json.has("sounds")) json.get("sounds").getAsJsonArray().forEach(kv -> action.sounds.add(kv.getAsString()));
        if (json.has("global_sounds")) json.get("global_sounds").getAsJsonArray().forEach(kv -> action.globalSounds.add(kv.getAsString()));
        if (json.has("offline_actions")) json.get("offline_actions").getAsJsonArray().forEach(kv -> action.offlineActions.add(ActionSlot.parse(action, kv.getAsJsonObject())));
        if (json.has("check_actions")) json.get("check_actions").getAsJsonObject().entrySet().forEach(kv -> action.checkActions.add(Toast.of(kv.getKey(), ActionSlot.parse(action, kv.getValue().getAsJsonObject()))));
        return action;
    }

    private void _invoke(Player player, Apply apply) {
        for (Toast2<String, String> arg : this.args) apply.add(arg.val0, ChatHelper.formatText(arg.val1, apply));
        if (online && !player.isOnline()) {
            offlineActions.forEach(v -> v.invoke(player, apply, true));
            return;
        }
        commands.forEach(cmd -> {
            String _cmd = ChatHelper.formatText(cmd, apply);
            if (_cmd.isEmpty()) return;
            if (_cmd.startsWith("!")) ExtMethods.executeCommand(_cmd.substring(1));
            else Bukkit.dispatchCommand(player, _cmd);
        });
        messages.forEach(msg -> player.sendMessage(ChatHelper.formatComponent(msg, apply)));
        logs.forEach(msg -> lime.logOP(ChatHelper.formatComponent(msg, apply)));
        sql.stream()
                .map(sql -> ChatHelper.formatText(sql, apply))
                .filter(v -> !v.isEmpty())
                .forEach(sql -> org.lime.gp.database.Methods.SQL.Async
                        .rawSql(sql, () -> {})
                        .withSQL((_sql) -> Logged.log(player, _sql, this))
                );
        if (close) player.closeInventory();
        else if (this.page != null) {
            String _page = ChatHelper.formatText(this.page, apply);
            String[] _args = _page.split(":");

            HashMap<String, String> localArgs = new HashMap<>();

            for (Toast2<String, String> arg : pageArgs) {
                if (arg.val0.equals("*")) {
                    apply.list().forEach((k, v) -> {
                        if (Regex.compareRegex(k, arg.val1))
                            localArgs.put(k, v);
                    });
                } else {
                    localArgs.put(arg.val0, ChatHelper.formatText(arg.val1, apply));
                }
            }
            if (_args.length == 1) MenuCreator.showOwner(player, _args[0], this, Apply.of().add(localArgs));
            else MenuCreator.showOwner(player, _args[0], _args[1], this, Apply.of().add(localArgs));
        }
        actions.forEach(action -> action.invoke(player, apply, true));
        sounds.forEach(sound -> Sounds.playSound(ChatHelper.formatText(sound, apply), player));
        Location location = player.getLocation();
        globalSounds.forEach(sound -> Sounds.playSound(ChatHelper.formatText(sound, apply), location));
        for (var kv : checkActions) {
            if (JavaScript.isJsTrue(ChatHelper.formatText(kv.val0, apply)).filter(_v -> _v).isEmpty()) continue;
            kv.val1.invoke(player, apply, true);
            break;
        }
    }
    public void invoke(Player player, Apply apply, boolean isWait) {
        if (isDeleted()) return;
        if (!isWait || wait <= 0) _invoke(player, apply);
        else lime.onceTicks(() -> _invoke(player, apply), wait);
    }

    @Override public boolean isDeleted() { return deleteHandle.isDeleted(); }
    @Override public void delete() { deleteHandle.delete(); }
}









