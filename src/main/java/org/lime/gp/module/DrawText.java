package org.lime.gp.module;

import com.google.common.collect.Streams;
import net.kyori.adventure.text.Component;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.entity.EntityAreaEffectCloud;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.entity.Player;
import org.lime.core;
import org.lime.display.DisplayManager;
import org.lime.display.Displays;
import org.lime.display.EditedDataWatcher;
import org.lime.display.ObjectDisplay;
import org.lime.gp.lime;
import org.lime.system;
import org.lime.gp.chat.ChatHelper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DrawText {
    public static core.element create() {
        return core.element.create(DrawText.class)
                .withInit(DrawText::init);
    }
    private static final TextManager TEXT_MANAGER = new TextManager();
    public static void init() {
        Displays.initDisplay(TEXT_MANAGER);
        lime.repeatTicks(() -> shows.values().removeIf(IShow::tryRemove), 1);
    }

    private static long next = 0;
    private static long getNext() { return next++; }

    private static final ConcurrentHashMap<String, IShow> shows = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<IShowGroup> groups = new ConcurrentLinkedQueue<>();

    public abstract static class IShowTimed implements IShow {
        private final long next;
        public IShowTimed(double sec) {
            next = System.currentTimeMillis() + Math.round(sec * 1000);
        }
        @Override public boolean tryRemove() { return next < System.currentTimeMillis(); }
    }

    public static Optional<Integer> getEntityID(String id) {
        return TEXT_MANAGER.getDisplay(id).map(v -> v.entityID);
    }

    public interface IShow {
        String getID();

        boolean filter(Player player);
        Component text(Player player);
        default Optional<Integer> parent() { return Optional.empty(); }
        Location location();
        double distance();
        boolean tryRemove();

        static IShow create(Player player, Location location, Component text, double sec) {
            UUID uuid = player.getUniqueId();
            long next = System.currentTimeMillis() + Math.round(sec * 1000);
            String id = getNext() + "";
            return new IShow() {
                @Override public String getID() { return id; }

                @Override public boolean filter(Player player) { return uuid.equals(player.getUniqueId()); }
                @Override public Component text(Player player) { return text; }
                @Override public Location location() { return location; }
                @Override public double distance() { return 5; }
                @Override public boolean tryRemove() { return next < System.currentTimeMillis(); }
            };
        }
        static IShow create(Location location, Component text, double sec) {
            long next = System.currentTimeMillis() + Math.round(sec * 1000);
            String id = getNext() + "";
            return new IShow() {
                @Override public String getID() { return id; }

                @Override public boolean filter(Player player) { return true; }
                @Override public Component text(Player player) { return text; }
                @Override public Location location() { return location; }
                @Override public double distance() { return 5; }
                @Override public boolean tryRemove() { return next < System.currentTimeMillis(); }
            };
        }
    }
    public static abstract class IShowID implements IShow {
        public final String id;
        @Override public String getID() { return id; }
        public IShowID(String id) { this.id = id; }
    }
    public interface IShowGroup {
        Stream<IShow> list();

        static <T>IShowGroup create(Collection<T> data, system.Func1<T, IShow> show) {
            return () -> data.stream().map(show);
        }
    }

    public static void show(IShow show) {
        shows.put(show.getID(), show);
    }
    public static void load(IShowGroup group) {
        groups.add(group);
    }

    public static class TextDisplay extends ObjectDisplay<IShow, EntityAreaEffectCloud> {
        private IShow show;
        private Integer last_parent = null;

        @Override public Location location() { return show.location(); }
        @Override public double getDistance() { return show.distance(); }
        @Override public boolean isFilter(Player player) { return show.filter(player); }

        protected TextDisplay(IShow data) {
            show = data;
            postInit();
        }

        @Override protected void editDataWatcher(Player player, EditedDataWatcher dataWatcher) {
            super.editDataWatcher(player, dataWatcher);
            Component component = show.text(player);
            if (component == null) {
                hide(player);
                return;
            }
            keyOf(player, "last_text", component);
            dataWatcher.setCustom(EditedDataWatcher.DATA_CUSTOM_NAME, Optional.of(ChatHelper.toNMS(component)));
        }

        @Override public void update(IShow iShow, double delta) {
            show.parent().filter(parent -> !Objects.equals(parent, last_parent)).ifPresent(parent -> {
                last_parent = parent;
                Displays.addPassengerID(parent, entityID);
                hideAll();
            });

            super.update(iShow, delta);
            Location location = lastLocation();
            entity.moveTo(location.getX(), location.getY(), location.getZ());

            this.show = iShow;

            invokeAll(this::update);
        }

        public void update(Player player) {
            Component old = this.<Component>keyOf(player, "last_text").orElse(null);
            Component component = show.text(player);
            if (Objects.equals(old, component)) return;
            sendDataWatcher(player);
        }

        private static final IChatBaseComponent BASE_TEXT = ChatHelper.toNMS(ChatHelper.formatComponent("<#FF0000>Loading...</>"));
        @Override protected EntityAreaEffectCloud createEntity(Location location) {
            EntityAreaEffectCloud stand = new EntityAreaEffectCloud(
                    ((CraftWorld)location.getWorld()).getHandle(),
                    location.getX(), location.getY(), location.getZ());
            stand.setDuration(2000000000);
            stand.tickCount = 2000000000;
            stand.setCustomName(BASE_TEXT);
            stand.setRadius(0);
            stand.setCustomNameVisible(true);
            return stand;
        }
    }
    public static class TextManager extends DisplayManager<String, IShow, TextDisplay> {
        @Override public boolean isFast() { return true; }
        @Override public boolean isAsync() { return true; }
        @Override public Map<String, IShow> getData() {
            return Streams.concat(shows.values().stream(), groups.stream().flatMap(IShowGroup::list)).collect(Collectors.toMap(IShow::getID, v -> v));
        }

        @Override public TextDisplay create(String id, IShow show) { return new TextDisplay(show); }
    }
}






















