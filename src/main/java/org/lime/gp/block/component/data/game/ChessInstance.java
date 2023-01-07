package org.lime.gp.block.component.data.game;

import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.map.MapMonitor;
import org.lime.gp.module.DrawMap;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.util.*;
import java.util.stream.Stream;

public class ChessInstance extends ITableGameInstance<ChessInstance.TypeElement> {
    private Selector select = null;
    private boolean isNowBlack = false;
    private WinType winType = WinType.NONE;
    private enum WinType {
        NONE,
        BLACK,
        WHITE
    }
    public ChessInstance(ComponentDynamic<?, ?> component, CustomTileMetadata metadata) {
        super(8, TypeElement.class, component, metadata);
    }

    @Override public TypeElement readElement(JsonObjectOptional json) {
        return json.getAsEnum(Type.class, "type").map(type -> switch (type) {
            case Empty -> new Empty();
            case Pawn -> new Pawn(json.getAsBoolean("isBlack").orElse(false));
            case Knight -> new Knight(json.getAsBoolean("isBlack").orElse(false));
            case Bishop -> new Bishop(json.getAsBoolean("isBlack").orElse(false));
            case Rook -> new Rook(json.getAsBoolean("isBlack").orElse(false), json.getAsBoolean("isMoved").orElse(false));
            case Queen -> new Queen(json.getAsBoolean("isBlack").orElse(false));
            case King -> new King(json.getAsBoolean("isBlack").orElse(false), json.getAsBoolean("isMoved").orElse(false));
        }).orElseGet(Empty::new);
    }
    @Override public void setupDefault() {
        for (int x = 0; x < count; x++)
            for (int y = 0; y < count; y++)
                setOf(x,y, new Empty());

        for (int x = 0; x < count; x++) {
            setOf(x, 1, new Pawn(true));
            setOf(x, count - 2, new Pawn(false));
        }

        for (int _isBlack = 0; _isBlack <= 1; _isBlack++) {
            boolean isBlack = _isBlack == 1;
            int y = isBlack ? 0 : count - 1;
            setOf(0, y, new Rook(isBlack, false));
            setOf(1, y, new Knight(isBlack));
            setOf(2, y, new Bishop(isBlack));
            setOf(3, y, new Queen(isBlack));
            setOf(4, y, new King(isBlack, false));
            setOf(5, y, new Bishop(isBlack));
            setOf(6, y, new Knight(isBlack));
            setOf(7, y, new Rook(isBlack, false));
        }
    }

    @Override public void read(JsonObjectOptional json) {
        super.read(json);
        isNowBlack = json.getAsBoolean("isNowBlack").orElse(false);
        winType = json.getAsEnum(WinType.class, "winType").orElse(WinType.NONE);
    }
    @Override public system.json.builder.object write() {
        return super.write()
                .add("isNowBlack", isNowBlack)
                .add("winType", winType.name());
    }

    private static final byte WHITE_BOX = DrawMap.to(Color.fromRGB(228,229,172));
    private static final byte BLACK_BOX = DrawMap.to(Color.fromRGB(109,151,136));

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

    public record Selector(system.Toast2<Integer, Integer> index, Map<system.Toast2<Integer, Integer>, system.Action0> target) {
    }

    public enum Type {
        Empty(0, Double.NEGATIVE_INFINITY),
        Pawn(9, 1),
        Knight(2, 3),
        Bishop(2, 3.5),
        Rook(2, 5),
        Queen(1, 9),
        King(1, Double.NEGATIVE_INFINITY);

        public final int count;
        public final double weight;

        Type(int count, double weight) {
            this.count = count;
            this.weight = weight;
        }

