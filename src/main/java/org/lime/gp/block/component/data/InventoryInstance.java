package org.lime.gp.block.component.data;

import com.mojang.math.Transformation;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.TileInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerChest;
import net.minecraft.world.inventory.Containers;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.lime.display.models.shadow.Builder;
import org.lime.display.models.shadow.IBuilder;
import org.lime.display.models.shadow.NoneBuilder;
import org.lime.gp.block.BlockComponentInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.display.BlockDisplay;
import org.lime.gp.block.component.display.block.IModelBlock;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.list.InventoryComponent;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.item.data.Checker;
import org.lime.gp.item.settings.list.TableDisplaySetting;
import org.lime.gp.lime;
import org.lime.gp.module.loot.PopulateLootEvent;
import org.lime.gp.player.inventory.InterfaceManager;
import org.lime.json.JsonElementOptional;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.ItemUtils;

import java.util.*;

public class InventoryInstance extends BlockComponentInstance<InventoryComponent> implements CustomTileMetadata.Tickable, CustomTileMetadata.Lootable, CustomTileMetadata.Interactable, BlockDisplay.Displayable {
    private final InventorySubcontainer items_container;
    private boolean changed = true;
    private final LockToast1<IBuilder> display = Toast.lock(null);
    public InventoryInstance(InventoryComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
        items_container = new InventorySubcontainer(6 * 9) {
            @Override public void setChanged() {
                super.setChanged();
                changed = true;
            }
        };
    }

    /*private static final Builder builder_interact = lime.models.builder(EntityTypes.ARMOR_STAND)
            .nbt(() -> {
                EntityArmorStand stand = new EntityArmorStand(EntityTypes.ARMOR_STAND, lime.MainWorld.getHandle());
                stand.setNoBasePlate(true);
                stand.setShowArms(true);
                stand.setSmall(true);
                return stand;
            })
            .invisible(true);*/

    @Override public void read(JsonObjectOptional json) {
        items_container.clearContent();
        List<net.minecraft.world.item.ItemStack> items = json.getAsJsonArray("items")
                .stream()
                .flatMap(Collection::stream)
                .map(JsonElementOptional::getAsString)
                .map(v -> v.orElse(null))
                .map(ItemUtils::loadItem)
                .map(CraftItemStack::asNMSCopy)
                .toList();
        int length = Math.min(items.size(), items_container.items.size());
        for (int i = 0; i < length; i++) items_container.setItem(i, items.get(i));
    }
    @Override public json.builder.object write() {
        return json.object()
                .addArray("items", v -> v.add(items_container.items, _v -> ItemUtils.saveItem(_v == null || _v.is(net.minecraft.world.item.Items.AIR) ? null : _v.asBukkitMirror())));
    }

    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        if (changed) {
            changed = false;
            InventoryComponent component = component();
            NoneBuilder builder = lime.models.builder().none();
            for (Map.Entry<Integer, Transformation> kv : component.display.entrySet()) {
                net.minecraft.world.item.ItemStack item = items_container.getItem(kv.getKey());
                if (item.isEmpty()) continue;
                builder = builder.addChild(TableDisplaySetting.builderItem(CraftItemStack.asBukkitCopy(item), kv.getValue(), TableDisplaySetting.TableType.inventory, component.type));
/*
                builder = builder.addChild(builder_interact
                        .local(kv.getValue())
                        .nbt(v -> v.putBoolean("Invulnerable", true))
                        .nbt(v -> v.putBoolean("Marker", true))
                        .addEquipment(EnumItemSlot.HEAD, Items.getOptional(TableDisplaySetting.class, item)
                                .flatMap(v -> v.of(TableDisplaySetting.TableType.inventory, component.type))
                                .map(v -> v.display(item))
                                .orElseGet(item::copy))
                );
 */
            }
            display.set0(builder);
            metadata()
                .list(DisplayInstance.class)
                .forEach(DisplayInstance::variableDirty);
            saveData();
        }
    }
    @Override public void onLoot(CustomTileMetadata metadata, PopulateLootEvent event) {
        event.addItems(items_container.getContents().stream().map(CraftItemStack::asBukkitCopy).toList());
    }
    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        InventoryComponent component = component();

        UUID block_uuid = metadata.key.uuid();
        UUID unique_key = unique();
        ContainerAccess context = ContainerAccess.create(metadata.skull.getLevel(), metadata.skull.getBlockPos());

        event.player().openMenu(new TileInventory((syncId, inventory, target) -> new ContainerChest(switch (component.rows) {
            case 1 -> Containers.GENERIC_9x1;
            case 2 -> Containers.GENERIC_9x2;
            case 3 -> Containers.GENERIC_9x3;
            case 4 -> Containers.GENERIC_9x4;
            case 5 -> Containers.GENERIC_9x5;
            default -> Containers.GENERIC_9x6;
        }, syncId, inventory, items_container, component.rows) {
            @Override protected Slot addSlot(Slot slot) {
                if (slot.container == items_container) {
                    Checker checker = component.slots.get(this.slots.size());
                    return checker == null
                            ? super.addSlot(InterfaceManager.AbstractSlot.noneSlot(slot))
                            : super.addSlot(InterfaceManager.filterSlot(slot, checker::check));
                }
                return super.addSlot(slot);
            }

            @Override public boolean stillValid(EntityHuman player) {
                return stillValid(context, player, block_uuid);
            }
            public boolean stillValid(ContainerAccess context, EntityHuman player, UUID block_uuid) {
                return context.evaluate((world, blockposition) -> world.getBlockEntity(blockposition) instanceof TileEntityLimeSkull skull
                                && skull.customUUID().filter(block_uuid::equals).isPresent()
                                && InventoryInstance.this.unique().equals(unique_key)
                                && player.distanceToSqr(blockposition.getX() + 0.5, blockposition.getY() + 0.5, blockposition.getZ() + 0.5) <= 64.0,
                        true);
            }
        }, ChatHelper.toNMS(component.title)));
        return EnumInteractionResult.CONSUME;
    }
    @Override public Optional<IModelBlock> onDisplayAsync(Player player, World world, BlockPosition position, IBlockData data) {
        IBuilder model = display.get0();
        return model == null ? Optional.empty() : Optional.of(IModelBlock.of(null, model, BlockDisplay.getChunkSize(10), Double.POSITIVE_INFINITY));
    }
}


















