package org.lime.gp.player.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.PacketPlayInWindowClick;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.Slot;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftContainer;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.lime.core;
import org.lime.display.PacketManager;
import org.lime.gp.lime;
import org.lime.reflection;
import org.lime.system;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.gp.chat.ChatHelper;

import java.util.*;


@SuppressWarnings("unchecked")
public class InterfaceManager implements Listener {
    public static core.element create() {
        return core.element.create(InterfaceManager.class)
                .withInstance()
                .withInit(InterfaceManager::init);
    }

    public static void init() {
        PacketManager.adapter()
                .add(PacketPlayInWindowClick.class, (packet, event) -> {
                    if (event.isCancelled()) return;
                    PlayerConnection connection = ((CraftPlayer)event.getPlayer()).getHandle().connection;
                    if (connection.player.isImmobile()) return;
                    if (connection.player.containerMenu.containerId == packet.getContainerId()) {
                        int i2 = packet.getSlotNum();
                        if (!connection.player.containerMenu.isValidSlotIndex(i2)) return;
                        if (packet.getSlotNum() < -1 && packet.getSlotNum() != -999) return;
                        switch (packet.getClickType()) {
                            case PICKUP -> {
                                if (packet.getButtonNum() != 0 && packet.getButtonNum() != 1) break;
                                if (packet.getSlotNum() == -999) break;
                                if (packet.getSlotNum() < 0) break;
                                Slot slot = connection.player.containerMenu.getSlot(packet.getSlotNum());
                                if (slot instanceof AbstractSlot aslot) aslot.onSlotClickAsync(connection.player);
                            }
                            case QUICK_MOVE -> {
                                if (packet.getButtonNum() != 0 && packet.getButtonNum() != 1) break;
                                if (packet.getSlotNum() < 0) break;
                                Slot slot = connection.player.containerMenu.getSlot(packet.getSlotNum());
                                if (slot instanceof AbstractSlot aslot) aslot.onSlotClickAsync(connection.player);
                            }
                            case SWAP -> {
                                if ((packet.getButtonNum() < 0 || packet.getButtonNum() >= 9) && packet.getButtonNum() != 40) break;
                                Slot slot = connection.player.containerMenu.getSlot(packet.getSlotNum());
                                if (slot instanceof AbstractSlot aslot) aslot.onSlotClickAsync(connection.player);
                            }
                            case THROW -> {
                                if (packet.getSlotNum() >= 0) {
                                    if (packet.getButtonNum() == 0) {
                                        if (connection.player.containerMenu.getSlot(packet.getSlotNum()) instanceof AbstractSlot aslot) aslot.onSlotClickAsync(connection.player);
                                        break;
                                    }
                                    if (packet.getButtonNum() != 1) break;
                                    if (connection.player.containerMenu.getSlot(packet.getSlotNum()) instanceof AbstractSlot aslot) aslot.onSlotClickAsync(connection.player);
                                }
                            }
                            default -> {}
                        }
                    }
                })
                .listen();
    }
    public static class GUI {
        public final Inventory inventory;
        system.Action3<GUI, Player, system.Action1<ItemStack>> onOpen;
        system.Action7<GUI, Player, Integer, Inventory, ItemStack, ClickType, system.Action1<ItemStack>> onClick;
        system.Action2<GUI, Player> onClose;

        public GUI(Component title, int size, system.Action3<GUI, Player, system.Action1<ItemStack>> onOpen, system.Action7<GUI, Player, Integer, Inventory, ItemStack, ClickType, system.Action1<ItemStack>> onClick, system.Action2<GUI, Player> onClose) {
            inventory = Bukkit.createInventory(null, size, title);
            guis.put(inventory, this);
            this.onOpen = onOpen;
            this.onClick = onClick;
            this.onClose = onClose;
        }
        public void show(Player player) {
            player.openInventory(inventory);
        }
        public void onOpen(Player player, system.Action1<ItemStack> setCursor) {
            onOpen.invoke(this, player, setCursor);
        }
        public void onClose(Player player) {
            onClose.invoke(this, player);
            dispose();
        }
        public void dispose() {
            guis.remove(inventory);
        }
    }
    public static abstract class IGUI {
        public void init(GUI gui) {}
        public void onOpen(GUI gui, Player player, system.Action1<ItemStack> setCursor) {}
        public void onClick(GUI gui, Player player, Integer slot, Inventory inventory, ItemStack item, ClickType click, system.Action1<ItemStack> setCursor) {}
        public void onClose(GUI gui, Player player) {}
    }

