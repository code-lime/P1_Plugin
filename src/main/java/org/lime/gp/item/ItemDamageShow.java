package org.lime.gp.item;

import com.comphenix.protocol.events.PacketContainer;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemSword;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.core;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.module.damage.EntityDamageByPlayerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemDamageShow {
    public static core.element create() {
        return core.element.create(ItemDamageShow.class)
                .withInit(ItemDamageShow::init);
    }

    private static void unMapLore(ItemMeta meta) {

    }
    private static void unMapEdit(ItemStack item) {
        Items.getOptional(Settings.DurabilitySetting.class, item)
                .map(v -> v.maxDurability)
                .ifPresent(maxDurability -> {
                    int maxDamageDefault = item.getItem().getMaxDamage();
                    int damage = item.getDamageValue();
                    double damagePercent = damage / (double)maxDamageDefault;
                    item.setDamageValue((int)Math.ceil(maxDurability * damagePercent));
                });
    }
    private static Optional<ItemStack> unMapCopy(ItemStack item) {
        return Items.getOptional(Settings.DurabilitySetting.class, item)
                .map(v -> v.maxDurability)
                .map(maxDurability -> {
                    int maxDamageDefault = item.getItem().getMaxDamage();
                    int damage = item.getDamageValue();
                    double damagePercent = damage / (double)maxDamageDefault;
                    ItemStack _item = item.copy();
                    _item.setDamageValue((int)Math.ceil(maxDurability * damagePercent));
                    return _item;
                });
    }

    private static void mapLore(ItemMeta meta) {

    }
    private static void mapEdit(ItemStack item) {
        Items.getOptional(Settings.DurabilitySetting.class, item)
                .map(v -> v.maxDurability)
                .ifPresent(maxDurability -> {
                    int maxDamageDefault = item.getItem().getMaxDamage();
                    int damage = item.getDamageValue();
                    double damagePercent = damage / (double)maxDurability;
                    item.setDamageValue((int)Math.ceil(maxDamageDefault * damagePercent));

                    /*PacketPlayInUseEntity

                    ItemStack
                    ItemSword*/

                    /*CraftItemStack of_lore = CraftItemStack.asCraftMirror(item);
                    ItemMeta meta = of_lore.getItemMeta();
                    of_lore.setItemMeta(meta);*/
                });
    }
    private static Optional<ItemStack> mapCopy(ItemStack item) {
        return Items.getOptional(Settings.DurabilitySetting.class, item)
                .map(v -> v.maxDurability)
                .map(maxDurability -> {
                    int maxDamageDefault = item.getItem().getMaxDamage();
                    int damage = item.getDamageValue();
                    double damagePercent = damage / (double)maxDurability;
                    ItemStack _item = item.copy();
                    _item.setDamageValue((int)Math.ceil(maxDamageDefault * damagePercent));
                    return _item;
                });
    }

    public static void init() {
        PacketManager.adapter()
                .add(PacketPlayInSetCreativeSlot.class, (packet, event) -> {
                    unMapEdit(packet.getItem());
                })
                .add(PacketPlayInWindowClick.class, (packet, event) -> {
                    unMapEdit(packet.getCarriedItem());
                    packet.getChangedSlots().values().forEach(ItemDamageShow::unMapEdit);
                })
                .add(PacketPlayOutEntityEquipment.class, (packet, event) -> {
                    List<Pair<EnumItemSlot, ItemStack>> items = new ArrayList<>(packet.getSlots());
                    boolean edited = false;
                    int count = items.size();
                    for (int i = 0; i < count; i++) {
                        Pair<EnumItemSlot, ItemStack> pair = items.get(i);
                        Optional<ItemStack> item = mapCopy(pair.getSecond());
                        if (item.isEmpty()) continue;
                        edited = true;
                        items.set(i, Pair.of(pair.getFirst(), item.get()));
                    }
                    if (edited) {
                        if (items.isEmpty()) event.setCancelled(true);
                        else event.setPacket(new PacketContainer(event.getPacketType(), new PacketPlayOutEntityEquipment(packet.getEntity(), items)));
                    }
                })
                .add(PacketPlayOutSetSlot.class, (packet, event) -> {
                    mapEdit(packet.getItem());
                })
                .add(PacketPlayOutWindowItems.class, (packet, event) -> {
                    mapEdit(packet.getCarriedItem());
                    packet.getItems().forEach(ItemDamageShow::mapEdit);
                })
                .listen();
    }
}


























