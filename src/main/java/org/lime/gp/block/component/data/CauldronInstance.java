package org.lime.gp.block.component.data;

import com.mojang.math.Transformation;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.joml.Vector3f;
import org.lime.display.models.shadow.Builder;
import org.lime.display.models.shadow.IBuilder;
import org.lime.display.models.shadow.ItemBuilder;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.display.BlockDisplay;
import org.lime.gp.block.component.display.CacheBlockDisplay;
import org.lime.gp.block.component.display.block.IModelBlock;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.craft.book.ContainerWorkbenchBook;
import org.lime.gp.craft.book.Recipes;
import org.lime.gp.craft.slot.output.IOutputVariable;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.extension.inventory.ReadonlyInventory;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.ThirstSetting;
import org.lime.gp.lime;
import org.lime.gp.module.loot.PopulateLootEvent;
import org.lime.gp.player.level.LevelModule;
import org.lime.gp.player.perm.Perms;
import org.lime.json.JsonElementOptional;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.ItemUtils;

import java.util.*;
import java.util.stream.Stream;

public class CauldronInstance extends BlockInstance implements CustomTileMetadata.Childable, CustomTileMetadata.Shapeable, CustomTileMetadata.Interactable {
    private static final double STEP_TIMER = 0.25 * 60;

    private static final VoxelShape CAULDRON_INSIDE = AbstractCauldronBlock.box(2.0, 4.0, 2.0, 14.0, 16.0, 14.0);
    private static final VoxelShape CAULDRON_SHAPE = VoxelShapes.join(VoxelShapes.block(), VoxelShapes.or(AbstractCauldronBlock.box(0.0, 0.0, 4.0, 16.0, 3.0, 12.0), AbstractCauldronBlock.box(4.0, 0.0, 0.0, 12.0, 3.0, 16.0), AbstractCauldronBlock.box(2.0, 0.0, 2.0, 14.0, 3.0, 14.0), CAULDRON_INSIDE), OperatorBoolean.ONLY_FIRST);

    private ICauldron data;

    public CauldronInstance(ComponentDynamic<?, ?> component, CustomTileMetadata metadata) {
        super(component, metadata);
        data = new EmptyCauldron();
        data.syncBlock();
    }

    public ICauldron data() { return data; }
    public <T extends ICauldron>CauldronInstance data(T data) {
        if (this.data == data) return this;
        this.data.onEdit(data);
        this.data = data;
        data.syncBlock();
        saveData();
        DisplayInstance.markDirtyBlock(metadata().position());
        return this;
    }
    public CauldronInstance level(int level) {
        if (this.data instanceof LayeredCauldron cauldron) cauldron.level(level);
        return this;
    }
    public CauldronInstance full(type type) {
        return data(type.create(this));
    }
    @Override public Stream<CustomTileMetadata.Element> childs() { return Stream.of(data); }


    public enum type {
        empty(v -> v.new EmptyCauldron()),
        water(v -> v.new WaterCauldron(), (v, j) -> v.new WaterCauldron(j)),
        snow(v -> v.new SnowCauldron(), (v, j) -> v.new SnowCauldron(j)),
        potion(v -> v.new PotionCauldron(), (v, j) -> v.new PotionCauldron(j)),
        lava(v -> v.new LavaCauldron());

        private final Func1<CauldronInstance, ICauldron> creator;
        private final Func2<CauldronInstance, JsonObjectOptional, ICauldron> reader;

        type(Func1<CauldronInstance, ICauldron> creator) { this(creator, (v,j) -> creator.invoke(v)); }
        type(Func1<CauldronInstance, ICauldron> creator, Func2<CauldronInstance, JsonObjectOptional, ICauldron> reader) {
            this.creator = creator;
            this.reader = reader;
        }

        public ICauldron create(CauldronInstance cauldron) { return creator.invoke(cauldron); }
        public ICauldron create(CauldronInstance cauldron, JsonObjectOptional json) { return reader.invoke(cauldron, json); }

        public static Optional<type> tryValueOf(String name) {
            try { return Optional.of(type.valueOf(name)); }
            catch (Exception e) { return Optional.empty(); }
        }
    }
    public abstract class ICauldron implements CustomTileMetadata.Element {
        public UUID unique() { return CauldronInstance.this.unique(); }
        public abstract type type();
        public abstract json.builder.object save();
        public abstract boolean isFull();
        public abstract Map<net.minecraft.world.item.Item, CauldronBlockInteraction> interactions();
        public abstract String state();
        @Override public String toString() { return type() + "[" + state() + "]"; }
        public void onEdit(ICauldron cauldron) {}
        public abstract void syncBlock();
    }
    public class EmptyCauldron extends ICauldron {
        @Override public json.builder.object save() { return json.object(); }
        @Override public type type() { return type.empty; }
        @Override public boolean isFull() { return false; }
        @Override public Map<net.minecraft.world.item.Item, CauldronBlockInteraction> interactions() { return CauldronBlockInteraction.EMPTY; }
        @Override public String state() { return ""; }

