package org.lime.gp.block.component.data.voice;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.lime.Position;
import org.lime.core;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.block.Blocks;
import org.lime.gp.chat.Apply;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.lime;
import org.lime.gp.module.TimeoutData;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.player.module.TabManager;
import org.lime.gp.player.voice.DistanceData;
import org.lime.gp.player.voice.MegaPhoneData;
import org.lime.gp.player.voice.Radio;
import org.lime.gp.player.voice.RadioData;

public class RadioLoader implements Listener {
    public static core.element create() {
        return core.element.create(RadioLoader.class)
                .withInstance()
                .withInit(RadioLoader::init);
    }
    public static void init() {
        Radio.addListener(() -> TimeoutData.values(RadioInstance.RadioVoiceData.class));

        AnyEvent.addEvent("radio.set", AnyEvent.type.other, builder -> builder.createParam("level", "state", "volume").createParam("[value]"), (p, state, value) -> {
            switch (state) {
                case "level" -> ExtMethods.parseUnsignedInt(value).ifPresent(level -> RadioData.modifyData(p.getInventory().getItemInMainHand(), data -> data.level = level));
                case "volume" -> ExtMethods.parseUnsignedInt(value).ifPresent(volume -> RadioData.modifyData(p.getInventory().getItemInMainHand(), data -> data.volume = volume));
                case "state" -> RadioData.modifyData(p.getInventory().getItemInMainHand(), v -> v.enable = "true".equals(value));
            }
        });
        AnyEvent.addEvent("radio.get", AnyEvent.type.other, RadioLoader::openRadioMenu);

        AnyEvent.addEvent("radio.block.set", AnyEvent.type.other, builder -> builder.createParam(Integer::parseInt, "x").createParam(Integer::parseInt, "y").createParam(Integer::parseInt, "z").createParam("level", "volume", "distance", "state").createParam("[value]"), (p, x, y, z, state, value) -> {
            Position position = Position.of(p.getWorld(), x, y, z);
            switch (state) {
                case "level" ->  ExtMethods.parseUnsignedInt(value).ifPresent(level -> RadioData.modifyData(position.getBlock(), data -> data.level = level));
                case "volume" ->  ExtMethods.parseUnsignedInt(value).ifPresent(volume -> RadioData.modifyData(position.getBlock(), data -> data.volume = volume));
                case "distance" ->  ExtMethods.parseUnsignedInt(value).ifPresent(distance -> DistanceData.modifyData(position.getBlock(), data -> data.distance = (short)(int)distance));
                case "state" -> RadioData.modifyData(position.getBlock(), data -> data.enable = "true".equals(value));
            }
        });
        AnyEvent.addEvent("radio.block.get", AnyEvent.type.other, builder -> builder.createParam(Integer::parseInt, "x").createParam(Integer::parseInt, "y").createParam(Integer::parseInt, "z"), (p, x, y, z) -> RadioLoader.openRadioBlockMenu(p,Position.of(p.getWorld(),x,y,z)));

        AnyEvent.addEvent("megaphone.set", AnyEvent.type.other, builder -> builder.createParam("distance", "volume").createParam("[value]"), (p, state, value) -> {
            switch (state) {
                case "distance" -> ExtMethods.parseUnsignedInt(value).ifPresent(distance -> MegaPhoneData.modifyData(p.getInventory().getItemInMainHand(), data -> data.distance = (short)(int)distance));
                case "volume" -> ExtMethods.parseUnsignedInt(value).ifPresent(volume -> MegaPhoneData.modifyData(p.getInventory().getItemInMainHand(), data -> data.volume = volume));
            }
        });
        AnyEvent.addEvent("megaphone.get", AnyEvent.type.other, RadioLoader::openMegaPhoneMenu);

        lime.once(() -> TabManager.hideNickName.addEntry("."), 1);
    }

    private static boolean openRadioMenu(Player player) {
        return RadioData.getData(player.getInventory().getItemInMainHand()).map(data -> {
            MenuCreator.show(player, "radio.menu", Apply.of().add(data.map()));
            return true;
        }).isPresent();
    }
    private static boolean openRadioBlockMenu(Player player, Position position) {
        return Blocks.of(position.getBlock())
                .flatMap(Blocks::customOf)
                .flatMap(v -> v.list(RadioInstance.class).findAny())
                .map(v -> {
                    v.showMenu(player);
                    return true;
                })
                .isPresent();
    }
    private static boolean openMegaPhoneMenu(Player player) {
        return MegaPhoneData.getData(player.getInventory().getItemInMainHand()).map(data -> {
            MenuCreator.show(player, "megaphone.menu", Apply.of().add(data.map()));
            return true;
        }).isPresent();
    }

    @EventHandler(priority = EventPriority.HIGHEST) public static void on(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!e.getAction().isRightClick()) return;
        if (e.useItemInHand() == Event.Result.DENY) return;
        Player player = e.getPlayer();
        if (!player.isSneaking()) return;
        if (openRadioMenu(player) || openMegaPhoneMenu(player)) {
            e.setCancelled(true);
            return;
        }
    }
}
