package org.lime.gp.block.component.data;

import com.google.gson.JsonElement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.minecraft.core.BlockPosition;
import net.minecraft.resources.MinecraftKey;
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
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDamageEvent;
import org.joml.Vector3f;
import org.lime.Position;
import org.lime.gp.block.BlockComponentInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.display.IDisplayVariable;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.list.WaitingComponent;
import org.lime.gp.chat.ChatColorHex;
import org.lime.gp.craft.book.ContainerWorkbenchBook;
import org.lime.gp.craft.book.Recipes;
import org.lime.gp.craft.recipe.AbstractRecipe;
import org.lime.gp.craft.recipe.WaitingRecipe;
import org.lime.gp.craft.slot.output.IOutputVariable;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.extension.inventory.ReadonlyInventory;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.ThirstSetting;
import org.lime.gp.lime;
import org.lime.gp.module.DrawText;
import org.lime.gp.module.loot.PopulateLootEvent;
import org.lime.gp.player.level.LevelModule;
import org.lime.gp.player.perm.Perms;
import org.lime.json.JsonElementOptional;
import org.lime.json.JsonObjectOptional;
import org.lime.system.Time;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.ItemUtils;

import java.util.*;

public class WaitingInstance extends BlockComponentInstance<WaitingComponent> implements CustomTileMetadata.Tickable, CustomTileMetadata.Interactable, CustomTileMetadata.Damageable, CustomTileMetadata.Lootable, IDisplayVariable {
    public WaitingInstance(WaitingComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
        writeDebug("ctor");
    }

    //private static final Position DEBUG_LOCATION = new Position(lime.MainWorld, 1736, 11, 821);
    private void writeDebug(String line) { writeDebug(() -> line); }
    private void writeDebug(List<String> lines) {
        String save = this.metadata().position().toSave();
        lines.forEach(line -> lime.logToFile("waiting/"+save, "[{time}] " + line));
    }
    private void writeDebug(Func0<String> line) {
        String save = this.metadata().position().toSave();
        lime.logToFile("waiting/"+save, "[{time}] " + line.invoke());
    }

    private static abstract class BaseInput {
        public abstract String color();
        public abstract Material material();
        public abstract int cmd();
        public abstract int count();
        public abstract json.builder.object save();
        public abstract ItemStack nms();
        public abstract Toast2<BaseInput, EnumInteractionResult> interact(WaitingInstance instance, CustomTileMetadata metadata, BlockSkullInteractInfo event);
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

        public BaseItemInput(JsonElement value) { item = CraftItemStack.asNMSCopy(ItemUtils.loadItem(value.getAsString())); }
        public BaseItemInput(ItemStack item) { this.item = item.copy(); }

