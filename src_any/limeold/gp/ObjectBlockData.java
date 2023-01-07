package org.lime.gp.block.data;

import org.lime.core;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.lime.gp.chat.ApplyOld;
import org.lime.gp.extension.Cooldown;
import org.lime.gp.chat.LangMessages;
import org.limeold.gp.lime;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.extension.Zone;
import org.limeold.gp.web.DataReader;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ObjectBlockData implements Listener {
    public static abstract class IBlockData {
        public abstract Location getLocation();
        public Block getBlock() { return getLocation().getBlock(); }
        public abstract boolean isDestroy();
        public abstract void onDestroy();
        public abstract void onUpdate(double timeDelta);
    }
    private static final HashMap<String, IBlockData> blocks = new HashMap<>();

    public static core.element create() {
        return core.element.create(ObjectBlockData.class)
                .withInit(ObjectBlockData::Init)
                .withUninit(ObjectBlockData::Uninit)
                .withInstance();
    }

    private static String getStringLocation(Location location) {
        return new Zone.Position(location.toVector()) + " " + location.getWorld().getUID();
    }

    public static <T extends IBlockData>Map<String, T> getDatas(Class<T> tClass) {
        return blocks.entrySet().stream().filter(kv -> tClass.isInstance(kv.getValue())).collect(Collectors.toMap(Map.Entry::getKey, kv -> (T)kv.getValue()));
    }
    public static <T extends IBlockData>void destroyOn(Class<T> tClass) {
        blocks.entrySet().removeIf(v -> {
            if (tClass.isInstance(v.getValue())) {
                v.getValue().onDestroy();
                return true;
            }
            return false;
        });
    }

    private static final double TIME_DELTA = 1;
    public static void Init() {
        lime.repeat(ObjectBlockData::Update, TIME_DELTA);
    }
    public static void Update() {
        blocks.values().removeIf(block -> {
            if (block.isDestroy()) {
                block.onDestroy();
                return true;
            }
            block.onUpdate(TIME_DELTA);
            return false;
        });
        Bukkit.getWorlds().forEach(world -> world.getEntitiesByClass(LivingEntity.class).forEach(entity -> {
            if (entity.getLocation().getBlock().getType() != Material.STONECUTTER) return;
            entity.damage(2, entity);
        }));
    }
    public static void Uninit() {
        blocks.values().forEach(IBlockData::onDestroy);
        blocks.clear();
    }
    @EventHandler public static void OnClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = e.getClickedBlock();
        if (block == null) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        if (Cooldown.hasCooldown(uuid, "atm.click")) return;
        Cooldown.setCooldown(uuid, "atm.click", 2);
        Location location = block.getLocation();
        boolean sneaking = player.isSneaking();
        DataReader.GetATM(location.toVector(), (atm_id, atm_state) -> {
            if (sneaking) {
                MenuCreator.show(player, "atm.admin", ApplyOld.of("atm_id", String.valueOf(atm_id)));
                return;
            }
            if (atm_state == 0) {
                LangMessages.Message.Phone_AtmState_Offline.sendMessage(player);
                return;
            }
            String card = ItemManager.getCard(e.getItem());
            if (card == null) return;
            MenuCreator.show(player, "atm.open", ApplyOld.of("atm_id", String.valueOf(atm_id)).add("card", card));
        });
    }
    public static IBlockData GetBlock(Location location) {
        return blocks.getOrDefault(getStringLocation(location), null);
    }
    public static void ApplyBlock(Location location, IBlockData blockData) {
        String stLoc = getStringLocation(location);
        IBlockData data = blocks.getOrDefault(stLoc, null);
        if (data != null) data.onDestroy();
        data = blockData;
        if (data == null) return;
        blocks.put(stLoc, data);
    }
}