        public static Stream<Type> byWeight() {
            return Arrays.stream(Type.values()).sorted(Comparator.comparingDouble(v -> v.weight));
        }
    }
    public abstract class TypeElement implements ITableGameInstance.IGameElement {
        @Override public void select(Player player, int x, int y, int size, MapMonitor.ClickType click, DrawMap map) {
            if (winType != WinType.NONE) return;
            if (click.isClick) {
                system.Toast2<Integer, Integer> index = system.toast(x,y);
                if (select != null && select.target.containsKey(index)) {
                    system.Action0 modify_action = select.target.remove(index);
                    if (modify_action != null) modify_action.invoke();
                    if (getOf(select.index) instanceof GameElement moved) {
                        if ((y == 0 && !moved.isBlack) || (y == count - 1 && moved.isBlack)) {
                            /*
                            Map<Type, Long> count = getAll()
                                    .map(v -> v instanceof GameElement c ? c : null)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.groupingBy(TypeElement::type, Collectors.counting()));
                            system.Func1<Boolean, GameElement> create = switch (Type.byWeight()
                                    .filter(v -> count.getOrDefault(v, 0L) < v.count)
                                    .findFirst()
                                    .orElse(Type.Pawn)) {
                                case Knight -> Knight::new;
                                case Bishop -> Bishop::new;
                                case Rook -> Rook::new;
                                case Queen -> Queen::new;
                                case King -> King::new;
                                default -> Pawn::new;
                            };
                            moved = create.invoke(moved.isBlack);
                            */
                            if (moved.type() == Type.Pawn)
                                moved = new Queen(moved.isBlack);
                        }
                        if (moved instanceof MovedGameElement _moved) _moved.isMoved = true;
                        setOf(index, moved);
                    }
                    setOf(select.index, new Empty());
                    isNowBlack = !isNowBlack;
                    select = null;
                } else if (getOf(index) instanceof GameElement gameElement && gameElement.isBlack == isNowBlack) select = gameElement.selectMap(x,y);
                else select = null;

                getAll()
                        .map(v -> v instanceof King c ? c : null)
                        .filter(Objects::nonNull)
                        .map(v -> v.isBlack ? system.toast(1,0) : system.toast(0,1))
                        .reduce(system.toast(0,0), (v1,v2) -> system.toast(v1.val0+v2.val0, v1.val1+v2.val1))
                        .invoke((black_count, white_count) -> {
                            if (black_count == 0) winType = WinType.WHITE;
                            else if (white_count == 0) winType = WinType.BLACK;
                        });
                markDirty();
                saveData();
            }
            if (winType != WinType.NONE) return;
            draw(x, y, size, map);
            map.rectangle(x * size, y * size, size, size, click.isClick ? CLICK_BORDER : CURSOR_BORDER, false);
        }
        @Override public void draw(int x, int y, int size, DrawMap map) {
            if (winType != WinType.NONE) return;
            if (select != null && select.target.containsKey(system.toast(x,y))) map.rectangle(x * SIZE_BOX, y * SIZE_BOX, SIZE_BOX, SIZE_BOX, TARGET_BORDER, false);
        }

        @Override public system.json.builder.object write() { return system.json.object().add("type", type().name()); }
        public abstract Type type();
    }
    public class Empty extends TypeElement {
        @Override public Type type() { return Type.Empty; }
    }
    public abstract class GameElement extends TypeElement {
        public boolean isBlack;
        public GameElement(boolean isBlack) {
            this.isBlack = isBlack;
        }

        @Override public void draw(int x, int y, int size, DrawMap map) {
            if (winType != WinType.NONE) return;
            super.draw(x, y, size, map);

            ((select != null && system.toast(x,y).equals(select.index)) ? image_select() : image()).draw(map, x * size + 2, y * size + 2);
        }

        public abstract Selector selectMap(int x, int y);
        public abstract DrawMap.Images image();
        public abstract DrawMap.Images image_select();

        @Override public system.json.builder.object write() {
            return super.write()
                    .add("isBlack", isBlack);
        }

        protected void line(int x, int y, int dx, int dy, HashMap<system.Toast2<Integer, Integer>, system.Action0> points) {
            x += dx;
            y += dy;
            while (in(x, y)) {
                system.Toast2<Integer, Integer> point_index = system.toast(x, y);
                if (!(getOf(point_index) instanceof GameElement checker)) {
                    points.put(point_index, null);
                    x += dx;
                    y += dy;
                    continue;
                }
                if (checker.isBlack != isBlack) points.put(point_index, null);
                return;
            }
        }
    }
    public abstract class MovedGameElement extends GameElement {
        public boolean isMoved;
        public MovedGameElement(boolean isBlack, boolean isMoved) {
            super(isBlack);
            this.isMoved = isMoved;
        }
        @Override public system.json.builder.object write() {
            return super.write()
                    .add("isMoved", isMoved);
        }
    }
    public class Pawn extends GameElement {
        public Pawn(boolean isBlack) {
            super(isBlack);
        }

