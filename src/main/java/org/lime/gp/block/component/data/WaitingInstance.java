package org.lime.gp.block.component.data;

import com.google.gson.JsonElement;
import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemLiquidUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import net.minecraft.world.level.gameevent.GameEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDamageEvent;
import org.lime.gp.block.BlockComponentInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.list.WaitingComponent;
import org.lime.gp.chat.ChatColorHex;
import org.lime.gp.craft.RecipesBook;
import org.lime.gp.craft.recipe.Recipes;
import org.lime.gp.craft.recipe.WaitingRecipe;
import org.lime.gp.extension.inventory.ReadonlyInventory;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.ThirstSetting;
import org.lime.gp.lime;
import org.lime.gp.module.PopulateLootEvent;
import org.lime.gp.player.perm.Perms;
import org.lime.json.JsonElementOptional;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.util.List;
import java.util.*;

public class WaitingInstance extends BlockComponentInstance<WaitingComponent> implements CustomTileMetadata.Tickable, CustomTileMetadata.Interactable, CustomTileMetadata.Damageable, CustomTileMetadata.Lootable {
    public WaitingInstance(WaitingComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
    }

    private static final boolean DEBUG = false;

    private static abstract class BaseInput {
        public abstract String color();
        public abstract Material material();
        public abstract int cmd();
        public abstract int count();
        public abstract system.json.builder.object save();
        public abstract ItemStack nms();
        public abstract system.Toast2<BaseInput, EnumInteractionResult> interact(WaitingInstance instance, CustomTileMetadata metadata, BlockSkullInteractInfo event);
        public abstract BaseInput loot(List<org.bukkit.inventory.ItemStack> drop);
        public static Optional<BaseInput> read(String type, JsonElement value) {
            return switch (type) {
                case "item" -> Optional.of(new ItemInput(value));
                case "water" -> Optional.of(new WaterInput(value));
                default -> Optional.empty();
            };
        }

        private boolean isDirty = true;
        public boolean readDirty() {
            boolean value = isDirty;
            isDirty = false;
            return value;
        }
        public void setDirty() {
            isDirty = true;
        }
        public void clearDirty() { isDirty = false; }
    }
    private static abstract class BaseItemInput extends BaseInput {
        public ItemStack item;

        public BaseItemInput(JsonElement value) { item = CraftItemStack.asNMSCopy(system.loadItem(value.getAsString())); }
        public BaseItemInput(ItemStack item) { this.item = item.copy(); }

        @Override public Material material() { return CraftMagicNumbers.getMaterial(item.getItem()); }
        @Override public int cmd() { return Items.getIDByItem(item).orElse(0); }
        @Override public int count() { return item.getCount(); }
        public abstract String type();
        @Override public system.json.builder.object save() {
            return system.json.object()
                    .add("type", type())
                    .add("value", system.saveItem(CraftItemStack.asBukkitCopy(item)));
        }
        @Override public ItemStack nms() { return item; }

        @Override public String toString() { return type() + ":" + Items.getGlobalKeyByItem(item).orElse("AIR"); }

        public EnumInteractionResult tryAppendItem(World world, BlockPosition blockposition, EntityHuman entityhuman, List<ItemStack> items, ItemStack itemstack) {
            ItemStack toAppend = null;
            for (ItemStack item : items) {
                if (ItemStack.isSameItemSameTags(itemstack, item)) {
                    toAppend = item;
                    break;
                }
            }
            if (toAppend == null && items.size() >= 6) return EnumInteractionResult.PASS;
            ItemStack addItem = itemstack.copyWithCount(1);
            if (!entityhuman.getAbilities().instabuild) itemstack.shrink(1);
            if (toAppend != null) toAppend.grow(addItem.getCount());
            else items.add(addItem);
            setDirty();

            entityhuman.awardStat(StatisticList.ITEM_USED.get(itemstack.getItem()));
            world.playSound(null, blockposition, SoundEffects.ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 1.0f, 0.25f);
            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        }
    }
    private static class ItemInput extends BaseItemInput {
        public ItemInput(JsonElement value) { super(value); }
        public ItemInput(ItemStack item) { super(item); }

