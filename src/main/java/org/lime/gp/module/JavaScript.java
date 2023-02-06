package org.lime.gp.module;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.lime.core;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.database.Methods;
import org.lime.gp.database.MySql;
import org.lime.gp.extension.Cooldown;
import org.lime.gp.lime;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.system;

import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class JavaScript {
    public static org.lime.JavaScript js;
    public static void createAdd() {
        lime._plugin.add(core.element.create(JavaScript.class).addText("generic.js", v -> v.withInvoke(t -> {}).withDefault("")));
        
        js = (org.lime.JavaScript) lime._plugin.add(org.lime.JavaScript.create()).element().map(v -> v.instance).orElseThrow();
        js.instances.put("jsmenu", new JSMenu());
    }

    public static class JSMenu extends org.lime.JavaScript.InstanceJS {
        private static final ConcurrentHashMap<String, Object> staticMemory = new ConcurrentHashMap<>();
        public Object memoryGetOrAdd(String key, system.Func0<Object> def) {
            return staticMemory.computeIfAbsent(key, k -> def.invoke());
        }
        public Object memoryGet(String key) {
            return staticMemory.get(key);
        }
        public void memorySet(String key, Object value) {
            staticMemory.put(key, value);
        }

        public boolean has_permission(String uuid, String perm) {
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
            return player != null && player.hasPermission(perm);
        }
        public boolean has_any_permissions(String uuid, List<String> perms) {
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
            if (player == null) return false;
            for (String perm : perms)
                if (player.hasPermission(perm))
                    return true;
            return false;
        }
        public boolean has_all_permissions(String uuid, List<String> perms) {
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
            if (player == null) return false;
            for (String perm : perms)
                if (!player.hasPermission(perm))
                    return false;
            return true;
        }
        public boolean has_cooldown(String uuid, String key) { return Cooldown.hasCooldown(UUID.fromString(uuid), key); }
        public boolean set_cooldown(String uuid, String key, double sec) {
            Cooldown.setCooldown(UUID.fromString(uuid), key, sec);
            return true;
        }
        public void show_npc(String uuid, String npc) { NPC.show(UUID.fromString(uuid), npc); }
        public void hide_npc(String uuid, String npc) { NPC.hide(UUID.fromString(uuid), npc); }

        public List<String> shows_npc(String uuid) { return NPC.shows(UUID.fromString(uuid)); }

        public void target_npc(String uuid, String npc) {}
        public void untarget_npc(String uuid, String npc) {}
        public List<String> targets_npc(String uuid) { return Collections.emptyList(); }

        public List<String> getPlayers() { return Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getUniqueId).map(UUID::toString).collect(Collectors.toList()); }
        public MySql._async getSQL() { return Methods.SQL.Async; }
        public Class<?> findType(String type2) {
            try { return Class.forName(type2); } catch (ClassNotFoundException e) { return null; }
        }

        public Object read(ResultSet set, String name, Class<?> tClass) { return MySql.readObject(set, name, tClass); }
        public void log(String text) { lime.logOP(text); }
        public void repeat(system.Action0 func, double sec) {
            this.inits.add(lime.repeat(func, sec));
        }

        public String toJsonString(Object obj) {
            return system.json.by(obj).build().toString();
        }
        public void once(system.Action0 func, double sec) {
            system.Toast1<BukkitTask> bukkitTask = system.toast(null);
            bukkitTask.val0 = lime.once(func, sec);
            this.inits.add(bukkitTask.val0);
        }
        public String sql(String table, String check, String filter, String output) { return ChatHelper.applySqlJs(table, check, filter, output); }
        public void show(String uuid, String menu) { this.show(uuid, menu, 0, new HashMap<>()); }
        public void show(String uuid, String menu, int page) { this.show(uuid, menu, page, new HashMap<>()); }
        public void show(String uuid, String menu, int page, Map<String, String> args) { MenuCreator.show(Bukkit.getPlayer(UUID.fromString(uuid)), menu, page, Apply.of().add(args)); }
        public void show(String menu) { this.show(menu, 0, new HashMap<>()); }
        public void show(String menu, int page) { this.show(menu, page, new HashMap<>()); }
        public void show(String menu, int page, Map<String, String> args) { MenuCreator.show(menu, page, Apply.of().add(args)); }
    }

    public static Optional<Boolean> isJsTrue(String js) { return JavaScript.js.isJsTrue(js); }
    public static Optional<Number> getJsNumber(String js) { return JavaScript.js.getJsNumber(js); }
    public static Optional<Integer> getJsInt(String js) { return JavaScript.js.getJsInt(js); }
    public static Optional<Double> getJsDouble(String js) { return JavaScript.js.getJsDouble(js); }
    public static Optional<String> getJsString(String js) { return JavaScript.js.getJsString(js); }

    public static void getJsStringNext(String js, system.Action1<String> callback) { JavaScript.js.getJsStringNext(js, callback); }

    public static void invoke(String js) { JavaScript.js.invoke(js); }
    public static Optional<Object> invoke(String js, Map<String, Object> values) { return JavaScript.js.invoke(js, values); }

}
