package org.lime.gp.coreprotect;

import com.google.gson.JsonArray;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import net.coreprotect.config.Config;
import net.coreprotect.config.ConfigHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;
import org.lime.Position;
import org.lime.core;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.list.*;
import org.lime.gp.lime;
import org.lime.gp.player.perm.Grants;
import org.lime.gp.player.perm.Perms;
import org.lime.system;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CoreProtectHandle implements Listener {
    public static core.element create() {
        return core.element.create(CoreProtectHandle.class)
                .withInit(CoreProtectHandle::init)
                .withUninit(CoreProtectHandle::uninit)
                .<JsonArray>addConfig("inspecting", v -> v.withDefault(system.json.array().add("coreprotect.lookup").build()).withInvoke(CoreProtectHandle::config))
                .withInstance();
    }

    public static Component toLine(CoreProtectAPI.ParseResult result) {
        String time = system.formatMiniCalendar(system.getMoscowTime(result.getTimestamp()), true);
        return Component.join(JoinConfiguration.separator(Component.empty()),
                Component.text("[").color(NamedTextColor.GRAY),
                Component.text(time).style(b -> b
                        .hoverEvent(HoverEvent.showText(Component.text("Копировать время").color(NamedTextColor.GRAY)))
                        .clickEvent(ClickEvent.copyToClipboard(time))
                ).color(NamedTextColor.GRAY),
                Component.text("] ").color(NamedTextColor.GRAY),
                Component.text(result.getPlayer()).style(b -> b
                        .hoverEvent(HoverEvent.showText(Component.text("Копировать имя").color(NamedTextColor.GRAY)))
                        .clickEvent(ClickEvent.copyToClipboard(result.getPlayer()))
                ).color(NamedTextColor.GOLD),
                Component.text(" ").color(NamedTextColor.WHITE),
                Component.text(switch (result.getActionId()) {
                    case 0 -> "сломал";
                    case 1 -> "поставил";
                    case 2 -> "взаимодейстовал";
                    case 3 -> "убил";
                    default -> "*ошибка*";
                }).color(NamedTextColor.WHITE),
                Component.text(" ").color(NamedTextColor.WHITE),
                Component.translatable(result.getType()).hoverEvent(HoverEvent.showItem(result.getType().getKey(), 1)).color(NamedTextColor.GOLD)
        );
    }
    public static Component toLine(String[] data) {
        return toLine(CoreProtect.getInstance().getAPI().parseResult(data));
    }

    private static final List<String> permissionList = new ArrayList<>();
    public static void config(JsonArray json) {
        List<String> permissionList = new ArrayList<>();
        json.forEach(item -> permissionList.add(item.getAsString()));
        permissions.entrySet().removeIf(kv -> {
            kv.getValue().remove();
            return true;
        });
        CoreProtectHandle.permissionList.clear();
        CoreProtectHandle.permissionList.addAll(permissionList);
    }

    public static void init() {
        lime.repeat(CoreProtectHandle::update, 1);
    }
    public static void uninit() {
        coi_toggle.entrySet().removeIf((kv) -> {
            inspecting(kv.getKey(), false);
            return true;
        });
    }

    private static final ConcurrentHashMap<Player, PermissionAttachment> permissions = new ConcurrentHashMap<>();
    public static void inspecting(Player player, boolean enable) {
        PermissionAttachment attachment = permissions.compute(player, (k,v) -> v == null ? player.addAttachment(lime._plugin) : v);
        permissionList.forEach(perm -> attachment.setPermission(perm, enable));
        ConfigHandler.inspecting.put(player.getName(), enable);
        //lime.LogOP("Inspecting of "+player.getName()+": " + (enable ? "ENABLE" : "DISABLE"));
    }

    private static final HashMap<Player, UUID> coi_toggle = new HashMap<>();
    public static void update() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            Items.getItemCreator(player.getInventory().getItemInMainHand())
                    .map(v -> v instanceof ItemCreator _v ? _v : null)
                    .filter(creator -> creator.has(MagnifierSetting.class))
                    .filter(creator -> Perms.getCanData(player).isCanUse(creator.getKey()))
                    .ifPresentOrElse(creator -> Grants.getGrantData(player.getUniqueId()).filter(v -> v.magnifier).ifPresent(role -> {
                        if (coi_toggle.containsKey(player)) return;
                        inspecting(player, true);
                        coi_toggle.put(player, player.getUniqueId());
                    }), () -> {
                        if (coi_toggle.containsKey(player)) coi_toggle.put(player, null);
                    });
        });
        coi_toggle.entrySet().removeIf((kv) -> {
            if (kv.getValue() != null && kv.getKey().isOnline()) return false;
            inspecting(kv.getKey(), false);
            return true;
        });
    }

    private final static class _0 extends net.coreprotect.consumer.Queue {
        private static int getTime(boolean isDrop) {
            int time = (int)(System.currentTimeMillis() / 1000L);
            /*lime.LogOP("TIME: " + time);
            if (last_time > time) time = last_time;
            if (last_isDrop != isDrop) time++;
            lime.LogOP("TIME TO: " + time);
            last_time = time;
            last_isDrop = isDrop;*/
            return time;
        }

        private static void onEntityPickupItem(Location location, Player player, ItemStack itemStack) {
            String loggingItemId = player.getName().toLowerCase(Locale.ROOT) + "." + location.getBlockX() + "." + location.getBlockY() + "." + location.getBlockZ();
            int itemId = getItemId(loggingItemId);
            List<ItemStack> list = ConfigHandler.itemsPickup.getOrDefault(loggingItemId, new ArrayList<>());
            list.add(itemStack.clone());
            ConfigHandler.itemsPickup.put(loggingItemId, list);
            int time = getTime(false);//(int) (System.currentTimeMillis() / 1000L) + 1;
            queueItemTransaction(player.getName(), location.clone(), time, 0, itemId);
        }
        private static void onPlayerDropItem(Location location, Player player, ItemStack itemStack) {
            String loggingItemId = player.getName().toLowerCase(Locale.ROOT) + "." + location.getBlockX() + "." + location.getBlockY() + "." + location.getBlockZ();
            int itemId = getItemId(loggingItemId);
            List<ItemStack> list = ConfigHandler.itemsDrop.getOrDefault(loggingItemId, new ArrayList<>());
            list.add(itemStack.clone());
            ConfigHandler.itemsDrop.put(loggingItemId, list);
            int time = getTime(true);//(int)(System.currentTimeMillis() / 1000L) + 1;
            queueItemTransaction(player.getName(), location.clone(), time, 0, itemId);
        }
        private static void onBlockPlace(TileEntityLimeSkull skull, Player player) {
            World world = skull.getLevel().getWorld();
            if (!Config.getConfig(world).BLOCK_PLACE) return;
            BlockPosition pos = skull.getBlockPos();
            BlockState blockState = world.getBlockState(pos.getX(), pos.getY(), pos.getZ());
            net.coreprotect.consumer.Queue.queueBlockPlace(player.getName(), blockState, blockState.getType(), null, null, -1, 0, null);
        }
    }
    public static void logDrop(Location location, Player player, ItemStack item) {
        _0.onPlayerDropItem(location, player, item);
    }
    public static void logPickUp(Location location, Player player, ItemStack itemStack) {
        _0.onEntityPickupItem(location, player, itemStack);
    }
    public static void logSetBlock(TileEntityLimeSkull skull, Player player) {
        _0.onBlockPlace(skull, player);
    }
    public static void syncTransaction(Player player, Position position) {
        CoreProtect.getInstance().getAPI().logContainerTransaction(player.getName(), position.getLocation());
    }
}






















