package org.lime.gp.block.component.data;

import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemLiquidUtil;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import net.minecraft.world.level.gameevent.GameEvent;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.lime.plugin.CoreElement;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.list.WaterRecyclerComponent;
import org.lime.gp.item.settings.list.ThirstSetting;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;
import org.lime.system.utils.MathUtils;

import java.util.List;
import java.util.Optional;

public class WaterRecyclerInstance extends BlockInstance implements CustomTileMetadata.Interactable, CustomTileMetadata.Tickable {
    public static CoreElement create() {
        Blocks.addDefaultBlocks(new BlockInfo("water_recycler").add(v -> new WaterRecyclerComponent(v, 20, 10, 1/20.0)));
        return CoreElement.create(WaterRecyclerInstance.class);
    }

    @Override public WaterRecyclerComponent component() { return (WaterRecyclerComponent)super.component(); }
    public WaterRecyclerInstance(WaterRecyclerComponent component, CustomTileMetadata metadata) { super(component, metadata); }

    public double waterLevel = 0;
    public double clearLevel = 0;

    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        WaterRecyclerComponent component = component();
        if (clearLevel >= component.totalClearLevel) return;
        double maxDeltaLevel = Math.min(waterLevel, component.inTickLevel);
        if (maxDeltaLevel <= 0) return;
        maxDeltaLevel = Math.min(Math.min(component.totalClearLevel - clearLevel, maxDeltaLevel), component.inTickLevel);
        clearLevel = MathUtils.round(Math.min(clearLevel + maxDeltaLevel, component.totalClearLevel), 5);
        waterLevel = MathUtils.round(Math.max(waterLevel - maxDeltaLevel, 0), 5);
        syncDisplayVariable();
        saveData();
    }
    @Override public void read(JsonObjectOptional json) {
        waterLevel = json.getAsDouble("waterLevel").orElse(0.0);
        clearLevel = json.getAsDouble("clearLevel").orElse(0.0);
        syncDisplayVariable();
    }
    @Override public json.builder.object write() {
        return json.object()
                .add("waterLevel", waterLevel)
                .add("clearLevel", clearLevel);
    }
    private static final List<Material> POTION_TYPES = List.of(Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION);
    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        EntityHuman entityhuman = event.player();
        EnumHand enumhand = event.hand();
        net.minecraft.world.item.ItemStack itemstack = entityhuman.getItemInHand(enumhand);
        World world = event.world();
        BlockPosition blockposition = event.pos();
        WaterRecyclerComponent component = component();
        if (itemstack.getItem() == net.minecraft.world.item.Items.GLASS_BOTTLE) {
            if (!world.isClientSide) {
                if (clearLevel < 1) return EnumInteractionResult.SUCCESS;

                net.minecraft.world.item.ItemStack potion = CraftItemStack.asNMSCopy(ThirstSetting.createClearBottle());
                clearLevel--;
                syncDisplayVariable();
                saveData();

                Item item = itemstack.getItem();
                potion = potion == null ? PotionUtil.setPotion(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.POTION), Potions.WATER) : potion;

                entityhuman.setItemInHand(enumhand, ItemLiquidUtil.createFilledResult(itemstack, entityhuman, potion));
                entityhuman.awardStat(StatisticList.USE_CAULDRON);
                entityhuman.awardStat(StatisticList.ITEM_USED.get(item));
                world.playSound(null, blockposition, SoundEffects.BOTTLE_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
                world.gameEvent(null, GameEvent.FLUID_PICKUP, blockposition);
            }
            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        }
        return Optional.of(itemstack.asBukkitCopy())
                .filter(v -> POTION_TYPES.contains(v.getType()))
                .map(item -> {
                    if (waterLevel + 1 > component.totalWaterLevel) return EnumInteractionResult.SUCCESS;
                    waterLevel++;
                    syncDisplayVariable();
                    saveData();
                    entityhuman.setItemInHand(enumhand, ItemLiquidUtil.createFilledResult(itemstack, entityhuman, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE)));
                    entityhuman.awardStat(StatisticList.USE_CAULDRON);
                    entityhuman.awardStat(StatisticList.ITEM_USED.get(itemstack.getItem()));
                    world.playSound(null, blockposition, SoundEffects.BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0f, 1.0f);
                    world.gameEvent(null, GameEvent.FLUID_PLACE, blockposition);
                    return EnumInteractionResult.sidedSuccess(world.isClientSide);
                })
                .orElse(EnumInteractionResult.PASS);
    }
    private void syncDisplayVariable() {
        metadata().list(DisplayInstance.class).findAny().ifPresent(display -> display.modify(map -> {
            map.put("water_level", String.valueOf((int) Math.ceil(waterLevel)));
            map.put("clear_level", String.valueOf((int) Math.floor(clearLevel)));
            return true;
        }));
    }
}























