package org.lime.gp.block.component.data;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.Vector3f;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemLiquidUtil;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.lime.display.models.shadow.Builder;
import org.lime.display.models.shadow.EntityBuilder;
import org.lime.display.models.shadow.IBuilder;
import org.lime.display.models.shadow.NoneBuilder;
import org.lime.display.transform.LocalLocation;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.display.BlockDisplay;
import org.lime.gp.block.component.display.IDisplayVariable;
import org.lime.gp.block.component.display.block.IModelBlock;
import org.lime.gp.block.component.display.event.BlockMarkerEventInteract;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.list.LaboratoryComponent;
import org.lime.gp.chat.ChatColorHex;
import org.lime.gp.craft.book.ContainerWorkbenchBook;
import org.lime.gp.craft.book.Recipes;
import org.lime.gp.craft.slot.output.IOutputVariable;
import org.lime.gp.extension.inventory.ReadonlyInventory;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.TableDisplaySetting;
import org.lime.gp.item.settings.list.ThirstSetting;
import org.lime.gp.lime;
import org.lime.gp.module.loot.PopulateLootEvent;
import org.lime.gp.player.level.LevelModule;
import org.lime.gp.player.perm.Perms;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class LaboratoryInstance extends BlockInstance implements BlockDisplay.Displayable, BlockDisplay.Interactable, CustomTileMetadata.Interactable, CustomTileMetadata.Childable, CustomTileMetadata.Tickable, CustomTileMetadata.Damageable, CustomTileMetadata.Lootable, IDisplayVariable {
    @Override public LaboratoryComponent component() { return (LaboratoryComponent)super.component(); }
    private final IBuilder model_interact;
    public LaboratoryInstance(LaboratoryComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
        this.model_interact = component.model_interact;

        items = Arrays.stream(SlotType.values()).collect(ImmutableMap.toImmutableMap(type -> type, type -> system.func(type.thirst ? WaterLaboratorySlot::new : ItemLaboratorySlot::new).invoke(component(), type)));
    }

    public static IBuilder createInteract(LaboratoryComponent component) {
        NoneBuilder builder = lime.models.builder().none();
        List<LocalLocation> input_thirst = component.input_thirst;
        for (int i = 0; i < Math.min(3, input_thirst.size()); i++) {
            SlotType type = SlotType.slotOfIndex(i, true);
            builder = builder
                    .addChild(builder_interact
                            .addKey(type.key)
                            .local(input_thirst.get(i))
                    );
        }
        return builder;
    }

    private static final EntityBuilder builder_interact = lime.models.builder().entity()
            .entity(EntityTypes.ARMOR_STAND)
            .nbt(() -> {
                EntityArmorStand stand = new EntityArmorStand(EntityTypes.ARMOR_STAND, lime.MainWorld.getHandle());
                stand.setNoBasePlate(true);
                stand.setShowArms(true);
                stand.setSmall(true);
                stand.setLeftArmPose(new Vector3f(-90,0,0));
                stand.setRightArmPose(new Vector3f(-90,0,0));
                stand.setHeadPose(new Vector3f(90, 0, 0));
                return stand;
            })
            .invisible(true);

    @Override public Stream<? extends CustomTileMetadata.Element> childs() { return items.values().stream(); }

    private class ItemLaboratorySlot extends LaboratorySlot implements BlockDisplay.Displayable {
        private system.LockToast1<IBuilder> model;
        public ItemLaboratorySlot(LaboratoryComponent component, SlotType type) {
            super(component, type);
        }
        @Override public ItemLaboratorySlot set(ItemStack item) {
            super.set(item);
            if (model == null) model = system.<IBuilder>toast(null).lock();
            this.model.set0(lime.models.builder().none()
                    .local(local)
                    .addChild(builder_interact
                            .nbt(v -> v.putBoolean("Invulnerable", true))
                            .nbt(v -> v.putBoolean("Marker", true))
                            .addEquipment(EnumItemSlot.HEAD, Items.getOptional(TableDisplaySetting.class, item)
                                    .flatMap(v -> v.of(TableDisplaySetting.TableType.laboratory, null))
                                    .map(v -> v.display(item))
                                    .orElseGet(() -> CraftItemStack.asNMSCopy(item)))
                    ));
            LaboratoryInstance.this.metadata()
                .list(DisplayInstance.class)
                .forEach(DisplayInstance::variableDirty);
            return this;
        }
        @Override public Optional<IModelBlock> onDisplayAsync(Player player, World world, BlockPosition position, IBlockData data) {
            return Optional.of(IModelBlock.of(null, model.get0(), BlockDisplay.getChunkSize(10), Double.POSITIVE_INFINITY));
        }
    }
    private static class WaterLaboratorySlot extends LaboratorySlot {
        private String color;
        public WaterLaboratorySlot(LaboratoryComponent component, SlotType type) {
            super(component, type);
        }
        @Override public WaterLaboratorySlot set(ItemStack item) {
            super.set(item);
            color = ChatColorHex.toHex(Items.getOptional(ThirstSetting.class, item).map(v -> v.color).orElse(ThirstSetting.DEFAULT_WATER_COLOR)).substring(1);
            return this;
        }
        public void applyVariable(Map<String, String> variables) {
            variables.put("water_color_" + (type.index + 1), color);
            variables.put("water_level_" + (type.index + 1), isPresent() ? "1" : "0");
        }
    }
    private abstract static class LaboratorySlot implements CustomTileMetadata.Uniqueable {
        public final UUID unique;
        public final SlotType type;
        public final LocalLocation local;
        private ItemStack item;
        public LaboratorySlot(LaboratoryComponent component, SlotType type) {
            this.unique = UUID.randomUUID();
            this.type = type;
            this.local = (type.thirst ? component.input_thirst.get(type.index) : component.input_dust.add(ofHeight(type.index)));
            set(null);
        }

        private static LocalLocation ofHeight(int height) {
            return new LocalLocation(0, -0.4 + height * 0.025, -0.5, 0, 0);
        }

        public ItemStack get() { return this.item; }
        public LaboratorySlot set(ItemStack item) { this.item = item; return this; }
        public boolean isEmpty() { return this.item == null || this.item.getType().isAir(); }
        public boolean isPresent() { return !isEmpty(); }

        @Override public UUID unique() { return unique; }
    }

    private final ImmutableMap<SlotType, LaboratorySlot> items;
    private UUID last_click = null;
    private LaboratorySlot of(SlotType type) {
        return items.get(type);
    }
    @Override public void read(JsonObjectOptional json) {
        items.values().forEach(item -> item.set(null));

        last_click = json.getAsString("last_click").map(UUID::fromString).orElse(null);
        json.getAsJsonObject("items")
                .stream()
                .flatMap(v -> v.entrySet()
                        .stream()
                        .flatMap(kv -> kv.getValue().getAsString().stream().map(system::loadItem).map(_v -> system.toast(SlotType.valueOf(kv.getKey()), _v)))
                        .filter(kv -> kv.val1 != null)
                )
                .forEach(kv -> of(kv.val0).set(kv.val1));
        syncDisplayVariable();
    }
    @Override public system.json.builder.object write() {
        return system.json.object()
                .addObject("items", v -> v.add(items, Enum::name, _v -> system.saveItem(_v.get())))
                .add("last_click", last_click);
    }

    private enum SlotType {
        Thirst0(0, true),
        Thirst1(1, true),
        Thirst2(2, true),
        Dust0(0, false),
        Dust1(1, false),
        Dust2(2, false);

        public final boolean thirst;
        public final int index;
        public final String key;
        SlotType(int index, boolean thirst) {
            this.index = index;
            this.key = "click."+(thirst?"thirst":"dust")+"."+index;
            this.thirst = thirst;
        }

        public static Optional<SlotType> slotOf(Collection<String> keys) {
            for (SlotType type : SlotType.values())
                if (keys.contains(type.key))
                    return Optional.of(type);
            return Optional.empty();
        }

        public static SlotType slotOfIndex(int index, boolean thirst) {
            return values()[index + (thirst ? 0 : 3)];
        }
    }

    @Override public void onInteract(CustomTileMetadata metadata, BlockMarkerEventInteract event) {
        if (event.isAttack() || event.getHand() != EquipmentSlot.HAND) return;
        SlotType.slotOf(event.getClickDisplay().keys())
                .ifPresent(slotType -> {
                    EntityHuman entityhuman = ((CraftPlayer)event.getPlayer()).getHandle();
                    EnumHand enumhand = event.getHand() == EquipmentSlot.HAND ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
                    net.minecraft.world.item.ItemStack itemstack = entityhuman.getItemInHand(enumhand);
                    World world = metadata.skull.getLevel();
                    BlockPosition blockposition = metadata.skull.getBlockPos();

                    boolean isThirst = slotType.thirst;

                    if (isThirst) {
                        if (itemstack.getItem() == net.minecraft.world.item.Items.GLASS_BOTTLE) {
                            ItemStack thirstItem = of(slotType).get();
                            if (thirstItem == null) return;
                            net.minecraft.world.item.ItemStack potion = CraftItemStack.asNMSCopy(thirstItem);
                            of(slotType).set(null);
                            syncDisplayVariable();
                            last_click = entityhuman.getUUID();
                            ticks = 0;
                            saveData();

                            Item item = itemstack.getItem();

                            entityhuman.setItemInHand(enumhand, ItemLiquidUtil.createFilledResult(itemstack, entityhuman, potion));
                            entityhuman.awardStat(StatisticList.ITEM_USED.get(item));
                            world.playSound(null, blockposition, SoundEffects.BOTTLE_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
                            world.gameEvent(null, GameEvent.FLUID_PICKUP, blockposition);
                        } else {
                            if (of(slotType).isPresent()) return;
                            if (!Items.has(ThirstSetting.class, itemstack)) return;
                            if (Items.getGlobalKeyByItem(itemstack).filter(Recipes.LABORATORY.getCacheWhitelistKeys()::contains).isEmpty()) return;
                            of(slotType).set(itemstack.asBukkitCopy());
                            syncDisplayVariable();
                            last_click = entityhuman.getUUID();
                            ticks = 0;
                            saveData();

                            entityhuman.setItemInHand(enumhand, ItemLiquidUtil.createFilledResult(itemstack, entityhuman, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE)));
                            entityhuman.awardStat(StatisticList.ITEM_USED.get(itemstack.getItem()));
                            world.playSound(null, blockposition, SoundEffects.BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0f, 1.0f);
                            world.gameEvent(null, GameEvent.FLUID_PLACE, blockposition);
                        }
                    } else {
                        if (itemstack.isEmpty()) {
                            ItemStack dustItem = of(slotType).get();
                            if (dustItem == null) return;
                            net.minecraft.world.item.ItemStack dust = CraftItemStack.asNMSCopy(dustItem);
                            of(slotType).set(null);
                            last_click = entityhuman.getUUID();
                            ticks = 0;
                            saveData();

                            entityhuman.setItemInHand(enumhand, dust);
                            world.playSound(null, blockposition, SoundEffects.ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 1.0f, 0.25f);
                        } else {
                            if (of(slotType).isPresent()) return;
                            if (Items.has(ThirstSetting.class, itemstack)) return;
                            if (Items.getGlobalKeyByItem(itemstack).filter(Recipes.LABORATORY.getCacheWhitelistKeys()::contains).isEmpty()) return;

                            of(slotType).set(itemstack.asBukkitCopy().asOne());
                            if (!entityhuman.getAbilities().instabuild) itemstack.shrink(1);

                            last_click = entityhuman.getUUID();
                            ticks = 0;
                            saveData();
                            entityhuman.awardStat(StatisticList.ITEM_USED.get(itemstack.getItem()));
                            world.playSound(null, blockposition, SoundEffects.ITEM_FRAME_REMOVE_ITEM, SoundCategory.BLOCKS, 1.0f, 0.25f);
                        }
                    }
                });
    }
    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        return ContainerWorkbenchBook.open(event.player(), metadata, Recipes.LABORATORY, Recipes.LABORATORY.getAllRecipes());
    }
    @Override public Optional<IModelBlock> onDisplayAsync(Player player, World world, BlockPosition position, IBlockData data) {
        return Optional.of(IModelBlock.of(null, model_interact, BlockDisplay.getChunkSize(10), Double.POSITIVE_INFINITY));
    }
    private int ticks = 0;
    private static final int TOTAL_TICKS = 30 * 20;
    private static final ParticleBuilder PARTICLE = Particle.REDSTONE.builder().color(Color.fromRGB(0xFFFFFF), 1).force(false).offset(0.2, 0, 0.2);
    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        BlockPosition position = event.getPos();
        Location location = new Location(event.getWorld().getWorld(), position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5);
        for (org.bukkit.entity.Item item : location.clone().add(0, 0.5, 0).getNearbyEntitiesByType(org.bukkit.entity.Item.class, 0.5)) {
            ItemStack itemStack = item.getItemStack();
            UUID owner = item.getThrower();
            if (owner == null || itemStack.getAmount() == 0) continue;
            for (Map.Entry<SlotType, LaboratorySlot> kv : items.entrySet()) {
                if (!kv.getKey().thirst
                        && kv.getValue().isEmpty()
                        && !Items.has(ThirstSetting.class, itemStack)
                        && Items.getGlobalKeyByItem(itemStack).filter(Recipes.LABORATORY.getCacheWhitelistKeys()::contains).isPresent()
                ) {
                    kv.getValue().set(itemStack.asOne());
                    last_click = item.getThrower();
                    ticks = 0;
                    saveData();
                    itemStack.subtract(1);
                    break;
                }
            }
        }

        if (items.values().stream().noneMatch(LaboratorySlot::isPresent)) return;
        ticks++;
        if (ticks < TOTAL_TICKS) {
            event.getWorld().playSound(null, event.getPos(), SoundEffects.BUBBLE_COLUMN_BUBBLE_POP, SoundCategory.BLOCKS, 1.0f, 1.0f);
            if (ticks % 10 > 0) return;
            PARTICLE
                    .color(Color.fromRGB(0xFFFF00 + system.rand(0x00, 0xFF)), (float)system.rand(1.0, 2.0))
                    .count(1)
                    .location(metadata.location(0.5, 1.2, 0.5))
                    .spawn();
            return;
        }

        ticks = 0;
        List<net.minecraft.world.item.ItemStack> input_thirst = new ArrayList<>();
        List<net.minecraft.world.item.ItemStack> input_dust = new ArrayList<>();
        items.values().forEach(slot -> {
            if (slot.isPresent()) (slot.type.thirst ? input_thirst : input_dust).add(CraftItemStack.asNMSCopy(slot.get()));
            slot.set(null);
        });
        syncDisplayVariable();
        saveData();

        List<net.minecraft.world.item.ItemStack> slots = Stream.concat(
                IntStream.range(0, 3).mapToObj(i -> input_thirst.size() > i ? input_thirst.get(i) : net.minecraft.world.item.ItemStack.EMPTY),
                IntStream.range(0, 3).mapToObj(i -> input_dust.size() > i ? input_dust.get(i) : net.minecraft.world.item.ItemStack.EMPTY)
        ).toList();

        ReadonlyInventory inventory = ReadonlyInventory.ofNMS(slots);

        World world = event.getWorld();

        world.playSound(null, event.getPos(), SoundEffects.GENERIC_BURN, SoundCategory.BLOCKS, 1.0f, 0.2f);
        Perms.ICanData canData = Perms.getCanData(last_click);
        Recipes.LABORATORY.getAllRecipes(canData)
                .filter(v -> v.matches(inventory, event.getWorld()))
                .findFirst()
                .ifPresentOrElse(recipe -> {
                            PARTICLE
                                    .color(Color.fromRGB(0xFFFF00), 3)
                                    .count(3)
                                    .location(metadata.location(0.5, 1.2, 0.5))
                                    .spawn();
                            Perms.onRecipeUse(recipe, last_click, canData);
                            net.minecraft.world.item.ItemStack output = recipe.assemble(inventory, world.registryAccess(), IOutputVariable.of(last_click));
                            LevelModule.onCraft(last_click, recipe.getId());
                            Block.popResource(event.getWorld(), event.getPos().above(), output);
                        }, () -> PARTICLE
                                .color(Color.fromRGB(0x000000), 3)
                                .count(3)
                                .location(metadata.location(0.5, 1.2, 0.5))
                                .spawn()
                );

    }
    @Override public void onDamage(CustomTileMetadata metadata, BlockDamageEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            List<ItemStack> items = new ArrayList<>();
            this.items.values().forEach(slot -> {
                if (slot.type.thirst || slot.isEmpty()) return;
                items.add(slot.get());
                slot.set(null);
            });
            if (items.isEmpty()) return;
            Items.dropGiveItem(player, items, true);
            saveData();
            return;
        }
    }
    @Override public void onLoot(CustomTileMetadata metadata, PopulateLootEvent event) {
        this.items.values().forEach(slot -> {
            if (slot.type.thirst || slot.isEmpty()) return;
            event.addItem(slot.get());
            slot.set(null);
        });
        syncDisplayVariable();
    }
    @Override public final void syncDisplayVariable() {
        metadata().list(DisplayInstance.class).findAny().ifPresent(display -> display.modify(map -> {
            for (LaboratorySlot slot : items.values())
                if (slot instanceof WaterLaboratorySlot waterSlot)
                    waterSlot.applyVariable(map);
            return true;
        }));
    }
}

























