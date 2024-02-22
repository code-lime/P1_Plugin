package org.lime.gp.block.component.data;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import org.apache.commons.collections4.CollectionUtils;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.display.IDisplayVariable;
import org.lime.gp.block.component.list.ClickerComponent;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.list.DisplayComponent;
import org.lime.gp.craft.book.ContainerWorkbenchBook;
import org.lime.gp.craft.slot.output.IOutputVariable;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.extension.inventory.ReadonlyInventory;
import org.lime.gp.craft.recipe.ClickerRecipe;
import org.lime.gp.craft.book.Recipes;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.*;
import org.lime.gp.module.DrawText;
import org.lime.gp.module.loot.PopulateLootEvent;
import org.lime.gp.player.level.LevelModule;
import org.lime.gp.player.perm.Perms;
import org.lime.gp.sound.Sounds;
import org.lime.json.JsonElementOptional;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;
import org.lime.system.utils.ItemUtils;
import org.lime.system.utils.RandomUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ClickerInstance extends BlockInstance implements CustomTileMetadata.Lootable, CustomTileMetadata.Tickable, /*BlockDisplay.Displayable, */CustomTileMetadata.Damageable, CustomTileMetadata.Interactable, IDisplayVariable {
    @Override public ClickerComponent component() { return (ClickerComponent)super.component(); }
    public ClickerInstance(ClickerComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
    }

    private static final int MAX_STACK = 7;

    private final List<ItemStack> items = new ArrayList<>();
    private final ReadonlyInventory readonlyInventory = ReadonlyInventory.ofBukkit(items, metadata().location());
    private int clicks;
    private int damage;
    //private final LockToast1<IBuilder> model = system.<IBuilder>toast(null).lock();
    /*private static LocalLocation ofHeight(int height) {
        return new LocalLocation(0.5, -0.4 + height * 0.025, 0, 0, 0);
    }*/
    /*private static final Builder builder_item = lime.models.builder(EntityTypes.ARMOR_STAND)
            .nbt(() -> {
                EntityArmorStand stand = new EntityArmorStand(EntityTypes.ARMOR_STAND, lime.MainWorld.getHandle());
                stand.setNoBasePlate(true);
                stand.setSmall(true);
                stand.setInvisible(true);
                stand.setInvulnerable(true);
                stand.setMarker(true);
                stand.setHeadPose(new Vector3f(90, 90, 0));
                return stand;
            });*/

    private boolean tryAddItem(ItemStack item) {
        int maxStackSize = item.getMaxStackSize();
        int amount = item.getAmount();
        if (!items.isEmpty()) {
            ItemStack _item = items.get(items.size() - 1);
            if (_item.isSimilar(item)) {
                int size = _item.getAmount();
                if (maxStackSize - size != 0) {
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
        }

        List<String> clickerTypes = component().types;

        if (items.size() >= MAX_STACK) return false;
        if (Items.getGlobalKeyByItem(item)
                .filter(item_key -> Recipes.CLICKER.getCacheWhitelistKeys().contains(item_key))
                .filter(item_key -> Recipes.CLICKER.getAllRecipes().stream().filter(v -> clickerTypes.contains(v.clickerType)).flatMap(ClickerRecipe::getWhitelistKeys).anyMatch(v -> v.equals(item_key)))
                .isEmpty()) return false;
        items.add(item.clone());
        item.setAmount(0);
        clicks = 0;
        return true;
    }
    private void updateModel() {
        syncDisplayVariable(metadata());
        /*if (items.isEmpty()) {
            model.set0(null);
            metadata()
                .list(DisplayInstance.class)
                .forEach(DisplayInstance::variableDirty);
            return;
        }
        NoneBuilder builder = lime.models.builder().none();
        ClickerComponent component = component();
        Transformation offsetFore = component.show;
        Transformation offsetBack = component.show;
        boolean isFore = true;
        for (ItemStack item : items) {
            builder = builder.addChild(TableDisplaySetting.builderItem(item, isFore ? offsetFore : offsetBack, TableDisplaySetting.TableType.clicker, component.type));
            isFore = !isFore;
            if (isFore) offsetFore = offsetFore.compose(component.step_fore);
            else offsetBack = offsetBack.compose(component.step_back);
        }
        model.set0(builder);
        metadata()
            .list(DisplayInstance.class)
            .forEach(DisplayInstance::variableDirty);*/
    }

    private static int hurt(ItemStack damageItem) {
        return RandomUtils.rand_is(switch (damageItem.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.DURABILITY)) {
            case 0 -> 0;
            case 1 -> 0.15;
            case 2 -> 0.3;
            case 3 -> 0.45;
            default -> 0.6;
        }) ? 0 : 1;
    }

    @Override public void read(JsonObjectOptional json) {
        clicks = json.getAsInt("clicks").orElse(0);
        damage = json.getAsInt("damage").or(() -> json.getAsString("damage").flatMap(ExtMethods::parseInt)).orElse(0);
        items.clear();
        items.addAll(json.getAsJsonArray("items")
                .stream()
                .flatMap(Collection::stream)
                .map(JsonElementOptional::getAsString)
                .map(v -> v.orElse(null))
                .map(ItemUtils::loadItem)
                .toList()
        );
        updateModel();
        syncDisplayVariable(metadata());
    }
    @Override public json.builder.object write() {
        return json.object()
                .addArray("items", v -> v.add(items, ItemUtils::saveItem))
                .add("clicks", clicks)
                .add("damage", damage);
    }
    @Override public void onLoot(CustomTileMetadata metadata, PopulateLootEvent event) {
        event.addItems(this.items);
    }
    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        BlockPosition position = event.getPos();
        Location location = new Location(event.getWorld().getWorld(), position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5);
        boolean edited = false;
        for (Item item : location.clone().add(0, 0.5, 0).getNearbyEntitiesByType(Item.class, 0.75)) {
            ItemStack itemStack = item.getItemStack();
            if (itemStack.getAmount() == 0) continue;
            edited = tryAddItem(itemStack) || edited;
        }
        if (edited) {
            saveData();
            updateModel();
        }
    }
    /*@Override public Optional<IModelBlock> onDisplayAsync(Player player, World world, BlockPosition position, IBlockData data) {
        IBuilder model = this.model.get0();
        return model == null
                ? Optional.empty()
                : Optional.of(IModelBlock.of(null, model, BlockDisplay.getChunkSize(5), Double.POSITIVE_INFINITY));
    }*/
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
        ItemStack clicker = event.getItemInHand();
        ClickerComponent component = component();
        List<String> clickerTypes = component.types;
        if (player.getAttackCooldown() <= 0.9) return;

        List<String> filterClickerTypes = new ArrayList<>(clickerTypes);
        boolean isHand = clicker.getType().isAir();
        boolean isCanHand = component.hand_click;
        int clickCount = 1;
        if (isHand) {
            if (!isCanHand) return;
        } else {
            ClickerSetting clickerSetting = Items.getOptional(ClickerSetting.class, clicker)
                    .filter(v -> CollectionUtils.containsAny(v.types, clickerTypes))
                    .orElse(null);
            if (clickerSetting == null) {
                DrawText.show(DrawText.IShow.create(player, metadata().location(0.5, 0.4, 0.5), Component.text("✖").color(TextColor.color(0xFFFF00)), 0.5));
                return;
            }
            filterClickerTypes.removeIf(Predicate.not(clickerSetting.types::contains));
            clickCount = clickerSetting.clicks;
            ItemMeta meta = clicker.getItemMeta();
            if (meta instanceof Damageable damageable) {
                int value = damageable.getDamage() + hurt(clicker);
                if (value >= Items.getMaxDamage(clicker)) {
                    clicker.setAmount(0);
                } else {
                    damageable.setDamage(value);
                    clicker.setItemMeta(meta);
                }
            }
        }
        Perms.ICanData canData = Perms.getCanData(player);
        List<ClickerRecipe> recipes = Recipes.CLICKER.getAllRecipes(canData)
                .filter(v -> filterClickerTypes.contains(v.clickerType))
                .toList();
        if (recipes.size() == 0) {
            DrawText.show(DrawText.IShow.create(player, metadata().location(0.5, 0.4, 0.5), Component.text("✖").color(TextColor.color(0xFFFF00)), 0.5));
            return;
        }
        clicks += clickCount;
        Location location = metadata().location(0.5,0.5,0.5);
        if (component.particle != null) Particle.BLOCK_DUST
                .builder()
                .location(location.clone().add(0, 0.1, 0))
                .offset(0.1, 0.1, 0.1)
                .count(5)
                .data(component.particle.createBlockData())
                .force(false)
                .allPlayers()
                .spawn();
        Sounds.playSound(component.sound_click, location);

        List<ItemStack> items = new ArrayList<>(this.items);
        boolean can = false;
        World world = metadata().skull.getLevel();
        for (ClickerRecipe recipe : recipes) {
            if (!recipe.matches(readonlyInventory, world)) continue;
            can = true;
            if (recipe.clicks > clicks) continue;
            List<ItemStack> drop = recipe.assembleList(readonlyInventory, world.registryAccess(), IOutputVariable.of(player))
                    .map(CraftItemStack::asBukkitCopy)
                    .collect(Collectors.toList());
            LevelModule.onCraft(player.getUniqueId(), recipe.getId());
            Perms.onRecipeUse(recipe, player.getUniqueId(), canData);
            recipe.getRemainingItems(readonlyInventory).forEach(_item -> drop.add(CraftItemStack.asBukkitCopy(_item)));
            Items.dropGiveItem(player, drop, true);
            this.items.removeAll(items);
            Sounds.playSound(component.sound_result, location);
            clicks = 0;
            int max_damage = component.replace.max_damage();
            if (max_damage > 0) {
                damage++;
                syncDisplayVariable(metadata);
            }
            saveData();
            updateModel();
            if (max_damage > 0 && max_damage < damage) {
                component.replace.invoke(metadata.position(), metadata.list(DisplayInstance.class)
                        .findAny()
                        .flatMap(DisplayInstance::getRotation)
                        .orElse(InfoComponent.Rotation.Value.ANGLE_0)
                );
            }
            break;
        }

        if (!can) DrawText.show(DrawText.IShow.create(player, metadata().location(0.5, 0.4, 0.5), Component.text("✖").color(TextColor.color(0xFF0000)), 0.5));
        saveData();
    }
    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        List<String> types = component().types;
        return ContainerWorkbenchBook.open(event.player(), metadata, Recipes.CLICKER, types.get(0), Recipes.CLICKER
                .getAllRecipes()
                .stream()
                .filter(v -> types.contains(v.clickerType))
                .toList()
        );
    }

    @Override public final void syncDisplayVariable(CustomTileMetadata metadata) {
        metadata().list(DisplayInstance.class).findAny().ifPresent(display -> display.modify(map -> {
            map.put("clicker_damage", String.valueOf(damage));

            ClickerComponent component = component();
            int length = items.size();
            display.set("clicker.items.count", String.valueOf(length));
            for (int i = 0; i < length; i++)
                DisplayComponent.putItem(map, "clicker.items["+i+"]", TableDisplaySetting.builderItem(items.get(i), TableDisplaySetting.TableType.clicker, component.types.get(0)));

            return true;
        }));
    }
}
















