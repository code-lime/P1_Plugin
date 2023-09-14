package org.lime.gp.map;

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.Rotation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.lime.Position;
import org.lime.plugin.CoreElement;
import org.lime.display.Displays;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.lime;
import org.lime.gp.module.TimeoutData;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.*;

public class MapMonitor implements Listener {
    public static final class MonitorData extends TimeoutData.ITimeout {
        public Position position;
        public Vector offset;
        public MapRotation rotation;
        public double distance;
        public int mapID;
        public boolean isShow;
        public byte[] map;

        public MonitorData(MonitorInstance instance) {
            super(5);
            this.isShow = instance.isShow();
            this.map = instance.preMap();
            this.mapID = instance.MapID;
            this.rotation = instance.rotation();
            this.distance = instance.distance();
            this.offset = instance.offset();
            this.position = instance.metadata().position();
        }
    }

    public static CoreElement create() {
        return CoreElement.create(MapMonitor.class)
                .withInstance()
                .withInit(MapMonitor::init);
    }
    public static final MonitorDisplay.MonitorManager MONITOR_MANAGER = MonitorDisplay.manager();
    public static void init() {
        Displays.initDisplay(MONITOR_MANAGER);
        lime.repeatTicks(() -> {
            HashMap<MonitorInstance, List<Player>> tickMouse = new HashMap<>();
            Bukkit.getOnlinePlayers().forEach(player -> onMouse(player, false, monitor -> tickMouse.compute(monitor, (k, v) -> {
                if (v == null) v = new ArrayList<>();
                v.add(player);
                return v;
            })));
            TimeoutData.values(MonitorData.class)
                    .map(v -> Blocks.of(v.position.getBlock())
                            .flatMap(Blocks::customOf)
                            .flatMap(metadata -> metadata.list(MonitorInstance.class).findAny())
                    )
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(instance -> instance.onPostMouse(Optional.ofNullable(tickMouse.get(instance)).orElseGet(Collections::emptyList)));
        }, 10);
    }
    @EventHandler private static void on(PlayerInteractEvent e) {
        switch (e.getAction()) {
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK: break;
            default: return;
        }
        onMouse(e.getPlayer(), true, m -> e.setCancelled(true));
    }
    @EventHandler private static void on(PlayerUseUnknownEntityEvent e) {
        if (e.isAttack()) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        onMouse(e.getPlayer(), true, m -> {});
    }
    private static void onMouse(Player player, boolean isClick, Action1<MonitorInstance> clicked) {
        ClickType type = isClick ? player.isSneaking() ? ClickType.Shift : ClickType.Click : ClickType.None;
        Vector eyePosition = player.getEyeLocation().toVector();
        TimeoutData.values(MonitorData.class)
                .filter(v -> v.position.world == player.getWorld())
                .map(v -> Toast.of(v.position.toVector().distanceSquared(eyePosition), v))
                .filter(v -> v.val0 < 5 * 5)
                .sorted(Comparator.comparingDouble(v -> v.val0))
                .map(v -> Blocks.of(v.val1.position.getBlock())
                        .flatMap(Blocks::customOf)
                        .flatMap(metadata -> metadata.list(MonitorInstance.class).findAny())
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(monitor -> ViewPosition.of(monitor, player, type).map(v -> Toast.of(monitor, v)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .min(Comparator.comparingDouble(v -> v.val1.getLocation().toVector().distanceSquared(eyePosition)))
                .ifPresent(v -> {
                    v.val0.onMouse(player, v.val1);
                    clicked.invoke(v.val0);
                });
    }

    public enum MapRotation {
        NONE(Rotation.NONE),
        CLOCKWISE(Rotation.CLOCKWISE_45),
        FLIPPED(Rotation.CLOCKWISE),
        COUNTER_CLOCKWISE(Rotation.CLOCKWISE_135);

        public final Rotation rotation;

        MapRotation(Rotation rotation) { this.rotation = rotation; }

        public <T>T[] rotateMap(int sizeX, int sizeY, T[] data) {
            int count = switch (this) {
                case NONE -> 0;
                case CLOCKWISE -> 1;
                case FLIPPED -> 2;
                case COUNTER_CLOCKWISE -> 3;
            };

            for (int i = 0; i < count; i++) {
                T[] out_data = Arrays.copyOf(data, data.length);
                for (int y = 0; y < sizeY; y++) {
                    for (int x = 0; x < sizeX; x++) {
                        out_data[sizeY - y - 1 + x * sizeY] = data[x + y * sizeX];
                    }
                }
                data = out_data;
            }

            return data;


            /*Toast2<Integer, Integer> out = Toast.of(x - center_x, y - center_y);
            for (int i = 0; i < count; i++) out = Toast.of(out.val1, -out.val0);
            return Toast.of(out.val0 + center_x, out.val1 + center_y);*/
            //return data;
        }
        public static MapRotation of(InfoComponent.Rotation.Value rotation) {
            return switch (rotation) {
                case ANGLE_0, ANGLE_45 -> NONE;
                case ANGLE_90, ANGLE_135 -> CLOCKWISE;
                case ANGLE_180, ANGLE_225 -> FLIPPED;
                case ANGLE_270, ANGLE_315 -> COUNTER_CLOCKWISE;
            };
        }
    }
    public enum ClickType {
        None(false, false),
        Click(true, false),
        Shift(true, true);

        public final boolean isClick;
        public final boolean isShift;

        ClickType(boolean isClick, boolean isShift) {
            this.isClick = isClick;
            this.isShift = isShift;
        }
    }
}








