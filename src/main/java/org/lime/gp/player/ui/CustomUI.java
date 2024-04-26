package org.lime.gp.player.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayInResourcePackStatus;
import net.minecraft.network.protocol.game.PacketPlayOutBoss;
import net.minecraft.world.BossBattle;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.lime;
import org.lime.gp.player.module.Login;
import org.lime.gp.player.ui.respurcepack.ResourcePackData;
import org.lime.gp.player.ui.respurcepack.ShareData;
import org.lime.plugin.CoreElement;
import org.lime.system.execute.Func1;
import org.lime.system.json;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;
import org.lime.system.toast.Toast3;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.*;

@SuppressWarnings("unused")
public class CustomUI implements Listener {
    public static CoreElement create() {
        return CoreElement.create(CustomUI.class)
                .withInit(CustomUI::init)
                .withInstance()
                .addEmpty("rp-up", CustomUI::up)
                .addEmpty("rp-send", CustomUI::sendAll)
                .addEmpty("rp-share", CustomUI::share)
                .<JsonObject>addConfig("resourcepack", v -> v.withInvoke(CustomUI::config).withDefault(new JsonObject()))
                .<JsonElement>addConfig("share", v -> v.withInvoke(CustomUI::configShare).withDefault(JsonNull.INSTANCE))
                .<JsonObject>addConfig("data/unique_rp", v -> v.withInvoke(CustomUI::configUp).withDefault(json.object().addNull("id").build()));
    }

    private static class CustomBossBattle extends BossBattle {
        public CustomBossBattle(UUID uuid, IChatBaseComponent text, BarColor color, BarStyle style, float progress) {
            super(uuid, text, color, style);
            this.progress = progress;
        }
        public CustomBossBattle(UUID uuid) {
            this(uuid, null, BarColor.WHITE, BarStyle.PROGRESS, 0);
        }
        public void setChat(IChatBaseComponent text) {
            this.name = text;
        }
        public void setColor(BarColor color) {
            this.color = color;
        }
        public void setStyle(BarStyle style) {
            this.overlay = style;
        }
        public void setProgress(float progress) {
            this.progress = progress;
        }
    }

    public enum IType {
        ACTIONBAR,
        BOSSBAR,
        TITLE;

        private static final HashMap<UUID, Integer> is_show = new HashMap<>();

        public static void sync() {
            is_show.entrySet().removeIf(kv -> {
                int value = kv.getValue();
                value--;
                Player player = Bukkit.getPlayer(kv.getKey());
                if (player == null) return true;
                if (value <= 0) {
                    PacketManager.sendPacket(player, PacketPlayOutBoss.createRemovePacket(kv.getKey()));
                    return true;
                }
                kv.setValue(value);
                return false;
            });
        }
        public void show(Player player, Component component, boolean empty) {
            switch (this) {
                case ACTIONBAR -> player.sendActionBar(component.append(RP.notSupported()));
                case BOSSBAR -> {
                    if (!ExtMethods.isPlayerLoaded(player)) return;
                    UUID uuid = player.getUniqueId();
                    Integer count = is_show.getOrDefault(uuid, null);
                    Func1<CustomBossBattle, PacketPlayOutBoss> action = count != null
                            ? PacketPlayOutBoss::createUpdateNamePacket
                            : PacketPlayOutBoss::createAddPacket;
                    is_show.put(uuid, 3);
                    PacketManager.sendPacket(player, action.invoke(new CustomBossBattle(player.getUniqueId(), ChatHelper.toNMS(component), BossBattle.BarColor.WHITE, BossBattle.BarStyle.PROGRESS, 1)));
                }
                case TITLE -> {
                    if (!ExtMethods.isPlayerLoaded(player)) return;
                    if (Login.isTitleLogin(player)) return;
                    if (empty) return;
                    player.showTitle(Title.title(component, Component.empty(), Title.Times.times(Duration.ZERO, Duration.ofMillis(250), Duration.ZERO)));
                }
            }
        }
    }
    public interface IUI {
        Collection<ImageBuilder> getUI(Player player);
        IType getType();
    }
    public static abstract class GUI implements IUI {
        private final IType type;
        protected GUI(IType type) { this.type = type; }
        public abstract Collection<ImageBuilder> getUI(Player player);
        public IType getType() { return type; }
    }
    private static final List<IUI> iuis = new ArrayList<>();
    public static void addListener(IUI iui) {
        iuis.add(iui);
    }