        @Override public String type() { return "item"; }
        @Override public String color() { return ThirstSetting.DEFAULT_WATER_COLOR_HEX.substring(1); }

        @Override public system.Toast2<BaseInput, EnumInteractionResult> interact(WaitingInstance instance, CustomTileMetadata metadata, BlockSkullInteractInfo event) {
            EntityHuman entityhuman = event.player();
            EnumHand enumhand = event.hand();
            BlockPosition blockposition = metadata.skull.getBlockPos();
            World world = metadata.skull.getLevel();
            net.minecraft.world.item.ItemStack itemstack = entityhuman.getItemInHand(enumhand);
            if (itemstack.isEmpty()) return system.toast(this, instance.openWorkbench(entityhuman));
            if (ItemStack.isSameItemSameTags(itemstack, item)) {
                if (item.getCount() >= instance.component().max_count) return system.toast(this, EnumInteractionResult.PASS);
                if (!entityhuman.getAbilities().instabuild) itemstack.shrink(1);
                item.grow(1);
                setDirty();
                entityhuman.awardStat(StatisticList.ITEM_USED.get(itemstack.getItem()));
                world.playSound(null, blockposition, SoundEffects.ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 1.0f, 0.25f);
                return system.toast(item.isEmpty() ? new EmptyInput() : this, EnumInteractionResult.sidedSuccess(world.isClientSide));
            }
            if (!instance.isWhitelistItem(itemstack)) return system.toast(this, EnumInteractionResult.PASS);
            return system.toast(this, tryAppendItem(world, blockposition, entityhuman, instance.items, itemstack));
        }
        @Override public BaseInput loot(List<org.bukkit.inventory.ItemStack> drop) {
            drop.add(item.asBukkitCopy());
            return new EmptyInput();
        }
    }
    private static class WaterInput extends BaseItemInput {
        public WaterInput(JsonElement value) { super(value); }
        public WaterInput(ItemStack item) { super(item); }

        @Override public String type() { return "water"; }
        @Override public String color() { return ChatColorHex.toHex(Items.getOptional(ThirstSetting.class, item).map(v -> v.color).orElse(ThirstSetting.DEFAULT_WATER_COLOR)).substring(1); }

