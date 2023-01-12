package org.lime.gp.block.component.display.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.lime.system;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class TickTimeInfo {
    public int count = 1;

    public int calls = 0;

    public long users_ns = 0;
    public long variables_ns = 0;
    public long check_ns = 0;
    public long partial_ns = 0;
    public long metadata_ns = 0;
    public long apply_ns = 0;

    private long last_ns = 0;

    public long nextTime() {
        last_ns -= System.nanoTime();
        long delta = last_ns;
        last_ns = System.nanoTime();
        return -delta;
    }
    public void resetTime() {
        last_ns = System.nanoTime();
    }

    private Map<String, Long> nanoMap() {
        return system.map.<String, Long>of()
                .add("users", users_ns/count)
                .add("variables", variables_ns/count)
                .add("check", check_ns/count)
                .add("partial", partial_ns/count)
                .add("metadata", metadata_ns/count)
                .add("apply", apply_ns/count)
                .build();
    }

    public Component toComponent() {
        Map<String, Long> nanoMap = nanoMap();
        long total_ns = Math.max(1, nanoMap.values().stream().mapToLong(v -> v).sum());
        List<Component> components = new ArrayList<>();
        components.add(Component.text("calls: " + (calls / count) + "*" + count));
        nanoMap.forEach((name, ns) -> components.add(Component.empty()
                .append(Component.text("[" + name.charAt(0) + "")
                        .append(Component.text(":").color(NamedTextColor.WHITE))
                        .append(Component.text((ns * 100 / total_ns) + "%").color(NamedTextColor.AQUA))
                        .append(Component.text("]"))
                        .color(NamedTextColor.GREEN)
                        .hoverEvent(HoverEvent.showText(Component.text(String.join("\n",
                                "Name: " + name,
                                "Time: " + ns + " ns ("+TimeUnit.NANOSECONDS.toMillis(ns)+" ms)",
                                "Percent: " + (ns * 100 / total_ns) + "%"
                        ))))
                )));
        return Component.join(JoinConfiguration.separator(Component.text(" ")), components);
    }

    public void append(TickTimeInfo info) {
        this.count += info.count;
        this.calls += info.calls;
        this.users_ns += info.users_ns;
        this.variables_ns += info.variables_ns;
        this.check_ns += info.check_ns;
        this.partial_ns += info.partial_ns;
        this.metadata_ns += info.metadata_ns;
        this.apply_ns += info.apply_ns;
    }
}