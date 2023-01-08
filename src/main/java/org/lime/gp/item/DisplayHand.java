package org.lime.gp.item;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.game.PacketPlayOutEntityEquipment;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.lime.core;
import org.lime.gp.lime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DisplayHand {
    public static core.element create() {
        return core.element.create(DisplayHand.class)
                .withInit(DisplayHand::init);
    }
    public static void init() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(lime._plugin, PacketType.Play.Server.ENTITY_EQUIPMENT) {
            @Override public void onPacketSending(PacketEvent event) {
                PacketPlayOutEntityEquipment equipment = (PacketPlayOutEntityEquipment)event.getPacket().getHandle();
                List<Pair<EnumItemSlot, ItemStack>> slots = new ArrayList<>();
                boolean isEdited = false;
                for (Pair<EnumItemSlot, ItemStack> pair : equipment.getSlots()) {
                    Optional<ItemStack> _item = Optional.ofNullable(pair.getSecond())
                            .map(ItemStack::getTag)
                            .filter(v -> v.contains("CustomModelData"))
                            .map(v -> v.getInt("CustomModelData"))
                            .flatMap(id -> Optional.ofNullable(Items.creators.get(id))
                                    .map(v -> v instanceof Items.ItemCreator creator ? creator : null)
                                    .flatMap(creator -> Items.getOptional(Settings.DisplaySetting.class, creator))
                                    .flatMap(v -> v.item(id))
                            )
                            .map(CraftItemStack::asNMSCopy);
                    if (_item.isPresent()) isEdited = true;
                    slots.add(Pair.of(pair.getFirst(), _item.orElseGet(pair::getSecond)));
                }
                if (isEdited) event.setPacket(new PacketContainer(event.getPacketType(), new PacketPlayOutEntityEquipment(equipment.getEntity(), slots)));
            }
        });
    }
}
