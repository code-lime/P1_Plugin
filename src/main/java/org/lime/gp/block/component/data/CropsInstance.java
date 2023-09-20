package org.lime.gp.block.component.data;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.lime.display.models.shadow.IBuilder;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.display.BlockDisplay;
import org.lime.gp.block.component.display.block.IModelBlock;
import org.lime.gp.block.component.list.CropsComponent;
import org.lime.gp.item.ItemStackRaw;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.list.CropsSetting;
import org.lime.gp.item.settings.list.TableDisplaySetting;
import org.lime.gp.lime;
import org.lime.gp.module.loot.IPopulateLoot;
import org.lime.gp.module.loot.ModifyLootTable;
import org.lime.gp.module.loot.Parameters;
import org.lime.gp.module.loot.PopulateLootEvent;
import org.lime.gp.player.level.LevelModule;
import org.lime.json.JsonElementOptional;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.ItemUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CropsInstance extends BaseAgeableInstance<CropsComponent> implements BlockDisplay.Displayable, CustomTileMetadata.Lootable, CustomTileMetadata.Interactable {
    @Override protected String debugKey() { return "crops"; }

    public CropsInstance(CropsComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
        setItem(ItemStackRaw.EMPTY, false);
    }


    private static final AgeableData EMPTY = new AgeableData() {
        @Override public double tickAgeModify() { return 0; }
        @Override public int limitAge() { return 0; }
    };

    @Override public AgeableData ageableData() {
        return item.creator()
                .map(v -> v instanceof ItemCreator c ? c : null)
                .<AgeableData>flatMap(v -> v.getOptional(CropsSetting.class))
                .orElse(EMPTY);
    }

    //private final Builder builder;

    public final LockToast1<IBuilder> model = Toast.lock(null);
    private ItemStackRaw item;

    public void setItem(ItemStackRaw item, boolean save) {
        if (item.isEquals(this.item)) return;
        this.item = item;
        writeDebug("Set item: " + item.save());
        syncItem();
        if (save) {
            writeDebug("Save data item");
            saveData();
        }
        syncDisplayVariable();
    }
    public void syncItem() {
        model.set0(this.item.nms()
                .map(item -> TableDisplaySetting.builderItem(item, component().offset, TableDisplaySetting.TableType.crops, String.valueOf(age())))
                .orElse(lime.models.builder().none()));
    }

    @Override public void read(JsonObjectOptional json) {
        setItem(json.get("item").map(JsonElementOptional::base).map(ItemStackRaw::load).orElse(ItemStackRaw.EMPTY), false);
        super.read(json);
    }
    @Override public json.builder.object write() {
        var obj = super.write().add("item", item.save());
        writeDebug("Write: " + obj.build());
        return obj;
    }

    @Override public Optional<IModelBlock> onDisplayAsync(Player player, World world, BlockPosition position, IBlockData data) {
        return Optional.of(IModelBlock.of(null, model.get0(), BlockDisplay.getChunkSize(10), Double.POSITIVE_INFINITY));
    }
    @Override public void onLoot(CustomTileMetadata metadata, PopulateLootEvent event) {
        writeDebug("OL.0");
        this.item.item().ifPresent(item -> {
            writeDebug("OL.1: " + this.item.save());
            event.addItem(item);
        });
    }
    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        if (this.item.isEmpty()) return;
        super.onTick(metadata, event);
    }

    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        writeDebug("OI.0");
        EnumHand hand = event.hand();
        EntityHuman player = event.player();
        UUID uuid = player.getUUID();
        World world = event.world();

        net.minecraft.world.item.ItemStack itemStack = player.getItemInHand(hand);

        net.minecraft.world.item.ItemStack outputItem = this.item.nms().orElse(null);
        if (outputItem != null) {
            writeDebug("OI.ITEM.1: " + this.item.save());
            setItem(ItemStackRaw.EMPTY, true);
            Items.getOptional(CropsSetting.class, outputItem).filter(v -> age() == v.limitAge()).ifPresentOrElse(data -> {
                writeDebug("OI.ITEM.1");
                net.minecraft.world.item.ItemStack handItem = itemStack;
                IPopulateLoot loot = IPopulateLoot.of(world, List.of(
                        IPopulateLoot.var(Parameters.ThisEntity, player),
                        IPopulateLoot.var(Parameters.BlockEntity, metadata.skull),
                        IPopulateLoot.var(Parameters.BlockState, event.state()),
                        IPopulateLoot.var(Parameters.Origin, event.pos().getCenter()),
                        IPopulateLoot.var(Parameters.Tool, itemStack)
                ));
                String key = "crops/" + Items.getGlobalKeyByItem(outputItem).orElse("none").toLowerCase();
                writeDebug("OI.ITEM.2: " + key);
                LevelModule.onHarvest(uuid, key);
                for (ItemStack item : ModifyLootTable.getLoot(uuid, key, data.loot, loot).generateLoot(loot)) {
                    writeDebug("OI.ITEM.3: " + ItemUtils.saveItem(item));
                    net.minecraft.world.item.ItemStack _item = CraftItemStack.asNMSCopy(item);
                    if (handItem.isEmpty()) {
                        writeDebug("OI.ITEM.4");
                        player.setItemInHand(hand, _item);
                        handItem = _item;
                    } else if (!player.addItem(_item)) {
                        writeDebug("OI.ITEM.5");
                        player.drop(_item, false);
                    }
                    writeDebug("OI.ITEM.6");
                }
                writeDebug("OI.ITEM.7");
            }, () -> {
                writeDebug("OI.ITEM.8");
                if (itemStack.isEmpty()) {
                    writeDebug("OI.ITEM.9");
                    player.setItemInHand(hand, outputItem);
                } else if (!player.addItem(outputItem)) {
                    writeDebug("OI.ITEM.10");
                    player.drop(outputItem, false);
                }
                writeDebug("OI.ITEM.11");
            });
            age(0);
        } else {
            writeDebug("OI.AIR.2");
            if (!Items.has(CropsSetting.class, itemStack)) return EnumInteractionResult.PASS;
            writeDebug("OI.AIR.3");
            if (!component().filter.check(itemStack)) return EnumInteractionResult.PASS;
            writeDebug("OI.AIR.4");
            setItem(new ItemStackRaw(itemStack.copyWithCount(1)), true);
            age(0);
            if (!player.getAbilities().instabuild) {
                writeDebug("OI.AIR.5");
                itemStack.shrink(1);
            }
        }
        writeDebug("OI.2");
        return EnumInteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override protected void onAgeUpdated() {
        super.onAgeUpdated();
        syncItem();
    }

    @Override
    protected boolean modifyDisplayVariable(Map<String, String> map) {
        map.put("has", item.isEmpty() ? "false" : "true");
        return super.modifyDisplayVariable(map);
    }
}





























