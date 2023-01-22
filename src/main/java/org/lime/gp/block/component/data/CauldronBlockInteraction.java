package org.lime.gp.block.component.data;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockShulkerBox;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntityBanner;
import net.minecraft.world.level.gameevent.GameEvent;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.lime.core;
import org.lime.system;
import org.lime.gp.item.settings.list.DyeColorSetting;

import java.util.Map;
import java.util.function.Predicate;

public interface CauldronBlockInteraction {
    static core.element create() {
        return core.element.create(CauldronBlockInteraction.class)
                .withInit(CauldronBlockInteraction::bootStrap);
    }
    Map<Item, CauldronBlockInteraction> EMPTY = CauldronBlockInteraction.newInteractionMap();
    Map<Item, CauldronBlockInteraction> WATER = CauldronBlockInteraction.newInteractionMap();
    Map<Item, CauldronBlockInteraction> LAVA = CauldronBlockInteraction.newInteractionMap();
    Map<Item, CauldronBlockInteraction> POWDER_SNOW = CauldronBlockInteraction.newInteractionMap();
    CauldronBlockInteraction FILL_WATER = (cauldron, world, blockposition, entityhuman, enumhand, itemstack) -> CauldronBlockInteraction.emptyBucket(cauldron, world, blockposition, entityhuman, enumhand, itemstack, _cauldron -> _cauldron.full(CauldronInstance.type.water), SoundEffects.BUCKET_EMPTY);
    CauldronBlockInteraction FILL_LAVA = (cauldron, world, blockposition, entityhuman, enumhand, itemstack) -> CauldronBlockInteraction.emptyBucket(cauldron, world, blockposition, entityhuman, enumhand, itemstack, _cauldron -> _cauldron.full(CauldronInstance.type.lava), SoundEffects.BUCKET_EMPTY_LAVA);
    CauldronBlockInteraction FILL_POWDER_SNOW = (cauldron, world, blockposition, entityhuman, enumhand, itemstack) -> CauldronBlockInteraction.emptyBucket(cauldron, world, blockposition, entityhuman, enumhand, itemstack, _cauldron -> _cauldron.full(CauldronInstance.type.snow), SoundEffects.BUCKET_EMPTY_POWDER_SNOW);
    CauldronBlockInteraction SHULKER_BOX = (cauldron, world, blockposition, entityhuman, enumhand, itemstack) -> {
        Block block = Block.byItem(itemstack.getItem());
        if (!(block instanceof BlockShulkerBox)) {
            return EnumInteractionResult.PASS;
        }
        if (org.lime.gp.item.Items.getIDByItem(itemstack)
                .map(org.lime.gp.item.Items.creators::get)
                .filter(v -> v instanceof org.lime.gp.item.Items.ItemCreator c ? c.getOptional(DyeColorSetting.class).map(_v -> !_v.dyeColor).orElse(true) : true)
                .isPresent()
        ) return EnumInteractionResult.PASS;
        if (!world.isClientSide) {
            if (!cauldron.decreaseLevel()) return EnumInteractionResult.SUCCESS; //CauldronLevelChangeEvent.ChangeReason.SHULKER_WASH
            ItemStack itemstack1 = new ItemStack(Blocks.SHULKER_BOX);
            if (itemstack.hasTag()) itemstack1.setTag(itemstack.getTag().copy());
            entityhuman.setItemInHand(enumhand, itemstack1);
            entityhuman.awardStat(StatisticList.CLEAN_SHULKER_BOX);
        }
        return EnumInteractionResult.sidedSuccess(world.isClientSide);
    };
    CauldronBlockInteraction BANNER = (cauldron, world, blockposition, entityhuman, enumhand, itemstack) -> {
        if (TileEntityBanner.getPatternCount(itemstack) <= 0) return EnumInteractionResult.PASS;
        if (org.lime.gp.item.Items.getIDByItem(itemstack)
                .map(org.lime.gp.item.Items.creators::get)
                .filter(v -> v instanceof org.lime.gp.item.Items.ItemCreator c ? c.getOptional(DyeColorSetting.class).map(_v -> !_v.dyeColor).orElse(true) : true)
                .isPresent()
        ) return EnumInteractionResult.PASS;
        if (!world.isClientSide) {
            if (!cauldron.decreaseLevel()) return EnumInteractionResult.SUCCESS; //CauldronLevelChangeEvent.ChangeReason.BANNER_WASH
            ItemStack itemstack1 = itemstack.cloneItemStack(false);
            itemstack1.setCount(1);
            TileEntityBanner.removeLastPattern(itemstack1);
            if (!entityhuman.getAbilities().instabuild) itemstack.shrink(1);

            if (itemstack.isEmpty()) entityhuman.setItemInHand(enumhand, itemstack1);
            else if (entityhuman.getInventory().add(itemstack1)) entityhuman.inventoryMenu.sendAllDataToRemote();
            else entityhuman.drop(itemstack1, false);

            entityhuman.awardStat(StatisticList.CLEAN_BANNER);
        }
        return EnumInteractionResult.sidedSuccess(world.isClientSide);
    };
    CauldronBlockInteraction DYED_ITEM = (cauldron, world, blockposition, entityhuman, enumhand, itemstack) -> {
        Item item = itemstack.getItem();
        if (!(item instanceof IDyeable idyeable)) return EnumInteractionResult.PASS;
        if (!idyeable.hasCustomColor(itemstack)) return EnumInteractionResult.PASS;
        if (org.lime.gp.item.Items.getIDByItem(itemstack)
                .map(org.lime.gp.item.Items.creators::get)
                .filter(v -> v instanceof org.lime.gp.item.Items.ItemCreator c ? c.getOptional(DyeColorSetting.class).map(_v -> !_v.dyeColor).orElse(true) : true)
                .isPresent()
        ) return EnumInteractionResult.PASS;
        if (!world.isClientSide) {
            if (!cauldron.decreaseLevel()) return EnumInteractionResult.SUCCESS; //CauldronLevelChangeEvent.ChangeReason.ARMOR_WASH
            idyeable.clearColor(itemstack);
            entityhuman.awardStat(StatisticList.CLEAN_ARMOR);
        }
        return EnumInteractionResult.sidedSuccess(world.isClientSide);
    };

