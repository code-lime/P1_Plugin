package org.lime.gp.admin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.lime.plugin.CoreElement;
import org.lime.gp.database.Methods;
import org.lime.gp.lime;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.lime.system.execute.*;
import org.lime.system.json;
import org.lime.system.toast.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnyEvent {
    public static CoreElement create() {
        return CoreElement.create(AnyEvent.class)
                .addCommand("any.event", v -> v
                        .withExecutor(AnyEvent::execute)
                        .withTab(AnyEvent::executeTab)
                )
                .addCommand("other.event", v -> v
                        .withExecutor(AnyEvent::executeOther)
                        .withTab(AnyEvent::executeOtherTab)
                );
    }

    private record Event(String name, type other, List<Func0<Collection<String>>> tab, Action2<Player, String[]> event, Action1<Exception> error) {
        public boolean tryInvoke(boolean isOther, Player player, String[] args) {
            if (isOther != (other == type.other)) return false;
            if (args.length != tab.size()) return false;
            lime.log("Execute " + other.name() + "." + name + " of " + (player == null ? "NULL" : player.getUniqueId()) + " with " + json.by(args).build());
            Methods.commandExecute(player == null ? null : player.getUniqueId(), other.name() + "." + name + " " + json.by(args).build(), () -> {});
            try { event.invoke(player, args); }
            catch (Exception e) { lime.logStackTrace(e); }
            return true;
        }

        public Collection<String> getTab(int index) {
            return tab.size() > index ? tab.get(index).invoke() : Collections.emptyList();
        }
    }

    public static class Builder<T> {
        private final String event;
        private final List<Func0<Collection<String>>> tab;
        private final Func2<Player, String[], T> callback;
        private Action1<Exception> error;
        private Builder(String event, Func2<Player, String[], T> callback, List<Func0<Collection<String>>> tab, Action1<Exception> error) {
            this.event = event;
            this.tab = tab;
            this.callback = callback;
            this.error = error;
        }
        public static Builder<Player> create(String event) {
            return new Builder<>(event, (p,a) -> p, new ArrayList<>(), null);
        }
        public static <T>T nothing(T value) {
            return value;
        }
        public Builder<T> onError(Action1<Exception> error) {
            this.error = error;
            return this;
        }
        public Builder<Toast2<T,String>> createParam(String... values) {
            return createParam(v -> v, values);
        }
        @SuppressWarnings("unchecked")
        public <T1 extends Enum<T1>>Builder<Toast2<T,T1>> createParam(T1... values) {
            HashMap<String, T1> map = new HashMap<>();
            for (T1 value : values) map.put(value.name(), value);
            return createParam(map::get, map::keySet);
        }
        public <T1>Builder<Toast2<T,T1>> createParam(Func1<String, T1> parse, String... tab) {
            return createParam(parse, () -> Arrays.asList(tab));
        }
        public <T1>Builder<Toast2<T,T1>> createParam(Func1<String, T1> parse, Func0<Collection<String>> tab) {
            int arg = this.tab.size();
            this.tab.add(tab);
            return new Builder<>(event, (p, a) -> Toast.of(this.callback.invoke(p, a), parse.invoke(a[arg])), this.tab, this.error);
        }
        public Event build(type other, Action1<T> execute) {
            return new Event(event, other, tab, (p,a) -> execute.invoke(callback.invoke(p,a)), error);
        }
    }

    public enum type {
        none(false),
        other(true),
        owner(false),
        owner_console(false);

        public final boolean isOther;

        type(boolean isOther) { this.isOther = isOther; }
    }

    private record EventKey(String event, int argsLength, boolean other) { }
    private static final HashMap<EventKey, Event> events = new HashMap<>();

    public static void addEvent(String event, type other, Action1<Player> execute) { addEvent(event, other, b -> b, execute); }
    public static <T>void addEvent(String event, type other, Func1<Builder<Player>, Builder<T>> build, Action1<T> execute) {
        Event data = build.invoke(Builder.create(event)).build(other, execute);
        events.put(new EventKey(event, data.tab.size(), other.isOther), data);
    }

//<generator name="AnyEvent.js">
    public static <T0, T1>void addEvent(String event, type type, Func1<Builder<Player>, Builder<Toast2<T0,T1>>> build, Action2<T0, T1> execute) {
        addEvent(event, type, build, (Action1<Toast2<T0,T1>>)execute);
    }
    public static <T0, T1, T2>void addEvent(String event, type type, Func1<Builder<Player>, Builder<Toast2<Toast2<T0,T1>,T2>>> build, Action3<T0, T1, T2> execute) {
        addEvent(event, type, build, (Action1<Toast2<Toast2<T0,T1>,T2>>)execute);
    }
    public static <T0, T1, T2, T3>void addEvent(String event, type type, Func1<Builder<Player>, Builder<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>>> build, Action4<T0, T1, T2, T3> execute) {
        addEvent(event, type, build, (Action1<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>>)execute);
    }
    public static <T0, T1, T2, T3, T4>void addEvent(String event, type type, Func1<Builder<Player>, Builder<Toast2<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>,T4>>> build, Action5<T0, T1, T2, T3, T4> execute) {
        addEvent(event, type, build, (Action1<Toast2<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>,T4>>)execute);
    }
    public static <T0, T1, T2, T3, T4, T5>void addEvent(String event, type type, Func1<Builder<Player>, Builder<Toast2<Toast2<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>,T4>,T5>>> build, Action6<T0, T1, T2, T3, T4, T5> execute) {
        addEvent(event, type, build, (Action1<Toast2<Toast2<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>,T4>,T5>>)execute);
    }
    public static <T0, T1, T2, T3, T4, T5, T6>void addEvent(String event, type type, Func1<Builder<Player>, Builder<Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>>> build, Action7<T0, T1, T2, T3, T4, T5, T6> execute) {
        addEvent(event, type, build, (Action1<Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>>)execute);
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7>void addEvent(String event, type type, Func1<Builder<Player>, Builder<Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>,T7>>> build, Action8<T0, T1, T2, T3, T4, T5, T6, T7> execute) {
        addEvent(event, type, build, (Action1<Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>,T7>>)execute);
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8>void addEvent(String event, type type, Func1<Builder<Player>, Builder<Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>,T7>,T8>>> build, Action9<T0, T1, T2, T3, T4, T5, T6, T7, T8> execute) {
        addEvent(event, type, build, (Action1<Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>,T7>,T8>>)execute);
    }
//</generator>

    private static Collection<String> executeTab(CommandSender sender, String[] args) {
        int length = args.length;
        if (length == 1) return events.entrySet().stream().filter(v -> !v.getValue().other.isOther).map(Map.Entry::getKey).map(EventKey::event).collect(Collectors.toList());
        return events.entrySet()
                .stream()
                .filter(v -> v.getKey().event().equals(args[0]))
                .map(Map.Entry::getValue)
                .filter(event -> !event.other.isOther)
                .flatMap(event -> event.getTab(length - 2).stream())
                .collect(Collectors.toList());
    }
    private static boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) return false;
        Event event = events.getOrDefault(new EventKey(args[0], args.length - 1, false), null);
        if (event == null) return false;
        boolean isOp = sender.isOp();
        Player player = sender instanceof Player _v ? _v : null;

        switch (event.other) {
            case other: return false;

            case none: isOp = true;
            case owner: if (player == null) return false;
            case owner_console: if (!isOp) return false;
        }
        return event.tryInvoke(false, player, Arrays.stream(args).skip(1).toArray(String[]::new));
    }

    private static Collection<String> executeOtherTab(CommandSender sender, String[] args) {
        int length = args.length;
        if (length == 1) return Stream.concat(Bukkit.getOnlinePlayers()
                .stream()
                .map(Entity::getUniqueId)
                .map(UUID::toString), Stream.of(".self"))
                .collect(Collectors.toList());
        if (length == 2) return events.entrySet().stream().filter(v -> v.getValue().other.isOther).map(Map.Entry::getKey).map(EventKey::event).collect(Collectors.toList());

        return events.entrySet()
                .stream()
                .filter(v -> v.getKey().event().equals(args[1]))
                .map(Map.Entry::getValue)
                .filter(event -> event.other.isOther)
                .flatMap(event -> event.getTab(length - 3).stream())
                .collect(Collectors.toList());
    }
    private static boolean executeOther(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) return false;
        if (args.length < 2) return false;
        Player other;
        try {
            other = args[0].equals(".self") && sender instanceof Player self ? self : Bukkit.getPlayer(UUID.fromString(args[0]));
        } catch (Exception e) {
            lime.log("Execute other."+args[1]+" of " + args[0] + " with " + json.by(args).build());
            lime.logStackTrace(e);
            return false;
        }
        Event event = events.getOrDefault(new EventKey(args[1], args.length - 2, true), null);
        if (event == null || other == null) return false;
        return event.tryInvoke(true, other, Arrays.stream(args).skip(2).toArray(String[]::new));
    }
}