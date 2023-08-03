package org.lime.gp.item.elemental.step.group;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.lime;
import org.lime.gp.module.JavaScript;

import javax.annotation.Nullable;
import java.util.*;

public record FunctionStep(IStep step, String js, Map<String, Object> args) implements IStep {
    private @Nullable Vector tryConvert(Iterable<?> i) {
        double x = 0;
        double y = 0;
        double z = 0;
        int index = 0;
        for (Object item : i) {
            if (!(item instanceof Number num)) return null;
            switch (index) {
                case 0 -> x = num.doubleValue();
                case 1 -> y = num.doubleValue();
                case 2 -> z = num.doubleValue();
                default -> { return null; }
            }
            index++;
        }
        return new Vector(x,y,z);
    }
    @Override public void execute(Player player, Vector position) {
        double x = position.getX();
        double y = position.getY();
        double z = position.getZ();
        HashMap<String, Object> args = new HashMap<>(this.args);
        args.put("x", x);
        args.put("y", y);
        args.put("z", z);
        args.put("pos", List.of(x, y, z));
        args.put("uuid", player.getUniqueId().toString());

        List<Vector> points = new ArrayList<>();
        if (!JavaScript.invoke(js, args)
                .map(v -> v instanceof Iterable<?> i ? i : null)
                .map(i -> {
                    for (Object item : i) {
                        if (!(item instanceof Iterable<?> j)) return false;
                        Vector point = tryConvert(j);
                        if (point == null) return false;
                        points.add(point);
                    }
                    return true;
                })
                .orElse(false)
        )
            lime.logOP("Error execute JS '" + js + "'. Return format of execute not equals [[x,y,z],[x,y,z],...,[x,y,z]]");

        points.forEach(point -> step.execute(player, point));
    }
}
