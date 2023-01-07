package org.lime.gp.module;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.util.BlockStateListPopulator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.util.Vector;
import org.lime.core;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.lime;
import org.lime.system;
import org.lime.gp.player.ui.Infection;
import org.lime.gp.extension.ScoreboardManager;
import org.lime.gp.block.FastEditSession;

import java.util.*;

public class Nether implements Listener {
    public static boolean isEnable = false;
    public static class NetherConfig {
        public static int size;
        public static int border;
        public static int destroy_time;
        public static int min_regen_time;
        public static int max_regen_time;

        public static void config(JsonObject json) {
            size = json.get("size").getAsInt();
            border = json.get("border").getAsInt();
            destroy_time = json.get("destroy_time").getAsInt();
            JsonObject regen_time = json.getAsJsonObject("regen_time");
            min_regen_time = system.formattedTime(regen_time.get("min").getAsString());
            max_regen_time = system.formattedTime(regen_time.get("max").getAsString());
        }
    }
    public static class PortalConfig {
        public static Vector position;
        public static double radius;

        public static Location location() { return new Location(lime.MainWorld, 0,0,0).add(PortalConfig.position); }

        public static void config(JsonObject json) {
            position = system.getVector(json.get("pos").getAsString());
            radius = json.get("radius").getAsDouble();
        }
    }
    private static double infection;

    public static core.element create() {
        return core.element.create(Nether.class)
                .withInit(Nether::init)
                .withInstance()
                //.disable()
                .<JsonObject>addConfig("nether", v -> v
                        .withInvoke(Nether::config)
                        .withDefault(system.json.object()
                                .addObject("portal", _v -> _v
                                        .add("pos", "0 0 0")
                                        .add("radius", 5)
                                )
                                .addObject("nether", _v -> _v
                                        .add("size", 300)
                                        .add("border", 10)
                                        .add("destroy_time", 60)
                                        .addObject("regen_time", __v -> __v
                                                .add("min", "1d16h")
                                                .add("max", "2d5h")
                                        )
                                )
                                .add("infection_sec", 600)
                                .add("enable", false)
                                //.add("meteor_sec", -0.2)
                                .build()
                        )
                );
    }
    public static void init() {
        Infection.add("nether", player -> {
            if (player.getWorld() != lime.NetherWorld) return 0;
            Set<String> tags = player.getScoreboardTags();
            if (tags.contains("infection.immunity")) return 0;
            //if (tags.contains("meteor")) return meteor;
            return infection;
        });
        AnyEvent.addEvent("regen.nether", AnyEvent.type.owner_console, player -> Nether.regenNether());
        AnyEvent.addEvent("destroy.nether", AnyEvent.type.owner_console, builder -> builder.createParam(Integer::parseInt, "[time]"), (player, value) -> Nether.destroyNether(value, () -> lime.logOP("Nether destroyed!")));
        lime.repeat(Nether::update, 1);
    }
    public static void config(JsonObject json) {
        isEnable = !json.has("enable") || json.get("enable").getAsBoolean();
        PortalConfig.config(json.get("portal").getAsJsonObject());
        NetherConfig.config(json.get("nether").getAsJsonObject());
        infection = 20 / json.get("infection_sec").getAsDouble();
        //meteor = json.get("meteor").getAsDouble();
    }
    private static boolean regening = false;
    public static void update() {
        if (!isEnable) return;
        if (regening) return;
        int value = ScoreboardManager.edit("nether.timer", "value", v -> Math.max(0, v - 1), () -> system.rand(NetherConfig.min_regen_time, NetherConfig.max_regen_time));
        if (value <= 0) {
            ScoreboardManager.reset("nether.timer", "value");
            regening = true;
            destroyNether(NetherConfig.destroy_time, Nether::regenNether);
        }
    }