        @Override public void syncBlock() {
            CacheBlockDisplay.replaceCacheBlock(CauldronInstance.this.metadata().skull,
                    CacheBlockDisplay.ICacheInfo.of(net.minecraft.world.level.block.Blocks.CAULDRON.defaultBlockState()));
            DisplayInstance.markDirtyBlock(CauldronInstance.this.metadata().position());
        }
    }
    public abstract class LayeredCauldron extends ICauldron {
        private int level;
        public void level(int level) {
            this.level = Math.max(Math.min(level, LayeredCauldronBlock.MAX_FILL_LEVEL), LayeredCauldronBlock.MIN_FILL_LEVEL);
            CauldronInstance.this.saveData();
            syncBlock();
        }
        public int level() { return level; }
        private LayeredCauldron(int level) {
            this.level = level;
        }
        public LayeredCauldron() { this(LayeredCauldronBlock.MAX_FILL_LEVEL); }
        public LayeredCauldron(JsonObjectOptional json) { this(json.getAsInt("level").orElse(LayeredCauldronBlock.MAX_FILL_LEVEL)); }
        public abstract Block material();
        @Override public json.builder.object save() { return json.object().add("level", level); }
        @Override public boolean isFull() { return level == LayeredCauldronBlock.MAX_FILL_LEVEL; }
        @Override public String state() { return "level=" + level; }

