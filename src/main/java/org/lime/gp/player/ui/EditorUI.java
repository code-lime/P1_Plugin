package org.lime.gp.player.ui;

import io.papermc.paper.adventure.AdventureComponent;
import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.EnumHand;
import net.minecraft.world.TileInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.lime;
import org.lime.system;

import javax.annotation.Nullable;
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

    public static void openInput(Player player, Component title, system.Func3<Integer, PlayerInventory, EntityHuman, ContainerInput> init) {
        openInput(((CraftPlayer)player).getHandle(), title, init);
    }
    public static void openInput(EntityHuman player, Component title, system.Func3<Integer, PlayerInventory, EntityHuman, ContainerInput> init) {
        player.openMenu(new TileInventory(init::invoke, new AdventureComponent(title)));
    }
    public static void openSign(Player player, List<String> lines, system.Action1<List<String>> callback){
        new SignEditor(player, lines).callback(callback).open();
    }
    public static void openBook(Player player, List<Component> pages){ new BookEditor(player, pages, false).open(); }
    public static void openBook(Player player, List<Component> pages, system.Action1<List<String>> callback) {
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
        private system.Action1<List<String>> callback = null;
        public SignEditor(Player player, List<String> text) {
            super(player);
            text = new ArrayList<>(text);
            for (int i = text.size(); i < SIGN_LINES; i++) text.add("");
            this.text = text;

            Location location = player.getLocation();
            position = new net.minecraft.core.BlockPosition(location.getBlockX(), location.getWorld().getMinHeight(), location.getBlockZ());
        }

        public SignEditor callback(system.Action1<List<String>> callback) {
            this.callback = callback;
            return this;
        }

        @Override public void open() {
            if (!player.isOnline()) return;
            player.closeInventory();

            NBTTagCompound signNBT = new NBTTagCompound();

            for (int line = 0; line < SIGN_LINES; line++) signNBT.putString("Text" + (line + 1), this.text.size() > line ? String.format("{\"text\":\"%s\"}", color(this.text.get(line))) : "");

            signNBT.putInt("x", position.getX());
            signNBT.putInt("y", position.getY());
            signNBT.putInt("z", position.getZ());
            signNBT.putString("id", "minecraft:sign");
            signNBT.putString("Color", "black");
            signNBT.putBoolean("GlowingText", false);

            PacketManager.sendPacket(player, new PacketPlayOutBlockChange(position, Blocks.OAK_SIGN.defaultBlockState()));
            PacketManager.sendPacket(player, ReflectionAccess.init_PacketPlayOutTileEntityData.newInstance(position, TileEntityTypes.SIGN, signNBT));
            PacketManager.sendPacket(player, new PacketPlayOutOpenSignEditor(position));

            inputs.put(player, this);
        }
        @Override public void use(PacketPlayInUpdateSign packet) {
            if (callback == null) return;
            if (!packet.getPos().equals(position)) return;
            lime.onceTicks(() -> {
                this.callback.invoke(Arrays.asList(packet.getLines()));
                if (!(player.isOnline() && player instanceof CraftPlayer cplayer)) return;
                PacketManager.sendPacket(player, new PacketPlayOutBlockChange(cplayer.getHandle().level, position));
            }, 2);
        }
        private String color(String input) {
            return ChatColor.translateAlternateColorCodes('&', input);
        }
    }
    private static class BookEditor extends IEditor<PacketPlayInBEdit> {
        public final List<Component> pages;
        public final boolean editable;
        private system.Action1<List<String>> callback = null;

        public BookEditor(Player player, List<Component> pages, boolean editable) {
            super(player);
            this.pages = pages;
            this.editable = editable;
        }

        public BookEditor callback(system.Action1<List<String>> callback) {
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
    /*
    private static final class Menu {
        private final List<String> text;
        private net.minecraft.core.BlockPosition position;
        private system.Action2<Player, String[]> callback;

        Menu(List<String> text) {
            text = new ArrayList<>(text);
            for (int i = text.size(); i < 4; i++) text.add("");
            this.text = text;
        }

        public Menu callback(system.Action2<Player, String[]> callback) {
            this.callback = callback;
            return this;
        }

        public void open(Player player) {
            Objects.requireNonNull(player, "player");
            if (!player.isOnline()) return;
            player.closeInventory();
            Location location = player.getLocation();

            position = new net.minecraft.core.BlockPosition(location.getBlockX(), location.getWorld().getMinHeight(), location.getBlockZ());

            NBTTagCompound signNBT = new NBTTagCompound();

            for (int line = 0; line < SIGN_LINES; line++) {
                signNBT.putString("Text" + (line + 1), this.text.size() > line ? String.format(NBT_FORMAT, color(this.text.get(line))) : "");
            }
            signNBT.putInt("x", position.getX());
            signNBT.putInt("y", position.getY());
            signNBT.putInt("z", position.getZ());
            signNBT.putString("id", "minecraft:sign");
            signNBT.putString("Color", "black");
            signNBT.putBoolean("GlowingText", false);

            player.sendBlockChange(new Location(location.getWorld(), position.getX(), position.getY(), position.getZ()), Material.OAK_SIGN.createBlockData());
            PacketManager.sendPacket(player, ReflectionAccess.init_PacketPlayOutTileEntityData.newInstance(position, TileEntityTypes.SIGN, signNBT));
            PacketManager.sendPacket(player, new PacketPlayOutOpenSignEditor(position));

            lime.once(() -> inputs.put(player, this), 0.2);
        }

        public void close(Player player) {
            if (player.isOnline()) player.closeInventory();
        }
    }
    */
}

