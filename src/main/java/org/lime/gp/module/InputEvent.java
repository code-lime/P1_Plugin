package org.lime.gp.module;

import org.lime.gp.lime;
import org.lime.packetwrapper.WrapperPlayClientSteerVehicle;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InputEvent extends Event {
    public static final InputEvent EMPTY = new InputEvent(Axis.None, Axis.None, false, false, null);
    private static final ConcurrentHashMap<UUID, Toast2<InputEvent, Long>> last_inputs = new ConcurrentHashMap<>();
    public static CoreElement create() {
        return CoreElement.create(InputEvent.class)
                .withInit(InputEvent::init);
    }
    public static void init() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(lime._plugin, PacketType.Play.Client.STEER_VEHICLE) {
            @Override public void onPacketReceiving(PacketEvent event) {
                WrapperPlayClientSteerVehicle packet = new WrapperPlayClientSteerVehicle(event.getPacket());
                Player player = event.getPlayer();
                InputEvent input = new InputEvent(packet, player);
                lime.invokeSync(() -> {
                    Bukkit.getServer().getPluginManager().callEvent(input);
                    last_inputs.put(player.getUniqueId(), Toast.of(input, System.currentTimeMillis() + 100));
                });
            }
        });
        lime.repeat(() -> {
            long now = System.currentTimeMillis();
            last_inputs.entrySet().removeIf(kv -> Bukkit.getPlayer(kv.getKey()) == null || kv.getValue().val1 < now);
        }, 1);
    }

    public static InputEvent last(Player player) {
        if (player == null) return EMPTY;
        Toast2<InputEvent, Long> input = last_inputs.getOrDefault(player.getUniqueId(), null);
        if (input != null) return input.val0;
        return new InputEvent(Axis.None, Axis.None, false, false, player);
    }

    public enum Axis {
        Up(1),
        None(0),
        Down(-1);

        private final int delta;
        public int delta() { return delta; }
        Axis(int delta) { this.delta = delta; }
        private static Axis getAxis(float axis) {
            if (axis > 0.5) return Axis.Up;
            else if (axis < -0.5) return Axis.Down;
            else return Axis.None;
        }
    }

    private final Player player;
    private final Entity vehicle;
    private final Axis vertical;
    private final Axis horizontal;
    private final boolean jump;
    private final boolean unmount;

    private InputEvent(Axis vertical, Axis horizontal, boolean jump, boolean unmount, Player player) {
        this.vertical = vertical;
        this.horizontal = horizontal;
        this.jump = jump;
        this.unmount = unmount;
        this.player = player;
        this.vehicle = player == null ? null : player.getVehicle();
    }
    private InputEvent(WrapperPlayClientSteerVehicle packet, Player player) {
        this(Axis.getAxis(packet.getForward()), Axis.getAxis(packet.getSideways()), packet.isJump(), packet.isUnmount(), player);
    }

    public boolean isJump() { return jump; }
    public boolean isUnmount() { return unmount; }
    public Player getPlayer() { return player; }
    public Entity getVehicle() { return vehicle; }
    public Axis getVertical() { return vertical; }
    public Axis getHorizontal() { return horizontal; }

    private static final HandlerList handlers = new HandlerList();
    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}



