        @Override public Material material() { return CraftMagicNumbers.getMaterial(item.getItem()); }
        @Override public int cmd() { return Items.getIDByItem(item).orElse(0); }
        @Override public int count() { return item.getCount(); }
        public abstract String type();
        @Override public json.builder.object save() {
            return json.object()
                    .add("type", type())
                    .add("value", ItemUtils.saveItem(CraftItemStack.asBukkitCopy(item)));
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

        @Override public Toast2<BaseInput, EnumInteractionResult> interact(WaitingInstance instance, CustomTileMetadata metadata, BlockSkullInteractInfo event) {
            EntityHuman entityhuman = event.player();
            EnumHand enumhand = event.hand();
            BlockPosition blockposition = metadata.skull.getBlockPos();
            World world = metadata.skull.getLevel();
            ItemStack itemstack = entityhuman.getItemInHand(enumhand);
            if (itemstack.isEmpty()) return Toast.of(this, instance.openWorkbench(entityhuman));
            if (ItemStack.isSameItemSameTags(itemstack, item)) {
                if (item.getCount() >= instance.component().max_count) return Toast.of(this, EnumInteractionResult.PASS);
                if (!entityhuman.getAbilities().instabuild) itemstack.shrink(1);
                item.grow(1);
                setDirty();
                entityhuman.awardStat(StatisticList.ITEM_USED.get(itemstack.getItem()));
                world.playSound(null, blockposition, SoundEffects.ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 1.0f, 0.25f);
                return Toast.of(item.isEmpty() ? new EmptyInput() : this, EnumInteractionResult.sidedSuccess(world.isClientSide));
            }
            if (!instance.isWhitelistItem(itemstack)) return Toast.of(this, EnumInteractionResult.PASS);
            return Toast.of(this, tryAppendItem(world, blockposition, entityhuman, instance.items, itemstack));
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

        @Override public Toast2<BaseInput, EnumInteractionResult> interact(WaitingInstance instance, CustomTileMetadata metadata, BlockSkullInteractInfo event) {
            EntityHuman entityhuman = event.player();
            EnumHand enumhand = event.hand();
            BlockPosition blockposition = metadata.skull.getBlockPos();
            World world = metadata.skull.getLevel();
            ItemStack itemstack = entityhuman.getItemInHand(enumhand);
            if (itemstack.isEmpty()) return Toast.of(this, instance.openWorkbench(entityhuman));
            if (itemstack.getItem() == net.minecraft.world.item.Items.GLASS_BOTTLE) {
                ItemStack potion = item.split(1);
                setDirty();

                Item item = itemstack.getItem();
                entityhuman.setItemInHand(enumhand, ItemLiquidUtil.createFilledResult(itemstack, entityhuman, potion));
                entityhuman.awardStat(StatisticList.ITEM_USED.get(item));
                world.playSound(null, blockposition, SoundEffects.BOTTLE_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
                world.gameEvent(null, GameEvent.FLUID_PICKUP, blockposition);
                return Toast.of(this.item.isEmpty() ? new EmptyInput() : this, EnumInteractionResult.sidedSuccess(world.isClientSide));
            } else if (ItemStack.isSameItemSameTags(item, itemstack)) {
                if (item.getCount() >= instance.component().max_count) return Toast.of(this, EnumInteractionResult.PASS);
                item.grow(1);
                setDirty();

                entityhuman.setItemInHand(enumhand, ItemLiquidUtil.createFilledResult(itemstack, entityhuman, new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE)));
                entityhuman.awardStat(StatisticList.ITEM_USED.get(itemstack.getItem()));
                world.playSound(null, blockposition, SoundEffects.BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0f, 1.0f);
                world.gameEvent(null, GameEvent.FLUID_PLACE, blockposition);
                return Toast.of(this, EnumInteractionResult.sidedSuccess(world.isClientSide));
            }
            if (!instance.isWhitelistItem(itemstack)) return Toast.of(this, EnumInteractionResult.PASS);
            return Toast.of(item.isEmpty() ? new EmptyInput() : this, tryAppendItem(world, blockposition, entityhuman, instance.items, itemstack));
        }
        @Override public BaseInput loot(List<org.bukkit.inventory.ItemStack> drop) { return this; }
    }
    private static class EmptyInput extends BaseInput {
        @Override public String color() { return ThirstSetting.DEFAULT_WATER_COLOR_HEX; }
        @Override public Material material() { return Material.AIR; }
        @Override public int cmd() { return 0; }
        @Override public int count() { return 0; }
        @Override public json.builder.object save() {
            return json.object()
                    .add("type", "empty")
                    .addNull("value");
        }
        @Override public ItemStack nms() { return ItemStack.EMPTY; }
        @Override public Toast2<BaseInput, EnumInteractionResult> interact(WaitingInstance instance, CustomTileMetadata metadata, BlockSkullInteractInfo event) {
            EntityHuman entityhuman = event.player();
            EnumHand enumhand = event.hand();
            BlockPosition blockposition = metadata.skull.getBlockPos();
            World world = metadata.skull.getLevel();
            ItemStack itemstack = entityhuman.getItemInHand(enumhand);
            if (itemstack.isEmpty()) return Toast.of(this, instance.openWorkbench(entityhuman));
            if (!instance.isWhitelistItem(itemstack)) return Toast.of(this, EnumInteractionResult.PASS);
            BaseInput input;
            if (Items.has(ThirstSetting.class, itemstack)) {
                input = new WaterInput(itemstack);
                entityhuman.setItemInHand(enumhand, ItemLiquidUtil.createFilledResult(itemstack, entityhuman, new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE)));
                entityhuman.awardStat(StatisticList.ITEM_USED.get(itemstack.getItem()));
                world.playSound(null, blockposition, SoundEffects.BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0f, 1.0f);
                world.gameEvent(null, GameEvent.FLUID_PLACE, blockposition);
            } else {
                input = new ItemInput(itemstack.copyWithCount(1));
                if (!entityhuman.getAbilities().instabuild) itemstack.shrink(1);
                entityhuman.awardStat(StatisticList.ITEM_USED.get(itemstack.getItem()));
                world.playSound(null, blockposition, SoundEffects.ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 1.0f, 0.25f);
            }
            return Toast.of(input, EnumInteractionResult.sidedSuccess(world.isClientSide));
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
        return ContainerWorkbenchBook.open(player, metadata(), Recipes.WAITING, type, Recipes.WAITING.getAllRecipes().stream().filter(v -> v.waiting_type.equals(type)).toList());
    }

    private UUID last_click = null;
    private BaseInput input = new EmptyInput();
    private final List<ItemStack> items = new ArrayList<>();

    private long startTime = 0;
    private long endTime = 0;

    private int lastShowProgress = -1;
    private int readLoader = 0;

    @Override public void read(JsonObjectOptional json) {
        writeDebug("Read: " + json);
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
                        .map(ItemUtils::loadItem)
                        .map(CraftItemStack::asNMSCopy)
                        .filter(item -> {
                            if (item.isEmpty()) {
                                writeDebug("!!!READ WARNING!!! FOUND EMPTY ITEM IN WAITING INSTANCE LOADER: " + element.getAsString().orElse("NULL"));
                                lime.logOP("!!!WARNING!!! FOUND EMPTY ITEM IN WAITING INSTANCE LOADER: " + element.getAsString().orElse("NULL"));
                                return false;
                            }
                            return true;
                        })
                        .ifPresent(items::add)
                ));
        readLoader = 200;
    }
    @Override public json.builder.object write() {
        if (items.removeIf(Objects::isNull)) {
            writeDebug("!!!WRITE WARNING!!! FOUND EMPTY ITEMS IN WAITING INSTANCE");
            lime.logOP("!!!WARNING!!! FOUND EMPTY ITEMS IN WAITING INSTANCE");
        }
        var obj = json.object()
                .add("input", input.save())
                .addArray("items", v -> v.add(items.stream()
                        .map(CraftItemStack::asBukkitCopy)
                        .map(ItemUtils::saveItem)
                        .iterator()
                ))
                .add("start_time", startTime)
                .add("last_click", last_click);
        writeDebug("Write: " + obj.build());
        return obj;
    }