    static Object2ObjectOpenHashMap<Item, CauldronBlockInteraction> newInteractionMap() {
        return SystemUtils.make(new Object2ObjectOpenHashMap<>(), object2objectopenhashmap -> object2objectopenhashmap.defaultReturnValue((cauldron, world, blockposition, entityhuman, enumhand, itemstack) -> EnumInteractionResult.PASS));
    }

    EnumInteractionResult interact(CauldronInstance cauldron, World world, BlockPosition blockposition, EntityHuman entityhuman, EnumHand enumhand, ItemStack itemstack);
    default EnumInteractionResult interact(CauldronInstance cauldron, ItemStack itemStack, BlockSkullInteractInfo event) {
        return interact(cauldron, event.world(), event.pos(), event.player(), event.hand(), itemStack);
    }

    static void bootStrap() {
        CauldronBlockInteraction.addDefaultInteractions(EMPTY);
        EMPTY.put(Items.POTION, (cauldron, world, blockposition, entityhuman, enumhand, itemstack) -> {
            if (PotionUtil.getPotion(itemstack) != Potions.WATER) {
                return EnumInteractionResult.PASS;
            }
            if (!world.isClientSide) {
                CauldronInstance.WaterCauldron waterCauldron = cauldron.new WaterCauldron();
                cauldron.data(waterCauldron); //CauldronLevelChangeEvent.ChangeReason.BOTTLE_EMPTY
                waterCauldron.level(1);
                Item item = itemstack.getItem();
                entityhuman.setItemInHand(enumhand, ItemLiquidUtil.createFilledResult(itemstack, entityhuman, new ItemStack(Items.GLASS_BOTTLE)));
                entityhuman.awardStat(StatisticList.USE_CAULDRON);
                entityhuman.awardStat(StatisticList.ITEM_USED.get(item));
                world.playSound((EntityHuman)null, blockposition, SoundEffects.BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0f, 1.0f);
                world.gameEvent(null, GameEvent.FLUID_PLACE, blockposition);
            }
            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        });
        CauldronBlockInteraction.addDefaultInteractions(WATER);
        WATER.put(Items.BUCKET, (cauldron, world, blockposition, entityhuman, enumhand, itemstack) -> CauldronBlockInteraction.fillBucket(cauldron, world, blockposition, entityhuman, enumhand, itemstack, new ItemStack(Items.WATER_BUCKET), cauldron1 -> cauldron1.data().isFull(), SoundEffects.BUCKET_FILL));
        WATER.put(Items.GLASS_BOTTLE, (cauldron, world, blockposition, entityhuman, enumhand, itemstack) -> {
            if (!world.isClientSide) {
                ItemStack potion = null;
                if (cauldron.data() instanceof CauldronInstance.PotionCauldron potionCauldron) {
                    org.bukkit.inventory.ItemStack ret = potionCauldron.getResult();
                    potion = ret == null ? null : CraftItemStack.asNMSCopy(ret);
                }
                if (!cauldron.decreaseLevel()) return EnumInteractionResult.SUCCESS; //CauldronLevelChangeEvent.ChangeReason.BOTTLE_FILL

                Item item = itemstack.getItem();
                potion = potion == null ? PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.WATER) : potion;

                entityhuman.setItemInHand(enumhand, ItemLiquidUtil.createFilledResult(itemstack, entityhuman, potion));
                entityhuman.awardStat(StatisticList.USE_CAULDRON);
                entityhuman.awardStat(StatisticList.ITEM_USED.get(item));
                world.playSound((EntityHuman)null, blockposition, SoundEffects.BOTTLE_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
                world.gameEvent(null, GameEvent.FLUID_PICKUP, blockposition);
            }
            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        });
        WATER.put(Items.POTION, (cauldron, world, blockposition, entityhuman, enumhand, itemstack) -> {
            if (!cauldron.data().isFull() && PotionUtil.getPotion(itemstack) == Potions.WATER) {
                if (!world.isClientSide) {
                    if (!cauldron.increaseLevel()) return EnumInteractionResult.SUCCESS; //CauldronLevelChangeEvent.ChangeReason.BOTTLE_FILL
                    entityhuman.setItemInHand(enumhand, ItemLiquidUtil.createFilledResult(itemstack, entityhuman, new ItemStack(Items.GLASS_BOTTLE)));
                    entityhuman.awardStat(StatisticList.USE_CAULDRON);
                    entityhuman.awardStat(StatisticList.ITEM_USED.get(itemstack.getItem()));
                    world.playSound((EntityHuman)null, blockposition, SoundEffects.BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0f, 1.0f);
                    world.gameEvent(null, GameEvent.FLUID_PLACE, blockposition);
                }
                return EnumInteractionResult.sidedSuccess(world.isClientSide);
            }
            return EnumInteractionResult.PASS;
        });
        WATER.put(Items.LEATHER_BOOTS, DYED_ITEM);
        WATER.put(Items.LEATHER_LEGGINGS, DYED_ITEM);
        WATER.put(Items.LEATHER_CHESTPLATE, DYED_ITEM);
        WATER.put(Items.LEATHER_HELMET, DYED_ITEM);
        WATER.put(Items.LEATHER_HORSE_ARMOR, DYED_ITEM);
        WATER.put(Items.WHITE_BANNER, BANNER);
        WATER.put(Items.GRAY_BANNER, BANNER);
        WATER.put(Items.BLACK_BANNER, BANNER);
        WATER.put(Items.BLUE_BANNER, BANNER);
        WATER.put(Items.BROWN_BANNER, BANNER);
        WATER.put(Items.CYAN_BANNER, BANNER);
        WATER.put(Items.GREEN_BANNER, BANNER);
        WATER.put(Items.LIGHT_BLUE_BANNER, BANNER);
        WATER.put(Items.LIGHT_GRAY_BANNER, BANNER);
        WATER.put(Items.LIME_BANNER, BANNER);
        WATER.put(Items.MAGENTA_BANNER, BANNER);
        WATER.put(Items.ORANGE_BANNER, BANNER);
        WATER.put(Items.PINK_BANNER, BANNER);
        WATER.put(Items.PURPLE_BANNER, BANNER);
        WATER.put(Items.RED_BANNER, BANNER);
        WATER.put(Items.YELLOW_BANNER, BANNER);
        WATER.put(Items.WHITE_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.GRAY_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.BLACK_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.BLUE_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.BROWN_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.CYAN_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.GREEN_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.LIGHT_BLUE_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.LIGHT_GRAY_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.LIME_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.MAGENTA_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.ORANGE_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.PINK_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.PURPLE_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.RED_SHULKER_BOX, SHULKER_BOX);
        WATER.put(Items.YELLOW_SHULKER_BOX, SHULKER_BOX);
        LAVA.put(Items.BUCKET, (cauldron, world, blockposition, entityhuman, enumhand, itemstack) -> CauldronBlockInteraction.fillBucket(cauldron, world, blockposition, entityhuman, enumhand, itemstack, new ItemStack(Items.LAVA_BUCKET), cauldron1 -> true, SoundEffects.BUCKET_FILL_LAVA));
        CauldronBlockInteraction.addDefaultInteractions(LAVA);
        POWDER_SNOW.put(Items.BUCKET, (cauldron, world, blockposition, entityhuman, enumhand, itemstack) -> CauldronBlockInteraction.fillBucket(cauldron, world, blockposition, entityhuman, enumhand, itemstack, new ItemStack(Items.POWDER_SNOW_BUCKET), cauldron1 -> cauldron1.data().isFull(), SoundEffects.BUCKET_FILL_POWDER_SNOW));
        CauldronBlockInteraction.addDefaultInteractions(POWDER_SNOW);
    }

