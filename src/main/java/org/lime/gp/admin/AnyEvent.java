package org.lime.gp.admin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.lime.core;
import org.lime.gp.database.Methods;
import org.lime.gp.lime;
import org.lime.system;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnyEvent {
    public static core.element create() {
        return core.element.create(AnyEvent.class)
                .addCommand("any.event", v -> v
                        .withExecutor(AnyEvent::execute)
                        .withTab(AnyEvent::executeTab)
                )
                .addCommand("other.event", v -> v
                        .withExecutor(AnyEvent::executeOther)
                        .withTab(AnyEvent::executeOtherTab)
                );
    }

    private record Event(String name, type other, List<system.Func0<Collection<String>>> tab, system.Action2<Player, String[]> event, system.Action1<Exception> error) {
        public boolean TryInvoke(boolean isOther, Player player, String[] args) {
            if (isOther != (other == type.other)) return false;
            if (args.length != tab.size()) return false;
            lime.log("Execute " + other.name() + "." + name + " of " + (player == null ? "NULL" : player.getUniqueId()) + " with " + system.json.by(args).build());
            Methods.commandExecute(player == null ? null : player.getUniqueId(), other.name() + "." + name + " " + system.json.by(args).build(), () -> {});
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
        private final List<system.Func0<Collection<String>>> tab;
        private final system.Func2<Player, String[], T> callback;
        private system.Action1<Exception> error;
        private Builder(String event, system.Func2<Player, String[], T> callback, List<system.Func0<Collection<String>>> tab, system.Action1<Exception> error) {
            this.event = event;
            this.tab = tab;
            this.callback = callback;
        }
        public static Builder<Player> create(String event) {
            return new Builder<>(event, (p,a) -> p, new ArrayList<>(), null);
        }
        public static <T>T nothing(T value) {
            return value;
        }
        public Builder<T> onError(system.Action1<Exception> error) {
            this.error = error;
            return this;
        }
        public Builder<system.Toast2<T,String>> createParam(String... values) {
            return createParam(v -> v, values);
        }
        @SuppressWarnings("unchecked")
        public <T1 extends Enum<T1>>Builder<system.Toast2<T,T1>> createParam(T1... values) {
            HashMap<String, T1> map = new HashMap<>();
            for (T1 value : values) map.put(value.name(), value);
            return createParam(map::get, map::keySet);
        }
        public <T1>Builder<system.Toast2<T,T1>> createParam(system.Func1<String, T1> parse, String... tab) {
            return createParam(parse, () -> Arrays.asList(tab));
        }
        public <T1>Builder<system.Toast2<T,T1>> createParam(system.Func1<String, T1> parse, system.Func0<Collection<String>> tab) {
            int arg = this.tab.size();
            this.tab.add(tab);
            return new Builder<>(event, (p, a) -> system.toast(this.callback.invoke(p, a), parse.invoke(a[arg])), this.tab, this.error);
        }
        public Event build(type other, system.Action1<T> execute) {
            return new Event(event, other, tab, (p,a) -> execute.invoke(callback.invoke(p,a)), error);
        }
    }

    public enum type {
        none,
        other,
        owner,
        owner_console;
    }

    private static final HashMap<system.Toast2<String, Integer>, Event> events = new HashMap<>();
    public static void addEvent(String event, type other, system.Action1<Player> execute) { addEvent(event, other, b -> b, execute); }
    public static <T>void addEvent(String event, type other, system.Func1<Builder<Player>, Builder<T>> build, system.Action1<T> execute) {
        Event data = build.invoke(Builder.create(event)).build(other, execute);
        events.put(system.toast(event, data.tab.size()), data);
    }

//<generator name="AnyEvent.js">
    public static <T0, T1>void addEvent(String event, type other, system.Func1<Builder<Player>, Builder<system.Toast2<T0,T1>>> build, system.Action2<T0, T1> execute) {
        addEvent(event, other, build, (system.Action1<system.Toast2<T0,T1>>)execute);
    }
    public static <T0, T1, T2>void addEvent(String event, type other, system.Func1<Builder<Player>, Builder<system.Toast2<system.Toast2<T0,T1>,T2>>> build, system.Action3<T0, T1, T2> execute) {
        addEvent(event, other, build, (system.Action1<system.Toast2<system.Toast2<T0,T1>,T2>>)execute);
    }
    public static <T0, T1, T2, T3>void addEvent(String event, type other, system.Func1<Builder<Player>, Builder<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>>> build, system.Action4<T0, T1, T2, T3> execute) {
        addEvent(event, other, build, (system.Action1<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>>)execute);
    }
    public static <T0, T1, T2, T3, T4>void addEvent(String event, type other, system.Func1<Builder<Player>, Builder<system.Toast2<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>,T4>>> build, system.Action5<T0, T1, T2, T3, T4> execute) {
        addEvent(event, other, build, (system.Action1<system.Toast2<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>,T4>>)execute);
    }
    public static <T0, T1, T2, T3, T4, T5>void addEvent(String event, type other, system.Func1<Builder<Player>, Builder<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>,T4>,T5>>> build, system.Action6<T0, T1, T2, T3, T4, T5> execute) {
        addEvent(event, other, build, (system.Action1<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>,T4>,T5>>)execute);
    }
    public static <T0, T1, T2, T3, T4, T5, T6>void addEvent(String event, type other, system.Func1<Builder<Player>, Builder<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>>> build, system.Action7<T0, T1, T2, T3, T4, T5, T6> execute) {
        addEvent(event, other, build, (system.Action1<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>>)execute);
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7>void addEvent(String event, type other, system.Func1<Builder<Player>, Builder<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>,T7>>> build, system.Action8<T0, T1, T2, T3, T4, T5, T6, T7> execute) {
        addEvent(event, other, build, (system.Action1<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>,T7>>)execute);
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8>void addEvent(String event, type other, system.Func1<Builder<Player>, Builder<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>,T7>,T8>>> build, system.Action9<T0, T1, T2, T3, T4, T5, T6, T7, T8> execute) {
        addEvent(event, other, build, (system.Action1<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>,T7>,T8>>)execute);
    }
//</generator>

    private static Collection<String> executeTab(CommandSender sender, String[] args) {
        int length = args.length;
        if (length == 1) return events.entrySet().stream().filter(v -> v.getValue().other != type.other).map(Map.Entry::getKey).map(v -> v.val0).collect(Collectors.toList());
        return events.entrySet()
                .stream()
                .filter(v -> v.getKey().val0.equals(args[0]))
                .map(Map.Entry::getValue)
                .filter(event -> event.other != type.other)
                .flatMap(event -> event.getTab(length - 2).stream())
                .collect(Collectors.toList());
    }
    private static boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) return false;
        Event event = events.getOrDefault(system.toast(args[0], args.length - 1), null);
        if (event == null) return false;
        boolean isOp = sender.isOp();
        Player player = sender instanceof Player _v ? _v : null;

        switch (event.other) {
            case other: return false;

            case none: isOp = true;
            case owner: if (player == null) return false;
            case owner_console: if (!isOp) return false;
        }
        return event.TryInvoke(false, player, Arrays.stream(args).skip(1).toArray(String[]::new));
    }

    private static Collection<String> executeOtherTab(CommandSender sender, String[] args) {
        int length = args.length;
        if (length == 1) return Stream.concat(Bukkit.getOnlinePlayers()
                .stream()
                .map(Entity::getUniqueId)
                .map(UUID::toString), Stream.of(".self"))
                .collect(Collectors.toList());
        if (length == 2) return events.entrySet().stream().filter(v -> v.getValue().other == type.other).map(Map.Entry::getKey).map(v -> v.val0).collect(Collectors.toList());

        return events.entrySet()
                .stream()
                .filter(v -> v.getKey().val0.equals(args[1]))
                .map(Map.Entry::getValue)
                .filter(event -> event.other == type.other)
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
            lime.log("Execute other."+args[1]+" of " + args[0] + " with " + system.json.by(args).build());
            lime.logStackTrace(e);
            return false;
        }
        Event event = events.getOrDefault(system.toast(args[1], args.length - 2), null);
        if (event == null || other == null) return false;
        return event.TryInvoke(true, other, Arrays.stream(args).skip(2).toArray(String[]::new));
    }
}