    private ReadonlyInventory createReadonly() {
        List<ItemStack> items = new ArrayList<>();
        items.add(input.nms());
        items.addAll(this.items);
        return ReadonlyInventory.ofNMS(items, metadata().location());
    }

    private Optional<WaitingRecipe> currentRecipe() {
        if (last_click == null) return Optional.empty();
        WaitingComponent component = component();
        String waiting_type = component.type;
        Perms.ICanData canData = Perms.getCanData(last_click);
        World world = metadata().skull.getLevel();
        ReadonlyInventory readonlyInventory = createReadonly();
        return Recipes.WAITING.getAllRecipes(canData)
                .filter(v -> v.waiting_type.equals(waiting_type))
                .filter(v -> v.matches(readonlyInventory, world))
                .findFirst();
    }
    private void updateDebug() {
        WaitingComponent component = component();
        String id = this.unique() + ":DEBUG";
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);
        String startTimeDisplay = Time.formatCalendar(calendar, true);
        calendar.setTimeInMillis(endTime);
        String endTimeDisplay = Time.formatCalendar(calendar, true);

        long currentTime = System.currentTimeMillis();

        int progress;
        if (startTime == 0 || endTime == 0) {
            progress = -1;
        } else {
            double currentDelta = currentTime - startTime;
            double totalDelta = endTime - startTime;

            progress = (int)Math.ceil(currentDelta * 100 / totalDelta);
        }

        Component text = Component.join(JoinConfiguration.newlines(),
                Component.text("Start: " + startTimeDisplay),
                Component.text("End: " + endTimeDisplay),
                Component.text("Progress: " + lastShowProgress + " / " + component.progress + " (" + progress + "%)"),
                Component.text("Owner: " + Optional.ofNullable(last_click).map(Bukkit::getOfflinePlayer).map(OfflinePlayer::getName).orElse("NAN")),
                Component.text("Recipe: " + currentRecipe().map(AbstractRecipe::getId).map(MinecraftKey::toString).orElse("NAN"))
        );

        Vector3f scale = new Vector3f(0.5f, 0.5f, 0.5f);
        Location location = metadata().location(0.5, 1.5, 0.5);

