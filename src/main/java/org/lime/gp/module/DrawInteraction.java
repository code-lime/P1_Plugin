package org.lime.gp.module;

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import com.google.common.collect.Streams;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.lime.display.DisplayManager;
import org.lime.display.Displays;
import org.lime.display.MoveObjectDisplay;
import org.lime.display.Passenger;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;
import org.lime.system.execute.Func1;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DrawInteraction implements Listener {
    public static CoreElement create() {
        return CoreElement.create(DrawInteraction.class)
                .withInstance()
                .withInit(DrawInteraction::init);
    }
    private static final InteractionManager INTERACTION_MANAGER = new InteractionManager();
    public static void init() {
        Displays.initDisplay(INTERACTION_MANAGER);
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
        return INTERACTION_MANAGER.getDisplay(id).map(v -> v.entityID);
    }

    public interface IShow {
        String getID();

        boolean filter(Player player);
        default Optional<Integer> parent() { return Optional.empty(); }
        Location location();
        double width();
        double height();
        double distance();
        boolean tryRemove();

        void click(PlayerUseUnknownEntityEvent event);
    }
    public static abstract class IShowID implements IShow {
        public final String id;
        @Override public String getID() { return id; }
        public IShowID(String id) { this.id = id; }
    }
    public interface IShowGroup {
        Stream<IShow> list();

        static <T>IShowGroup create(Collection<T> data, Func1<T, IShow> show) {
            return () -> data.stream().map(show);
        }
    }

    public static void show(IShow show) {
        shows.put(show.getID(), show);
    }
    public static void load(IShowGroup group) {
        groups.add(group);
    }

    public static class InteractionDisplay extends MoveObjectDisplay<IShow, Interaction> {
        private IShow show;
        private Integer last_parent = null;

        @Override public Location location() {
            Location location = show.location();
            return new Location(location.getWorld(), location.getX(), location.getY(), location.getZ());
        }
        @Override public double getDistance() { return show.distance(); }
        @Override public boolean isFilter(Player player) { return show.filter(player); }

        protected InteractionDisplay(IShow data) {
            show = data;
            postInit();
        }

        @Override public void update(IShow iShow, double delta) {
            show.parent().filter(parent -> !Objects.equals(parent, last_parent)).ifPresent(parent -> {
                last_parent = parent;
                Passenger.addPassengerID(parent, entityID);
                hideAll();
            });

            super.update(iShow, delta);

            this.show = iShow;

            invokeAll(this::sendData);
        }
        @Override protected void show(Player player) {
            super.show(player);
            keyOf(player, "init", true);
        }
        @Override public void hide(Player player) {
            super.hide(player);
        }

        @Override protected Interaction createMoveEntity(Location location) {
            Interaction interaction = new Interaction(EntityTypes.INTERACTION, ((CraftWorld)location.getWorld()).getHandle());
            interaction.moveTo(location.getX(), location.getY(), location.getZ());
            interaction.setWidth((float) show.width());
            interaction.setHeight((float) show.height());
            return interaction;
        }
    }
    /*public static class TextDisplay extends ObjectDisplay<IShow, Display.TextDisplay> {
        private IShow show;
        private Integer last_parent = null;

        @Override public Location location() { return show.location(); }
        @Override public double getDistance() { return show.distance(); }
        @Override public boolean isFilter(Player player) { return show.filter(player); }

        private boolean DEBUG = false;

        protected TextDisplay(IShow data) {
            show = data;
            DEBUG = data instanceof NickName.ShowNickName;
            postInit();
        }

        @Override protected void editDataWatcher(Player player, EditedDataWatcher dataWatcher) {
            super.editDataWatcher(player, dataWatcher);
            if (DEBUG) lime.logOP("[EDW." + player.getName() + "] C.0");
            Component component = show.text(player);
            if (DEBUG) lime.logOP("[EDW." + player.getName() + "] C.1");
            if (component == null) {
                if (DEBUG) lime.logOP("[EDW." + player.getName() + "] C.HIDE");
                hide(player);
                return;
            }
            if (DEBUG) lime.logOP(Component.text("[EDW." + player.getName() + "] C.2").append(component));
            keyOf(player, "last_text", component);
            if (DEBUG) lime.logOP("[EDW." + player.getName() + "] C.3");
            dataWatcher.setCustom(EditedDataWatcher.DATA_TEXT_ID, ChatHelper.toNMS(component));
            if (DEBUG) lime.logOP("[EDW." + player.getName() + "] C.4");
        }

        @Override public void update(IShow iShow, double delta) {
            show.parent().filter(parent -> !Objects.equals(parent, last_parent)).ifPresent(parent -> {
                last_parent = parent;
                Displays.addPassengerID(parent, entityID);
                hideAll();
            });

            super.update(iShow, delta);
            Location location = lastLocation();
            entity.teleportTo(null, delta, delta, delta, null, entityID, next)
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

        @Override protected void show(Player player) {
            if (DEBUG) lime.logOP("[SHOW." + player.getName() + "] 0");
            super.show(player);
            if (DEBUG) lime.logOP("[SHOW." + player.getName() + "] 1");
        }
        @Override public void hide(Player player) {
            if (DEBUG) lime.logOP("[HIDE." + player.getName() + "] 0");
            super.hide(player);
            if (DEBUG) lime.logOP("[HIDE." + player.getName() + "] 1");
        }

        private static final IChatBaseComponent BASE_TEXT = ChatHelper.toNMS(ChatHelper.formatComponent("<#FF0000>Loading...</>"));
        @Override protected Display.TextDisplay createEntity(Location location) {
            Display.TextDisplay text = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, ((CraftWorld)location.getWorld()).getHandle());
            text.moveTo(location.getX(), location.getY(), location.getZ());
            text.setText(BASE_TEXT);
            text.setTransformation(new Transformation(new Vector3f(0, 1, 0), null, null, null));
            text.setLineWidth(100000);
            text.setBillboardConstraints(Display.BillboardConstraints.VERTICAL);
            return text;
        }
    }*/
    public static class InteractionManager extends DisplayManager<String, IShow, InteractionDisplay> {
        @Override public boolean isFast() { return true; }
        @Override public boolean isAsync() { return true; }
        @Override public Map<String, IShow> getData() {
            return Streams.concat(shows.values().stream(), groups.stream().flatMap(IShowGroup::list)).collect(Collectors.toMap(IShow::getID, v -> v));
        }

        @Override public InteractionDisplay create(String id, IShow show) { return new InteractionDisplay(show); }
    }

    @EventHandler public static void on(PlayerUseUnknownEntityEvent event) {
        int eid = event.getEntityId();
        INTERACTION_MANAGER.getDisplays().values().forEach(display -> {
            if (display.entityID != eid) return;
            display.show.click(event);
        });
    }
}






















