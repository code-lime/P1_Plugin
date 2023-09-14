package org.lime.gp.block.component.display.instance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.lime.system.toast.*;

public class TickTimeInfo {
    public int count = 1;

    public int calls = 0;

    public long filter_ns = 0;
    public long users_ns = 0;
    public long variables1_ns = 0;
    public long variables2_ns = 0;
    public long variables3_ns = 0;
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

    private List<Toast2<String, Long>> nanoMap() {
        return Arrays.asList(
            Toast.of("filter", filter_ns/count),
            Toast.of("users", users_ns/count),
            Toast.of("variables1", variables1_ns/count),
            Toast.of("variables2", variables2_ns/count),
            Toast.of("variables3", variables3_ns/count),
            Toast.of("check", check_ns/count),
            Toast.of("partial", partial_ns/count),
            Toast.of("metadata", metadata_ns/count),
            Toast.of("apply", apply_ns/count));
    }

    public Component toComponent() {
        List<Toast2<String, Long>> nanoMap = nanoMap();
        long total_ns = Math.max(1, nanoMap.stream().mapToLong(v -> v.val1).sum());
        List<Component> components = new ArrayList<>();
        components.add(Component.text("calls: " + (calls / count) + "*" + count));
        nanoMap.forEach(v -> v.invoke((name, ns) -> components.add(Component.empty()
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
                ))));
        return Component.join(JoinConfiguration.separator(Component.text(" ")), components);
    }

    public void append(TickTimeInfo info) {
        this.count += info.count;
        this.calls += info.calls;
        this.filter_ns += info.filter_ns;
        this.users_ns += info.users_ns;
        this.variables1_ns += info.variables1_ns;
        this.variables2_ns += info.variables2_ns;
        this.variables3_ns += info.variables3_ns;
        this.check_ns += info.check_ns;
        this.partial_ns += info.partial_ns;
        this.metadata_ns += info.metadata_ns;
        this.apply_ns += info.apply_ns;
    }
}