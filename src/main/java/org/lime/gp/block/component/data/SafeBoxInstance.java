package org.lime.gp.block.component.data;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import net.minecraft.world.level.block.entity.TileEntitySkullEventRemove;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.Position;
import org.lime.gp.block.BlockComponentInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.CustomTileMetadata.Tickable;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.list.SafeBoxComponent;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.database.Methods;
import org.lime.gp.database.Rows;
import org.lime.gp.database.Tables;
import org.lime.gp.extension.Cooldown;
import org.lime.gp.extension.inventory.ReadonlyInventory;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.*;
import org.lime.gp.module.TimeoutData;
import org.lime.gp.player.inventory.InterfaceManager;
import org.lime.gp.player.perm.Perms;
import org.lime.gp.player.ui.ImageBuilder;
import org.lime.gp.sound.Sounds;
import org.lime.json.JsonElementOptional;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SafeBoxInstance extends BlockComponentInstance<SafeBoxComponent> implements CustomTileMetadata.Interactable, Tickable, CustomTileMetadata.Removeable {
    public final InventorySubcontainer items_container = new InventorySubcontainer(3 * 9);
    public SafeBoxInstance(SafeBoxComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
    }

    public static class SafeBoxCounter extends TimeoutData.ITimeout {
        public final Position position;
        public SafeBoxCounter(Position position) {
            this.position = position;
        }
    }

    public Long timeToClose = null;

    @Override public void read(JsonObjectOptional json) {
        timeToClose = json.getAsLong("time_to_close").orElse(null);
        items_container.clearContent();
        json.getAsJsonArray("items")
                .ifPresent(items -> IntStream.range(0, items.size())
                        .forEach(i -> items.get(i)
                                .flatMap(JsonElementOptional::getAsString)
                                .map(system::loadItem)
                                .map(CraftItemStack::asNMSCopy)
                                .ifPresent(item -> items_container.setItem(i, item))
                        ));
        syncDisplayVariable();
    }
    @Override public system.json.builder.object write() {
        return system.json.object()
                .add("time_to_close", timeToClose)
                .add("items", system.json.array().add(items_container.getContents(), item -> item == null || item.isEmpty() ? null : system.saveItem(item.asBukkitMirror())));
    }
    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        if (component().small) TimeoutData.put(metadata.key.uuid(), SafeBoxCounter.class, new SafeBoxCounter(metadata.position()));
        if (timeToClose == null) return;
        long now = System.currentTimeMillis();
        if (now > timeToClose) {
            timeToClose = null;
            syncDisplayVariable();
            close();
        }
    }
    public void close() {
        String insert_type = component().insert;
        int cash = items_container.getContents()
                .stream()
                .mapToInt(item -> Items.getOptional(InsertSetting.class, item).filter(v -> insert_type.equals(v.type)).map(v -> v.weight).orElse(0))
                .sum();
        if (cash > 0) Methods.bankReturnOPG(cash, () -> {});
        items_container.clearContent();
        saveData();
    }
    public void open() {
        if (!component().small) return;
        Location location = metadata().location();
        List<Rows.HouseRow> houseList = Tables.HOUSE_TABLE.getRowsBy(v -> v.inZone(location));
        int count = (int)TimeoutData.map(SafeBoxCounter.class)
                .values()
                .stream()
                .map(v -> v.position)
                .map(Position::getLocation)
                .filter(v -> houseList.stream().anyMatch(_v -> _v.inZone(v)))
                .count();
        items_container.clearContent();
        Methods.bankOPG(count, cash -> InsertSetting.createOf(component().insert, cash)
                .forEach(item -> items_container.addItem(CraftItemStack.asNMSCopy(item)))
        );
        system.randomize(items_container.items);
        saveData();
    }
    @Override public void onRemove(CustomTileMetadata metadata, TileEntitySkullEventRemove event) {
        saveData();
    }

    public static boolean useDeKey(EntityHuman entityhuman, EnumHand hand) {
        ItemStack deKey = entityhuman.getItemInHand(hand);
        if (Items.getItemCreator(deKey)
                .map(v -> v instanceof Items.ItemCreator c ? c : null)
                .filter(v -> v.has(DeKeySetting.class))
                .filter(v -> Perms.getCanData(entityhuman.getUUID()).isCanUse(v.getKey()))
                .isEmpty()
        ) return false;
        deKey.shrink(1);
        entityhuman.setItemInHand(hand, deKey);
        return true;
    }
    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        List<Rows.HouseRow> houseRows = Rows.HouseRow.getInHouse(metadata.location());
        if (houseRows.stream().noneMatch(v -> v.type == Rows.HouseRow.HouseType.BANK_VAULT)) return EnumInteractionResult.PASS;
        if (Rows.HouseRow.useType(houseRows, event.player().getUUID()) != Rows.HouseRow.UseType.Deny) return EnumInteractionResult.PASS;
        UUID block_uuid = metadata.key.uuid();
        UUID unique_key = unique();
        ContainerAccess context = ContainerAccess.create(metadata.skull.getLevel(), metadata.skull.getBlockPos());

        if (timeToClose != null) {
            if (!component().small) return EnumInteractionResult.PASS;
            event.player().openMenu(new TileInventory((syncId, inventory, target) -> new ContainerChest(Containers.GENERIC_9x3, syncId, inventory, items_container, 3) {
                @Override
                protected Slot addSlot(Slot slot) {
                    if (slot.container == items_container) {
                        return super.addSlot(new InterfaceManager.AbstractBaseSlot(slot) {
                            @Override public boolean mayPickup(EntityHuman playerEntity) {
                                return true;
                            }
                            @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                                return false;
                            }
                        });
                    }
                    return super.addSlot(slot);
                }

                @Override
                public boolean stillValid(EntityHuman player) {
                    return stillValid(context, player, block_uuid);
                }
                public boolean stillValid(ContainerAccess context, EntityHuman player, UUID block_uuid) {
                    return context.evaluate((world, blockposition) -> world.getBlockEntity(blockposition) instanceof TileEntityLimeSkull skull
                                    && skull.customUUID().filter(block_uuid::equals).isPresent()
                                    && SafeBoxInstance.this.timeToClose != null
                                    && SafeBoxInstance.this.unique().equals(unique_key)
                                    && player.distanceToSqr(blockposition.getX() + 0.5, blockposition.getY() + 0.5, blockposition.getZ() + 0.5) <= 64.0,
                            true);
                }
            }, ChatHelper.toNMS(Component.text("Банковская ячейка"))));
            return EnumInteractionResult.CONSUME;
        }
        if (metadata.list(DisplayInstance.class).anyMatch(v -> "true".equals(v.getAll().get("open_state")))) return EnumInteractionResult.PASS;
        if (Bukkit.getOnlinePlayers().size() / (double)Bukkit.getMaxPlayers() < 0.5 || !useDeKey(event.player(), event.hand())) return EnumInteractionResult.CONSUME;

        SafeBoxComponent component = component();

        ReadonlyInventory view = ReadonlyInventory.ofNMS(NonNullList.withSize((component.small ? 4 : 6) * 9, net.minecraft.world.item.ItemStack.EMPTY));

        event.player().openMenu(new ITileInventory() {
            public final List<Integer> values = component.small ? Arrays.asList(-1, 1) : Arrays.asList(-2,-1,1,2);
            public final int[] progress = new int[9];
            public final int[] data = IntStream.range(0, 9).map(v -> system.rand(values)).toArray();
            private IChatBaseComponent title = createTitle();
            public final Map<Integer, ItemStack> preview = system.map.<Integer, ItemStack>of()
                    .add((component.small
                        ? Stream.of(system.toast(1, 10015), system.toast(0, 0), system.toast(-1, 10016))
                        : Stream.of(system.toast(2, 10011), system.toast(1, 10012), system.toast(0, 0), system.toast(-1, 10013), system.toast(-2, 10014))
                    ).toList(), kv -> kv.val0, kv -> {
                        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.BARRIER);
                        ItemMeta meta = item.getItemMeta();
                        meta.setCustomModelData(kv.val1);
                        meta.displayName(Component.text("Использовать").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
                        item.setItemMeta(meta);
                        return CraftItemStack.asNMSCopy(item);
                    })
                    .build();
            @Override public Container createMenu(int syncId, PlayerInventory inventory, EntityHuman target) {
                ITileInventory _this = this;
                return new ContainerChest((component.small ? Containers.GENERIC_9x4 : Containers.GENERIC_9x6), syncId, inventory, view, (component.small ? 4 : 6)) {
                    public void progress(boolean crash) {
                        if (crash) {
                            Sounds.playSound(component.sound_crash, metadata.location(0.5, 0.5, 0.5));
                            if (!useDeKey(target, event.hand())) {
                                target.closeContainer();
                                return;
                            }
                        } else {
                            Sounds.playSound(component.sound_progress, metadata.location(0.5, 0.5, 0.5));
                        }
                        title = createTitle();
                        target.openMenu(_this);
                    }
                    public void open() {
                        Sounds.playSound(component.sound_open, metadata.location(0.5, 0.5, 0.5));
                        timeToClose = System.currentTimeMillis() + 1000L * component.close_time;
                        syncDisplayVariable();
                        saveData();
                        SafeBoxInstance.this.open();
                        target.closeContainer();
                    }
                    @Override protected Slot addSlot(Slot slot) {
                        if (slot.container == view) {
                            return super.addSlot(new InterfaceManager.AbstractBaseSlot(slot) {
                                @Override public void onSlotClick(EntityHuman player) {
                                    if (Cooldown.hasOrSetCooldown(player.getUUID(), "safe_box.click", 0.1)) return;
                                    int x = getRowX();
                                    int y = getRowY();
                                    if (y == (component.small ? 3 : 5)) return;
                                    if (progress[x] == 0 && (x <= 0 || progress[x - 1] != 0)) {
                                        int delta = (component.small ? 1 : 2) - y;
                                        if (delta != 0) {
                                            if (data[x] == delta) {
                                                progress[x] = delta;
                                                progress(false);
                                            }
                                            else progress(true);
                                        }
                                        if (x == 8 && progress[x] != 0) open();
                                    }
                                }

                                @Override public net.minecraft.world.item.ItemStack getItem() {
                                    int x = getRowX();
                                    int y = getRowY();
                                    if (progress[x] == 0 && (x <= 0 || progress[x - 1] != 0)) {
                                        int delta = (component.small ? 1 : 2) - y;
                                        if (delta == 0) return ItemStack.EMPTY;
                                        return preview.getOrDefault(delta, ItemStack.EMPTY);
                                    }
                                    return ItemStack.EMPTY;
                                }
                                @Override public boolean mayPickup(EntityHuman playerEntity) { return false; }
                                @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return false; }
                            });
                        }
                        return InterfaceManager.AbstractSlot.noneInteractSlot(super.addSlot(slot));
                    }
                    @Override public boolean stillValid(EntityHuman player) {
                        return stillValid(context, player, block_uuid);
                    }
                    public boolean stillValid(ContainerAccess context, EntityHuman player, UUID block_uuid) {
                        return context.evaluate((world, blockposition) -> world.getBlockEntity(blockposition) instanceof TileEntityLimeSkull skull
                                && skull.customUUID().filter(block_uuid::equals).isPresent()
                                && SafeBoxInstance.this.timeToClose == null
                                && SafeBoxInstance.this.unique().equals(unique_key)
                                && player.distanceToSqr(blockposition.getX() + 0.5, blockposition.getY() + 0.5, blockposition.getZ() + 0.5) <= 64.0,
                                true);
                    }
                };
            }

            private IChatBaseComponent createTitle() {
                boolean small = component.small;
                List<ImageBuilder> components = new ArrayList<>();
                components.add(ImageBuilder.of(small ? 0xE740 : 0xE746, 176).addOffset(81));
                int distance = -1;
                for (int i = 0; i < 9; i++) {
                    if (progress[i] != 0) distance = i;
                    if (small) {
                        components.add(ImageBuilder.of(switch (progress[i]) {
                            case -1 -> 0xE73F;
                            default -> 0xE73E;
                            case 1 -> 0xE73D;
                        }, 14).addOffset(9 + 18 * i));
                    } else {
                        components.add(ImageBuilder.of(switch (progress[i]) {
                            case -2 -> 0xE745;
                            case -1 -> 0xE744;
                            default -> 0xE743;
                            case 1 -> 0xE742;
                            case 2 -> 0xE741;
                        }, 14).addOffset(9 + 18 * i));
                    }
                }
                distance++;
                for (int i = 0; i < 9; i++) {
                    int delta = distance - i;
                    if (delta < 0) continue;
                    components.add((switch (delta) {
                        case 0 -> ImageBuilder.of(small ? 0xE73C : 0xE74A, 12).addOffset(-3);
                        case 1, 2, 3 -> ImageBuilder.of(small ? 0xE73B : 0xE749, 18);
                        case 4 -> ImageBuilder.of(small ? 0xE73A : 0xE748, 18);
                        default -> ImageBuilder.of(small ? 0xE739 : 0xE747, 18);
                    }).addOffset(9 + 18 * i));
                }
                return ChatHelper.toNMS(ImageBuilder.join(components).color(NamedTextColor.WHITE));
            }

            @Override public IChatBaseComponent getDisplayName() {
                return title;
            }
        });
        return EnumInteractionResult.CONSUME;
    }
    private void syncDisplayVariable() {
        if (!component().small) return;
        metadata().list(DisplayInstance.class).findAny().ifPresent(display -> display.set("open_state", timeToClose == null ? "false" : "true"));
    }
}


























