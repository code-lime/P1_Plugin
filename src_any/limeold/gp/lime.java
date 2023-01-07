package org.limeold.gp;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.lime.core;
import org.lime.gp.admin.Administrator;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.block.data.*;
import org.lime.gp.chat.ChatMessages;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.coreprotect.CoreProtectHandle;
import org.lime.gp.display.Displays;
import org.lime.gp.display.Model;
import org.lime.gp.item.BookPaper;
import org.lime.gp.item.Weapon;
import org.lime.gp.extension.Path;
import org.limeold.gp.data.Train;
import org.lime.gp.module.*;
import org.lime.gp.player.inventory.*;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.player.module.*;
import org.lime.gp.player.selector.UserSelector;
import org.lime.gp.player.ui.*;
import org.lime.gp.town.ChurchManager;
import org.lime.gp.town.Prison;
import org.lime.system;
import org.lime.gp.map.MapMonitor;
import org.lime.gp.module.DrawText;
import org.lime.gp.module.MobManager;
import org.lime.gp.module.NPC;
import org.lime.gp.module.RadioVoice;
import org.limeold.gp.web.DataReader;

public final class lime extends core {
    public static boolean isNoError(system.Action0 action) {
        try { action.invoke(); return true; }
        catch (Exception e) { return false; }
    }
    public static <T>boolean isNoError(system.Func0<T> action, system.Action1<T> callback) {
        try { callback.invoke(action.invoke()); return true; }
        catch (Exception e) { return false; }
    }
    public static <T, TRet>boolean isNoError(system.Func1<T, TRet> action, T arg, system.Action1<TRet> callback) {
        try { callback.invoke(action.invoke(arg)); return true; }
        catch (Exception e) { return false; }
    }

    public static World MainWorld;
    public static World NetherWorld;
    public static World LoginWorld;
    public static World EndWorld;


    public static system.Toast3<String, String, Integer> get() {
        return system.toast("", "", 22);
    }

    //private static final HashMap<UUID, Integer> allowFly = new HashMap<>();
    public static void allowFly(Player player, int ticks) {
        /*allowFly.put(player.getUniqueId(), ticks);
        if (!player.getAllowFlight()) player.setAllowFlight(true);*/
    }

    public static org.lime.autodownload autodownload;

    @Override protected void init() {
        Bukkit.getWorlds().forEach(world -> world.setGameRule(GameRule.NATURAL_REGENERATION, false));
        MainWorld = Bukkit.getWorlds().get(0);
        NetherWorld = Bukkit.getServer().getWorld("world_nether");
        EndWorld = Bukkit.getServer().getWorld("world_the_end");
        Bukkit.getWorlds().add(LoginWorld = new WorldCreator("world_login").type(WorldType.FLAT).createWorld());

        lime.autodownload = (org.lime.autodownload)add(org.lime.autodownload.create()).element().map(v -> v.instance).orElseThrow();
        add(CommandLogger.create());
        add(SingleModules.create());
        add(ThreadPool.create());
        add(PopulateLootEvent.create());
        add(InputEvent.create());
        add(JavaScript.create());
        add(LangMessages.create());
        add(AnyEvent.create());
        add(Death.create());
        add(Knock.create());
        add(HandCuffs.create());
        add(Search.create());
        add(Fishing.create());
        add(ItemManager.create());
        add(UserSelector.create());
        add(WalletInventory.create());
        add(MainPlayerInventory.create());
        add(InterfaceManager.create());
        add(DataReader.create());
        add(PhoneInventory.create());
        add(Displays.create());
        add(TownInventory.create());
        add(Discord.create());
        add(Prison.create());
        add(SignUI.create());
        add(Login.create());
        add(BookPaper.create());
        add(MenuCreator.create());
        add(FixCursorSlot.create());
        add(ScoreboardUI.create());
        add(RadioBlockData.create());
        add(Model.create());
        add(Path.create());
        add(Skins.create());
        add(MobManager.create());
        add(Weapon.create());
        add(Infection.create());

        add(ObjectBlockData.create());
        add(CauldronBlockData.create());
        add(AnvilBlockData.create());
        add(BookshelfBlockData.create());
        add(CustomUI.create());
        add(ChurchManager.create());
        add(Thirst.create());
        add(Saturation.create());
        add(HorseRiders.create());
        add(TabManager.create());
        add(Settings.create());
        add(NickName.create());
        add(ChatMessages.create());
        add(RoleAndWork.create());
        add(Compass.create());
        add(NPC.create());
        add(CoreProtectHandle.create());
        add(PayDay.create());
        add(DayManager.create());
        add(Administrator.create());
        add(HideCommands.create());
        add(PetManager.create());
        add(RadioVoice.create());
        add(Advancements.create());

        add(MapMonitor.create());
        add(CartographyBlockData.create());

        add(PacketLogger.create());
        add(Train.create());
        add(Nether.create());
        add(DrawText.create());
        add(RustBackPack.create());
        add(PlayerLogger.create());

        add(Blocks.create());
        add(BedrockPatternDisable.create());
    }

}


