    private static ResourcePackData RP = ResourcePackData.createEmpty();
    private static @Nullable ShareData SHARE;
    private static boolean REQIRE_RP;

    public static class TextGlobalUI extends GUI {
        private TextGlobalUI() { super(IType.ACTIONBAR); }
        private static Toast3<String, Integer, TextColor> showTexts = null;

        public static void show(String text) {
            show(text, 3);
        }
        public static void show(String text, int ticks) {
            show(text, 3, NamedTextColor.WHITE);
        }
        public static void show(String text, int ticks, TextColor color) {
            showTexts = Toast.of(text, ticks, color);
            CustomUI.update();
        }
        public static void hide() {
            showTexts = null;
            CustomUI.update();
        }
        @Override public Collection<ImageBuilder> getUI(Player player) {
            UUID uuid = player.getUniqueId();
            if (showTexts != null) {
                showTexts.val1--;
                if (showTexts.val1 <= 0) showTexts = null;
            }
            return showTexts == null ?
                    Collections.emptyList() :
                    Collections.singleton(ImageBuilder.of(player, showTexts.val0).withColor(showTexts.val2));
        }
    }
    public static class TextUI extends GUI {
        private TextUI() { super(IType.ACTIONBAR); }
        private static final HashMap<UUID, Toast2<Integer, Collection<ImageBuilder>>> showTexts = new HashMap<>();

        public static void show(Player player, ImageBuilder text) {
            show(player, text, 3);
        }
        public static void show(Player player, ImageBuilder text, int ticks) {
            showTexts.put(player.getUniqueId(), Toast.of(ticks, Collections.singleton(text)));
            CustomUI.updatePlayer(player);
        }
        public static void show(Player player, Collection<ImageBuilder> text) {
            show(player, text, 3);
        }
        public static void show(Player player, Collection<ImageBuilder> text, int ticks) {
            showTexts.put(player.getUniqueId(), Toast.of(ticks, text));
            CustomUI.updatePlayer(player);
        }
        public static void hide(Player player) {
            showTexts.remove(player.getUniqueId());
            CustomUI.updatePlayer(player);
        }
        @Override public Collection<ImageBuilder> getUI(Player player) {
            UUID uuid = player.getUniqueId();
            Toast2<Integer, Collection<ImageBuilder>> uses = showTexts.getOrDefault(uuid, null);
            if (uses == null) return Collections.emptyList();
            uses.val0--;
            if (uses.val0 <= 0) showTexts.remove(uuid);
            return uses.val1;
        }
    }
    public static class BossBarUI extends GUI {
        private BossBarUI() { super(IType.BOSSBAR); }
        private static final HashMap<UUID, Toast2<Integer, Collection<ImageBuilder>>> showTexts = new HashMap<>();

