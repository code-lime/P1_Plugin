package org.lime.gp.block.component.data;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.Vector3f;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.lime.display.Models;
import org.lime.display.transform.LocalLocation;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.display.BlockDisplay;
import org.lime.gp.block.component.display.block.IModelBlock;
import org.lime.gp.block.component.list.CropsComponent;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.CropsSetting;
import org.lime.gp.item.settings.list.TableDisplaySetting;
import org.lime.gp.lime;
import org.lime.gp.module.PopulateLootEvent;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.util.Map;
import java.util.Optional;

public class CropsInstance extends BaseAgeableInstance<CropsComponent> implements BlockDisplay.Displayable, CustomTileMetadata.Lootable, CustomTileMetadata.Interactable {
    public CropsInstance(CropsComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
        builder = lime.models.builder(EntityTypes.ARMOR_STAND)
                .local(new LocalLocation(component.offset))
                .nbt(() -> {
                    EntityArmorStand stand = new EntityArmorStand(EntityTypes.ARMOR_STAND, lime.MainWorld.getHandle());
                    stand.setNoBasePlate(true);
                    stand.setSmall(true);
                    stand.setInvisible(true);
                    stand.setInvulnerable(true);
                    stand.setMarker(true);
                    stand.setHeadPose(new Vector3f(90, 0, 0));
                    return stand;
                });
        setItem(null, false);
    }

    private static final AgeableData EMPTY = new AgeableData() {
        @Override public double tickAgeModify() {
            return 0;
        }
        @Override public int limitAge() {
            return 0;
        }
    };

    @Override
    public AgeableData ageableData() {
        return Items.getOptional(CropsSetting.class, head).<AgeableData>map(v -> v).orElse(EMPTY);
    }

    private final Models.Builder builder;

    public final system.LockToast1<Models.Model> model = system.<Models.Model>toast(null).lock();
    private org.bukkit.inventory.ItemStack head;

    public void setItem(org.bukkit.inventory.ItemStack item, boolean save) {
        ItemStack oldHead = head;
        if (item == null) {
            head = new org.bukkit.inventory.ItemStack(Material.AIR);
        }
        else {
            head = item.clone();
        }
        if (head.equals(oldHead)) return;
        syncItem();
        if (save) saveData();
        syncDisplayVariable();
    }
    public void syncItem() {
        model.set0(builder.addEquipment(EnumItemSlot.HEAD, Items.getOptional(TableDisplaySetting.class, head)
                .flatMap(v -> v.of(TableDisplaySetting.TableType.crops, String.valueOf(age())))
                .map(v -> v.display(head))
                .orElseGet(() -> CraftItemStack.asNMSCopy(head))
        ).build());

    }

    @Override public void read(JsonObjectOptional json) {
        super.read(json);
        setItem(json.getAsString("item").map(system::loadItem).orElse(null), false);
    }
    @Override public system.json.builder.object write() {
        return super.write()
                .add("item", head.getType().isAir() ? null : system.saveItem(head));
    }

    @Override public Optional<IModelBlock> onDisplayAsync(Player player, World world, BlockPosition position, IBlockData data) {
        return Optional.of(IModelBlock.of(null, model.get0(), BlockDisplay.getChunkSize(10)));
    }
    @Override public void onLoot(CustomTileMetadata metadata, PopulateLootEvent event) {
        if (!head.getType().isAir()) {
            event.addItem(head);
        }
    }
    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        if (head.getType().isAir()) return;
        super.onTick(metadata, event);
    }

    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        EnumHand hand = event.hand();
        EntityHuman player = event.player();
        World world = event.world();

        net.minecraft.world.item.ItemStack itemStack = player.getItemInHand(hand);

        if (!head.getType().isAir()) {
            net.minecraft.world.item.ItemStack outputItem = CraftItemStack.asNMSCopy(head);
            setItem(null, true);
            Items.getOptional(CropsSetting.class, outputItem).filter(v -> age() == v.limitAge()).ifPresentOrElse(data -> {
                net.minecraft.world.item.ItemStack handItem = itemStack;
                for (ItemStack item : data.loot.generate()) {
                    net.minecraft.world.item.ItemStack _item = CraftItemStack.asNMSCopy(item);
                    if (handItem.isEmpty()) {
                        player.setItemInHand(hand, _item);
                        handItem = _item;
                    } else if (!player.addItem(_item)) {
                        player.drop(_item, false);
                    }
                }
            }, () -> {
                if (itemStack.isEmpty()) {
                    player.setItemInHand(hand, outputItem);
                } else if (!player.addItem(outputItem)) {
                    player.drop(outputItem, false);
                }
            });
            age(0);
        } else {
            if (!Items.has(CropsSetting.class, itemStack)) return EnumInteractionResult.PASS;
            setItem(CraftItemStack.asBukkitCopy(itemStack.copyWithCount(1)), true);
            age(0);
            if (!player.getAbilities().instabuild) {
                itemStack.shrink(1);
            }
        }
        return EnumInteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override protected void onAgeUpdated() {
        super.onAgeUpdated();
        syncItem();
    }

    @Override
    protected boolean modifyDisplayVariable(Map<String, String> map) {
        map.put("has", head.getType().isAir() ? "false" : "true");
        return super.modifyDisplayVariable(map);
    }
}





