    static void addDefaultInteractions(Map<Item, CauldronBlockInteraction> behavior) {
        behavior.put(Items.LAVA_BUCKET, FILL_LAVA);
        behavior.put(Items.WATER_BUCKET, FILL_WATER);
        behavior.put(Items.POWDER_SNOW_BUCKET, FILL_POWDER_SNOW);
    }

    static EnumInteractionResult fillBucket(CauldronInstance cauldron, World world, BlockPosition pos, EntityHuman player, EnumHand hand, ItemStack stack, ItemStack output, Predicate<CauldronInstance> predicate, SoundEffect soundEvent) {
        if (!predicate.test(cauldron)) {
            return EnumInteractionResult.PASS;
        }
        if (!world.isClientSide) {
            cauldron.full(CauldronInstance.type.empty); //CauldronLevelChangeEvent.ChangeReason.BUCKET_FILL
            Item item = stack.getItem();
            player.setItemInHand(hand, ItemLiquidUtil.createFilledResult(stack, player, output));
            player.awardStat(StatisticList.USE_CAULDRON);
            player.awardStat(StatisticList.ITEM_USED.get(item));
            world.playSound((EntityHuman)null, pos, soundEvent, SoundCategory.BLOCKS, 1.0f, 1.0f);
            world.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
        }
        return EnumInteractionResult.sidedSuccess(world.isClientSide);
    }
    static EnumInteractionResult emptyBucket(CauldronInstance cauldron, World world, BlockPosition pos, EntityHuman player, EnumHand hand, ItemStack stack, system.Action1<CauldronInstance> edit, SoundEffect soundEvent) {
        if (!world.isClientSide) {
            edit.invoke(cauldron); //CauldronLevelChangeEvent.ChangeReason.BUCKET_EMPTY
            Item item = stack.getItem();
            player.setItemInHand(hand, ItemLiquidUtil.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            player.awardStat(StatisticList.FILL_CAULDRON);
            player.awardStat(StatisticList.ITEM_USED.get(item));
            world.playSound((EntityHuman)null, pos, soundEvent, SoundCategory.BLOCKS, 1.0f, 1.0f);
            world.gameEvent(null, GameEvent.FLUID_PLACE, pos);
        }
        return EnumInteractionResult.sidedSuccess(world.isClientSide);
    }
}