        @Override public system.Toast2<BaseInput, EnumInteractionResult> interact(WaitingInstance instance, CustomTileMetadata metadata, BlockSkullInteractInfo event) {
            EntityHuman entityhuman = event.player();
            EnumHand enumhand = event.hand();
            BlockPosition blockposition = metadata.skull.getBlockPos();
            World world = metadata.skull.getLevel();
            ItemStack itemstack = entityhuman.getItemInHand(enumhand);
            if (itemstack.isEmpty()) return system.toast(this, instance.openWorkbench(entityhuman));
            if (itemstack.getItem() == net.minecraft.world.item.Items.GLASS_BOTTLE) {
                ItemStack potion = item.split(1);
                setDirty();

                Item item = itemstack.getItem();
                entityhuman.setItemInHand(enumhand, ItemLiquidUtil.createFilledResult(itemstack, entityhuman, potion));
                entityhuman.awardStat(StatisticList.ITEM_USED.get(item));
                world.playSound(null, blockposition, SoundEffects.BOTTLE_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
                world.gameEvent(null, GameEvent.FLUID_PICKUP, blockposition);
                return system.toast(this.item.isEmpty() ? new EmptyInput() : this, EnumInteractionResult.sidedSuccess(world.isClientSide));
            } else if (ItemStack.isSameItemSameTags(item, itemstack)) {
                if (item.getCount() >= instance.component().max_count) return system.toast(this, EnumInteractionResult.PASS);
                item.grow(1);
                setDirty();

                entityhuman.setItemInHand(enumhand, ItemLiquidUtil.createFilledResult(itemstack, entityhuman, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE)));
                entityhuman.awardStat(StatisticList.ITEM_USED.get(itemstack.getItem()));
                world.playSound(null, blockposition, SoundEffects.BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0f, 1.0f);
                world.gameEvent(null, GameEvent.FLUID_PLACE, blockposition);
                return system.toast(this, EnumInteractionResult.sidedSuccess(world.isClientSide));
            }
            if (!instance.isWhitelistItem(itemstack)) return system.toast(this, EnumInteractionResult.PASS);
            return system.toast(item.isEmpty() ? new EmptyInput() : this, tryAppendItem(world, blockposition, entityhuman, instance.items, itemstack));
        }
        @Override public BaseInput loot(List<org.bukkit.inventory.ItemStack> drop) { return this; }
    }
    private static class EmptyInput extends BaseInput {
        @Override public String color() { return ThirstSetting.DEFAULT_WATER_COLOR_HEX; }
        @Override public Material material() { return Material.AIR; }
        @Override public int cmd() { return 0; }
        @Override public int count() { return 0; }
        @Override public system.json.builder.object save() {
            return system.json.object()
                    .add("type", "empty")
                    .addNull("value");
        }
        @Override public ItemStack nms() { return ItemStack.EMPTY; }
        @Override public system.Toast2<BaseInput, EnumInteractionResult> interact(WaitingInstance instance, CustomTileMetadata metadata, BlockSkullInteractInfo event) {
            EntityHuman entityhuman = event.player();
            EnumHand enumhand = event.hand();
            BlockPosition blockposition = metadata.skull.getBlockPos();
            World world = metadata.skull.getLevel();
            net.minecraft.world.item.ItemStack itemstack = entityhuman.getItemInHand(enumhand);
            if (itemstack.isEmpty()) return system.toast(this, instance.openWorkbench(entityhuman));
            if (!instance.isWhitelistItem(itemstack)) return system.toast(this, EnumInteractionResult.PASS);
            BaseInput input;
            if (Items.has(ThirstSetting.class, itemstack)) {
                input = new WaterInput(itemstack);
                entityhuman.setItemInHand(enumhand, ItemLiquidUtil.createFilledResult(itemstack, entityhuman, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE)));
                entityhuman.awardStat(StatisticList.ITEM_USED.get(itemstack.getItem()));
                world.playSound(null, blockposition, SoundEffects.BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0f, 1.0f);
                world.gameEvent(null, GameEvent.FLUID_PLACE, blockposition);
            } else {
                input = new ItemInput(itemstack.copyWithCount(1));
                if (!entityhuman.getAbilities().instabuild) itemstack.shrink(1);
                entityhuman.awardStat(StatisticList.ITEM_USED.get(itemstack.getItem()));
                world.playSound(null, blockposition, SoundEffects.ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 1.0f, 0.25f);
            }
            return system.toast(input, EnumInteractionResult.sidedSuccess(world.isClientSide));
        }
        @Override public BaseInput loot(List<org.bukkit.inventory.ItemStack> drop) { return this; }
        @Override public String toString() { return "empty"; }
    }
    private boolean isWhitelistItem(ItemStack item) {
        WaitingComponent component = component();
        String waiting_type = component.type;
        return Items.getGlobalKeyByItem(item)
                .filter(Recipes.WAITING.getCacheWhitelistKeys()::contains)
                .filter(item_key -> Recipes.WAITING.getAllRecipes().stream().filter(v -> waiting_type.equals(v.waiting_type)).flatMap(WaitingRecipe::getWhitelistKeys).anyMatch(item_key::equals))
                .isPresent();
    }
    private EnumInteractionResult openWorkbench(EntityHuman player) {
        String type = component().type;
        return RecipesBook.openCustomWorkbench(player, metadata(), Recipes.WAITING, type, Recipes.WAITING.getAllRecipes().stream().filter(v -> v.waiting_type.equals(type)).toList());
    }

    private UUID last_click = null;
    private BaseInput input = new EmptyInput();
    private final List<ItemStack> items = new ArrayList<>();

    private long startTime = 0;
    private long endTime = 0;

    private int lastShowProgress = -1;

    @Override public void read(JsonObjectOptional json) {
        last_click = json.getAsString("last_click").map(UUID::fromString).orElse(null);
        input = json.getAsJsonObject("input")
                .flatMap(input -> input.getAsString("type")
                        .flatMap(type -> input.get("value")
                                .map(JsonElementOptional::base)
                                .flatMap(value -> BaseInput.read(type, value))
                        )
                ).orElseGet(EmptyInput::new);
        startTime = json.getAsLong("start_time").orElse(0L);
        endTime = 0;
        items.clear();
        json.getAsJsonArray("items")
                .ifPresent(arr -> arr.forEach(element -> element.getAsString()
                        .map(system::loadItem)
                        .map(CraftItemStack::asNMSCopy)
                        .ifPresent(items::add)
                ));
        syncRecipe("READ", false);
    }
    @Override public system.json.builder.object write() {
        return system.json.object()
                .add("input", input.save())
                .addArray("items", v -> v.add(items.stream()
                        .map(CraftItemStack::asBukkitCopy)
                        .map(system::saveItem)
                        .iterator()
                ))
                .add("start_time", startTime)
                .add("last_click", last_click);
    }

    private ReadonlyInventory createReadonly() {
        List<ItemStack> items = new ArrayList<>();
        items.add(input.nms());
        items.addAll(this.items);
        return ReadonlyInventory.ofNMS(items, metadata().location());
    }

    private int ticks = 0;
    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        if (endTime == 0 || startTime == 0) {
            if (lastShowProgress != 0) {
                lastShowProgress = 0;
                syncDisplayVariable();
            }
            return;
        }
        if (ticks % 400 == 0) {
            ticks = 0;
            syncRecipe("TICK", false);
        }
        ticks++;
        if (endTime == 0 || startTime == 0) return;

        long currentTime = System.currentTimeMillis();

        double currentDelta = currentTime - startTime;
        double totalDelta = endTime - startTime;

        WaitingComponent component = component();

        int showProgress = Math.min(Math.max((int)Math.ceil(currentDelta * component.progress / totalDelta), 1), component.progress);
        if (showProgress != lastShowProgress) {
            lastShowProgress = showProgress;
            syncDisplayVariable();
        }
        if (currentDelta >= totalDelta) {
            syncRecipe("TIME_END", false).ifPresent(recipe -> {
                ItemStack item = recipe.assemble(createReadonly(), event.getWorld().registryAccess());
                if (DEBUG) lime.logOP("Result: " + item);
                if (item.isEmpty()) input = new EmptyInput();
                else if (Items.has(ThirstSetting.class, item)) input = new WaterInput(item);
                else input = new ItemInput(item);
                items.clear();
                syncRecipe("TIME_END_RESYNC", true);
                saveData();
            });
        }
    }

    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        EntityHuman entityhuman = event.player();
        system.Toast2<BaseInput, EnumInteractionResult> result = input.interact(this, metadata, event);
        if (input != result.val0) input = result.val0;
        if (input.readDirty()) {
            last_click = entityhuman.getUUID();
            syncRecipe("INTERACT", true);
            saveData();

            syncDisplayVariable();
        }
        return result.val1;
    }
    @Override public void onDamage(CustomTileMetadata metadata, BlockDamageEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            List<org.bukkit.inventory.ItemStack> items = new ArrayList<>();
            input = input.loot(items);
            this.items.removeIf(item -> {
                items.add(item.asBukkitCopy());
                return true;
            });
            if (!input.readDirty() && items.isEmpty()) return;
            last_click = player.getUniqueId();
            syncRecipe("DAMAGE", true);
            saveData();

            syncDisplayVariable();

            if (items.isEmpty()) return;
            Items.dropGiveItem(player, items, true);
            Location location = event.getBlock().getLocation();
            location.getWorld().playSound(location, Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM, org.bukkit.SoundCategory.BLOCKS, 1.0f, 0.25f);
        }
    }

    private Optional<WaitingRecipe> syncRecipe(String prefix, boolean resetTime) {
        if (DEBUG) lime.logOP("SR.0: " + prefix);
        int total_sec = 0;
        WaitingComponent component = component();
        WaitingRecipe recipe = null;
        if (last_click != null) {
            if (DEBUG) lime.logOP("SR.1");
            String waiting_type = component.type;
            Perms.ICanData canData = Perms.getCanData(last_click);
            World world = metadata().skull.getLevel();
            ReadonlyInventory readonlyInventory = createReadonly();
            recipe = Recipes.WAITING.getAllRecipes(canData)
                    .filter(v -> v.waiting_type.equals(waiting_type))
                    .filter(v -> v.matches(readonlyInventory, world))
                    .findFirst()
                    .orElse(null);
            if (recipe != null) total_sec = recipe.total_sec;
            if (DEBUG) lime.logOP("SR.2: " + recipe + " / " + total_sec + " with " + input);
        }
        if (DEBUG) lime.logOP("SR.3");
        boolean change = false;
        if (resetTime) {
            if (startTime != 0 || endTime != 0) change = true;
            startTime = 0;
            endTime = 0;
        }
        if (startTime == 0) {
            if (total_sec > 0) {
                if (DEBUG) lime.logOP("SR.3.0");
                startTime = System.currentTimeMillis();
                endTime = startTime + total_sec * 1000L;
                change = true;
            } else if (endTime != 0) {
                if (DEBUG) lime.logOP("SR.3.1");
                endTime = 0;
                change = true;
            }
        } else if (endTime == 0) {
            if (DEBUG) lime.logOP("SR.3.2");
            if (total_sec <= 0) startTime = 0;
            else endTime = startTime + total_sec * 1000L;
            change = true;
        } else if (total_sec <= 0) {
            if (DEBUG) lime.logOP("SR.3.3");
            startTime = 0;
            endTime = 0;
            change = true;
        }
        //if (DEBUG) lime.logOP("SR.3.4: " + startTime + " -> " + endTime + " = " + (endTime - startTime));
        if (startTime == 0 || endTime == 0) {
            int showProgress = 0;
            if (showProgress != lastShowProgress) {
                lastShowProgress = showProgress;
                syncDisplayVariable();
            }
            if (change) saveData();
            return Optional.empty();
        } else if (change) {
            long currentTime = System.currentTimeMillis();
            double currentDelta = currentTime - startTime;
            double totalDelta = endTime - startTime;

            component = component();

            int showProgress = (int)Math.ceil(currentDelta * component.progress / totalDelta);
            if (showProgress != lastShowProgress) {
                lastShowProgress = showProgress;
                syncDisplayVariable();
            }
            saveData();
        }
        if (DEBUG) lime.logOP("SR.3.5: " + recipe);
        return Optional.of(recipe);
    }

    @Override public void onLoot(CustomTileMetadata metadata, PopulateLootEvent event) {
        List<org.bukkit.inventory.ItemStack> items = new ArrayList<>();
        input.loot(items);
        this.items.removeIf(item -> {
            items.add(item.asBukkitCopy());
            return true;
        });
        event.addItems(items);
    }

    protected final void syncDisplayVariable() {
        metadata().list(DisplayInstance.class).findAny().ifPresent(display -> {
            display.modify(map -> {
                int size = items.size();
                map.put("waiting.input.type", input.material().name());
                map.put("waiting.input.id", String.valueOf(input.cmd()));
                map.put("waiting.input.count", String.valueOf(input.count()));
                for (int i = 0; i < size; i++) {
                    ItemStack item = items.get(i);
                    map.put("waiting.slot."+i+".type", CraftMagicNumbers.getMaterial(item.getItem()).name());
                    map.put("waiting.slot."+i+".id", String.valueOf(Items.getIDByItem(item).orElse(0)));
                    map.put("waiting.slot."+i+".count", String.valueOf(items.get(i).getCount()));
                }
                for (int i = size; i < 6; i++) {
                    map.put("waiting.slot."+i+".type", "AIR");
                    map.put("waiting.slot."+i+".id", "0");
                    map.put("waiting.slot."+i+".count", "0");
                }
                map.put("waiting.color", input.color());
                map.put("waiting.count", String.valueOf(input.count()));
                map.put("waiting.progress", String.valueOf(lastShowProgress));
                return true;
            });
        });
    }
}
