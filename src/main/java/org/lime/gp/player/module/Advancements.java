package org.lime.gp.player.module;

import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.endercentral.crazy_advancements.*;
import eu.endercentral.crazy_advancements.advancement.AdvancementDisplay;
import eu.endercentral.crazy_advancements.advancement.AdvancementFlag;
import eu.endercentral.crazy_advancements.advancement.AdvancementVisibility;
import eu.endercentral.crazy_advancements.manager.AdvancementManager;
import net.kyori.adventure.text.Component;
import net.minecraft.network.chat.IChatBaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.lime.core;
import org.lime.gp.lime;
import org.lime.system;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.item.data.ItemCreator;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Advancements implements Listener {
    public static core.element create() {
        return core.element.create(Advancements.class)
                .withInit(Advancements::init)
                .withUninit(Advancements::uninit)
                .withInstance()
                .<JsonObject>addConfig("advancements", v -> v.withDefault(new JsonObject()).withInvoke(Advancements::config));
    }
    private static AdvancementManager manager;
    private static final HashMap<String, Advancement> advancements = new HashMap<>();
    public static void init() {
        AnyEvent.addEvent("advancements", AnyEvent.type.other, builder -> builder.createParam("grant","revoke").createParam(key -> Objects.equals(key, "*") ? null : advancements.get(key), () -> Streams.concat(advancements.keySet().stream(), Stream.of("*")).collect(Collectors.toList())), (player, state, advancement) -> {
            switch (state) {
                case "grant":
                    if (advancement == null) manager.getAdvancements().forEach(item -> manager.grantAdvancement(player, item));
                    else manager.grantAdvancement(player, advancement.instance);
                    savePlayer(player);
                    syncAdvancements(player);
                    return;
                case "revoke":
                    if (advancement == null) manager.getAdvancements().forEach(item -> manager.revokeAdvancement(player, item));
                    else manager.revokeAdvancement(player, advancement.instance);
                    savePlayer(player);
                    syncAdvancements(player);
                    return;
            }
        });
    }

    private static void syncAdvancements(Player player) {
        if (manager != null) manager.updateProgress(player, manager.getAdvancements().toArray(eu.endercentral.crazy_advancements.advancement.Advancement[]::new));
    }

    private static void savePlayer(Player player) {
        if (manager != null) manager.saveProgress(player);
    }
    private static void loadPlayer(Player player) {
        if (manager != null) {
            manager.loadProgress(player);
            syncAdvancements(player);
        }
    }

    public enum AdvancementState {
        None,
        Parent,
        Done
    }

    public static AdvancementState advancementState(Player player, String key) {
        if (manager == null) return AdvancementState.None;
        eu.endercentral.crazy_advancements.advancement.Advancement current = manager.getAdvancement(new NameKey("lime", key));
        boolean doneCurrent = Optional.ofNullable(current)
                .map(v -> v.isGranted(player))
                .orElse(false);
        boolean doneParent = Optional.ofNullable(current)
                .map(eu.endercentral.crazy_advancements.advancement.Advancement::getParent)
                .map(v -> v.isGranted(player))
                .orElse(true);
        return doneCurrent
                ? AdvancementState.Done
                : doneParent
                    ? AdvancementState.Parent
                    : AdvancementState.None;
    }

    @EventHandler private static void on(PlayerJoinEvent e) {
        lime.once(() -> {
            manager.addPlayer(e.getPlayer());
            loadPlayer(e.getPlayer());
        }, 1);
    }
    @EventHandler private static void on(PlayerQuitEvent e) {
        lime.once(() -> {
            savePlayer(e.getPlayer());
            manager.removePlayer(e.getPlayer());
        }, 1);
    }
    public static class Advancement {
        public final String key;
        public final Advancement parent;
        public final ItemStack icon;
        public final ComponentMessage title;
        public final ComponentMessage description;
        public final String background;
        public final AdvancementDisplay.AdvancementFrame frame;
        public final double x;
        public final double y;
        public final AdvancementVisibility visibility;
        public final List<AdvancementFlag> flags = new ArrayList<>();
        private eu.endercentral.crazy_advancements.advancement.Advancement instance;

        private static class ComponentMessage extends JSONMessage {
            private final IChatBaseComponent component;
            private ComponentMessage(Component component) {
                super(ChatHelper.toMD5(component));
                this.component = ChatHelper.toNMS(component);
            }

            @Override public IChatBaseComponent getBaseComponent() { return component; }

            public static ComponentMessage create(String format) {
                return new ComponentMessage(ChatHelper.formatComponent(format));
            }
        }

        private static <T>T ofOrDefault(JsonObject json, String key, system.Func1<JsonElement, T> func, T def) {
            if (!json.has(key)) return def;
            JsonElement el = json.get(key);
            if (el.isJsonNull()) return def;
            return func.invoke(el);
        }
        public Advancement(String key, JsonObject json, HashMap<String, Advancement> map) {
            this.key = key;
            this.parent = ofOrDefault(json, "parent", v -> map.get(v.getAsString()), null);

            this.icon = ItemCreator.parse(json.getAsJsonObject("icon")).createItem();

            this.title = ComponentMessage.create(json.get("title").getAsString());
            this.description = ComponentMessage.create(json.get("description").getAsString());
            this.background = ofOrDefault(json, "background", JsonElement::getAsString, null);
            this.frame = ofOrDefault(json, "frame", v -> AdvancementDisplay.AdvancementFrame.valueOf(v.getAsString()), AdvancementDisplay.AdvancementFrame.TASK);
            this.x = json.has("x") ? json.get("x").getAsDouble() : 0;
            this.y = json.has("y") ? json.get("y").getAsDouble() : 0;
            this.visibility = ofOrDefault(json, "visibility", v -> AdvancementVisibility.parseVisibility(v.getAsString()), AdvancementVisibility.ALWAYS);
            if (json.has("flags")) json.getAsJsonArray("flags").forEach(flag -> flags.add(AdvancementFlag.valueOf(flag.getAsString())));
        }
        public static Advancement parse(String key, JsonObject json, HashMap<String, Advancement> map) {
            return new Advancement(key, json, map);
        }
        public eu.endercentral.crazy_advancements.advancement.Advancement advancement() {
            AdvancementDisplay display = new AdvancementDisplay(icon, title, description, frame, visibility);
            if (parent == null) display.setBackgroundTexture(background);
            else display.setCoordinates((float)x, (float)y);
            return instance == null
                    ? instance = new eu.endercentral.crazy_advancements.advancement.Advancement(parent == null ? null : parent.instance, new NameKey("lime", key), display, flags.toArray(AdvancementFlag[]::new))
                    : instance;
        }
    }

    public static void config(JsonObject json) {
        if (manager != null) manager.getPlayers().stream().toList().forEach(manager::removePlayer);
        manager = new AdvancementManager(new NameKey("lime", "advancement"));
        lime.once(() -> {
            try {
                LinkedHashMap<String, Advancement> map = new LinkedHashMap<>();
                json.entrySet().forEach(_kv -> _kv.getValue().getAsJsonObject().entrySet().forEach(kv -> map.put(kv.getKey(), Advancement.parse(kv.getKey(), kv.getValue().getAsJsonObject(), map))));
                eu.endercentral.crazy_advancements.advancement.Advancement[] list = map.values().stream().map(Advancement::advancement).toArray(eu.endercentral.crazy_advancements.advancement.Advancement[]::new);
                if (list.length > 0) manager.addAdvancement(list);
                advancements.clear();
                advancements.putAll(map);

                Bukkit.getOnlinePlayers().forEach(player -> {
                    manager.addPlayer(player);
                    loadPlayer(player);
                });
            } catch (Exception e) {
                lime.logStackTrace(e);
            }
        }, 1);
    }
    public static void uninit() {
        if (manager != null) manager.getPlayers().stream().toList().forEach(manager::removePlayer);
        manager = null;
    }
}


