        @Override public Type type() { return Type.Pawn; }
        @Override public DrawMap.Images image() { return isBlack ? DrawMap.Images.CHESS_FIGURE_BLACK_PAWN : DrawMap.Images.CHESS_FIGURE_WHITE_PAWN; }
        @Override public DrawMap.Images image_select() { return isBlack ? DrawMap.Images.CHESS_FIGURE_BLACK_PAWN_SELECT : DrawMap.Images.CHESS_FIGURE_WHITE_PAWN_SELECT; }
        @Override public Selector selectMap(int x, int y) {
            HashMap<system.Toast2<Integer, Integer>, system.Action0> points = new HashMap<>();
            int fore = isBlack ? 1 : -1;
            int foreDamage = fore;
            system.Toast2<Integer, Integer> index;
            if (in(x, y + fore) && getOf(index = system.toast(x, y + fore)).type() == Type.Empty) points.put(index, null);
            if (y == (isBlack ? 1 : count - 2) && !points.isEmpty()) {
                fore *= 2;
                if (in(x, y + fore) && getOf(index = system.toast(x, y + fore)).type() == Type.Empty) points.put(index, null);
            }
            if (in(x + 1, y + foreDamage) && getOf(index = system.toast(x + 1, y + foreDamage)) instanceof GameElement element && element.isBlack != isBlack) points.put(index, null);
            if (in(x - 1, y + foreDamage) && getOf(index = system.toast(x - 1, y + foreDamage)) instanceof GameElement element && element.isBlack != isBlack) points.put(index, null);
            return new Selector(system.toast(x, y), points);
        }
    }
    public class Knight extends GameElement {
        public Knight(boolean isBlack) {
            super(isBlack);
        }

        @Override public Type type() { return Type.Knight; }
        @Override public DrawMap.Images image() { return isBlack ? DrawMap.Images.CHESS_FIGURE_BLACK_KNIGHT : DrawMap.Images.CHESS_FIGURE_WHITE_KNIGHT; }
        @Override public DrawMap.Images image_select() { return isBlack ? DrawMap.Images.CHESS_FIGURE_BLACK_KNIGHT_SELECT : DrawMap.Images.CHESS_FIGURE_WHITE_KNIGHT_SELECT; }

        private static final List<system.Toast2<Integer, Integer>> paths = new ArrayList<>();
        private static system.Toast2<Integer, Integer> rotate(system.Toast2<Integer, Integer> pos) {
            return system.toast(pos.val1, -pos.val0);
        }
        static {
            system.Toast2<Integer, Integer> path1 = system.toast(1, 2);
            system.Toast2<Integer, Integer> path2 = system.toast(-1, 2);
            for (int i = 0; i < 4; i++) {
                path1 = rotate(path1);
                path2 = rotate(path2);
                paths.add(path1);
                paths.add(path2);
            }
        }

        @Override public Selector selectMap(int x, int y) {
            HashMap<system.Toast2<Integer, Integer>, system.Action0> points = new HashMap<>();
            paths.forEach(path -> path.invoke((_x, _y) -> {
                system.Toast2<Integer, Integer> index;
                if (in(x + _x, y + _y)) {
                    TypeElement typeElement = getOf(index = system.toast(x + _x, y + _y));
                    if ((typeElement instanceof GameElement element && element.isBlack != isBlack) || typeElement.type() == Type.Empty)
                        points.put(index, null);
                }
            }));
            return new Selector(system.toast(x, y), points);
        }
    }
    public class Bishop extends GameElement {
        public Bishop(boolean isBlack) {
            super(isBlack);
        }

        @Override public Type type() { return Type.Bishop; }
        @Override public DrawMap.Images image() { return isBlack ? DrawMap.Images.CHESS_FIGURE_BLACK_BISHOP : DrawMap.Images.CHESS_FIGURE_WHITE_BISHOP; }
        @Override public DrawMap.Images image_select() { return isBlack ? DrawMap.Images.CHESS_FIGURE_BLACK_BISHOP_SELECT : DrawMap.Images.CHESS_FIGURE_WHITE_BISHOP_SELECT; }
        @Override public Selector selectMap(int x, int y) {
            HashMap<system.Toast2<Integer, Integer>, system.Action0> points = new HashMap<>();

            line(x, y, -1, 1, points);
            line(x, y, 1, 1, points);
            line(x, y, -1, -1, points);
            line(x, y, 1, -1, points);

            return new Selector(system.toast(x, y), points);
        }
    }
    public class Rook extends MovedGameElement {
        public Rook(boolean isBlack, boolean isMoved) { super(isBlack, isMoved); }