    public static void destroyNether(int time, system.Action0 callback) {
        WorldBorder border = lime.NetherWorld.getWorldBorder();
        WorldServer handle = ((CraftWorld)lime.NetherWorld).getHandle();
        Vector center = border.getCenter().toVector();
        int center_x = center.getBlockX();
        int center_z = center.getBlockZ();
        int size = (int)Math.ceil(border.getSize() / 2);
        for (int i = 0; i < size; i++) {
            int _size = size - i;
            lime.onceTicks(() -> destroyNether(handle, time, center_x, center_z, _size), i * time + 1);
        }
        if (callback == null) return;
        lime.onceTicks(callback, size * time + 1);
    }
    private static void destroyNether(WorldServer handle, int time, int center_x, int center_z, int size) {
        List<system.Toast2<Integer, Integer>> list = getSize(center_x, center_z, size, true);
        int length = list.size();
        int delta = (int)Math.ceil(length / (double)time);
        for (int i = 0; i < length; i += delta) {
            lime.onceTicks(() -> {
                BlockStateListPopulator blockList = new BlockStateListPopulator(handle);
                for (int _i = 0; _i < delta; _i++) {
                    system.Toast2<Integer, Integer> pos = list.size() == 0 ? null : list.remove(0);
                    if (pos == null) continue;
                    int x = pos.val0;
                    int z = pos.val1;
                    for (int y = 0; y < 256; y++) {
                        BlockPosition position = new BlockPosition(x,y,z);
                        blockList.setBlock(position, Blocks.BARRIER.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
                blockList.updateList();
            }, i + 1);
        }
        lime.onceTicks(() -> {
            BlockStateListPopulator blockList = new BlockStateListPopulator(handle);
            list.removeIf(pos -> {
                int x = pos.val0;
                int z = pos.val1;
                for (int y = 0; y < 256; y++) {
                    BlockPosition position = new BlockPosition(x,y,z);
                    blockList.setBlock(position, Blocks.BARRIER.defaultBlockState(), Block.UPDATE_ALL);
                }
                return true;
            });
            blockList.updateList();
        }, length + 1);
    }
    private static List<system.Toast2<Integer, Integer>> getSize(int center_x, int center_z, int size, boolean shuffle) {
        List<system.Toast2<Integer, Integer>> list = new ArrayList<>();
        int delta = size - 1;
        for (int i = -delta; i <= delta; i++) {
            list.add(system.toast(center_x + i, center_z + delta));
            list.add(system.toast(center_x + i, center_z - delta));
            list.add(system.toast(center_x + delta, center_z + i));
            list.add(system.toast(center_x - delta, center_z + i));
        }
        if (shuffle) Collections.shuffle(list);
        return list;
    }
    public static void regenNether() {
        WorldBorder border = lime.NetherWorld.getWorldBorder();
        double size = border.getSize() + 20;
        Vector pos = border.getCenter().toVector();
        int offset = (int)(Math.max(Math.abs(pos.getX()), Math.abs(pos.getZ())) + size / 2) + NetherConfig.size / 2 + NetherConfig.border + 1;

        int offset_x = offset;
        int offset_y = offset;

        border.setCenter(offset_x + 0.5, offset_y + 0.5);
        border.setSize(NetherConfig.size + (NetherConfig.border * 2) - 2);
        WorldServer handle = ((CraftWorld)lime.NetherWorld).getHandle();
        FastEditSession session = new FastEditSession(handle);
        int length = ((int)Math.ceil(NetherConfig.size + 1) / 2) + 1;
        IBlockData barrier = Blocks.BARRIER.defaultBlockState();
        IBlockData black_concrete = Blocks.BLACK_CONCRETE.defaultBlockState();
        lime.invokeAsync(() -> {
            getSize(offset_x, offset_y, length + NetherConfig.border, false).forEach(p -> {
                int x = p.val0;
                int z = p.val1;
                for (int y = 0; y < 256; y++) session.set(new BlockPosition(x, y, z), black_concrete);
            });
            for (int i = 0; i < NetherConfig.border; i++) {
                int _length = length + i;
                getSize(offset_x, offset_y, _length, false).forEach(p -> {
                    int x = p.val0;
                    int z = p.val1;
                    for (int y = 0; y < 256; y++) session.set(new BlockPosition(x, y, z), barrier);
                });
            }
            part(getSize(offset_x, offset_y, length - 1, true), 0.5, p -> {
                int x = p.val0;
                int z = p.val1;
                for (int y = 0; y < 256; y++) session.set(new BlockPosition(x, y, z), barrier);
            });
        }, () -> {
            session.update(300);

            create_portal = true;
            handle.getPortalForcer().createPortal(new BlockPosition(offset_x, 64, offset_y), EnumDirection.EnumAxis.X);
            create_portal = false;
            regening = false;
        });
    }
    private static <T>void part(List<T> list, double part, system.Action1<T> callback) {
        double size = list.size() * part;
        for (int i = 0; i < size; i++) callback.invoke(list.get(i));
    }

    private static boolean create_portal = false;
    @EventHandler public static void on(PortalCreateEvent e) {
        if (create_portal) return;
        if (e.getReason() == PortalCreateEvent.CreateReason.FIRE
                && e.getWorld() == lime.MainWorld
                && isEnable
                && e.getBlocks().stream().anyMatch(v -> v.getLocation().toVector().distance(PortalConfig.position) < PortalConfig.radius)
        ) return;
        e.setCancelled(true);
    }
    @EventHandler public static void on(PlayerPortalEvent e) {
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) return;
        if (!isEnable){
            e.setCancelled(true);
            return;
        }
        e.setCanCreatePortal(false);
        if (e.getFrom().getWorld() == lime.MainWorld) {
            if (e.getFrom().toVector().distance(PortalConfig.position) > PortalConfig.radius) {
                e.setCancelled(true);
                return;
            }
            e.setTo(lime.NetherWorld.getWorldBorder().getCenter().add(0, 64, 0));
        } else if (e.getFrom().getWorld() == lime.NetherWorld) e.setTo(PortalConfig.location());
        else e.setCancelled(true);
    }
}





















