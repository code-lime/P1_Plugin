package org.lime.gp.player.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutBoss;
import net.minecraft.world.BossBattle;
import org.apache.http.client.utils.URIBuilder;
import org.lime.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.lime;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.component.list.RemoteExecuteComponent;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.item.Items;
import org.lime.gp.item.Settings;
import org.lime.gp.item.Items.ItemCreator;
import org.lime.gp.chat.ChatHelper;

import java.util.*;

@SuppressWarnings("unused")
public class CustomUI implements Listener {
    public static core.element create() {
        return core.element.create(CustomUI.class)
                .withInit(CustomUI::init)
                .withInstance()
                .addEmpty("rp-send", CustomUI::sendAll)
                .addEmpty("rp-share", CustomUI::share)
                .<JsonObject>addConfig("resourcepack", v -> v.withInvoke(CustomUI::config).withDefault(new JsonObject()))
                .<JsonElement>addConfig("share", v -> v.withInvoke(CustomUI::configShare).withDefault(JsonNull.INSTANCE));
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
        BOSSBAR;

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
        public void show(Player player, Component component) {
            switch (this) {
                case ACTIONBAR: player.sendActionBar(component.append(NOT_SUPPORTED)); return;
                case BOSSBAR: {
                    if (!ExtMethods.isPlayerLoaded(player)) return;
                    UUID uuid = player.getUniqueId();
                    Integer count = is_show.getOrDefault(uuid, null);
                    system.Func1<CustomBossBattle, PacketPlayOutBoss> action = count != null
                            ? PacketPlayOutBoss::createUpdateNamePacket
                            : PacketPlayOutBoss::createAddPacket;
                    is_show.put(uuid, 3);
                    PacketManager.sendPacket(player, action.invoke(new CustomBossBattle(player.getUniqueId(), ChatHelper.toNMS(component), BossBattle.BarColor.WHITE, BossBattle.BarStyle.PROGRESS, 1)));
                    return;
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

    public record Share(
        String url,
        String owner,
        String repo,
        String branch,
        Map<String, String> headers) {
        public static Share parse(JsonObject json) {
            return new Share(
                json.get("url").getAsString(),
                json.get("owner").getAsString(),
                json.get("repo").getAsString(),
                json.get("branch").getAsString(),
                system.map.<String, String>of()
                    .add(json.get("headers")
                        .getAsJsonObject()
                        .entrySet()
                        .iterator(),
                        kv -> kv.getKey(),
                        kv -> kv.getValue().getAsString())
                    .build()
            );
        }

        public void share() {
            lime.logOP("[Share] Setup remote generator...");
            String jsonString = system.json.object()
                .addArray("execute", _v -> _v
                    .add(Items.creatorIDs.values()
                        .stream()
                        .map(v -> v instanceof ItemCreator c ? c : null)
                        .filter(Objects::nonNull)
                        .flatMap(v -> Items.getAll(Settings.RemoteExecuteSetting.class, v).stream())
                        .flatMap(v -> v.execute.stream())
                        .iterator(), item -> item)
                    .add(Blocks.creators.values()
                        .stream()
                        .flatMap(v -> v.components.values().stream())
                        .map(v -> v instanceof RemoteExecuteComponent c ? c : null)
                        .filter(Objects::nonNull)
                        .flatMap(v -> v.execute.stream())
                        .iterator(), item -> item)
                )
                .add("owner", owner)
                .add("repo", repo)
                .add("branch", branch)
                .build()
                .toString();
            lime.logOP("[Share] Execute remote generator...");
            web.method.POST
                    .create(url, jsonString)
                    .expectContinue(true)
                    .lines()
                    .executeAsync((lines, code) -> {
                        lime.logOP("[Share] Remote generator logs:");
                        lines.forEach(line -> {
                            lime.logOP("[Share]  - " + line);
                        });
                    });
        }
    }
    private static class ResourcePack {
        public String SEND_URL;
        public String RP_URL;
        public String VERSION;

        public ResourcePack(String URL, String VERSION) {
            this.RP_URL = URL;
            this.SEND_URL = createUrl(URL, VERSION);
            this.VERSION = VERSION;
        }

        private static String createUrl(String url, String version) {
            try {
                URIBuilder _url = new URIBuilder(url);
                _url.addParameter("v", version);
                return _url.toString();
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
        public void send(Player player) {
            if (SEND_URL == null) return;
            player.setResourcePack(SEND_URL, VERSION);
        }
    }
    private static ResourcePack RP;
    private static Share SHARE;
    private static String VERSION;
    private static Component NOT_SUPPORTED;
    private static boolean REQIRE_RP;

    public static class TextGlobalUI extends GUI {
        private TextGlobalUI() { super(IType.ACTIONBAR); }
        private static system.Toast3<String, Integer, TextColor> showTexts = null;

        public static void show(String text) {
            show(text, 3);
        }
        public static void show(String text, int ticks) {
            show(text, 3, NamedTextColor.WHITE);
        }
        public static void show(String text, int ticks, TextColor color) {
            showTexts = system.toast(text, ticks, color);
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
                    Collections.singleton(ImageBuilder.of(showTexts.val0).withColor(showTexts.val2));
        }
    }
    public static class TextUI extends GUI {
        private TextUI() { super(IType.ACTIONBAR); }
        private static final HashMap<UUID, system.Toast2<String, Integer>> showTexts = new HashMap<>();

        public static void show(Player player, String text) {
            show(player, text, 3);
        }
        public static void show(Player player, String text, int ticks) {
            showTexts.put(player.getUniqueId(), system.toast(text, ticks));
            CustomUI.updatePlayer(player);
        }
        public static void hide(Player player) {
            showTexts.remove(player.getUniqueId());
            CustomUI.updatePlayer(player);
        }
        @Override public Collection<ImageBuilder> getUI(Player player) {
            UUID uuid = player.getUniqueId();
            system.Toast2<String, Integer> uses = showTexts.getOrDefault(uuid, null);
            if (uses == null) return Collections.emptyList();
            uses.val1--;
            if (uses.val1 <= 0) showTexts.remove(uuid);
            return Collections.singleton(ImageBuilder.of(uses.val0));
        }
    }
    public static class TitleUI extends GUI {
        private TitleUI() { super(IType.BOSSBAR); }
        private static final HashMap<UUID, system.Toast2<String, Integer>> showTexts = new HashMap<>();

        public static void show(Player player, String text) {
            show(player, text, 3);
        }
        public static void show(Player player, String text, int ticks) {
            showTexts.put(player.getUniqueId(), system.toast(text, ticks));
            CustomUI.updatePlayer(player);
        }
        public static void hide(Player player) {
            showTexts.remove(player.getUniqueId());
            CustomUI.updatePlayer(player);
        }
        @Override public Collection<ImageBuilder> getUI(Player player) {
            UUID uuid = player.getUniqueId();
            system.Toast2<String, Integer> uses = showTexts.getOrDefault(uuid, null);
            if (uses == null) return Collections.emptyList();
            uses.val1--;
            if (uses.val1 <= 0) showTexts.remove(uuid);
            return Collections.singleton(ImageBuilder.of(uses.val0));
        }
    }

    public static void init() {
        CustomUI.addListener(new TextUI());
        CustomUI.addListener(new TextGlobalUI());
        CustomUI.addListener(new TitleUI());
        lime.repeatTicks(CustomUI::update, 2);
    }
    public static void config(JsonObject json) {
        VERSION = json.get("version").isJsonNull() ? "0.0.0" : json.get("version").getAsString();
        String v0 = "0";
        String v1 = "0";
        String v2 = "0";
        String build = "0";
        String[] key = VERSION.split("\\.");
        for (int i = 0; i < key.length; i++) {
            switch (i) {
                case 0: v0 = key[i]; break;
                case 1: v1 = key[i]; break;
                case 2: v2 = key[i]; break;
                case 3: build = key[i]; break;
                default: break;
            }
        }
        for (int i = 0; i < 3; i++)
        VERSION = String.join(".", v0, v1, v2);
        Component not_supported = Component.translatable("ЭТОТ РЕСУРСПАК НЕ ПОДДЕРЖИВАЕТСЯ! v"+VERSION+" | THIS RESOURCEPACK IS NOT SUPPORTED! v"+VERSION+" | ", TextColor.color(255, 0, 0));
        NOT_SUPPORTED = Component.empty();
        for (int i = 0; i < 10; i++) NOT_SUPPORTED = NOT_SUPPORTED.append(not_supported);
        RP = new ResourcePack(
                json.get("version").isJsonNull() ? null : json.get("default").getAsString(),
                VERSION + "." + build);
    }
    public static void configShare(JsonElement _json) {
        if (_json.isJsonNull()) {
            SHARE = null;
        } else {
            SHARE = Share.parse(_json.getAsJsonObject());
        }
    }
    public static void sendAll() {
        Bukkit.getOnlinePlayers().forEach(RP::send);
    }
    public static void share() {
        if (SHARE == null) {
            lime.logOP("Share disabled!");
            return;
        }
        lime.logOP("Start share...");
        SHARE.share();
    }
    /*public static void upload() {
        if (RP.URL == null) return;
        lime.logOP("Download 'resourcepack.zip'...");
        HashMap<String, byte[]> map = zip.unzip(web.method.GET.create(RP.URL).data().execute().val0);
        map.put("tmp3.txt", "tmp_data".getBytes());
        Path path = Paths.get("/", "var","www","html","p1", "resourcepack.zip");
        lime.logOP("Path: " + path);
        system.<Path, byte[]>actionEx(java.nio.file.Files::write).throwable().invoke(path, zip.zip(map));
        lime.logOP("Saved in " + new File("/var/www/html/p1/resourcepack.zip").toPath().toAbsolutePath());
    }*/
    public static void update() {
        Bukkit.getOnlinePlayers().forEach(CustomUI::updatePlayer);
        IType.sync();
    }
    public static void updatePlayer(Player player) {
        HashMap<IType, List<ImageBuilder>> builders = new HashMap<>();
        for (IType type : IType.values()) builders.put(type, new LinkedList<>());
        for (IUI iui : iuis) builders.get(iui.getType()).addAll(iui.getUI(player));
        builders.forEach((type, list) -> type.show(player, Component.empty().append(ImageBuilder.join(list, 1))));
    }
    @EventHandler public static void on(PlayerJoinEvent e) { RP.send(e.getPlayer()); }
}





