    static HashMap<Inventory, GUI> guis = new HashMap<>();

    @EventHandler public static void on(final InventoryClickEvent e) {
        GUI gui = guis.getOrDefault(e.getInventory(), null);
        if (gui == null) return;
        e.setCancelled(true);
        final ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        final Player p = (Player) e.getWhoClicked();
        gui.onClick.invoke(gui, p, e.getSlot(), e.getClickedInventory(), clickedItem, e.getClick(), e.getView()::setCursor);
    }
    @EventHandler public static void on(final InventoryDragEvent e) {
        if (!guis.containsKey(e.getInventory())) return;
        e.setCancelled(true);
    }
    @EventHandler public static void on(PlayerDropItemEvent e) {
        GUI gui = guis.getOrDefault(e.getPlayer().getOpenInventory().getTopInventory(), null);
        if (gui == null) return;
        e.getItemDrop().getItemStack().setAmount(0);
    }
    @EventHandler public static void on(final InventoryCloseEvent e) {
        GUI gui = guis.getOrDefault(e.getInventory(), null);
        if (gui == null) return;
        gui.onClose((Player) e.getPlayer());
    }
    @EventHandler public static void on(final InventoryOpenEvent e) {
        GUI gui = guis.getOrDefault(e.getInventory(), null);
        if (gui == null) return;
        gui.onOpen((Player) e.getPlayer(), e.getView()::setCursor);
    }

    public static GUI create(Component title, int size, IGUI igui) {
        return create(title, size, igui::init, igui::onOpen, igui::onClick, igui::onClose);
    }
    public static GUI create(Component title, int size, system.Action1<GUI> init, system.Action3<GUI, Player, system.Action1<ItemStack>> onOpen, system.Action7<GUI, Player, Integer, Inventory, ItemStack, ClickType, system.Action1<ItemStack>> onClick, system.Action2<GUI, Player> onClose) {
        GUI gui = new GUI(title, size, onOpen, onClick, onClose);
        init.invoke(gui);
        return gui;
    }
    public static ItemStack createItem(final Material material, final String name, final String... lore) {
        return createItem(material, name, null, lore);
    }
    @SuppressWarnings("deprecation")
    public static ItemStack createItem(final Material material, final String name, system.Action1<ItemStack> init, final String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();

        if (name != null) meta.setDisplayName(ChatColor.RESET + name);
        if (lore.length > 0) {
            List<String> _lore = new ArrayList<>();
            for (String _item : lore) _lore.add(ChatColor.RESET + _item);
            meta.setLore(_lore);
        }
        item.setItemMeta(meta);

        if (init != null) init.invoke(item);

        return item;
    }