        DrawText.show(new DrawText.IShowTimed(0.1) {
            @Override public String getID() { return id; }
            @Override public boolean filter(Player player) { return true; }
            @Override public Component text(Player player) { return text; }
            @Override public Location location() { return location; }
            @Override public double distance() { return 10; }
            @Override public Vector3f scale() { return scale; }
        });
    }

    private int ticks = 0;
    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        if (readLoader > 0) {
            readLoader--;
            if (readLoader > 0 && last_click != null && UserRow.getBy(last_click).isEmpty()) return;
            readLoader = 0;
            syncRecipe("READ", false);
        }
        WaitingComponent component = component();

        if (component.debug) updateDebug();
        if (endTime == 0 || startTime == 0) {
            if (lastShowProgress != 0) {
                writeDebug("OT.0: " + lastShowProgress + " -> 0");
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

        int showProgress = Math.min(Math.max((int)Math.ceil(currentDelta * component.progress / totalDelta), 1), component.progress);
        if (showProgress != lastShowProgress) {
            writeDebug("OT.1: " + lastShowProgress + " -> " + showProgress);
            lastShowProgress = showProgress;
            syncDisplayVariable();
        }
        if (currentDelta >= totalDelta) {
            writeDebug("DETECT DELTA: " + currentDelta + " >= " + totalDelta);
            syncRecipe("TIME_END", false)
                    .ifPresent(recipe -> recipe
                            .assembleWithCount(createReadonly(), event.getWorld().registryAccess(), IOutputVariable.of(last_click))
                            .invoke((item, count) -> {
                                writeDebug("Craft '"+recipe.getId()+"' * " + count + " done!");
                                for (int i = 0; i < count; i++)
                                    LevelModule.onCraft(last_click, recipe.getId());

                                writeDebug(() -> "Result: " + ItemUtils.saveItem(item.asBukkitCopy()));
                                if (item.isEmpty()) input = new EmptyInput();
                                else if (Items.has(ThirstSetting.class, item)) input = new WaterInput(item);
                                else input = new ItemInput(item);
                                writeDebug("Swap: " + input);
                                items.clear();
                                syncRecipe("TIME_END_RESYNC", true);
                                saveData();
                                writeDebug("DELTA END!");
                            })
                    );
        }
    }

    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        writeDebug("OI.0");
        EntityHuman entityhuman = event.player();
        Toast2<BaseInput, EnumInteractionResult> result = input.interact(this, metadata, event);
        writeDebug("OI.1");
        if (input != result.val0) {
            writeDebug("OI.2: " + input + " -> " + result.val0);
            input = result.val0;
        }
        writeDebug("OI.3");
        if (input.readDirty()) {
            last_click = entityhuman.getUUID();
            writeDebug("OI.4: " + last_click);
            syncRecipe("INTERACT", true);
            saveData();

            syncDisplayVariable();
        }
        writeDebug("OI.5: " + result.val1);
        return result.val1;
    }
    @Override public void onDamage(CustomTileMetadata metadata, BlockDamageEvent event) {
        writeDebug("OD.0");
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            writeDebug("OD.1");
            List<org.bukkit.inventory.ItemStack> items = new ArrayList<>();
            input = input.loot(items);
            writeDebug("ON DROP:");
            writeDebug(" - Item count: " + this.items.size());
            this.items.removeIf(item -> {
                writeDebug(() -> " - Drop: " + ItemUtils.saveItem(item.asBukkitCopy()));
                items.add(item.asBukkitCopy());
                return true;
            });
            writeDebug(" - Drop count: " + items.size());
            if (!input.readDirty() && items.isEmpty()) return;
            writeDebug("OD.2");
            last_click = player.getUniqueId();
            syncRecipe("DAMAGE", true);
            saveData();

            syncDisplayVariable();

            if (items.isEmpty()) return;
            writeDebug("OD.3");
            Items.dropGiveItem(player, items, true);
            Location location = event.getBlock().getLocation();
            location.getWorld().playSound(location, Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM, org.bukkit.SoundCategory.BLOCKS, 1.0f, 0.25f);
        }
    }

    private Optional<WaitingRecipe> syncRecipe(String prefix, boolean resetTime) {
        boolean CHANGED = false;
        List<String> logs = new ArrayList<>(); // writeDebug("Write: " + obj);
        logs.add("SR.0: " + prefix);
        int total_sec = 0;
        WaitingComponent component = component();
        WaitingRecipe recipe = null;
        logs.add("SR.1");
        if (last_click != null) {
            logs.add("SR.1.0: " + last_click);
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
            logs.add("SR.1.1: " + recipe + " / " + total_sec + " with " + input);
        }
        logs.add("SR.2: " + resetTime);
        boolean change = false;
        if (resetTime) {
            if (startTime != 0 || endTime != 0) change = true;
            logs.add("SR.2.1: " + startTime + " / " + endTime);
            startTime = 0;
            endTime = 0;
            logs.add("SR.2.2: " + startTime + " / " + endTime);
            CHANGED = true;
        }
        logs.add("SR.3: " + startTime + " / " + endTime + " AND " + total_sec);
        if (startTime == 0) {
            logs.add("SR.3.0: " + total_sec);
            if (total_sec > 0) {
                startTime = System.currentTimeMillis();
                endTime = startTime + total_sec * 1000L;
                logs.add("SR.3.1: " + startTime + " / " + endTime);
                CHANGED = true;
                change = true;
            } else if (endTime != 0) {
                logs.add("SR.3.2");
                endTime = 0;
                CHANGED = true;
                change = true;
                logs.add("SR.3.3: " + startTime + " / " + endTime);
            }
            logs.add("SR.3.4: " + startTime + " / " + endTime);
        } else if (endTime == 0) {
            logs.add("SR.3.5: " + total_sec);
            if (total_sec <= 0) {
                startTime = 0;
                logs.add("SR.3.6: " + startTime + " / " + endTime);
            }
            else {
                endTime = startTime + total_sec * 1000L;
                logs.add("SR.3.7: " + startTime + " / " + endTime);
            }
            CHANGED = true;
            change = true;
            logs.add("SR.3.8");
        } else if (total_sec <= 0) {
            logs.add("SR.3.9: " + total_sec);
            startTime = 0;
            endTime = 0;
            CHANGED = true;
            change = true;
            logs.add("SR.3.10: " + startTime + " / " + endTime);
        }
        logs.add("SR.4: " + startTime + " -> " + endTime + " = " + (endTime - startTime) + " || " + change);
        if (startTime == 0 || endTime == 0) {
            int showProgress = 0;
            logs.add("SR.4.1: " + lastShowProgress);
            if (showProgress != lastShowProgress) {
                lastShowProgress = showProgress;
                logs.add("SR.4.2: " + lastShowProgress);
                CHANGED = true;
                syncDisplayVariable();
            }
            logs.add("SR.4.3");
            if (change) {
                logs.add("SR.4.4");
                saveData();
                CHANGED = true;
            }
            logs.add("SR.4.5");
            if (CHANGED) writeDebug(logs);
            return Optional.empty();
        } else if (change) {
            logs.add("SR.4.6");
            long currentTime = System.currentTimeMillis();
            double currentDelta = currentTime - startTime;
            double totalDelta = endTime - startTime;
            logs.add("SR.4.7: " + startTime + " / " + endTime + " / " + currentTime + " : " + currentDelta + " / " + totalDelta);

            component = component();

            int showProgress = (int)Math.ceil(currentDelta * component.progress / totalDelta);
            logs.add("SR.4.8: " + showProgress + " / " + lastShowProgress);
            if (showProgress != lastShowProgress) {
                logs.add("SR.4.9");
                lastShowProgress = showProgress;
                syncDisplayVariable();
            }
            logs.add("SR.4.10");
            saveData();
            CHANGED = true;
        }
        logs.add("SR.5: " + recipe);
        if (CHANGED) writeDebug(logs);
        return Optional.of(recipe);
    }

    @Override public void onLoot(CustomTileMetadata metadata, PopulateLootEvent event) {
        List<org.bukkit.inventory.ItemStack> items = new ArrayList<>();
        input.loot(items);
        writeDebug("ON LOOT:");
        writeDebug(" - Item count: " + this.items.size());
        this.items.removeIf(item -> {
            writeDebug(() -> " - Drop: " + ItemUtils.saveItem(item.asBukkitCopy()));
            items.add(item.asBukkitCopy());
            return true;
        });
        writeDebug(" - Drop count: " + items.size());
        event.addItems(items);
    }

    @Override public final void syncDisplayVariable() {
        writeDebug("syncDisplayVariable");
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