        public static void show(Player player, ImageBuilder text) {
            show(player, text, 3);
        }
        public static void show(Player player, ImageBuilder text, int ticks) {
            showTexts.put(player.getUniqueId(), Toast.of(ticks, Collections.singleton(text)));
            CustomUI.updatePlayer(player);
        }
        public static void show(Player player, Collection<ImageBuilder> text) {
            show(player, text, 3);
        }
        public static void show(Player player, Collection<ImageBuilder> text, int ticks) {
            showTexts.put(player.getUniqueId(), Toast.of(ticks, text));
            CustomUI.updatePlayer(player);
        }
        public static void hide(Player player) {
            showTexts.remove(player.getUniqueId());
            CustomUI.updatePlayer(player);
        }
        @Override public Collection<ImageBuilder> getUI(Player player) {
            UUID uuid = player.getUniqueId();
            Toast2<Integer, Collection<ImageBuilder>> uses = showTexts.getOrDefault(uuid, null);
            if (uses == null) return Collections.emptyList();
            uses.val0--;
            if (uses.val0 <= 0) showTexts.remove(uuid);
            return uses.val1;
        }
    }
    public static class TitleUI extends GUI {
        private TitleUI() { super(IType.TITLE); }
        private static final HashMap<UUID, Toast2<Integer, Collection<ImageBuilder>>> showTexts = new HashMap<>();

        public static void show(Player player, ImageBuilder text) {
            show(player, text, 3);
        }
        public static void show(Player player, ImageBuilder text, int ticks) {
            showTexts.put(player.getUniqueId(), Toast.of(ticks, Collections.singleton(text)));
            CustomUI.updatePlayer(player);
        }
        public static void show(Player player, Collection<ImageBuilder> text) {
            show(player, text, 3);
        }
        public static void show(Player player, Collection<ImageBuilder> text, int ticks) {
            showTexts.put(player.getUniqueId(), Toast.of(ticks, text));
            CustomUI.updatePlayer(player);
        }
        public static void hide(Player player) {
            showTexts.remove(player.getUniqueId());
            CustomUI.updatePlayer(player);
        }
        @Override public Collection<ImageBuilder> getUI(Player player) {
            UUID uuid = player.getUniqueId();
            Toast2<Integer, Collection<ImageBuilder>> uses = showTexts.getOrDefault(uuid, null);
            if (uses == null) return Collections.emptyList();
            uses.val0--;
            if (uses.val0 <= 0) showTexts.remove(uuid);
            return uses.val1;
        }
    }

    public static void init() {
        CustomUI.addListener(new TextUI());
        CustomUI.addListener(new TextGlobalUI());
        CustomUI.addListener(new BossBarUI());
        CustomUI.addListener(new TitleUI());
        lime.repeatTicks(CustomUI::update, 2);
    }
    public static void config(JsonObject json) {
        RP = json.get("version").isJsonNull()
                ? RP.empty()
                : RP.update(json.get("default").getAsString(), json.get("version").getAsString());
    }
    public static void sendAll() {
        Bukkit.getOnlinePlayers().forEach(v -> RP.send(v, SHARE));
    }
    public static void update() {
        Bukkit.getOnlinePlayers().forEach(CustomUI::updatePlayer);
        IType.sync();
    }

    public static void configShare(JsonElement _json) {
        SHARE = _json.isJsonNull()
                ? null
                : ShareData.parse(_json.getAsJsonObject());
    }
    public static void share() {
        if (SHARE == null) {
            lime.logOP("Share disabled!");
            return;
        }
        lime.logOP("Start share...");
        SHARE.share();
    }

    public static void configUp(JsonObject _json) {
        JsonElement id = _json.get("id");
        RP = RP.updateIndex(id.isJsonNull() ? null : id.getAsString());
    }
    public static void up() {
        String unique = UUID.randomUUID().toString();
        RP = RP.updateIndex(unique);
        lime.writeAllConfig("data/unique_rp", json.format(json.object().add("id", unique).build()));
    }

    public static void updatePlayer(Player player) {
        HashMap<IType, List<ImageBuilder>> builders = new HashMap<>();
        for (IType type : IType.values()) builders.put(type, new LinkedList<>());
        for (IUI iui : iuis) builders.get(iui.getType()).addAll(iui.getUI(player));
        builders.forEach((type, list) -> type.show(player, Component.empty().append(ImageBuilder.join(list, 1)), list.isEmpty()));
    }
    @EventHandler public static void on(PlayerJoinEvent e) {
        RP.send(e.getPlayer(), SHARE);
    }
}





























