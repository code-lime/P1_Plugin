package org.lime.gp.player.module.cinv;

import net.kyori.adventure.text.Component;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.network.protocol.game.PacketPlayOutAutoRecipe;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.InventoryClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.IRecipe;
import net.minecraft.world.item.crafting.RecipeCrafting;
import org.bukkit.event.inventory.ClickType;
import org.lime.gp.craft.book.ContainerWorkbenchBook;
import org.lime.gp.craft.book.IHandleAutoRecipe;
import org.lime.gp.craft.book.RecipePackets;
import org.lime.gp.craft.book.Recipes;
import org.lime.gp.craft.recipe.IDisplayRecipe;
import org.lime.gp.item.data.Checker;
import org.lime.gp.lime;
import org.lime.gp.player.inventory.InterfaceManager;
import org.lime.gp.player.ui.EditorUI;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CraftQuery {
    private final ViewContainer viewContainer;
    private CraftQuery(ViewContainer viewContainer) { this.viewContainer = viewContainer; }

    public void openQuery(EntityHuman human, Checker whitelist) {
        EditorUI.openRaw(human, Component.text("Поиск предметов"), (syncId, inventory, player) -> containerInit(syncId, inventory, player, whitelist));
    }
    public static void openQuery(ViewContainer viewContainer, EntityHuman human, Checker whitelist) {
        new CraftQuery(viewContainer).openQuery(human, whitelist);
    }

    private ContainerWorkbenchBook containerInit(int syncId, net.minecraft.world.entity.player.PlayerInventory inventory, EntityHuman player, Checker whitelist) {
        IRegistryCustom custom = player.level().registryAccess();
        HashMap<MinecraftKey, Toast2<IQueryRecipe, IRecipe<?>>> recipeMap = new HashMap<>();
        Recipes.CRAFTING_MANAGER.getRecipes()
                .forEach(recipe -> IQueryRecipe.ofRecipe(custom, recipe)
                        .forEach(query -> query.getDisplayRecipe(custom)
                                .forEach(display -> recipeMap.put(display.getId(), Toast.of(query, display)))));
        class HandleContainerWorkbenchBook extends ContainerWorkbenchBook implements IHandleAutoRecipe {
            private @Nullable Toast2<IQueryRecipe, IRecipe<?>> currentRecipe = null;
            private Checker whitelist;
            public HandleContainerWorkbenchBook(Checker whitelist) {
                super(syncId, inventory, Collections.emptyList(), null, ContainerAccess.NULL);
                this.whitelist = whitelist;
            }

            private void clickSlot(int slotIndex, EntityHuman human, ClickType click) {
                if (!(human instanceof EntityPlayer player)) return;
                if (currentRecipe == null) return;
                Collection<ItemStack> items = currentRecipe.val0.slots().get(slotIndex);
                if (items == null || items.isEmpty()) {
                    player.connection.send(new PacketPlayOutAutoRecipe(containerId, currentRecipe.val1));
                    return;
                }
                this.whitelist = Checker.createRawCheck(currentRecipe.val0.slots().get(slotIndex));
                currentRecipe = null;
                RecipePackets.syncRecipe(player);
            }
            @Override public Stream<? extends IDisplayRecipe> getRecipesCustom() {
                return recipeMap.values().stream().map(Toast2::get0).filter(v -> v.check(this.whitelist));
            }

            @Override public Slot changeSlot(Slot slot) {
                return slot.container == player.getInventory() ? super.changeSlot(slot) : new InterfaceManager.AbstractBaseSlot(slot) {
                    @Override public boolean isPacketOnly() { return true; }
                    @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return false; }
                    @Override public net.minecraft.world.item.ItemStack getItem() { return ItemStack.EMPTY; }
                    @Override public void onSlotClick(EntityHuman human, InventoryClickType type, ClickType click) { clickSlot(index, human, click); }
                    @Override public boolean mayPickup(EntityHuman human) { return false; }
                };
            }
            @Override public void handle(IRecipe<?> baseRecipe, @Nullable RecipeCrafting displayRecipe) {
                IRecipe<?> rawRecipe = displayRecipe != null ? displayRecipe : baseRecipe;
                currentRecipe = recipeMap.get(rawRecipe.getId());
            }
        }
        return new HandleContainerWorkbenchBook(whitelist);

            /*
        return new ContainerWorkbenchBook(syncId, inventory, Collections.emptyList(), null, ContainerAccess.NULL) {
            @Override public Collection<? extends IDisplayRecipe> getRecipesCustom() {
                return super.getRecipesCustom();
            }
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
                offset -= click.isRightClick() ? showLength : stepLength;
                syncInput();
            }
            @Override public void clickCenter(EntityHuman human, ClickType click) {
                offset += click.isRightClick() ? showLength : stepLength;
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
                    @Override public boolean mayPlace(ItemStack stack) { return false; }
                    @Override public ItemStack getItem() { return getElement(deltaIndex).map(ItemElement::show).map(CraftItemStack::asNMSCopy).orElse(ItemStack.EMPTY); }
                    @Override public boolean mayPickup(EntityHuman human) { return false; }
                    @Override public boolean isPacketOnly() { return true; }

                    @Override public void onSlotClick(EntityHuman human, InventoryClickType type, ClickType click) {
                        if (human instanceof EntityPlayer handler)
                            getElement(deltaIndex).ifPresent(element -> element.execute(handler.getBukkitEntity(), click.isShiftClick()));
                    }
                };
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
            */
    }
}













