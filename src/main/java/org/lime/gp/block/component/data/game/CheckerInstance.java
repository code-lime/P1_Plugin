package org.lime.gp.block.component.data.game;

import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.map.MapMonitor;
import org.lime.gp.module.DrawMap;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.*;
import java.util.stream.Stream;

public class CheckerInstance extends ITableGameInstance<CheckerInstance.TypeElement> {
    private Selector select = null;
    private boolean isNowBlack = false;
    private WinType winType = WinType.NONE;
    private enum WinType {
        NONE,
        BLACK,
        WHITE
    }
    public CheckerInstance(ComponentDynamic<?, ?> component, CustomTileMetadata metadata) {
        super(8, TypeElement.class, component, metadata);
    }

    @Override public TypeElement readElement(JsonObjectOptional json) {
        return json.getAsEnum(Type.class, "type").map(type -> switch (type) {
            case Empty -> new Empty();
            case Checker -> new Checker(json.getAsBoolean("isBlack").orElse(false), json.getAsBoolean("isKing").orElse(false));
        }).orElseGet(Empty::new);
    }
    @Override public void setupDefault() {
        for (int x = 0; x < count; x++)
            for (int y = 0; y < count; y++)
                setOf(x, y, new Empty());

        for (int x = 0; x < count; x++)
            for (int y = 0; y < 3; y++)
                if ((x % 2 + y % 2) % 2 == 1)
                    setOf(x,y, new Checker(true, false));

        for (int x = 0; x < count; x++)
            for (int y = count - 3; y < count; y++)
                if ((x % 2 + y % 2) % 2 == 1)
                    setOf(x,y,new Checker(false, false));
    }

    @Override public void read(JsonObjectOptional json) {
        super.read(json);
        isNowBlack = json.getAsBoolean("isNowBlack").orElse(false);
        winType = json.getAsEnum(WinType.class, "winType").orElse(WinType.NONE);
    }
    @Override public json.builder.object write() {
        return super.write()
                .add("isNowBlack", isNowBlack)
                .add("winType", winType.name());
    }

    private static final byte WHITE_BOX = DrawMap.to(Color.fromRGB(228,229,172));
    private static final byte BLACK_BOX = DrawMap.to(Color.fromRGB(109,151,136));
    private static final byte WHITE_CHECKER = DrawMap.to(Color.WHITE);
    private static final byte BLACK_CHECKER = DrawMap.to(Color.BLACK);
    private static final byte SELECT_CHECKER = DrawMap.to(Color.fromRGB(0x677535));
    private static final byte KING_CHECKER = DrawMap.to(Color.GRAY);
    private static final byte TARGET_BORDER = DrawMap.to(Color.PURPLE);
    private static final byte CLICK_BORDER = DrawMap.to(Color.YELLOW);
    private static final byte CURSOR_BORDER = DrawMap.to(Color.AQUA);

    @Override public void drawBackground(DrawMap map) {
        switch (winType) {
            case NONE:
                for (int x = 0; x < count; x++)
                    for (int y = 0; y < count; y++)
                        map.rectangle(x * SIZE_BOX, y * SIZE_BOX, SIZE_BOX, SIZE_BOX, (x % 2 + y % 2) % 2 == 1 ? BLACK_BOX : WHITE_BOX);
                break;
            case BLACK:
                DrawMap.Images.CHECKERS_WIN_BLACK.draw(map, 0, 0);
                break;
            case WHITE:
                DrawMap.Images.CHECKERS_WIN_WHITE.draw(map, 0, 0);
                break;
        }
    }
    @Override public void drawBackground(int x, int y, DrawMap map) {
        switch (winType) {
            case NONE -> map.rectangle(x * SIZE_BOX, y * SIZE_BOX, SIZE_BOX, SIZE_BOX, (x % 2 + y % 2) % 2 == 1 ? BLACK_BOX : WHITE_BOX);
            case BLACK -> DrawMap.Images.CHECKERS_WIN_BLACK.draw(map, 0, 0);
            case WHITE -> DrawMap.Images.CHECKERS_WIN_WHITE.draw(map, 0, 0);
        }
    }