        @Override public void syncBlock() {
            CacheBlockDisplay.replaceCacheBlock(CauldronInstance.this.metadata().skull,
                    CacheBlockDisplay.ICacheInfo.of(material().defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, this.level)));
            DisplayInstance.markDirtyBlock(CauldronInstance.this.metadata().position());
        }
    }
    public abstract class StaticLayeredCauldron extends LayeredCauldron {
        private final type type;
        private final Block material;
        private final Map<net.minecraft.world.item.Item, CauldronBlockInteraction> interactions;
        public StaticLayeredCauldron(type type, Block material, Map<net.minecraft.world.item.Item, CauldronBlockInteraction> interactions) {
            this.type = type;
            this.material = material;
            this.interactions = interactions;
        }
        public StaticLayeredCauldron(type type, Block material, Map<net.minecraft.world.item.Item, CauldronBlockInteraction> interactions, JsonObjectOptional json) {
            super(json);
            this.type = type;
            this.material = material;
            this.interactions = interactions;
        }
        @Override public type type() { return type; }
        @Override public Block material() { return material; }
        @Override public Map<net.minecraft.world.item.Item, CauldronBlockInteraction> interactions() { return interactions; }
    }
    public class WaterCauldron extends StaticLayeredCauldron implements CustomTileMetadata.Tickable {
        public WaterCauldron() { super(type.water, Blocks.WATER_CAULDRON, CauldronBlockInteraction.WATER); }
        public WaterCauldron(JsonObjectOptional json) { super(type.water, Blocks.WATER_CAULDRON, CauldronBlockInteraction.WATER, json); }
        @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
            World world = event.getWorld();
            BlockPosition position = event.getPos();
            IBlockData data = world.getBlockState(position.below());
            if (data.is(Blocks.CAMPFIRE) && data.getOptionalValue(BlockCampfire.LIT).filter(v -> v).isPresent()) {
                PotionCauldron cauldron = new PotionCauldron();
                CauldronInstance.this.data(cauldron);
                cauldron.level(level());
                return;
            }
            unBurn(world, position, this);
        }
    }
    public class SnowCauldron extends StaticLayeredCauldron implements CustomTileMetadata.Tickable {
        public SnowCauldron() { super(type.snow, Blocks.POWDER_SNOW_CAULDRON, CauldronBlockInteraction.POWDER_SNOW); }
        public SnowCauldron(JsonObjectOptional json) { super(type.snow, Blocks.POWDER_SNOW_CAULDRON, CauldronBlockInteraction.POWDER_SNOW, json); }
        @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
            unBurn(event.getWorld(), event.getPos(), this);
        }
    }
    public class PotionCauldron extends StaticLayeredCauldron implements CustomTileMetadata.Tickable, CustomTileMetadata.Lootable, BlockDisplay.Displayable {
        private enum State {
            EMPTY,
            WAITING,
            COMBINE,
            RECIPE
        }
        private final List<Toast2<String, Integer>> steps = new ArrayList<>();
        private ItemStack result = null;
        private double timer = 0;
        private State state = State.EMPTY;
        private UUID last_owner = null;

        private static final ReadonlyInventory.ItemList.ItemBoxed<Toast2<String, Integer>> ITEM_BOXED = new ReadonlyInventory.ItemList.ItemBoxed<>() {
            @Override public net.minecraft.world.item.ItemStack of(Toast2<String, Integer> item) {
                return Items.getItemCreator(item.val0).map(v -> v.createItem(item.val1)).map(CraftItemStack::asNMSCopy).orElse(net.minecraft.world.item.ItemStack.EMPTY);
            }
            private static final String EMPTY_MATERIAL = Items.getMaterialKey(Material.AIR);
            @Override public boolean isEmpty(Toast2<String, Integer> item) { return item.val1 <= 0 || EMPTY_MATERIAL.equals(item.val0); }
        };
        public final ReadonlyInventory readonlyInventory = ReadonlyInventory.of(steps, ITEM_BOXED);

        public PotionCauldron() { super(type.potion, Blocks.WATER_CAULDRON, CauldronBlockInteraction.WATER); }
        public PotionCauldron(JsonObjectOptional json) {
            super(type.potion, Blocks.WATER_CAULDRON, CauldronBlockInteraction.WATER, json);
            json.getAsJsonArray("steps")
                    .stream()
                    .flatMap(Collection::stream)
                    .map(JsonElementOptional::getAsJsonObject)
                    .flatMap(Optional::stream)
                    .flatMap(v -> v.getAsString("key").flatMap(key -> v.getAsInt("count").map(count -> Toast.of(key, count))).stream())
                    .forEach(steps::add);
            result = json.getAsString("result").map(ItemUtils::loadItem).orElse(null);
            timer = json.getAsFloat("timer").orElse(0.0F);
            state = json.getAsEnum(State.class, "state").orElse(State.EMPTY);
            refreshDisplay();
        }
        private long old_tick_ms = System.currentTimeMillis();
        private int iterator = 0;

        @Override public void level(int level) {
            int old_level = level();
            super.level(level);
            int new_level = level();
            refreshDisplay();
            if (old_level >= new_level) return;
            dropNotRecipeReset(metadata().location(0.5,0.5,0.5));
        }
        @Override public void onEdit(ICauldron data) {
            dropNotRecipeReset(metadata().location(0.5,0.5,0.5));
        }

        @Override public json.builder.object save() {
            return super.save()
                    .addArray("steps", v -> v.add(steps, kv -> json.object().add("key", kv.val0).add("count", kv.val1)))
                    .add("result", result == null ? null : ItemUtils.saveItem(result))
                    .add("timer", timer)
                    .add("last_owner", last_owner)
                    .add("state", state == null ? null : state.name());
        }

        private Stream<ItemStack> stepItems() {
            return steps
                    .stream()
                    .map(kv -> Items.getItemCreator(kv.val0).map(v -> v.createItem(kv.val1)))
                    .flatMap(Optional::stream)
                    .flatMap(Items::splitOptimize);
        }

        private void dropNotRecipeReset(Location center) {
            if (state != State.RECIPE) dropItems(center);
            reset();
        }
        private void dropItems(Location center) {
            Items.dropItem(center, stepItems().toList());
            steps.clear();
        }
        private void reset() {
            reset(State.EMPTY);
        }
        private void reset(State state) {
            reset(state, true);
        }
        private void reset(State state, boolean isClearStep) {
            this.state = state;
            this.timer = STEP_TIMER;
            if (isClearStep) result = null;
            if (state != State.RECIPE || isClearStep) refreshDisplay();
        }

        private int particleTick = 0;
        private void spawnParticle(Location center) {
            particleTick = (particleTick + 1) % 4;
            if (particleTick != 0) return;
            center.getWorld().playSound(center, Sound.BLOCK_POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, org.bukkit.SoundCategory.BLOCKS, 1, 0.1f);
            center.getWorld().spawnParticle(Particle.WATER_BUBBLE, center.clone().add(0, 0.2 * level(), 0), 3, 0.1, 0, 0.1);
        }
        private boolean isAdded(Item drop) {
            UUID thrower = drop.getThrower();
            if (thrower == null) return false;
            ItemStack item = drop.getItemStack();
            return Items.getGlobalKeyByItem(item)
                    .filter(key -> {
                        if (!Recipes.CAULDRON.getCacheWhitelistKeys().contains(key)) return false;
                        int step_list = steps.size();
                        if (step_list == 0) {
                            steps.add(Toast.of(key, item.getAmount()));
                        } else {
                            Toast2<String, Integer> step = steps.get(step_list - 1);
                            if (key.equals(step.val0)) step.val1 += item.getAmount();
                            else steps.add(Toast.of(key, item.getAmount()));
                        }
                        last_owner = thrower;
                        return true;
                    })
                    .isPresent();
        }
        private boolean checkResource(Location center) {
            boolean step = false;
            for (Item drop : center.getNearbyEntitiesByType(Item.class, 0.5)) {
                if (!isAdded(drop)) continue;
                step = true;
                PacketManager.getEntityHandle(drop).kill();
            }
            return step;
        }

        public ItemStack getResult() {
            switch (state) {
                case COMBINE:
                    if (steps.size() == 0) return Items.createItem("Potion.Clear_Water").orElse(null);
                    break;
                case RECIPE:
                    return result;
                default: return null;
            }
            return null;
        }

        @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
            long tick_ms = System.currentTimeMillis();
            double timeDelta = (tick_ms - old_tick_ms) / 1000.0;
            old_tick_ms = tick_ms;

            World world = event.getWorld();
            BlockPosition position = event.getPos();

            switch (state) {
                case RECIPE, COMBINE:
                    if ((iterator = (iterator + 1) % 40) == 0) {
                        world.getEntitiesOfClass(Entity.class, new AxisAlignedBB(position)).forEach(entity -> entity.hurt(entity.damageSources().onFire(), 1));
                    }
                    break;
                default:
                    break;
            }
            IBlockData campfire = world.getBlockState(position.below());
            if (!campfire.is(Blocks.CAMPFIRE) || campfire.getOptionalValue(BlockCampfire.LIT).filter(v -> v).isEmpty()) {
                PotionCauldron cauldron = new PotionCauldron();
                CauldronInstance.this.data(cauldron);
                cauldron.level(level());
                return;
            }
            switch (state) {
                case EMPTY: reset(State.WAITING); break;
                case WAITING: {
                    timer -= timeDelta;
                    int size = steps.size();
                    if (size != 0 && timer > STEP_TIMER - 10) {
                        if (checkResource(metadata.location(0.5, 0.5, 0.5))) {
                            reset(State.WAITING, false);
                            break;
                        }
                    }
                    if (timer <= 0) {
                        Perms.ICanData canData = Perms.getCanData(last_owner);
                        Recipes.CAULDRON.getAllRecipes(canData)
                                .filter(v -> v.matches(readonlyInventory, world))
                                .findFirst()
                                .ifPresentOrElse(recipe -> {
                                    this.result = recipe.assemble(readonlyInventory, world.registryAccess(), IOutputVariable.of(last_owner)).asBukkitCopy();
                                    Perms.onRecipeUse(recipe, last_owner, canData);
                                    LevelModule.onCraft(last_owner, recipe.getId());
                                    this.last_owner = null;

                                    Location center = metadata.location(0.5,0.5,0.5);
                                    Items.dropItem(center, recipe.getRemainingItems(readonlyInventory).stream().filter(v -> !v.isEmpty()).map(net.minecraft.world.item.ItemStack::asBukkitCopy).toList());

                                    this.steps.clear();
                                    refreshDisplay();

                                    reset(State.RECIPE, false);
                                }, () -> reset(State.COMBINE));
                    }
                    break;
                }
                case COMBINE: {
                    Location center = metadata.location(0.5,0.5,0.5);
                    spawnParticle(center);
                    timer -= timeDelta;
                    if (checkResource(center)) reset(State.WAITING, false);
                    else if (timer <= 0) {
                        reset(State.COMBINE, false);
                        CauldronInstance.this.decreaseLevel();
                        return;
                    }
                    break;
                }
                case RECIPE: {
                    spawnParticle(metadata.location(0.5,0.5,0.5));
                    timer -= timeDelta;

                    if (timer <= 0) {
                        reset(State.RECIPE, false);
                        CauldronInstance.this.decreaseLevel();
                        return;
                    }
                    break;
                }
            }
            saveData();
        }
        @Override public void onLoot(CustomTileMetadata metadata, PopulateLootEvent event) {
            event.addItems(stepItems().toList());
        }

        /*private static final Builder builder = lime.models.builder(EntityTypes.ARMOR_STAND)
                .nbt(() -> {
                    EntityArmorStand stand = new EntityArmorStand(EntityTypes.ARMOR_STAND, lime.MainWorld.getHandle());
                    stand.setNoBasePlate(true);
                    stand.setSmall(true);
                    stand.setInvisible(true);
                    stand.setInvulnerable(true);
                    stand.setMarker(true);
                    stand.setHeadPose(new Vector3f(0, 0, 0));
                    stand.setYRot(0);
                    stand.setXRot(0);
                    return stand;
                });*/
        private static final ItemBuilder builder = lime.models.builder().item()
                .transform(new Transformation(
                        new Vector3f(0, 0.001f, 0),
                        null,
                        new Vector3f(1f, 1f, 1f),
                        null
                ));
        private final LockToast1<IBuilder> model = Toast.lock(null);

        private void refreshDisplay() {
            this.model.set0(Items.getOptional(ThirstSetting.class, result)
                    .map(v -> v.color)
                    .map(color -> {
                        int cmd;
                        switch (level()) {
                            default -> { return null; }
                            case 1 -> cmd = 8080001;
                            case 2 -> cmd = 8080002;
                            case 3 -> cmd = 8080003;
                        }
                        ItemStack item = new ItemStack(Material.LEATHER_CHESTPLATE);
                        LeatherArmorMeta meta = (LeatherArmorMeta)item.getItemMeta();
                        meta.setColor(color);
                        meta.setCustomModelData(cmd);
                        item.setItemMeta(meta);
                        return builder.item(CraftItemStack.asNMSCopy(item));
                    })
                    .orElse(null));
            CauldronInstance.this.metadata()
                .list(DisplayInstance.class)
                .forEach(DisplayInstance::variableDirty);
        }
        @Override public Optional<IModelBlock> onDisplayAsync(Player player, World world, BlockPosition position, IBlockData data) {
            IBuilder model = this.model.get0();
            return model == null ? Optional.empty() : Optional.of(IModelBlock.of(null, model, BlockDisplay.getChunkSize(10), Double.POSITIVE_INFINITY));
        }
    }
    public class LavaCauldron extends ICauldron implements CustomTileMetadata.Tickable {
        @Override public json.builder.object save() { return json.object(); }
        @Override public type type() { return type.lava; }
        @Override public boolean isFull() { return true; }
        @Override public Map<net.minecraft.world.item.Item, CauldronBlockInteraction> interactions() { return CauldronBlockInteraction.LAVA; }
        @Override public String state() { return ""; }

        @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
            event.getWorld().getEntitiesOfClass(Entity.class, new AxisAlignedBB(event.getPos())).forEach(Entity::isInLava);
        }

        @Override public void syncBlock() {
            CacheBlockDisplay.replaceCacheBlock(CauldronInstance.this.metadata().skull,
                    CacheBlockDisplay.ICacheInfo.of(Blocks.LAVA_CAULDRON.defaultBlockState()));
            DisplayInstance.markDirtyBlock(CauldronInstance.this.metadata().position());
        }
    }

    @Override public void read(JsonObjectOptional json) {
        data = json.getAsString("type").flatMap(type::tryValueOf).orElse(type.empty).create(this, json);
        data.syncBlock();
    }
    @Override public json.builder.object write() {
        return json.object()
                .add("type", data.type())
                .add(data.save());
    }

    public boolean decreaseLevel() {
        if (!(data instanceof LayeredCauldron layered)) return false;
        int level = layered.level() - 1;
        if (level < LayeredCauldronBlock.MIN_FILL_LEVEL) data(new EmptyCauldron());
        else layered.level(level);
        return true;
    }
    public boolean increaseLevel() {
        if (!(data instanceof LayeredCauldron layered)) return false;
        if (layered.isFull()) return false;
        layered.level(layered.level() + 1);
        return true;
    }

    private void unBurn(World world, BlockPosition position, ICauldron cauldron) {
        world.getEntitiesOfClass(Entity.class, new AxisAlignedBB(position), Entity::isOnFire).forEach(v -> {
            if (decreaseLevel())
                v.clearFire();
        });
    }

    @Override public VoxelShape onShape(CustomTileMetadata metadata, BlockSkullShapeInfo event) {
        return CAULDRON_SHAPE;
    }
    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        net.minecraft.world.item.ItemStack itemStack = event.player().getItemInHand(event.hand());
        CauldronBlockInteraction cauldronInteraction = data.interactions().get(itemStack.getItem());
        if (cauldronInteraction != null) {
            EnumInteractionResult result = cauldronInteraction.interact(this, itemStack, event);
            if (result.consumesAction()) return result;
            else return ContainerWorkbenchBook.open(event.player(), metadata, Recipes.CAULDRON, Recipes.CAULDRON.getAllRecipes());
        }
        return EnumInteractionResult.PASS;
    }
}





















