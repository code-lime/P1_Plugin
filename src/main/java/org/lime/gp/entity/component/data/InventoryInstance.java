package org.lime.gp.entity.component.data;

import com.google.common.collect.ImmutableMap;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.EntityLimeMarker;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.ContainerChest;
import net.minecraft.world.inventory.Containers;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.EntityComponentInstance;
import org.lime.gp.entity.component.display.display.EntityModelDisplay;
import org.lime.gp.entity.component.display.instance.DisplayInstance;
import org.lime.gp.entity.component.display.instance.DisplayMap;
import org.lime.gp.entity.component.list.InventoryComponent;
import org.lime.gp.entity.event.EntityMarkerEventInteract;
import org.lime.gp.entity.event.EntityMarkerEventTick;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.Checker;
import org.lime.gp.module.TimeoutData;
import org.lime.gp.module.loot.PopulateLootEvent;
import org.lime.gp.player.inventory.gui.InterfaceManager;
import org.lime.json.JsonElementOptional;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;
import org.lime.system.utils.ItemUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InventoryInstance extends EntityComponentInstance<InventoryComponent> implements
        CustomEntityMetadata.Tickable,
        CustomEntityMetadata.Lootable,
        CustomEntityMetadata.Interactable,
        CustomEntityMetadata.FirstTickable
{
    private final ImmutableMap<String, InventorySubcontainer> containers;
    private boolean changed = true;
    public InventoryInstance(InventoryComponent component, CustomEntityMetadata metadata) {
        super(component, metadata);
        containers = component.containers
                .keySet()
                .stream()
                .collect(ImmutableMap.toImmutableMap(k -> k, k -> new InventorySubcontainer(6 * 9) {
                    @Override public void setChanged() {
                        super.setChanged();
                        changed = true;
                    }
                }));
    }

    @Override public void read(JsonObjectOptional json) {
        containers.values().forEach(InventorySubcontainer::clearContent);
        json.getAsJsonObject("containers")
                .stream()
                .flatMap(v -> v.entrySet().stream())
                .forEach(kv -> {
                    InventorySubcontainer container = containers.get(kv.getKey());
                    if (container == null) return;
                    List<net.minecraft.world.item.ItemStack> items = kv.getValue()
                            .getAsJsonObject().flatMap(v -> v.getAsJsonArray("items"))
                            .stream()
                            .flatMap(Collection::stream)
                            .map(JsonElementOptional::getAsString)
                            .map(v -> v.orElse(null))
                            .map(ItemUtils::loadItem)
                            .map(CraftItemStack::asNMSCopy)
                            .toList();
                    int length = Math.min(items.size(), container.items.size());
                    for (int i = 0; i < length; i++) container.setItem(i, items.get(i));
                });
    }
    @Override public json.builder.object write() {
        return json.object()
                .addObject("containers", __v -> __v
                        .add(containers, k -> k, container -> json.object()
                                .addArray("items", v -> v
                                        .add(container.items, _v -> ItemUtils.saveItem(_v == null || _v.is(net.minecraft.world.item.Items.AIR)
                                                ? null
                                                : _v.asBukkitMirror()
                                        ))))
                );
    }

    @Override public void onTick(CustomEntityMetadata metadata, EntityMarkerEventTick event) {
        if (changed) {
            changed = false;
            saveData();
            syncDisplayVariable();
        }
    }
    @Override public void onLoot(CustomEntityMetadata metadata, PopulateLootEvent event) {
        containers.values().forEach(container -> event.addItems(container.getContents().stream().map(CraftItemStack::asBukkitCopy).toList()));
    }
    @Override public EnumInteractionResult onInteract(CustomEntityMetadata metadata, EntityMarkerEventInteract event) {
        return event.getPlayer() instanceof CraftPlayer player
                ? event.getClickDisplay()
                .singleOfKey("inventory")
                .map(display -> {
                    for (Map.Entry<String, InventorySubcontainer> kv : containers.entrySet()) {
                        String key = "inventory:" + kv.getKey();
                        if (display.hasThisKey(key)) {
                            InventorySubcontainer inventory = kv.getValue();

                            InventoryComponent component = component();
                            InventoryComponent.Data data = component.containers.get(kv.getKey());
                            EntityLimeMarker marker = metadata.marker;

                            UUID displayUnique = event.getParentDisplay().key.element();

                            InterfaceManager.openInventory(player, inventory, (syncId, playerInventory, itemsInventory) -> {
                                ContainerChest container = new ContainerChest(switch (data.rows()) {
                                    case 1 -> Containers.GENERIC_9x1;
                                    case 2 -> Containers.GENERIC_9x2;
                                    case 3 -> Containers.GENERIC_9x3;
                                    case 4 -> Containers.GENERIC_9x4;
                                    case 5 -> Containers.GENERIC_9x5;
                                    default -> Containers.GENERIC_9x6;
                                }, syncId, playerInventory, itemsInventory, data.rows()) {
                                    @Override protected @NotNull Slot addSlot(Slot slot) {
                                        if (slot.container == itemsInventory) {
                                            Checker checker = data.slots().get(this.slots.size());
                                            return checker == null
                                                    ? super.addSlot(InterfaceManager.AbstractSlot.noneSlot(slot))
                                                    : super.addSlot(InterfaceManager.filterSlot(slot, checker::check));
                                        }
                                        return super.addSlot(slot);
                                    }

                                    @Override public boolean stillValid(EntityHuman player) {
                                        return stillValid(marker, player, displayUnique);
                                    }
                                    public boolean stillValid(EntityLimeMarker marker, EntityHuman player, UUID displayUnique) {
                                        Vector playerPositionA = CraftVector.toBukkit(player.position());
                                        Vector playerPositionB = playerPositionA.clone().setY(playerPositionA.getY() - 4);
                                        return marker.isAlive()
                                                && EntityModelDisplay.of(TimeoutData.get(displayUnique, DisplayMap.class)
                                                        .stream()
                                                        .flatMap(v -> v.map.keySet().stream()))
                                                .flatMap(v -> v.model.ofKey("inventory").stream())
                                                .filter(v -> v.hasThisKey(key))
                                                .map(v -> v.lastLocation().toVector())
                                                .anyMatch(v -> v.distanceSquared(playerPositionA) <= 48 || v.distanceSquared(playerPositionB) <= 48);
                                    }
                                };
                                container.setTitle(ChatHelper.toNMS(data.title()));
                                return container;
                            });
                            return EnumInteractionResult.CONSUME;
                        }
                    }
                    return EnumInteractionResult.PASS;
                })
                .orElse(EnumInteractionResult.PASS)
                : EnumInteractionResult.PASS;
    }

    public final void syncDisplayVariable() {
        metadata().list(DisplayInstance.class).findAny().ifPresent(display -> {
            InventoryComponent component = component();
            containers.forEach((key, items) -> {
                InventoryComponent.Data data = component.containers.get(key);
                if (data == null || !data.variable()) return;
                List<ItemStack> itemList = items.items;
                for (int i = 0; i < itemList.size(); i++) {
                    display.set("inventory." + key + "." + i, Items.getGlobalKeyByItem(itemList.get(i)).orElse(Items.AIR));
                }
            });
        });
    }

    @Override public void onFirstTick(CustomEntityMetadata metadata, EntityMarkerEventTick event) {
        syncDisplayVariable();
    }
}


