    public record Selector(Toast2<Integer, Integer> index, Map<Toast2<Integer, Integer>, Toast2<Integer, Integer>> target) {
        public Stream<Toast2<Integer, Integer>> damage() {
            return target.entrySet().stream().filter(v -> v.getValue() != null).map(Map.Entry::getKey);
        }
    }

    public enum Type {
        Empty,
        Checker
    }
    public abstract class TypeElement implements ITableGameInstance.IGameElement {
        @Override public void select(Player player, int x, int y, int size, MapMonitor.ClickType click, DrawMap map) {
            if (winType != WinType.NONE) return;
            if (click.isClick) {
                Toast2<Integer, Integer> index = Toast.of(x,y);
                if (getOf(x,y) instanceof Checker checker) {
                    if (checker.isBlack != isNowBlack) select = null;
                    else {
                        Selector selector = checker.selectMap(x,y);
                        if (selector.damage().findAny().isPresent()) select = selector;
                        else {
                            boolean isAny = false;
                            for (int _x = 0; _x < count; _x++)
                                for (int _y = 0; _y < count; _y++)
                                    if (!isAny
                                            && getOf(_x, _y) instanceof Checker _checker
                                            && _checker.isBlack == isNowBlack
                                            && _checker.selectMap(_x, _y).damage().findAny().isPresent()
                                    )
                                        isAny = true;
                            select = isAny ? null : selector;
                        }
                    }
                } else {
                    if (select != null && select.target.containsKey(index)) {
                        Toast2<Integer, Integer> remove_index = select.target.remove(index);
                        if (remove_index != null) setOf(remove_index, new Empty());
                        if (getOf(select.index) instanceof Checker moved) {
                            if ((y == 0 && !moved.isBlack) || (y == count - 1 && moved.isBlack)) moved.isKing = true;
                            setOf(index, moved);
                        }
                        setOf(select.index, new Empty());

                        Map<Toast2<Integer, Integer>, Selector> damage = new HashMap<>();
                        for (int _x = 0; _x < count; _x++)
                            for (int _y = 0; _y < count; _y++)
                                if (getOf(_x, _y) instanceof Checker checker && checker.isBlack == isNowBlack) {
                                    Selector _selector = checker.selectMap(_x, _y);
                                    checker.selectMap(_x, _y).damage().forEach(id -> damage.put(id, _selector));
                                }
                        Optional.ofNullable(damage.get(index))
                                .ifPresentOrElse(selector -> {
                                    select = selector;
                                }, () -> {
                                    isNowBlack = !isNowBlack;
                                    select = null;
                                });
                    } else {
                        select = null;
                    }
                    getAll()
                            .map(v -> v instanceof Checker c ? c : null)
                            .filter(Objects::nonNull)
                            .map(v -> v.isBlack ? Toast.of(1,0) : Toast.of(0,1))
                            .reduce(Toast.of(0,0), (v1,v2) -> Toast.of(v1.val0+v2.val0, v1.val1+v2.val1))
                            .invoke((black_count, white_count) -> {
                                if (black_count == 0) winType = WinType.WHITE;
                                else if (white_count == 0) winType = WinType.BLACK;
                            });
                }
                markDirty();
                saveData();
            }
            if (winType != WinType.NONE) return;
            draw(x, y, size, map);
            map.rectangle(x * size, y * size, size, size, click.isClick ? CLICK_BORDER : CURSOR_BORDER, false);
        }
        @Override public void draw(int x, int y, int size, DrawMap map) {
            if (winType != WinType.NONE) return;
            if (select != null && select.target.containsKey(Toast.of(x,y))) map.rectangle(x * SIZE_BOX, y * SIZE_BOX, SIZE_BOX, SIZE_BOX, TARGET_BORDER, false);
        }

