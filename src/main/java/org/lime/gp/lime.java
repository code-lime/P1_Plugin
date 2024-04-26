package org.lime.gp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.geco.gsit.api.GSitAPI;
import dev.geco.gsit.objects.GSeat;
import dev.geco.gsit.objects.GetUpReason;
import dev.geco.gsit.objects.IGPoseSeat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.scheduler.BukkitTask;
import org.lime.core;
import org.lime.gp.docs.Docs;
import org.lime.gp.item.settings.use.UseSetting;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.display.models.Models;
import org.lime.display.models.shadow.BaseBuilder;
import org.lime.gp.admin.Administrator;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.component.data.BookshelfInstance;
import org.lime.gp.block.component.data.CartographyInstance;
import org.lime.gp.block.component.data.voice.RadioLoader;
import org.lime.gp.block.component.display.BlockDisplay;
import org.lime.gp.chat.ChatMessages;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.coreprotect.CoreProtectHandle;
import org.lime.gp.craft.Crafts;
import org.lime.gp.craft.Locked;
import org.lime.gp.database.Methods;
import org.lime.gp.database.readonly.ReadonlySync;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.item.*;
import org.lime.gp.item.weapon.Bullets;
import org.lime.gp.item.weapon.WeaponLoader;
import org.lime.gp.map.MapMonitor;
import org.lime.gp.module.*;
import org.lime.gp.module.damage.EntityDamageByPlayerEvent;
import org.lime.gp.module.biome.time.DayManager;
import org.lime.gp.module.loot.PopulateLootEvent;
import org.lime.gp.player.inventory.gui.InterfaceManager;
import org.lime.gp.player.inventory.MainPlayerInventory;
import org.lime.gp.player.inventory.TownInventory;
import org.lime.gp.player.inventory.WalletInventory;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.player.menu.page.Insert;
import org.lime.gp.player.module.*;
import org.lime.gp.player.module.needs.food.FoodUI;
import org.lime.gp.player.module.needs.thirst.Thirst;
import org.lime.gp.player.module.needs.food.ProxyFoodMetaData;
import org.lime.gp.player.module.pets.Pets;
import org.lime.gp.player.perm.Grants;
import org.lime.gp.player.perm.Perms;
import org.lime.gp.player.perm.Works;
import org.lime.gp.player.selector.UserSelector;
import org.lime.gp.player.ui.*;
import org.lime.gp.player.voice.Radio;
import org.lime.gp.player.voice.Voice;
import org.lime.gp.sound.Sounds;
import org.lime.gp.town.ChurchManager;
import org.lime.gp.town.Prison;
import org.lime.invokable.IInvokable;
import org.lime.plugin.IConfig;
import org.lime.plugin.TimerBuilder;
import org.lime.plugin.Timers;
import patch.gp.MutatePatcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class lime extends core {
    public static lime _plugin;

    @Override public String getLogPrefix() { return "LIME:P1"; }
    @Override public String getConfigFile() { return "plugins/p1/"; }

    static {
        MutatePatcher.register();
    }

    //<editor-fold desc="CORE INIT">
    public static void logToFile(String key, String text) { _plugin._logToFile(key, text);}
    public static void log(String log) { _plugin._log(log);}
    public static void logAdmin(String log) { _plugin._logAdmin(log);}
    public static void logConsole(String log) { _plugin._logConsole(log);}
    public static void logOP(String log) { _plugin._logOP(log); }
    public static void logOP(Component log) { _plugin._logOP(log); }
    public static void logWithoutPrefix(String log) { _plugin._logWithoutPrefix(log);}
    public static void logStackTrace(Throwable exception) { _plugin._logStackTrace(exception); }
    public static void logStackTrace() { _plugin._logStackTrace(); }

    public static TimerBuilder timer() { return _plugin._timer(); }
    public static BukkitTask nextTick(Timers.IRunnable callback) { return _plugin._nextTick(callback); }
    public static BukkitTask onceNoCheck(Timers.IRunnable callback, double sec) { return _plugin._onceNoCheck(callback, sec); }
    public static BukkitTask once(Timers.IRunnable callback, double sec) { return _plugin._once(callback, sec); }
    public static BukkitTask onceTicks(Timers.IRunnable callback, long ticks) { return _plugin._onceTicks(callback, ticks); }
    public static BukkitTask repeat(Timers.IRunnable callback, double sec) { return _plugin._repeat(callback, sec); }
    public static BukkitTask repeatTicks(Timers.IRunnable callback, long ticks) { return _plugin._repeatTicks(callback, ticks); }
    public static BukkitTask repeat(Timers.IRunnable callback, double wait, double sec) { return _plugin._repeat(callback, wait, sec); }
    public static BukkitTask repeatTicks(Timers.IRunnable callback, long wait, long ticks) { return _plugin._repeatTicks(callback, wait, ticks); }
    public static <T>void repeat(T[] array, Action1<T> callback_part, Action0 callback_end, double sec, int inOneStep) { _plugin._repeat(array, callback_part, callback_end, sec, inOneStep); }
    public static BukkitTask invokeAsync(Action0 async, Timers.IRunnable nextSync) { return _plugin._invokeAsync(async, nextSync); }
    public static <T>BukkitTask invokeAsync(Func0<T> async, Action1<T> nextSync) { return _plugin._invokeAsync(async, nextSync); }
    public static void invokeSync(Timers.IRunnable sync) { _plugin._invokeSync(sync); }
    public static void invokable(IInvokable invokable) { _plugin._invokable(invokable); }

    public static JsonElement combineJson(JsonElement first, JsonElement second, boolean array_join) { return _plugin._combineJson(first, second, array_join); }
    public static JsonElement combineJson(JsonElement first, JsonElement second) { return _plugin._combineJson(first, second); }
    public static JsonObject combineParent(JsonObject json) { return _plugin._combineParent(json); }
    public static JsonObject combineParent(JsonObject json, boolean category, boolean array_join) { return _plugin._combineParent(json, category, array_join); }

    public static boolean existFile(String path) { return _plugin._existFile(path); }
    public static String readAllText(String path) { return _plugin._readAllText(path); }
    public static String readAllText(File file) { return _plugin._readAllText(file); }
    public static void writeAllText(String path, String text) { _plugin._writeAllText(path, text); }
    public static void deleteText(String path) { _plugin._deleteText(path); }
    public static File getConfigFile(String file) { return _plugin._getConfigFile(file); }
    public static boolean existConfig(String config) { return _plugin._existConfig(config); }
    public static String readAllConfig(String config) { return _plugin._readAllConfig(config); }
    public static void writeAllConfig(String config, String text) { _plugin._writeAllConfig(config, text); }
    public static void deleteConfig(String config) { _plugin._deleteConfig(config); }
    public static boolean existConfig(String config, String ext) { return _plugin._existConfig(config, ext); }
    public static String readAllConfig(String config, String ext) { return _plugin._readAllConfig(config, ext); }
    public static void writeAllConfig(String config, String ext, String text) { _plugin._writeAllConfig(config, ext, text); }
    public static void deleteConfig(String config, String ext) { _plugin._deleteConfig(config, ext); }
    //</editor-fold>

    public static CraftWorld MainWorld;
    public static CraftWorld NetherWorld;
    public static CraftWorld LoginWorld;
    public static CraftWorld EndWorld;
    public static CraftWorld CustomWorld;

    public static boolean isLay(Player player) {
        IGPoseSeat poseSeat = GSitAPI.getPose(player);
        return poseSeat != null && poseSeat.getPose() == Pose.SLEEPING;
    }
    public static boolean isSit(Player player) {
        return GSitAPI.isSitting(player);
    }
    public static boolean isCrawl(Player player) {
        return GSitAPI.isCrawling(player);
    }
    public static void unSit(Player player) {
        GSeat seat = GSitAPI.getSeat(player);
        if (seat == null) return;
        GSitAPI.removeSeat(seat.getEntity(), GetUpReason.PLUGIN, true);
    }
    public static void unLay(Player player) {
        IGPoseSeat seat = GSitAPI.getPose(player);
        if (seat == null) return;
        GSitAPI.removePose(seat.getPlayer(), GetUpReason.PLUGIN, true);
    }

    public static org.lime.modules.autodownload autodownload;
    public static org.lime.modules.proxy proxy;
    public static Models models;
    private static final boolean isDevelopment = System.getProperty("org.lime.development", "false").equalsIgnoreCase("true");

    @Override public org.lime.JavaScript js() { return JavaScript.js; }

    private enum ExitStatus {
        NORMAL,
        ERROR,
        CRASH
    }
    private static ExitStatus exitStatus = ExitStatus.CRASH;

    @Override protected void init() {
        Bukkit.getWorlds().forEach(world -> world.setGameRule(GameRule.NATURAL_REGENERATION, false));
        MainWorld = (CraftWorld)Bukkit.getWorlds().get(0);
        NetherWorld = (CraftWorld)Bukkit.getWorld("world_nether");
        EndWorld = (CraftWorld)Bukkit.getWorld("world_the_end");
        List<World> worlds = Bukkit.getWorlds();
        worlds.add(LoginWorld = (CraftWorld)new WorldCreator("world_login").type(WorldType.FLAT).createWorld());
        worlds.add(CustomWorld = (CraftWorld)new WorldCreator("world_custom").type(WorldType.FLAT).createWorld());
        
        add(branch.create());
        autodownload = (org.lime.modules.autodownload) add(org.lime.modules.autodownload.create()).element().map(v -> v.instance).orElseThrow();
        proxy = (org.lime.modules.proxy) add(org.lime.modules.proxy.create()).element().map(v -> v.instance).orElseThrow();
        add(ThreadPool.create());
        add(Methods.create());
        JavaScript.createAdd();
        models = (Models) add(Models.create(JavaScript.js, Docs.link.builderTypes())).element().map(v -> v.instance).orElseThrow();
        library(IConfig.getLibraryFiles("mp3spi-1.9.13.jar", "JDA-5.0.0-beta.12-withDependencies.jar"));
        //library("gdx-1.11.1-SNAPSHOT.jar", "gdx-jnigen-loader-2.3.1.jar", "gdx-bullet-1.11.1-SNAPSHOT.jar");
        //library("ode4j-core-0.4.1.jar");
        //add(Physics.create());

        add(test.create());

        add(ReJoin.create());
        add(EntityDamageByPlayerEvent.create());
        add(WorldGenModify.create());
        add(CommandLogger.create());
        add(DayManager.create());
        add(Discord.create());
        add(DrawMap.create());
        add(DrawText.create());
        add(EntityPosition.create());
        add(FixCursorSlot.create());
        add(HorseRiders.create());
        add(InputEvent.create());
        add(Nether.create());
        add(PacketLogger.create());
        add(PopulateLootEvent.create());
        add(SingleModules.create());
        add(Sounds.create());
        add(TimeoutData.create());
        add(Voice.create());
        add(Radio.create());
        add(InvisibleItemFrame.create());

        add(ReadonlySync.create());
        add(Tables.create());

        add(MapMonitor.create());

        add(CoreProtectHandle.create());

        add(ChatMessages.create());
        add(LangMessages.create());

        add(Items.create());
        add(DisplayHand.create());
        add(BookPaper.create());
        add(Enchants.create());
        add(UseSetting.create());
        add(WeaponLoader.create());
        add(Bullets.create());

        add(Crafts.create());
        add(Locked.create());

        add(Blocks.create());
        add(CampfireSync.create());
        add(BlockDisplay.create());
        add(BookshelfInstance.create());
        add(CartographyInstance.create());
        add(RadioLoader.create());
        add(CartographyBucket.create());
        add(CartographyBrush.create());

        add(Administrator.create());
        add(AnyEvent.create());

        add(InterfaceManager.create());
        add(MainPlayerInventory.create());
        add(TownInventory.create());
        add(WalletInventory.create());
        add(MenuCreator.create());
        add(Insert.create());
        add(Advancements.create());
        add(Death.create());
        add(Fishing.create());
        add(HandCuffs.create());
        add(HideCommands.create());
        add(Interact.create());
        add(Knock.create());
        add(Login.create());
        add(PayDay.create());
        add(Pets.create());
        add(ProxyFoodMetaData.create());
        add(Search.create());
        add(org.lime.gp.player.module.Settings.create());
        add(Skins.create());
        add(TabManager.create());
        add(Perms.create());
        add(Grants.create());
        add(Works.create());
        add(UserSelector.create());

        add(Compass.create());
        add(CustomUI.create());
        add(Infection.create());
        add(FoodUI.create());
        //add(ScoreboardUI.create());
        add(EditorUI.create());
        add(Thirst.create());

        add(ChurchManager.create());
        add(Prison.create());

        List<Component> shows = new ArrayList<>();

        Toast3<Integer, Integer, Integer> showIndex = Toast.of(0, 0, 0);

        addOther().forEach(loadedElement -> lime.once(() -> {
            showIndex.val0++;
            Component text = loadedElement.element()
                    .map(element -> {
                        lime.log("Element '"+element.name+"' of class '"+element.tClass.toString()+"' is loaded other.");
                        showIndex.val1++;
                        return Component.text(element.name + " : " + element.tClass.toString() + " ").append(Component.text("[+]").color(NamedTextColor.GREEN));
                    })
                    .orElseGet(() -> {
                        lime.log("Element '"+loadedElement.name()+"' of class '"+loadedElement.type().toString()+"' is disabled.");
                        showIndex.val2++;
                        return Component.text(loadedElement.name() + " : " + loadedElement.type().toString() + " ").append(Component.text("[-]").color(NamedTextColor.GRAY));
                    });
            shows.add(text);
            if (shows.size() >= 20) {
                Component sendMessage = Component.text("Elements " + (showIndex.val0 - shows.size() + 1) + "..." + showIndex.val0 + " loaded status ")
                        .append(Component.empty()
                                .append(Component.text(showIndex.val1).color(NamedTextColor.GREEN))
                                .append(Component.text(" / "))
                                .append(Component.text(showIndex.val2).color(NamedTextColor.GRAY))
                        )
                        .append(Component.text(": "))
                        .append(Component.text("[...]")
                                .hoverEvent(Component.join(JoinConfiguration.newlines(), shows))
                        );
                Bukkit.getOnlinePlayers().forEach(p -> {
                    if (!p.isOp()) return;
                    p.sendMessage(Component.text("["+getLogPrefix()+"] ").color(NamedTextColor.YELLOW).append(Component.empty().append(sendMessage).color(NamedTextColor.WHITE)));
                });
                shows.clear();
                showIndex.val1 = 0;
                showIndex.val2 = 0;
            }
        }, 1));
        lime.once(() -> {
            if (shows.size() > 0) {
                Component sendMessage = Component.text("Elements " + (showIndex.val0 - shows.size() + 1) + "..." + showIndex.val0 + " loaded status ")
                        .append(Component.empty()
                                .append(Component.text(showIndex.val1).color(NamedTextColor.GREEN))
                                .append(Component.text(" / "))
                                .append(Component.text(showIndex.val2).color(NamedTextColor.GRAY))
                        )
                        .append(Component.text(": "))
                        .append(Component.text("[...]")
                                .hoverEvent(Component.join(JoinConfiguration.newlines(), shows))
                        );
                Bukkit.getOnlinePlayers().forEach(p -> {
                    if (!p.isOp()) return;
                    p.sendMessage(Component.text("["+getLogPrefix()+"] ").color(NamedTextColor.YELLOW).append(Component.empty().append(sendMessage).color(NamedTextColor.WHITE)));
                });

                shows.clear();
                showIndex.val1 = 0;
                showIndex.val2 = 0;
            }
        }, 1);

        /*addOther().forEach(loadedElement -> lime.once(() -> loadedElement.element()
                .ifPresentOrElse(
                        element -> lime.logOP("Element '"+element.name+"' of class '"+element.tClass.toString()+"' is loaded other."),
                        () -> lime.logOP("Element '"+loadedElement.name()+"' of class '"+loadedElement.type().toString()+"' is disabled.")
                ), 1));*/
        add(PlayerData.create());

        exitStatus = ExitStatus.NORMAL;
    }

    @Override protected void onErrorInit() {
        exitStatus = ExitStatus.ERROR;
        tryExit();
    }

    @Override public void onDisable() {
        if (exitStatus == ExitStatus.CRASH) tryExit();
        super.onDisable();
    }
    private static void tryExit() {
        lime.logOP("Development: " + isDevelopment);
        if (isDevelopment) return;
        try {
            if (lime.existConfig("alert")) {
                JsonObject alert = json.parse(lime.readAllConfig("alert")).getAsJsonObject();
                String webhook = alert.get("webhook").getAsString();
                long role = alert.get("role").getAsLong();
                String name = alert.get("name").getAsString();
                Discord.sendMessageToWebhook(webhook, "<@&"+role+"> Внимание! Сервер **"+name+"** не смог запуститься. Проводится автоматическая перезагрузка...", false);
            }
            Bukkit.getOnlinePlayers().forEach(player -> player.kick(Component.text("Проблемы в работе сервера")));
        } catch (Throwable ignore) {

        } finally {
            Bukkit.shutdown();
        }
    }
}

