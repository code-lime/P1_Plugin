package org.lime.gp.block.component.data.cauldron;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.TileEntitySkullEventTick;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftMagicNumbers;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.display.BlockDisplay;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.item.Items;
import org.lime.gp.module.PopulateLootEvent;
import org.lime.gp.module.TimeoutData;
import org.lime.gp.player.perm.Perms;
import org.lime.json.JsonElementOptional;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.util.*;
import java.util.stream.Stream;

public class CauldronInstance extends BlockInstance implements CustomTileMetadata.Childable, CustomTileMetadata.Shapeable, CustomTileMetadata.Interactable {
    private static final double STEP_TIMER = 0.25 * 60;

    private static final IBlockData LAVA_CAULDRON =  CraftMagicNumbers
            .getBlock(Material.LAVA_CAULDRON)
            .defaultBlockState();

    private static final VoxelShape CAULDRON_INSIDE = AbstractCauldronBlock.box(2.0, 4.0, 2.0, 14.0, 16.0, 14.0);
    private static final VoxelShape CAULDRON_SHAPE = VoxelShapes.join(VoxelShapes.block(), VoxelShapes.or(AbstractCauldronBlock.box(0.0, 0.0, 4.0, 16.0, 3.0, 12.0), AbstractCauldronBlock.box(4.0, 0.0, 0.0, 12.0, 3.0, 16.0), AbstractCauldronBlock.box(2.0, 0.0, 2.0, 14.0, 3.0, 14.0), CAULDRON_INSIDE), OperatorBoolean.ONLY_FIRST);

    private ICauldron data = new EmptyCauldron();

    public CauldronInstance(ComponentDynamic<?, ?> component, CustomTileMetadata metadata) { super(component, metadata); }

    public ICauldron data() { return data; }
    public <T extends ICauldron>CauldronInstance data(T data) {
        if (this.data == data) return this;
        this.data.onEdit(data);
        this.data = data;
        saveData();
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

        private final system.Func1<CauldronInstance, ICauldron> creator;
        private final system.Func2<CauldronInstance, JsonObjectOptional, ICauldron> reader;

        type(system.Func1<CauldronInstance, ICauldron> creator) { this(creator, (v,j) -> creator.invoke(v)); }
        type(system.Func1<CauldronInstance, ICauldron> creator, system.Func2<CauldronInstance, JsonObjectOptional, ICauldron> reader) {
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
        public abstract system.json.builder.object save();
        public abstract boolean isFull();
        public abstract Map<net.minecraft.world.item.Item, CauldronBlockInteraction> interactions();
        public abstract String state();
        @Override public String toString() { return type() + "[" + state() + "]"; }
        public void onEdit(ICauldron cauldron) {}
    }
    public class EmptyCauldron extends ICauldron implements BlockDisplay.Displayable {
        @Override public system.json.builder.object save() { return system.json.object(); }
        @Override public type type() { return type.empty; }
        @Override public boolean isFull() { return false; }
        @Override public Map<net.minecraft.world.item.Item, CauldronBlockInteraction> interactions() { return CauldronBlockInteraction.EMPTY; }
        @Override public String state() { return ""; }

        @Override public Optional<BlockDisplay.IBlock> onDisplay(Player player, World world, BlockPosition position, IBlockData data) {
            return Optional.of(BlockDisplay.IBlock.of(Blocks.CAULDRON.defaultBlockState()));
        }
    }
    public abstract class LayeredCauldron extends ICauldron implements BlockDisplay.Displayable {
        private int level;
        public void level(int level) {
            this.level = Math.max(Math.min(level, LayeredCauldronBlock.MAX_FILL_LEVEL), LayeredCauldronBlock.MIN_FILL_LEVEL);
            CauldronInstance.this.saveData();
        }
        public int level() { return level; }
        private LayeredCauldron(int level) { this.level = level; }
        public LayeredCauldron() { this(LayeredCauldronBlock.MAX_FILL_LEVEL); }
        public LayeredCauldron(JsonObjectOptional json) { this(json.getAsInt("level").orElse(LayeredCauldronBlock.MAX_FILL_LEVEL)); }
        public abstract Block material();
        @Override public system.json.builder.object save() { return system.json.object().add("level", level); }
        @Override public boolean isFull() { return level == LayeredCauldronBlock.MAX_FILL_LEVEL; }
        @Override public String state() { return "level=" + level; }

        @Override public Optional<BlockDisplay.IBlock> onDisplay(Player player, World world, BlockPosition position, IBlockData data) {
            return Optional.of(BlockDisplay.IBlock.of(material().defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, level)));
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
        @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullEventTick event) {
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
        @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullEventTick event) {
            unBurn(event.getWorld(), event.getPos(), this);
        }
    }
    public class PotionCauldron extends StaticLayeredCauldron implements CustomTileMetadata.Tickable, CustomTileMetadata.Lootable {
        private final List<LoadedStep> applySteps = new ArrayList<>();
        private LoadedStep loadingStep = null;
        private Recipe loadedRecipe = null;
        private double timer = 0;
        private State state = State.EMPTY;
        private CauldronDisplay.CauldronData display = null;

