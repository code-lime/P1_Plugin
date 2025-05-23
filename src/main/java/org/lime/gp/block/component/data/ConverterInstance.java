package org.lime.gp.block.component.data;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.core.NonNullList;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.TileInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.display.IDisplayVariable;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.list.ConverterComponent;
import org.lime.gp.block.component.list.DisplayComponent;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.craft.book.ContainerWorkbenchBook;
import org.lime.gp.craft.book.Recipes;
import org.lime.gp.craft.book.RecipesBook;
import org.lime.gp.craft.recipe.ConverterRecipe;
import org.lime.gp.craft.slot.output.IOutputSlot;
import org.lime.gp.craft.slot.output.IOutputVariable;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.extension.inventory.ReadonlyInventory;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.TableDisplaySetting;
import org.lime.gp.module.loot.PopulateLootEvent;
import org.lime.gp.player.inventory.gui.InterfaceManager;
import org.lime.gp.player.perm.Perms;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.utils.ItemUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ConverterInstance extends BlockInstance implements CustomTileMetadata.Damageable, CustomTileMetadata.Tickable, /*BlockDisplay.Displayable, */CustomTileMetadata.Lootable, CustomTileMetadata.Interactable, IDisplayVariable {

    @Override public ConverterComponent component() { return (ConverterComponent)super.component(); }
    public ConverterInstance(ConverterComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
        setItem(null, false);
    }

    //public final LockToast1<IBuilder> model = system.<IBuilder>toast(null).lock();
    private ItemStack head;
    private net.minecraft.world.item.ItemStack nms_head;
    private UUID unique_key;

    public void setItem(ItemStack item, boolean save) {
        if (item == null) head = new ItemStack(Material.AIR);
        else head = item.clone();
        syncDisplayVariable(metadata());
        //ConverterComponent component = component();
        //model.set0(TableDisplaySetting.builderItem(head, component.offset, TableDisplaySetting.TableType.converter, component.converter_type));
        if (save) saveData();
        unique_key = UUID.randomUUID();
        nms_head = CraftItemStack.asNMSCopy(head);
        metadata()
            .list(DisplayInstance.class)
            .forEach(DisplayInstance::variableDirty);
    }

    @Override public void read(JsonObjectOptional json) {
        setItem(json.getAsString("item").map(ItemUtils::loadItem).orElse(null), false);
    }
    @Override public json.builder.object write() {
        return json.object()
                .add("item", head.getType().isAir() ? null : ItemUtils.saveItem(head));
    }

    @Override public void onDamage(CustomTileMetadata metadata, BlockDamageEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking() && !head.getType().isAir()) {
            Items.dropGiveItem(player, head, true);
            setItem(null, true);
        }
    }
    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        if (head.getType().isAir()) {
            Location location = metadata().location(0.5, 1, 0.5);
            ConverterComponent component = component();
            String converter_type = component.converter_type;
            for (Item drop : location.getNearbyEntitiesByType(Item.class, 0.75)) {
                if (drop == null) continue;
                ItemStack item = drop.getItemStack();
                if (Items.getGlobalKeyByItem(item)
                        .filter(item_key -> Recipes.CONVERTER.getCacheWhitelistKeys().contains(item_key))
                        .filter(item_key -> Recipes.CONVERTER.getAllRecipes().stream().filter(v -> converter_type.equals(v.converter_type)).flatMap(ConverterRecipe::getWhitelistKeys).anyMatch(v -> v.equals(item_key)))
                        .isEmpty()) continue;
                ItemStack _item = item.asOne();
                item.subtract(1);
                if (item.getAmount() <= 0) PacketManager.getEntityHandle(drop).kill();
                setItem(_item, true);
                return;
            }
        }
    }
    /*@Override public Optional<IModelBlock> onDisplayAsync(Player player, World world, BlockPosition position, IBlockData data) {
        return Optional.of(IModelBlock.of(null, model.get0(), BlockDisplay.getChunkSize(10), Double.POSITIVE_INFINITY));
    }*/
    @Override public void onLoot(CustomTileMetadata metadata, PopulateLootEvent event) {
        if (!head.getType().isAir()) event.addItem(head);
    }

    private static final net.minecraft.world.item.ItemStack RECIPE_BOOK_ICON = Optional.of(new ItemStack(Material.KNOWLEDGE_BOOK, 1))
            .map(item -> {
                ItemMeta meta = item.getItemMeta();
                meta.displayName(Component.text("Просмотр рецептов").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
                item.setItemMeta(meta);
                return CraftItemStack.asNMSCopy(item);
            })
            .orElseThrow();
    private static final net.minecraft.world.item.ItemStack PAGE_NEXT = Optional.of(new ItemStack(Material.BOOK, 1))
            .map(item -> {
                ItemMeta meta = item.getItemMeta();
                meta.displayName(Component.text("Следующая страница").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
                meta.setCustomModelData(14);
                item.setItemMeta(meta);
                return CraftItemStack.asNMSCopy(item);
            })
            .orElseThrow();
    private static final net.minecraft.world.item.ItemStack PAGE_BACK = Optional.of(new ItemStack(Material.BOOK, 1))
            .map(item -> {
                ItemMeta meta = item.getItemMeta();
                meta.displayName(Component.text("Предыдущая страница").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
                meta.setCustomModelData(13);
                item.setItemMeta(meta);
                return CraftItemStack.asNMSCopy(item);
            })
            .orElseThrow();

    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        List<net.minecraft.world.item.ItemStack> items = NonNullList.withSize(5 * 9, net.minecraft.world.item.ItemStack.EMPTY);
        items.set(0, nms_head);
        ReadonlyInventory view = ReadonlyInventory.ofNMS(items);
        EntityHuman human = event.player();
        UUID uuid = human.getUUID();
        ConverterComponent component = component();

        World world = event.world();
        Perms.ICanData canData = Perms.getCanData(uuid);
        List<Toast3<ConverterRecipe, IOutputSlot, net.minecraft.world.item.ItemStack>> output = Recipes.CONVERTER.getAllRecipes(canData)
                .filter(v -> v.converter_type.equals(component.converter_type))
                .filter(v -> v.matches(view, world))
                .flatMap(v -> v.output.slots().map(_v -> Toast.of(v, _v, v.replace ? _v.create(true, IOutputVariable.of(human)) : _v.modify(nms_head, true, IOutputVariable.of(human)))))
                .toList();
        int maxPage = (output.size() - 1) / (4 * 9);

        UUID block_uuid = metadata.key.uuid();
        UUID unique_key = this.unique_key;
        ContainerAccess context = ContainerAccess.create(world, metadata.skull.getBlockPos());

        RecipesBook.getCustomWorkbenchName(Recipes.CONVERTER, "crafting").ifPresent(title -> event.player().openMenu(new TileInventory((syncId, inventory, target) -> new ContainerChest(Containers.GENERIC_9x5, syncId, inventory, view, 5) {
            public int page = 0;
            @Override protected Slot addSlot(Slot slot) {
                if (slot.container == view) {
                    return super.addSlot(new InterfaceManager.AbstractBaseSlot(slot) {
                        @Override public boolean isPacketOnly() { return true; }
                        @Override public net.minecraft.world.item.ItemStack getItem() {
                            if (getRowY() >= 4) {
                                return switch (getRowX()) {
                                    case 0 -> page > 0 ? PAGE_BACK : net.minecraft.world.item.ItemStack.EMPTY;
                                    case 2 -> RECIPE_BOOK_ICON;
                                    case 4 -> nms_head;
                                    case 8 -> maxPage > page ? PAGE_NEXT : net.minecraft.world.item.ItemStack.EMPTY;
                                    default -> net.minecraft.world.item.ItemStack.EMPTY;
                                };
                            }
                            int itemIndex = (page * 4 * 9) + index;
                            return output.size() <= itemIndex
                                    ? net.minecraft.world.item.ItemStack.EMPTY
                                    : output.get(itemIndex).val2;
                        }
                        @Override public boolean mayPickup(EntityHuman playerEntity) { return false; }
                        @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return false; }

                        @Override public void onSlotClick(EntityHuman human, InventoryClickType type, ClickType click) {
                            if (getRowY() >= 4) {
                                switch (getRowX()) {
                                    case 0 -> {
                                        if (page > 0) page--;
                                        return;
                                    }
                                    case 2 -> {
                                        ContainerWorkbenchBook.open(human,
                                                metadata,
                                                Recipes.CONVERTER,
                                                "recipes",
                                                Recipes.CONVERTER.getAllRecipes()
                                                        .stream()
                                                        .filter(v -> v.converter_type.equals(component.converter_type))
                                                        .toList()
                                        );
                                        return;
                                    }
                                    case 8 -> {
                                        if (maxPage > page) page++;
                                        return;
                                    }
                                }
                            }
                            int itemIndex = (page * 4 * 9) + index;
                            if (ConverterInstance.this.unique_key.equals(unique_key) && output.size() > itemIndex) {
                                Toast3<ConverterRecipe, IOutputSlot, net.minecraft.world.item.ItemStack> out = output.get(itemIndex);
                                Perms.onRecipeUse(out.val0, human.getUUID(), canData);
                                ConverterInstance.this.setItem((out.val0.replace ? out.val1.create(false, IOutputVariable.of(human)) : out.val1.modify(out.val2, true, IOutputVariable.of(human))).asBukkitCopy(), true);
                                human.closeContainer();
                            }
                        }
                    });
                }
                return InterfaceManager.AbstractSlot.noneInteractSlot(super.addSlot(slot));
            }
            @Override public boolean stillValid(EntityHuman player) {
                if (!this.checkReachable) return true;
                return stillValid(context, player, block_uuid);
            }
            private boolean stillValid(ContainerAccess context, EntityHuman player, UUID block_uuid) {
                return context.evaluate((world, blockposition) -> world.getBlockEntity(blockposition) instanceof TileEntityLimeSkull skull
                        && skull.customUUID().filter(block_uuid::equals).isPresent()
                        && ConverterInstance.this.unique_key.equals(unique_key)
                        && player.distanceToSqr(blockposition.getX() + 0.5, blockposition.getY() + 0.5, blockposition.getZ() + 0.5) <= 64.0, true);
            }
        }, ChatHelper.toNMS(title))));
        return EnumInteractionResult.CONSUME;
    }
    @Override public final void syncDisplayVariable(CustomTileMetadata metadata) {
        metadata().list(DisplayInstance.class).findAny().ifPresent(display -> display.modify(map -> {
            ConverterComponent component = component();
            DisplayComponent.putItem(map, "converter.input", TableDisplaySetting.builderItem(head, TableDisplaySetting.TableType.converter, component.converter_type));
            /*map.put("water_color", ChatColorHex.toHex(Optional.ofNullable(fluid).map(BottleInstance.IFluid::waterColor).orElse(ThirstSetting.DEFAULT_WATER_COLOR)).substring(1));
            map.put("water_level", level + "");*/
            return true;
        }));
    }
}



