    public static Slot readonlySlot(Slot slot) {
        Slot out = new Slot(slot.container, slot.slot, slot.x, slot.y) {
            @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return false; }
            @Override public boolean mayPickup(EntityHuman playerEntity) { return false; }
        };
        out.index = slot.index;
        return out;
    }
    public static Slot filterSlot(Slot slot, system.Func1<net.minecraft.world.item.ItemStack, Boolean> filter) {
        Slot out = new Slot(slot.container, slot.slot, slot.x, slot.y) {
            @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                return filter.invoke(stack);
            }
        };
        out.index = slot.index;
        return out;
    }
    public static Slot clickSlot(Slot slot, system.Func2<EntityHuman, net.minecraft.world.item.ItemStack, Boolean> action) {
        Slot out = new Slot(slot.container, slot.slot, slot.x, slot.y) {
            @Override public boolean mayPickup(EntityHuman playerEntity) {
                return action.invoke(playerEntity, this.getItem());
            }
        };
        out.index = slot.index;
        return out;
    }
    public static abstract class AbstractSlot extends Slot {
        public AbstractSlot(Slot slot) {
            super(slot.container, slot.slot, slot.x, slot.y);
            index = slot.index;
        }

        public int getRowX() { return index % 9; }
        public int getRowY() { return index / 9; }

        public void onSlotClick(EntityHuman player) { }
        public void onSlotClickAsync(EntityHuman player) {
            lime.invokeSync(() -> {
                if (player.containerMenu.stillValid(player))
                    onSlotClick(player);
            });
        }

        public static AbstractSlot noneSlot(Slot slot) {
            net.minecraft.world.item.ItemStack ITEM = CraftItemStack.asNMSCopy(MainPlayerInventory.createBarrier(false));
            return new AbstractSlot(slot) {
                @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return false; }
                @Override public boolean mayPickup(EntityHuman playerEntity) { return false; }
                @Override public net.minecraft.world.item.ItemStack getItem() { return ITEM; }
            };
        }
        public static AbstractSlot noneInteractSlot(Slot slot) {
            return new AbstractSlot(slot) {
                @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return false; }
                @Override public boolean mayPickup(EntityHuman playerEntity) { return false; }
            };
        }
    }
    public static abstract class AbstractBaseSlot extends AbstractSlot {
        public Slot base;
        public AbstractBaseSlot(Slot slot) {
            super(slot);
        }
    }

    public static final class Builder {
        private final EntityPlayer player;
        private final IInventory iinventory;
        private Container container;
        private Builder(EntityPlayer player, CraftInventory inventory) {
            int containerCounter = player.nextContainerCounter();
            this.player = player;
            this.iinventory = inventory.getInventory();
            this.container = new CraftContainer(inventory, player, containerCounter);
        }

        private static final reflection.field<Container> delegate_CraftContainer = reflection.field.of(CraftContainer.class, "delegate");
        private static void replaceSlots(IInventory top, Container container, system.Func1<Slot, Slot> slots) {
            NonNullList<Slot> list = container.slots;
            int length = list.size();
            for (int i = 0; i < length; i++) {
                Slot slot = list.get(i);
                if (slot.container == top) list.set(i, slots.invoke(slot));
            }
            if (container instanceof CraftContainer craftContainer) replaceSlots(top, delegate_CraftContainer.get(craftContainer), slots);
        }

        public Builder slots(system.Func1<Slot, Slot> slots) {
            replaceSlots(this.iinventory, this.container, slots);
            return this;
        }
        public Builder filter(system.Func1<net.minecraft.world.item.ItemStack, Boolean> filter) {
            return slots(slot -> filterSlot(slot, filter));
        }
        public boolean open() {
            if (player == null) return false;
            if (MinecraftInventory_class.isInstance(iinventory)) container.setTitle(ChatHelper.toNMS(title_MinecraftInventory.invoke(iinventory)));
            boolean cancelled = false;
            container = CraftEventFactory.callInventoryOpenEvent(player, container, cancelled);
            if (container == null && !cancelled) return false;
            player.containerMenu = container;
            if (!player.isFreezing()) player.connection.send(new PacketPlayOutOpenWindow(container.containerId, container.getType(), container.getTitle()));
            player.initMenu(container);
            return true;
        }
    }

    public static Builder of(Player player, CraftInventory inventory) {
        return of(((CraftPlayer)player).getHandle(), inventory);
    }
    public static Builder of(EntityHuman player, CraftInventory inventory) {
        return new Builder((EntityPlayer)player, inventory);
    }

    public static boolean openInventory(Player player, CraftInventory inventory, system.Func3<Integer, PlayerInventory, IInventory, Container> creator) {
        if (player == null) return false;
        return openInventory(((CraftPlayer)player).getHandle(), inventory, creator);
    }
    public static boolean openInventory(EntityHuman human, CraftInventory inventory, system.Func3<Integer, PlayerInventory, IInventory, Container> creator) {
        return human instanceof EntityPlayer player && openInventory(player, inventory, creator);
    }

    private static final Class<?> MinecraftInventory_class;
    private static final system.Func1<IInventory, Component> title_MinecraftInventory;
    static {
        try {
            MinecraftInventory_class = Arrays.stream(CraftInventoryCustom.class.getDeclaredClasses()).filter(v -> v.getSimpleName().equals("MinecraftInventory")).findAny().orElseThrow();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        title_MinecraftInventory = reflection.method.of(MinecraftInventory_class, "title").build(system.Func1.class);
    }

    public static boolean openInventory(EntityPlayer player, CraftInventory inventory, system.Func3<Integer, PlayerInventory, IInventory, Container> creator) {
        if (player == null) return false;
        int containerCounter = player.nextContainerCounter();
        IInventory iinventory = inventory.getInventory();
        Container container = creator.invoke(containerCounter, player.getInventory(), iinventory);
        if (MinecraftInventory_class.isInstance(iinventory))
            container.setTitle(ChatHelper.toNMS(title_MinecraftInventory.invoke(iinventory)));
        boolean cancelled = false;
        container = CraftEventFactory.callInventoryOpenEvent(player, container, cancelled);
        if (container == null && !cancelled) return false;
        player.containerMenu = container;
        if (!player.isFreezing()) player.connection.send(new PacketPlayOutOpenWindow(container.containerId, container.getType(), container.getTitle()));
        player.initMenu(container);
        return true;
    }
}


