        public PotionCauldron() { super(type.potion, Blocks.WATER_CAULDRON, CauldronBlockInteraction.WATER); }
        public PotionCauldron(JsonObjectOptional json) {
            super(type.potion, Blocks.WATER_CAULDRON, CauldronBlockInteraction.WATER, json);
            json.getAsJsonArray("steps")
                    .stream()
                    .flatMap(Collection::stream)
                    .map(JsonElementOptional::getAsJsonObject)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(JsonObjectOptional::base)
                    .map(LoadedStep::parse)
                    .forEach(applySteps::add);
            loadingStep = json.getAsJsonObject("loadingStep").map(JsonObjectOptional::base).map(LoadedStep::parse).orElse(null);
            loadedRecipe = json.getAsString("loadedRecipe").map(CauldronLoader::getRecipe).orElse(null);
            timer = json.getAsFloat("timer").orElse(0.0F);
            state = json.getAsEnum(State.class, "state").orElse(State.EMPTY);
            refreshDisplay();
        }
        private long old_tick_ms = System.currentTimeMillis();
        private int iterator = 0;
        private void refreshDisplay() {
            display = Optional.ofNullable(loadedRecipe)
                    .map(recipe -> recipe.color)
                    .flatMap(color -> CauldronDisplay.CauldronData.of(metadata().location(), level(), color))
                    .orElse(null);
        }

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

        @Override public system.json.builder.object save() {
            return super.save()
                    .addArray("steps", v -> v.add(applySteps, LoadedStep::toJson))
                    .add("loadingStep", loadingStep == null ? null : loadingStep.toJson())
                    .add("loadedRecipe", loadedRecipe == null ? null : loadedRecipe.key)
                    .add("timer", timer)
                    .add("state", state == null ? null : state.name());
        }

        private void dropNotRecipeReset(Location center) {
            if (state != State.RECIPE) dropItems(center);
            reset();
        }
        private void dropItems(Location center) {
            applySteps.forEach(step -> step.drop(center));
            if (loadingStep != null) {
                loadingStep.drop(center);
                loadingStep = null;
            }
            applySteps.clear();
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
            if (isClearStep) {
                loadingStep = null;
                loadedRecipe = null;
            }
            if (state != State.RECIPE || isClearStep) refreshDisplay();
        }
        private void spawnParticle(Location center) {
            center.getWorld().playSound(center, Sound.BLOCK_POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, org.bukkit.SoundCategory.BLOCKS, 1, 0.1f);
            center.getWorld().spawnParticle(Particle.WATER_BUBBLE, center.clone().add(0, 0.2 * level(), 0), 3, 0.1, 0, 0.1);
        }
        private boolean isAdded(Item drop) {
            UUID thrower = drop.getThrower();
            if (thrower == null) return false;
            if (!Perms.getCanData(thrower).isCanCauldron()) return false;
            ItemStack item = drop.getItemStack();
            if (loadingStep == null) {
                for (Step iitem : CauldronLoader.iitems) {
                    if (iitem.test(item)) {
                        loadingStep = new LoadedStep(iitem.item, item);
                        return true;
                    }
                }
            }
            else if (loadingStep.iitem.compare(item)) {
                loadingStep.addItem(item);
                return true;
            }
            return false;
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
                    if (applySteps.size() == 0 && CauldronLoader.waterRecipe != null) return Items.createItem(CauldronLoader.waterRecipe.result).orElseThrow();
                    break;
                case RECIPE:
                    if (loadedRecipe != null) return Items.createItem(loadedRecipe.result).orElseThrow();
                    break;
            }
            return null;
        }

