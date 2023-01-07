package org.lime.gp.block.component.data.anvil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.Vector3f;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.TileInventory;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerAnvil;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockAnvil;
import net.minecraft.world.level.block.BlockSkullEventInteract;
import net.minecraft.world.level.block.BlockSkullEventShape;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntitySkullEventTick;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftMagicNumbers;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.data.recipe.IRecipe;
import org.lime.gp.block.component.display.BlockDisplay;
import org.lime.gp.display.Model;
import org.lime.gp.display.transform.LocalLocation;
import org.lime.gp.extension.Cooldown;
import org.lime.gp.item.Items;
import org.lime.gp.item.Settings;
import org.lime.gp.lime;
import org.lime.gp.module.DrawText;
import org.lime.gp.module.PopulateLootEvent;
import org.lime.gp.player.perm.Perms;
import org.lime.json.JsonElementOptional;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class AnvilInstance extends BlockInstance implements CustomTileMetadata.Shapeable, CustomTileMetadata.Lootable, CustomTileMetadata.Tickable, BlockDisplay.Displayable, CustomTileMetadata.Interactable, CustomTileMetadata.Damageable {
    private static final VoxelShape BASE = net.minecraft.world.level.block.Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);
    private static final VoxelShape X_LEG1 = net.minecraft.world.level.block.Block.box(3.0, 4.0, 4.0, 13.0, 5.0, 12.0);
    private static final VoxelShape X_LEG2 = net.minecraft.world.level.block.Block.box(4.0, 5.0, 6.0, 12.0, 10.0, 10.0);
    private static final VoxelShape X_TOP = net.minecraft.world.level.block.Block.box(0.0, 10.0, 3.0, 16.0, 16.0, 13.0);
    private static final VoxelShape Z_LEG1 = net.minecraft.world.level.block.Block.box(4.0, 4.0, 3.0, 12.0, 5.0, 13.0);
    private static final VoxelShape Z_LEG2 = net.minecraft.world.level.block.Block.box(6.0, 5.0, 4.0, 10.0, 10.0, 12.0);
    private static final VoxelShape Z_TOP = net.minecraft.world.level.block.Block.box(3.0, 10.0, 0.0, 13.0, 16.0, 16.0);
    private static final VoxelShape X_AXIS_AABB = VoxelShapes.or(BASE, X_LEG1, X_LEG2, X_TOP);
    private static final VoxelShape Z_AXIS_AABB = VoxelShapes.or(BASE, Z_LEG1, Z_LEG2, Z_TOP);

    public AnvilInstance(ComponentDynamic<?, ?> component, CustomTileMetadata metadata) {
        super(component, metadata);
    }

    public enum AnvilType {
        anvil(Material.ANVIL),
        chipped_anvil(Material.CHIPPED_ANVIL),
        damaged_anvil(Material.DAMAGED_ANVIL);

        public final Material type;

        AnvilType(Material type) {
            this.type = type;
        }
        public ItemStack createDrop() {
            return new ItemStack(type);
        }
        public VoxelShape getShape(Direction direction) {
            return direction.direction.getAxis() == EnumDirection.EnumAxis.X ? X_AXIS_AABB : Z_AXIS_AABB;
        }
        public IBlockData getData(Direction direction) {
            return CraftMagicNumbers
                    .getBlock(type)
                    .defaultBlockState()
                    .setValue(BlockAnvil.FACING, direction.direction);
        }

        public static Optional<AnvilType> tryValueOf(String name) {
            try { return Optional.of(AnvilType.valueOf(name)); }
            catch (Exception e) { return Optional.empty(); }
        }
    }
    public enum Direction {
        NORTH(EnumDirection.NORTH),
        SOUTH(EnumDirection.SOUTH),
        WEST(EnumDirection.WEST),
        EAST(EnumDirection.EAST);

        public final EnumDirection direction;

        Direction(EnumDirection direction) {
            this.direction = direction;
        }

        public static Optional<Direction> tryValueOf(String name) {
            try { return Optional.of(Direction.valueOf(name)); }
            catch (Exception e) { return Optional.empty(); }
        }
        public static Direction of(BlockFace face) {
            return switch (face) {
                case SOUTH -> SOUTH;
                case WEST -> WEST;
                case EAST -> EAST;
                default -> NORTH;
            };
        }
    }

    private AnvilType type = AnvilType.anvil;
    private Direction direction = Direction.NORTH;

    private List<ItemStack> items = new ArrayList<>();
    private int clicks;
    private Model model = null;
    private static LocalLocation ofHeight(int height) {
        return new LocalLocation(0, -0.4 + height * 0.025, -0.5, (float) system.rand(-2.0, 2.0), 0);
    }
    private static final Model.Builder builder_item = Model.builder(EntityTypes.ARMOR_STAND)
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

    public AnvilType type() { return type; }
    public AnvilInstance type(AnvilType type) {
        this.type = type;
        saveData();
        return this;
    }
    public void next() {
        AnvilType nextType;
        switch (type()) {
            case anvil: nextType = AnvilType.chipped_anvil; break;
            case chipped_anvil: nextType = AnvilType.damaged_anvil; break;
            case damaged_anvil:
                List<ItemStack> items = new ArrayList<>(this.items);
                this.items.clear();
                Items.dropBlockItem(metadata().location(), items);
                metadata().setAir();
                return;
            default: return;
        }
        type(nextType);
    }
    public Direction direction() { return direction; }
    public AnvilInstance direction(Direction direction) {
        this.direction = direction;
        saveData();
        return this;
    }

    private boolean tryAddItem(ItemStack item) {
        int maxStackSize = item.getMaxStackSize();
        int amount = item.getAmount();
        for (ItemStack _item : items) {
            if (_item.isSimilar(item)) {
                int size = _item.getAmount();
                if (maxStackSize - size == 0) continue;
                if (amount + size > maxStackSize) {
                    int _amount = maxStackSize - size;
                    item.setAmount(amount - _amount);
                    amount = _amount;
                } else {
                    item.setAmount(0);
                }
                _item.setAmount(size + amount);
                clicks = 0;
                return true;
            }
        }
        if (items.size() >= AnvilLoader.MAX_STACK) return false;
        if (!AnvilLoader.isWhitelist(item.getType())) return false;
        items.add(item.clone());
        item.setAmount(0);
        clicks = 0;
        return true;
    }
    private void updateModel() {
        if (items.isEmpty()) {
            model = null;
            return;
        }
        Model.Builder builder = Model.builder();
        for (int i = 0; i < items.size(); i++) builder = builder.addChild(builder_item.local(ofHeight(i)).addEquipment(EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(items.get(i))));
        model = builder.build();
    }

    private static final IChatBaseComponent CONTAINER_TITLE = new ChatMessage("container.repair");
    private static ContainerAccess at(final net.minecraft.world.level.World world, final BlockPosition pos) {
        return new ContainerAccess(){
            @Override public net.minecraft.world.level.World getWorld() { return world; }
            @Override public BlockPosition getPosition() { return pos; }
            @Override public <T> Optional<T> evaluate(BiFunction<World, BlockPosition, T> getter) { return Optional.empty(); }
            @Override public void execute(BiConsumer<World, BlockPosition> function) { function.accept(world, pos); }
        };
    }
    private static ITileInventory getInventory(World world, BlockPosition pos) {
        return new TileInventory((syncId, inventory, player) -> new ContainerAnvil(syncId, inventory, at(world, pos)) {
            @Override protected boolean isValidBlock(IBlockData state) {
                return super.isValidBlock(state) || state.is(Blocks.SKELETON_SKULL);
            }
        }, CONTAINER_TITLE);
    }
    private static int hurt(ItemStack damageItem) {
        return system.rand_is(switch (damageItem.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.DURABILITY)) {
            case 0 -> 0;
            case 1 -> 0.15;
            case 2 -> 0.3;
            case 3 -> 0.45;
            default -> 0.6;
        }) ? 0 : 1;
    }
    private static List<IRecipe> canRecipeList(Perms.ICanData canData) {
        List<IRecipe> list = new ArrayList<>();
        AnvilLoader.recipeList.forEach((k,v) -> {
            if (canData.isCanAnvil(k))
                list.add(v);
        });
        return list;
    }

    @Override public void read(JsonObjectOptional json) {
        type = json.getAsString("type").flatMap(AnvilType::tryValueOf).orElse(AnvilType.anvil);
        direction = json.getAsString("direction").flatMap(Direction::tryValueOf).orElse(Direction.NORTH);
        clicks = json.getAsInt("clicks").orElse(0);
        items = json.getAsJsonArray("items")
                .stream()
                .flatMap(Collection::stream)
                .map(JsonElementOptional::getAsString)
                .map(v -> v.orElse(null))
                .map(system::loadItem)
                .collect(Collectors.toList());
        updateModel();
    }
    @Override public system.json.builder.object write() {

        return system.json.object()
                .add("type", type)
                .add("direction", direction)
                .addArray("items", v -> v.add(items, system::saveItem))
                .add("clicks", clicks);
    }
    @Override public void onShape(CustomTileMetadata metadata, BlockSkullEventShape event) {
        event.setResult(type.getShape(direction));
    }
    @Override public void onLoot(CustomTileMetadata metadata, PopulateLootEvent event) {
        event.addItem(type.createDrop());
        event.addItems(this.items);
        this.items.clear();
        saveData();
        updateModel();
    }
    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullEventTick event) {
        BlockPosition position = event.getPos();
        Location location = new Location(event.getWorld().getWorld(), position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5);
        boolean edited = false;
        for (Item item : location.clone().add(0, 0.5, 0).getNearbyEntitiesByType(Item.class, 0.5)) {
            ItemStack itemStack = item.getItemStack();
            if (itemStack.getAmount() == 0) continue;
            edited = tryAddItem(itemStack) || edited;
        }
        if (edited) {
            saveData();
            updateModel();
        }
        //TimeoutData.put(unique(), AnvilDisplay.AnvilData.class, new AnvilDisplay.AnvilData(new Location(event.getWorld().getWorld(), position.getX(), position.getY(), position.getZ()), new ArrayList<>(__items)));
    }
    @Override public Optional<BlockDisplay.IBlock> onDisplay(Player player, World world, BlockPosition position, IBlockData data) {
        return model == null
                ? Optional.of(BlockDisplay.IBlock.of(type.getData(direction)))
                : Optional.of(BlockDisplay.IModelBlock.of(type.getData(direction), model, 5));
    }
    @Override public void onInteract(CustomTileMetadata metadata, BlockSkullEventInteract event) {
        event.getPlayer().openMenu(getInventory(metadata.skull.getLevel(), metadata.skull.getBlockPos()));
        event.getPlayer().awardStat(StatisticList.INTERACT_WITH_ANVIL);
        event.setResult(EnumInteractionResult.CONSUME);
    }
    @Override public void onDamage(CustomTileMetadata metadata, BlockDamageEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            if (this.items.size() == 0) return;
            List<ItemStack> items = new ArrayList<>(this.items);
            this.items.clear();
            Items.dropGiveItem(player, items, true);
            saveData();
            updateModel();
            return;
        }
        ItemStack hummer = event.getItemInHand();
        if (!Items.has(Settings.HummerSetting.class, hummer)) return;
        if (Cooldown.hasCooldown(player.getUniqueId(), "anvil.hummer")) return;
        Cooldown.setCooldown(player.getUniqueId(), "anvil.hummer", 1);
        ItemMeta meta = hummer.getItemMeta();
        Damageable damageable = (Damageable)meta;
        int value = damageable.getDamage() + hurt(hummer);
        if (value >= hummer.getType().getMaxDurability()) {
            hummer.setAmount(0);
        } else {
            damageable.setDamage(value);
            hummer.setItemMeta(meta);
        }
        List<IRecipe> recipeList = canRecipeList(Perms.getCanData(player.getUniqueId()));
        if (recipeList.size() == 0) return;
        clicks++;
        Location location = metadata().location(0.5,0.5,0.5);
        Particle.BLOCK_DUST
                .builder()
                .location(location.clone().add(0, 0.1, 0))
                .offset(0.1, 0.1, 0.1)
                .count(5)
                .data(Material.ANVIL.createBlockData())
                .force(false)
                .allPlayers()
                .spawn();
        location.getWorld().playSound(location, Sound.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.1f, 0.75f);

        List<ItemStack> items = new ArrayList<>(this.items);
        boolean can = false;
        for (IRecipe recipe : recipeList) {
            ItemStack item = recipe.checkCraft(items);
            if (item == null || item.getType().isAir()) continue;
            can = true;
            if (recipe.getClicks() >= clicks) continue;
            this.items.removeAll(items);
            Items.dropGiveItem(player, item, true);
            lime.logToFile("log_anvil", String.join("", Arrays.asList(
                    "[{time}] ",
                    player.getName(),
                    "(",
                    player.getUniqueId().toString(),
                    ") ",
                    system.getString(location.toVector()),
                    ": ",
                    item.toString()
            )));
            location.getWorld().playSound(location, Sound.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 0.1f, 0.75f);
            if (system.rand_is(0.12)) next();
            clicks = 0;
            saveData();
            updateModel();
            break;
        }

        if (!can) DrawText.show(DrawText.IShow.create(player, metadata().location(0.5, 0.4, 0.5), Component.text("✖").color(TextColor.color(0xFF0000)), 0.5));
        if (clicks > AnvilLoader.MAX_CLICKS) clicks = 0;
        saveData();
    }
}
