        @Override public json.builder.object write() { return json.object().add("type", type().name()); }
        public abstract Type type();
    }
    public class Empty extends TypeElement {
        @Override public Type type() { return Type.Empty; }
    }
    public class Checker extends TypeElement {
        public boolean isBlack;
        public boolean isKing;

        public Checker(boolean isBlack, boolean isKing) {
            this.isBlack = isBlack;
            this.isKing = isKing;
        }

        @Override public Type type() { return Type.Checker; }
        @Override public void draw(int x, int y, int size, DrawMap map) {
            if (winType != WinType.NONE) return;
            super.draw(x, y, size, map);
            int center_size= size / 2;
            int radius = center_size - 2;
            if (select != null && Toast.of(x,y).equals(select.index)) {
                map.circle(x * size + center_size, y * size + center_size, radius, SELECT_CHECKER);
                map.circle(x * size + center_size, y * size + center_size, radius - 1, isBlack ? BLACK_CHECKER : WHITE_CHECKER);
            } else {
                map.circle(x * size + center_size, y * size + center_size, radius, isBlack ? BLACK_CHECKER : WHITE_CHECKER);
            }
            if (isKing) map.circle(x * size + center_size, y * size + center_size, radius / 2, KING_CHECKER);
        }
        private void line(int x, int y, int dx, int dy, HashMap<Toast2<Integer, Integer>, Toast2<Integer, Integer>> points, HashMap<Toast2<Integer, Integer>, Toast2<Integer, Integer>> damage) {
            x += dx;
            y += dy;
            while (in(x, y)) {
                Toast2<Integer, Integer> point_index = Toast.of(x, y);
                if (!(getOf(point_index) instanceof Checker checker)) {
                    points.put(point_index, null);
                    x += dx;
                    y += dy;
                    continue;
                }
                if (checker.isBlack == isBlack) return;
                x += dx;
                y += dy;
                if (!in(x, y)) return;
                Toast2<Integer, Integer> damage_index = Toast.of(x,y);
                if (getOf(damage_index).type() != Type.Empty) return;
                damage.put(damage_index, point_index);
                return;
            }
        }
        public Selector selectMap(int x, int y) {
            HashMap<Toast2<Integer, Integer>, Toast2<Integer, Integer>> points = new HashMap<>();
            HashMap<Toast2<Integer, Integer>, Toast2<Integer, Integer>> damage = new HashMap<>();
            if (isKing) {
                line(x, y, -1, 1, points, damage);
                line(x, y, 1, 1, points, damage);
                line(x, y, -1, -1, points, damage);
                line(x, y, 1, -1, points, damage);
            } else {
                Arrays.asList(
                        Toast.of(-1, 1, isNowBlack),
                        Toast.of(1, 1, isNowBlack),
                        Toast.of(-1, -1, !isNowBlack),
                        Toast.of(1, -1, !isNowBlack)
                ).forEach(_xy -> {
                    int _x = _xy.val0;
                    int _y = _xy.val1;
                    if (!in(x+_x,y+_y)) return;
                    Toast2<Integer, Integer> point_index = Toast.of(x+_x, y+_y);
                    if (!(getOf(point_index) instanceof Checker checker)) {
                        if (_xy.val2) points.put(point_index, null);
                        return;
                    }
                    if (checker.isBlack == isBlack) return;
                    if (!in(x+_x*2,y+_y*2)) return;
                    Toast2<Integer, Integer> damage_index = Toast.of(x+_x*2, y+_y*2);
                    if (getOf(damage_index).type() != Type.Empty) return;
                    damage.put(damage_index, point_index);
                });
            }
            return new Selector(Toast.of(x, y), damage.size() > 0 ? damage : points);
        }

        @Override public json.builder.object write() {
            return super.write()
                    .add("isBlack", isBlack)
                    .add("isKing", isKing);
        }
    }
}





