        @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullEventTick event) {
            long tick_ms = System.currentTimeMillis();
            double timeDelta = (tick_ms - old_tick_ms) / 1000.0;
            old_tick_ms = tick_ms;

            World world = event.getWorld();
            BlockPosition position = event.getPos();

            switch (state) {
                case RECIPE, COMBINE:
                    if ((iterator = (iterator + 1) % 40) == 0) {
                        world.getEntitiesOfClass(Entity.class, new AxisAlignedBB(position)).forEach(entity -> entity.hurt(DamageSource.IN_FIRE, 1));
                    }
                    break;
            }
            IBlockData campfire = world.getBlockState(position.below());
            if (!campfire.is(Blocks.CAMPFIRE) || campfire.getOptionalValue(BlockCampfire.LIT).filter(v -> v).isEmpty()) {
                PotionCauldron cauldron = new PotionCauldron();
                CauldronInstance.this.data(cauldron);
                cauldron.level(level());
                return;
            }
            TimeoutData.put(unique(), CauldronDisplay.CauldronData.class, display);
            switch (state) {
                case EMPTY: reset(State.WAITING); break;
                case WAITING: {
                    timer -= timeDelta;
                    int size = applySteps.size();
                    if (size != 0 && timer > STEP_TIMER - 10) {
                        if (checkResource(metadata.location(0.5, 0.5, 0.5))) {
                            reset(State.WAITING, false);
                            break;
                        }
                    }
                    if (timer <= 0) {
                        if (loadingStep != null) {
                            if (size > 0) {
                                LoadedStep lastStep = applySteps.get(size - 1);
                                if (lastStep.iitem.compare(loadingStep.iitem)) {
                                    lastStep.addItems(loadingStep.getItems());
                                    loadingStep = null;
                                }
                            }
                            if (loadingStep != null) {
                                applySteps.add(loadingStep);
                                loadingStep = null;
                            }
                        }

                        Recipe recipe = CauldronLoader.findRecipe(applySteps);
                        if (recipe != null) {
                            applySteps.clear();
                            loadedRecipe = recipe;
                            refreshDisplay();

                            Location center = metadata.location(0.5,0.5,0.5);
                            loadedRecipe.items.forEach(item -> Items.dropItem(center, item.output.create(item.count)));

                            reset(State.RECIPE, false);
                        } else {
                            reset(State.COMBINE);
                        }
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
            applySteps.forEach(step -> event.addItems(step.getItems()));
            if (loadingStep != null) event.addItems(loadingStep.getItems());
        }
    }
    public class LavaCauldron extends ICauldron implements CustomTileMetadata.Tickable, BlockDisplay.Displayable {
        @Override public system.json.builder.object save() { return system.json.object(); }
        @Override public type type() { return type.lava; }
        @Override public boolean isFull() { return true; }
        @Override public Map<net.minecraft.world.item.Item, CauldronBlockInteraction> interactions() { return CauldronBlockInteraction.LAVA; }
        @Override public String state() { return ""; }
        @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullEventTick event) {
            event.getWorld().getEntitiesOfClass(Entity.class, new AxisAlignedBB(event.getPos())).forEach(Entity::isInLava);
        }
        @Override public Optional<BlockDisplay.IBlock> onDisplay(Player player, World world, BlockPosition position, IBlockData data) {
            return Optional.of(BlockDisplay.IBlock.of(LAVA_CAULDRON));
        }
    }

    @Override public void read(JsonObjectOptional json) {
        data = json.getAsString("type").flatMap(type::tryValueOf).orElse(type.empty).create(this, json);
    }
    @Override public system.json.builder.object write() {
        return system.json.object()
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

    @Override public void onShape(CustomTileMetadata metadata, BlockSkullEventShape event) {
        event.setResult(CAULDRON_SHAPE);
    }
    @Override public void onInteract(CustomTileMetadata metadata, BlockSkullEventInteract event) {
        net.minecraft.world.item.ItemStack itemStack = event.getPlayer().getItemInHand(event.getHand());
        CauldronBlockInteraction cauldronInteraction = data.interactions().get(itemStack.getItem());
        if (cauldronInteraction == null) return;
        event.setResult(cauldronInteraction.interact(this, itemStack, event));
    }
}





















