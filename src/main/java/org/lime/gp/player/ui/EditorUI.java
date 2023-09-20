package org.lime.gp.player.ui;

import io.papermc.paper.adventure.AdventureComponent;
import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.EnumHand;
import net.minecraft.world.TileInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.TileEntitySign;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;
import org.lime.system.execute.Action1;
import org.lime.system.execute.Func3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public final class EditorUI {
    public static CoreElement create() {
        return CoreElement.create(EditorUI.class)
                .withInit(EditorUI::init);
    }

    public static void init() {
        PacketManager.adapter()
                .add(PacketPlayInBEdit.class, (packet, event) -> {
                    Player player = event.getPlayer();
                    if (!(inputs.remove(player) instanceof BookEditor editor)) return;
                    event.setCancelled(true);
                    editor.use(packet);
                })
                .add(PacketPlayInUpdateSign.class, (packet, event) -> {
                    Player player = event.getPlayer();
                    if (!(inputs.remove(player) instanceof SignEditor editor)) return;
                    event.setCancelled(true);
                    editor.use(packet);
                })
                .listen();
        lime.repeat(EditorUI::update, 1);
    }

    private static final HashMap<Player, IEditor<?>> inputs = new HashMap<>();

    public static void update() { inputs.entrySet().removeIf((kv)->!kv.getKey().isOnline()); }

    public static void openInput(Player player, Component title, Func3<Integer, PlayerInventory, EntityHuman, ContainerInput> init) {
        openInput(((CraftPlayer)player).getHandle(), title, init);
    }
    public static void openInput(EntityHuman player, Component title, Func3<Integer, PlayerInventory, EntityHuman, ContainerInput> init) {
        player.openMenu(new TileInventory(init::invoke, new AdventureComponent(title)));
    }
    public static void openSign(Player player, List<String> lines, Action1<List<String>> callback){
        new SignEditor(player, lines).callback(callback).open();
    }
    public static void openBook(Player player, List<Component> pages){ new BookEditor(player, pages, false).open(); }
    public static void openBook(Player player, List<Component> pages, Action1<List<String>> callback) {
        new BookEditor(player, pages, true).callback(callback).open();
    }

    private static abstract class IEditor<T extends Packet<?>> {
        public final Player player;
        public IEditor(Player player) {
            this.player = player;
        }
        public abstract void open();
        public abstract void use(T packet);
    }
    private static class SignEditor extends IEditor<PacketPlayInUpdateSign> {
        private static final int SIGN_LINES = 4;

        private final List<String> text;
        private final BlockPosition position;
        private Action1<List<String>> callback = null;
        public SignEditor(Player player, List<String> text) {
            super(player);
            text = new ArrayList<>(text);
            for (int i = text.size(); i < SIGN_LINES; i++) text.add("");
            this.text = text;

            Location location = player.getLocation();
            position = new net.minecraft.core.BlockPosition(location.getBlockX(), Math.max(location.getBlockY() - 4, location.getWorld().getMinHeight()), location.getBlockZ());
        }

        public SignEditor callback(Action1<List<String>> callback) {
            this.callback = callback;
            return this;
        }

        @Override public void open() {
            if (!player.isOnline()) return;
            player.closeInventory();

            TileEntitySign sign = new TileEntitySign(new BlockPosition(position.getX(), position.getY(), position.getZ()), Blocks.OAK_SIGN.defaultBlockState());
            SignText text = new SignText();
            for (int line = 0; line < SIGN_LINES; line++)
                if (this.text.size() > line)
                    text = text.setMessage(line, IChatBaseComponent.literal(color(this.text.get(line))));
            sign.setText(text, true);

            PacketManager.sendPacket(player, new ClientboundBundlePacket(List.of(
                    new PacketPlayOutBlockChange(position, Blocks.OAK_SIGN.defaultBlockState()),
                    sign.getUpdatePacket(),
                    new PacketPlayOutOpenSignEditor(position, true)
            )));
            inputs.put(player, this);
        }
        @Override public void use(PacketPlayInUpdateSign packet) {
            if (callback == null) return;
            if (!packet.getPos().equals(position)) return;
            lime.onceTicks(() -> {
                this.callback.invoke(Arrays.asList(packet.getLines()));
                if (!(player.isOnline() && player instanceof CraftPlayer cplayer)) return;
                PacketManager.sendPacket(player, new PacketPlayOutBlockChange(cplayer.getHandle().level(), position));
            }, 2);
        }
        private String color(String input) {
            return ChatColor.translateAlternateColorCodes('&', input);
        }
    }
    private static class BookEditor extends IEditor<PacketPlayInBEdit> {
        public final List<Component> pages;
        public final boolean editable;
        private Action1<List<String>> callback = null;

        public BookEditor(Player player, List<Component> pages, boolean editable) {
            super(player);
            this.pages = pages;
            this.editable = editable;
        }

        public BookEditor callback(Action1<List<String>> callback) {
            if (!editable) return this;
            this.callback = callback;
            return this;
        }

        private static int containerSlot(int index) {
            if (index < net.minecraft.world.entity.player.PlayerInventory.getSelectionSize()) {
                index += 36;
            } else if (index > 39) {
                index += 5;
            } else if (index > 35) {
                index = 8 - (index - 36);
            }
            return index;
        }

        private int containerSlot = -999;
        @Override public void open() {
            ItemStack show = new ItemStack(editable ? Material.WRITABLE_BOOK : Material.WRITTEN_BOOK);
            BookMeta meta = (BookMeta) show.getItemMeta();
            meta.pages(pages);
            meta.setAuthor("");
            meta.setTitle("");
            meta.setGeneration(BookMeta.Generation.ORIGINAL);
            show.setItemMeta(meta);

            final int slot = player.getInventory().getHeldItemSlot();
            final ItemStack old = player.getInventory().getItem(slot);

            EntityPlayer handle = ((CraftPlayer)player).getHandle();
            PlayerConnection connection = handle.connection;

            containerSlot = containerSlot(slot);

            connection.send(new PacketPlayOutSetSlot(handle.inventoryMenu.containerId, handle.inventoryMenu.incrementStateId(), containerSlot, CraftItemStack.asNMSCopy(show)));
            connection.send(new PacketPlayOutOpenBook(EnumHand.MAIN_HAND));
            connection.send(new PacketPlayOutSetSlot(handle.inventoryMenu.containerId, handle.inventoryMenu.incrementStateId(), containerSlot, CraftItemStack.asNMSCopy(old)));

            if (editable) inputs.put(player, this);
        }
        @Override public void use(PacketPlayInBEdit packet) {
            if (!editable) return;
            if (callback == null) return;
            if (packet.getSlot() != containerSlot) return;
            lime.onceTicks(() -> this.callback.invoke(packet.getPages()), 2);
        }
    }
}

