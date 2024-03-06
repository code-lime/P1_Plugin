package org.lime.gp.player.module.cinv;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.InventoryClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.display.lime;
import org.lime.gp.chat.Apply;
import org.lime.gp.item.data.MaterialCreator;
import org.lime.gp.player.inventory.gui.InterfaceManager;
import org.lime.gp.player.ui.ContainerInput;
import org.lime.gp.player.ui.EditorUI;
import org.lime.system.toast.*;

import java.util.*;
import java.util.stream.Stream;

public class SearchQuery {
    private final ViewContainer viewContainer;
    private SearchQuery(ViewContainer viewContainer) { this.viewContainer = viewContainer; }

    public void openSearch(EntityHuman human) {
        EditorUI.openRaw(human, Component.text("Поиск предметов"), this::containerInit);
    }
    public static void openSearch(ViewContainer viewContainer, EntityHuman human) { new SearchQuery(viewContainer).openSearch(human); }
    private ContainerInput containerInit(int syncId, net.minecraft.world.entity.player.PlayerInventory inventory, EntityHuman player) {
        return new ContainerInput(syncId, inventory) {
            @Override public boolean isValid() { return true; }

            private static final Component[] LEFT_RIGHT_FULL__UP;
            private static final Component[] LEFT_RIGHT_FULL__DOWN;
            static {
                LEFT_RIGHT_FULL__UP = Stream.concat(Stream.of(Component.text("UP")), Stream.of(ViewContainer.LEFT_RIGHT_FULL)).toArray(Component[]::new);
                LEFT_RIGHT_FULL__DOWN = Stream.concat(Stream.of(Component.text("DOWN")), Stream.of(ViewContainer.LEFT_RIGHT_FULL)).toArray(Component[]::new);
            }

            private void syncInput() {
                //lime.logOP("Sync update");
                if (itemName == null) itemName = "";

                input = ViewContainer.headOf(Component.text(itemName), ViewContainer.UP_HEAD, LEFT_RIGHT_FULL__UP);
                output = ViewContainer.headOf(Component.text(itemName), ViewContainer.DOWN_HEAD, LEFT_RIGHT_FULL__DOWN);
            }

            private ItemStack input = ViewContainer.headOf(Component.text(""), ViewContainer.UP_HEAD, LEFT_RIGHT_FULL__UP);
            private ItemStack output = ViewContainer.headOf(Component.text(""), ViewContainer.DOWN_HEAD, LEFT_RIGHT_FULL__DOWN);

            @Override public ItemStack getInput() { return input; }
            @Override public ItemStack getCenter() { return output; }
            @Override public ItemStack getOutput() { return ViewContainer.BACK; }

            private int offset = 0;

            private final LockToast1<List<ItemElement>> items = Toast.of(Collections.<ItemElement>emptyList()).lock();
            private final LockToast1<Integer> lastExecutor = Toast.of(0).lock();

            private final int stepLength = 9;
            private final int showLength = stepLength * 3;
            private Optional<ItemElement> getElement(int _index) {
                return items.call(v -> {
                    List<ItemElement> items = v.val0;
                    int count = items.size();
                    count = ((count / stepLength) + (count % stepLength > 0 ? 1 : 0)) * stepLength;
                    int delta = count - showLength;
                    if (delta <= 0) offset = 0;
                    else if (offset > delta) offset = delta;
                    else if (offset < 0) offset = 0;

                    int index = _index + this.offset;

                    return index < 0 || index >= items.size() ? Optional.empty() : Optional.of(items.get(index));
                });
            }

            @Override public void clickInput(EntityHuman human, ClickType click) {
                if (click.isShiftClick()) {
                    offset = 0;
                } else {
                    offset -= click.isRightClick() ? showLength : stepLength;
                }
                syncInput();
            }
            @Override public void clickCenter(EntityHuman human, ClickType click) {
                if (click.isShiftClick()) {
                    offset = Integer.MAX_VALUE;
                } else {
                    offset += click.isRightClick() ? showLength : stepLength;
                }
                syncInput();
            }
            @Override public void clickOutput(EntityHuman human, ClickType click) {
                viewContainer.forceOpen(human);
            }

            @Override public Slot createPlayerSlot(Slot slot) {
                int deltaIndex = slot.index - 3;
                if (deltaIndex >= showLength) return slot;
                //ItemStack indexedItem = ViewContainer.of(Component.text("SLOT: " + deltaIndex), deltaIndex + 1);
                return new InterfaceManager.AbstractBaseSlot(slot) {
                    @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return false; }
                    @Override public net.minecraft.world.item.ItemStack getItem() { return getElement(deltaIndex).map(ItemElement::show).map(CraftItemStack::asNMSCopy).orElse(ItemStack.EMPTY); }
                    @Override public boolean mayPickup(EntityHuman human) { return false; }
                    @Override public boolean isPacketOnly() { return true; }

                    @Override public void onSlotClick(EntityHuman human, InventoryClickType type, ClickType click) {
                        if (human instanceof EntityPlayer handler)
                            getElement(deltaIndex).ifPresent(element -> element.click(viewContainer, handler, click));
                    }
                };
                /*
        new InterfaceManager.AbstractBaseSlot(slot) {
            @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return false; }
            @Override public net.minecraft.world.item.ItemStack getItem() {
                return switch (index) {
                    case 0 -> getInput();
                    case 1 -> getCenter();
                    case 2 -> getOutput();
                    default -> net.minecraft.world.item.ItemStack.EMPTY;
                };
            }
            @Override public boolean mayPickup(EntityHuman human) {
                switch (index) {
                    case 0 -> clickInput(human);
                    case 1 -> clickCenter(human);
                    case 2 -> clickOutput(human);
                }
                return false;
            }
        }
                */
            }

            private static <T>Stream<T> timeout(Stream<T> stream, long timeoutMs) {
                return timeout(stream, timeoutMs, Optional.empty());
            }
            private static <T>Stream<T> timeout(Stream<T> stream, long timeoutMs, Optional<T> timeoutElement) {
                long stopMs = System.currentTimeMillis() + timeoutMs;
                Toast1<Boolean> timeout = Toast.of(false);
                return Stream.concat(
                        stream.takeWhile(v -> {
                            if (System.currentTimeMillis() < stopMs) return true;
                            timeout.val0 = true;
                            return false;
                        }),
                        timeoutElement.stream().filter(v -> timeout.val0)
                );
            }

            private static class TimeoutCreator extends MaterialCreator {
                public static final TimeoutCreator TIMEOUT = new TimeoutCreator();

                private TimeoutCreator() { super(Material.BARRIER); }

                @Override public org.bukkit.inventory.ItemStack createItem(int count, Apply apply) {
                    org.bukkit.inventory.ItemStack item = super.createItem(count, apply);
                    ItemMeta meta = item.getItemMeta();
                    meta.displayName(Component.text("ERROR")
                            .color(NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false)
                    );
                    meta.lore(List.of(Component.text("TIMEOUT")
                            .color(NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false)
                    ));
                    item.setItemMeta(meta);
                    return item;
                }
            }
            private static final ItemElement TIMEOUT_ELEMENT = new ItemElement(TimeoutCreator.TIMEOUT);

            @Override public void input(String line) {
                //lime.logOP("Input: " + line);
                int executorID = lastExecutor.edit0(v -> v + 1);
                if (line.isEmpty()) {
                    //lime.logOP("Clear search: " + executorID);
                    this.items.set0(Collections.emptyList());
                    //this.slotsChanged(inventory);
                    return;
                }
                //lime.logOP("Start search: " + executorID);
                lime.invokeAsync(() -> timeout(viewContainer.viewData().groups.rawSearch(line), 1000, Optional.of(TIMEOUT_ELEMENT)).toList(), items -> {
                    //lime.logOP("End search: " + executorID);
                    if (!Objects.equals(lastExecutor.get0(), executorID)) return;
                    //lime.logOP("Setup search: " + executorID);
                    this.items.set0(items);
                    //this.slotsChanged(inventory);
                });
            }
        };
    }
}













