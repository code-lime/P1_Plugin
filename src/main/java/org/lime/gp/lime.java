package org.lime.gp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.geco.gsit.api.GSitAPI;
import dev.geco.gsit.objects.GSeat;
import dev.geco.gsit.objects.GetUpReason;
import dev.geco.gsit.objects.IGPoseSeat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.scheduler.BukkitTask;
import org.lime.core;
import org.lime.display.Models;
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
import org.lime.gp.database.ReadonlySync;
import org.lime.gp.database.Rows;
import org.lime.gp.database.Tables;
import org.lime.gp.item.*;
import org.lime.gp.item.weapon.Bullets;
import org.lime.gp.item.weapon.WeaponLoader;
import org.lime.gp.map.MapMonitor;
import org.lime.gp.module.*;
import org.lime.gp.module.damage.EntityDamageByPlayerEvent;
import org.lime.gp.player.inventory.InterfaceManager;
import org.lime.gp.player.inventory.MainPlayerInventory;
import org.lime.gp.player.inventory.TownInventory;
import org.lime.gp.player.inventory.WalletInventory;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.player.menu.page.Insert;
import org.lime.gp.player.module.*;
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
import org.lime.system;
import org.lime.timings.lib.MCTiming;

import java.io.File;

public class lime extends core {
    public static lime _plugin;

    @Override public String getLogPrefix() { return "LIME:P1"; }
    @Override public String getConfigFile() { return "plugins/p1/"; }

    static {
        patch.patch(lime.class.getClassLoader().getResource("patch.json"));
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

    public static BukkitTask nextTick(ITimers.IRunnable callback) { return _plugin._nextTick(callback); }
    public static BukkitTask onceNoCheck(ITimers.IRunnable callback, double sec) { return _plugin._onceNoCheck(callback, sec); }
    public static BukkitTask once(ITimers.IRunnable callback, double sec) { return _plugin._once(callback, sec); }
    public static BukkitTask onceTicks(ITimers.IRunnable callback, long ticks) { return _plugin._onceTicks(callback, ticks); }
    public static BukkitTask repeat(ITimers.IRunnable callback, double sec) { return _plugin._repeat(callback, sec); }
    public static BukkitTask repeatTicks(ITimers.IRunnable callback, long ticks) { return _plugin._repeatTicks(callback, ticks); }
    public static BukkitTask repeat(ITimers.IRunnable callback, double wait, double sec) { return _plugin._repeat(callback, wait, sec); }
    public static BukkitTask repeatTicks(ITimers.IRunnable callback, long wait, long ticks) { return _plugin._repeatTicks(callback, wait, ticks); }
    public static <T>void repeat(T[] array, system.Action1<T> callback_part, system.Action0 callback_end, double sec, int inOneStep) { _plugin._repeat(array, callback_part, callback_end, sec, inOneStep); }
    public static BukkitTask invokeAsync(system.Action0 async, ITimers.IRunnable nextSync) { return _plugin._invokeAsync(async, nextSync); }
    public static <T>BukkitTask invokeAsync(system.Func0<T> async, system.Action1<T> nextSync) { return _plugin._invokeAsync(async, nextSync); }
    public static void invokeSync(ITimers.IRunnable sync) { _plugin._invokeSync(sync); }
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

    public static boolean isLay(Player player) {
        IGPoseSeat poseSeat = GSitAPI.getPose(player);
        return poseSeat != null && poseSeat.getPose() == Pose.SLEEPING;
    }
    public static boolean isSit(Player player) {
        return GSitAPI.isSitting(player);
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

    public static org.lime.autodownload autodownload;
    public static Models models;

    @Override protected org.lime.JavaScript js() { return JavaScript.js; }
    @Override protected void init() {
        Bukkit.getWorlds().forEach(world -> world.setGameRule(GameRule.NATURAL_REGENERATION, false));
        MainWorld = (CraftWorld)Bukkit.getWorlds().get(0);
        NetherWorld = (CraftWorld)Bukkit.getWorld("world_nether");
        EndWorld = (CraftWorld)Bukkit.getWorld("world_the_end");
        Bukkit.getWorlds().add(LoginWorld = (CraftWorld)new WorldCreator("world_login").type(WorldType.FLAT).createWorld());
        
        add(branch.create());
        autodownload = (org.lime.autodownload) add(org.lime.autodownload.create()).element().map(v -> v.instance).orElseThrow();
        add(ThreadPool.create());
        add(Methods.create());
        JavaScript.createAdd();
        models = (Models) add(Models.create(JavaScript.js)).element().map(v -> v.instance).orElseThrow();
        library("../libs/mp3spi-1.9.13.jar");
        //library("gdx-1.11.1-SNAPSHOT.jar", "gdx-jnigen-loader-2.3.1.jar", "gdx-bullet-1.11.1-SNAPSHOT.jar");
        //library("ode4j-core-0.4.1.jar");
        //add(Physics.create());

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
        add(NPC.create());
        add(PacketLogger.create());
        add(PopulateLootEvent.create());
        add(SingleModules.create());
        add(Sounds.create());
        add(TimeoutData.create());
        add(Voice.create());
        add(Radio.create());
        add(InvisibleItemFrame.create());

        add(ReadonlySync.create());
        add(Rows.create());
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
        add(Saturation.create());
        add(ScoreboardUI.create());
        add(EditorUI.create());
        add(Thirst.create());

        add(ChurchManager.create());
        add(Prison.create());

        addOther().forEach(loadedElement -> lime.once(() -> loadedElement.element()
                .ifPresentOrElse(
                        element -> lime.logOP("Element '"+element.name+"' of class '"+element.tClass.toString()+"' is loaded other."),
                        () -> lime.logOP("Element '"+loadedElement.name()+"' of class '"+loadedElement.type().toString()+"' is disabled.")
                ), 1));
        add(PlayerData.create());
    }
}