        @Override public Type type() { return Type.Rook; }
        @Override public DrawMap.Images image() { return isBlack ? DrawMap.Images.CHESS_FIGURE_BLACK_ROOK : DrawMap.Images.CHESS_FIGURE_WHITE_ROOK; }
        @Override public DrawMap.Images image_select() { return isBlack ? DrawMap.Images.CHESS_FIGURE_BLACK_ROOK_SELECT : DrawMap.Images.CHESS_FIGURE_WHITE_ROOK_SELECT; }
        @Override public Selector selectMap(int x, int y) {
            HashMap<system.Toast2<Integer, Integer>, system.Action0> points = new HashMap<>();

            line(x, y, 1, 0, points);
            line(x, y, -1, 0, points);
            line(x, y, 0, 1, points);
            line(x, y, 0, -1, points);

            return new Selector(system.toast(x, y), points);
        }
    }
    public class Queen extends GameElement {
        public Queen(boolean isBlack) {
            super(isBlack);
        }

        @Override public Type type() { return Type.Queen; }
        @Override public DrawMap.Images image() { return isBlack ? DrawMap.Images.CHESS_FIGURE_BLACK_QUEEN : DrawMap.Images.CHESS_FIGURE_WHITE_QUEEN; }
        @Override public DrawMap.Images image_select() { return isBlack ? DrawMap.Images.CHESS_FIGURE_BLACK_QUEEN_SELECT : DrawMap.Images.CHESS_FIGURE_WHITE_QUEEN_SELECT; }
        @Override public Selector selectMap(int x, int y) {
            HashMap<system.Toast2<Integer, Integer>, system.Action0> points = new HashMap<>();

            line(x, y, 1, 0, points);
            line(x, y, -1, 0, points);
            line(x, y, 0, 1, points);
            line(x, y, 0, -1, points);

            line(x, y, -1, 1, points);
            line(x, y, 1, 1, points);
            line(x, y, -1, -1, points);
            line(x, y, 1, -1, points);

            return new Selector(system.toast(x, y), points);
        }
    }
    public class King extends MovedGameElement {
        public King(boolean isBlack, boolean isMoved) { super(isBlack, isMoved); }

        @Override public Type type() { return Type.King; }
        @Override public DrawMap.Images image() { return isBlack ? DrawMap.Images.CHESS_FIGURE_BLACK_KING : DrawMap.Images.CHESS_FIGURE_WHITE_KING; }
        @Override public DrawMap.Images image_select() { return isBlack ? DrawMap.Images.CHESS_FIGURE_BLACK_KING_SELECT : DrawMap.Images.CHESS_FIGURE_WHITE_KING_SELECT; }

        private static final List<system.Toast2<Integer, Integer>> paths = new ArrayList<>();
        private static system.Toast2<Integer, Integer> rotate(system.Toast2<Integer, Integer> pos) {
            return system.toast(pos.val1, -pos.val0);
        }
        static {
            system.Toast2<Integer, Integer> path1 = system.toast(0, 1);
            system.Toast2<Integer, Integer> path2 = system.toast(1, 1);
            for (int i = 0; i < 4; i++) {
                path1 = rotate(path1);
                path2 = rotate(path2);
                paths.add(path1);
                paths.add(path2);
            }
        }

        @Override public Selector selectMap(int x, int y) {
            HashMap<system.Toast2<Integer, Integer>, system.Action0> points = new HashMap<>();
            paths.forEach(path -> path.invoke((_x, _y) -> {
                system.Toast2<Integer, Integer> index;
                if (in(x + _x, y + _y)) {
                    TypeElement typeElement = getOf(index = system.toast(x + _x, y + _y));
                    if ((typeElement instanceof GameElement element && element.isBlack != isBlack) || typeElement.type() == Type.Empty)
                        points.put(index, null);
                }
            }));
            if (!isMoved
                    && getOf(0, y) instanceof Rook rook
                    && !rook.isMoved
                    && Stream.of(1, 2, 3).allMatch(v -> getOf(v, y) instanceof Empty)
            ) {
                points.put(system.toast(2, y), () -> {
                    rook.isMoved = true;
                    setOf(0, y, new Empty());
                    setOf(3, y, rook);
                });
            } else if (!isMoved
                    && getOf(7, y) instanceof Rook rook
                    && !rook.isMoved
                    && Stream.of(5, 6).allMatch(v -> getOf(v, y) instanceof Empty)
            ) {
                points.put(system.toast(6, y), () -> {
                    rook.isMoved = true;
                    setOf(7, y, new Empty());
                    setOf(5, y, rook);
                });
            }

            return new Selector(system.toast(x, y), points);
        }
    }
}





















