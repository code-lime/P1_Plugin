package org.lime.gp.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.minecraft.network.protocol.game.PacketPlayOutMap;
import net.minecraft.world.level.saveddata.maps.WorldMap;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.map.MapPalette;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.lime;
import org.lime.gp.map.MapMonitor;
import org.lime.reflection;
import org.lime.system;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public final class DrawMap {
    private static final class CacheBuffer implements Listener {
        private static final ConcurrentHashMap<system.Toast2<UUID, Integer>, byte[]> cacheBuffer = new ConcurrentHashMap<>();
        private static void remove(UUID uuid) { cacheBuffer.entrySet().removeIf(kv -> kv.getKey().val0.equals(uuid)); }
        @EventHandler private static void on(PlayerQuitEvent e) { remove(e.getPlayer().getUniqueId()); }
        @EventHandler private static void on(PlayerJoinEvent e) { remove(e.getPlayer().getUniqueId()); }
    }
    public enum Images {
        CHECKERS_WIN_BLACK("checkers","win","black"),
        CHECKERS_WIN_WHITE("checkers","win","white"),

        CHESS_FIGURE_BLACK_PAWN(MapMonitor.MapRotation.FLIPPED, "chess","figure","black","pawn"),
        CHESS_FIGURE_BLACK_KNIGHT(MapMonitor.MapRotation.FLIPPED, "chess","figure","black","knight"),
        CHESS_FIGURE_BLACK_BISHOP(MapMonitor.MapRotation.FLIPPED, "chess","figure","black","bishop"),
        CHESS_FIGURE_BLACK_ROOK(MapMonitor.MapRotation.FLIPPED, "chess","figure","black","rook"),
        CHESS_FIGURE_BLACK_QUEEN(MapMonitor.MapRotation.FLIPPED, "chess","figure","black","queen"),
        CHESS_FIGURE_BLACK_KING(MapMonitor.MapRotation.FLIPPED, "chess","figure","black","king"),

        CHESS_FIGURE_BLACK_PAWN_SELECT(MapMonitor.MapRotation.FLIPPED, "chess","figure","black","pawn_select"),
        CHESS_FIGURE_BLACK_KNIGHT_SELECT(MapMonitor.MapRotation.FLIPPED, "chess","figure","black","knight_select"),
        CHESS_FIGURE_BLACK_BISHOP_SELECT(MapMonitor.MapRotation.FLIPPED, "chess","figure","black","bishop_select"),
        CHESS_FIGURE_BLACK_ROOK_SELECT(MapMonitor.MapRotation.FLIPPED, "chess","figure","black","rook_select"),
        CHESS_FIGURE_BLACK_QUEEN_SELECT(MapMonitor.MapRotation.FLIPPED, "chess","figure","black","queen_select"),
        CHESS_FIGURE_BLACK_KING_SELECT(MapMonitor.MapRotation.FLIPPED, "chess","figure","black","king_select"),

        CHESS_FIGURE_WHITE_PAWN("chess","figure","white","pawn"),
        CHESS_FIGURE_WHITE_KNIGHT("chess","figure","white","knight"),
        CHESS_FIGURE_WHITE_BISHOP("chess","figure","white","bishop"),
        CHESS_FIGURE_WHITE_ROOK("chess","figure","white","rook"),
        CHESS_FIGURE_WHITE_QUEEN("chess","figure","white","queen"),
        CHESS_FIGURE_WHITE_KING("chess","figure","white","king"),

        CHESS_FIGURE_WHITE_PAWN_SELECT("chess","figure","white","pawn_select"),
        CHESS_FIGURE_WHITE_KNIGHT_SELECT("chess","figure","white","knight_select"),
        CHESS_FIGURE_WHITE_BISHOP_SELECT("chess","figure","white","bishop_select"),
        CHESS_FIGURE_WHITE_ROOK_SELECT("chess","figure","white","rook_select"),
        CHESS_FIGURE_WHITE_QUEEN_SELECT("chess","figure","white","queen_select"),
        CHESS_FIGURE_WHITE_KING_SELECT("chess","figure","white","king_select");

        private record Data(int sizeX, int sizeY, Byte[] colors) {
            public static final Data EMPTY = new Data(0, 0, new Byte[0]);
            public static Data empty() { return EMPTY; }
            public static Data read(MapMonitor.MapRotation rotation, BufferedImage image) {
                int sizeX = image.getWidth();
                int sizeY = image.getHeight();
                //int centerX = sizeX / 2 - 1;
                //int centerY = sizeY / 2 - 1;
                Byte[] colors = new Byte[sizeX*sizeY];
                for (int x = 0; x < sizeX; x++)
                    for (int y = 0; y < sizeY; y++)
                    {
                        int pixel = image.getRGB(x, y);
                        int alpha = (pixel >> 24) & 0xFF;
                        colors[x + y * sizeX] = alpha < 128 ? null : to(pixel);
                        //rotation.rotate(x,y,centerX,centerY).invoke((_x, _y) -> colors[_x + _y * sizeX] = alpha < 128 ? null : to(pixel));
                    }
                return new Data(sizeX, sizeY, rotation.rotateMap(sizeX, sizeY, colors));
            }
        }
        public final List<String> args;
        public final MapMonitor.MapRotation rotation;

        Images(String... args) {
            this.rotation = MapMonitor.MapRotation.NONE;
            this.args = Arrays.asList(args);
        }
        Images(MapMonitor.MapRotation rotation, String... args) {
            this.rotation = rotation;
            this.args = Arrays.asList(args);
        }

        public String parse(JsonObject parent) {
            try
            {
                JsonElement dat = parent;
                for (String arg : args) dat = dat.getAsJsonObject().get(arg);
                return dat.isJsonNull() ? null : dat.getAsString();
            }
            catch (Exception e) {
                return null;
            }
        }

        public String path() {
            return getPath(args);
        }
        private static String getPath(List<String> args) {
            return String.join(".", args);
        }
        public void addDefault(JsonObject json) {
            JsonObject dat = json;
            int length = args.size();
            for (int i = 0; i < length; i++) {
                String arg = args.get(i);
                if (i == length - 1) {
                    dat.add(arg, JsonNull.INSTANCE);
                    return;
                }
                if (!dat.has(arg) || !dat.get(arg).isJsonObject()) dat.add(arg, new JsonObject());
                dat = dat.get(arg).getAsJsonObject();
            }
        }

        private Data image() {
            return Optional.ofNullable(images.get(this)).orElseGet(Data::empty);
        }

        public void draw(DrawMap map, int offsetX, int offsetY) {
            Data data = image();
            map.rectangle(offsetX, offsetY, data.sizeX, data.colors);
        }
    }
    private final static ConcurrentHashMap<Images, Images.Data> images = new ConcurrentHashMap<>();
    public static CoreElement create() {
        return CoreElement.create(DrawMap.class)
                .withInstance(new CacheBuffer())
                .withInit(DrawMap::init)
                .<JsonObject>addConfig("images", v -> v.withInvoke(DrawMap::config).withDefault(() -> {
                    JsonObject def = new JsonObject();
                    for (Images img : Images.values()) img.addDefault(def);
                    return def;
                }));
    }
    public static void config(JsonObject json) {
        images.clear();
        File dir = lime.getConfigFile("images");
        if (!dir.exists()) dir.mkdir();
        String path = dir.getAbsolutePath();
        for (Images img : Images.values()) {
            String file = img.parse(json);
            Path file_path;
            if (file == null) {
                lime.logOP(ChatColor.GOLD + "Image not founded: '" + img.path() + "'");
                img.addDefault(json);
            }
            else if (!Files.exists(file_path = Paths.get(path, file))) {
                lime.logOP(ChatColor.GOLD + "Image file not founded: '" + file_path + "'");
            }
            else images.put(img, Images.Data.read(img.rotation, system.<File, BufferedImage>funcEx(ImageIO::read).throwable().invoke(file_path.toFile())));
        }
        lime.writeAllConfig("images", system.toFormat(json));
    }

    private record hsv(double h,double s,double v) {
        public static double distance(hsv c0, hsv c1) {
            double _v = Math.abs(c1.h - c0.h);

            double dh = Math.min(_v, 360 - _v) / 180.0;
            double ds = Math.abs(c1.s - c0.s);
            double dv = Math.abs(c1.v - c0.v);
            
            return Math.sqrt(dh * dh + ds * ds + dv * dv);
        }
    }
    private record rgb(double r,double g,double b) {
        public static rgb of(java.awt.Color color) {
            return new rgb(color.getRed() / 256.0, color.getGreen() / 256.0, color.getBlue() / 256.0);
        }
        public static rgb of(int rgb) {
            return new rgb((rgb >> 16 & 0xFF) / 256.0, (rgb >> 8 & 0xFF) / 256.0, (rgb >> 0 & 0xFF) / 256.0);
        }
    }
    private static hsv rgb2hsv(rgb in) {
        double h, s, v;
        double      min, max, delta;

        min = in.r < in.g ? in.r : in.g;
        min = min  < in.b ? min  : in.b;

        max = in.r > in.g ? in.r : in.g;
        max = max  > in.b ? max  : in.b;

        v = max;                                // v
        delta = max - min;
        if (delta < 0.00001)
        {
            s = 0;
            h = 0; // undefined, maybe nan?
            return new hsv(h,s,v);
        }
        if( max > 0.0 ) { // NOTE: if Max is == 0, this divide would cause a crash
            s = (delta / max);                  // s
        } else {
            // if max is 0, then r = g = b = 0
            // s = 0, h is undefined
            s = 0.0;
            h = 0;                            // its now undefined
            return new hsv(h,s,v);
        }
        if( in.r >= max )                           // > is bogus, just keeps compilor happy
            h = ( in.g - in.b ) / delta;        // between yellow & magenta
        else
        if( in.g >= max )
            h = 2.0 + ( in.b - in.r ) / delta;  // between cyan & yellow
        else
            h = 4.0 + ( in.r - in.g ) / delta;  // between magenta & cyan

        h *= 60.0;                              // degrees

        if(h < 0.0 )
            h += 360.0;

        return new hsv(h,s,v);
    }

    private static final HashMap<Byte, Byte> negativeMap = new HashMap<>();
    private static final byte[] colorConverter = new byte[256*256*256];
    public static void init() {
        java.awt.Color[] colors = reflection.getField(MapPalette.class, "colors", null);
        for (int i = 4; i < colors.length; i++) {
            java.awt.Color color = colors[i];
            negativeMap.put((byte)i, to(Color.fromRGB(255-color.getRed(), 255-color.getGreen(), 255-color.getBlue())));
        }
        File colorConverterFile = lime.getConfigFile("colorConverter.cls");
        if (colorConverterFile.exists()) {
            System.arraycopy(system.funcEx(Files::readAllBytes).throwable().invoke(colorConverterFile.toPath()), 0, colorConverter, 0, colorConverter.length);
        } else {
            List<system.Toast2<hsv, Byte>> hsvColors = new ArrayList<>();
            for (int i = 4; i < colors.length; i++) {
                java.awt.Color color = colors[i];
                hsvColors.add(system.toast(rgb2hsv(rgb.of(color)), (byte)i));
            }
            int length = 256 * 256 * 256;
            int step = length / 100;
            system.LockToast1<Integer> last = system.toast(0).lock();
            IntStream.range(0, length)
                    .boxed()
                    .toList()
                    .parallelStream()
                    .forEach(i -> {
                        if (i % step == 0) last.edit0(v -> {
                            lime.logOP("["+ StringUtils.leftPad("" + v, 3, ' ').replace(" ", "...") +"%] Color " + (v * step) + " / " + length + "...");
                            return v + 1;
                        });
                        hsv color = rgb2hsv(rgb.of(i));
                        colorConverter[i] = hsvColors.stream()
                                .min(Comparator.comparingDouble(v -> hsv.distance(v.val0, color)))
                                .map(v -> v.val1)
                                .orElseThrow();
                    });
            lime.logOP("[100%] Color " + length + " / " + length+ "...");
            lime.logOP("[----] Saving colors...");
            system.<Path, byte[], Path>funcEx(Files::write).throwable().invoke(colorConverterFile.toPath(), colorConverter);
            lime.logOP("[----] Saved!");
        }
    }

    private static int nextMapID = -1;
    public static int getNextMapID() { return nextMapID--; }

    private final byte[] colors;

    public static byte to(int color) {
        int index = color & 0xFFFFFF;
        return colorConverter[index];
        //return MapPalette.matchColor(new java.awt.Color(color));
    }
    public static byte to(int color, byte old) {
        return to(color);
        /*java.awt.Color _color = new java.awt.Color(color);
        return _color.getAlpha() < 128 ? MapPalette.RED : MapPalette.matchColor(_color);*/
    }
    public static Byte to(Color color) { return color == null ? null : to(color.asRGB()); }
    @SuppressWarnings("deprecation")
    public static Color to(byte color) { return Color.fromRGB(MapPalette.getColor(color).getRGB() & 0xFFFFFF); }
    @SuppressWarnings("deprecation")
    public static String toHex(byte color) {
        return StringUtils.leftPad(Integer.toHexString(MapPalette.getColor(color).getRGB()), 8, '0').substring(2);
    }
    public static byte negative(byte color) {
        Color _color = to(color);
        return to(Color.fromRGB(255-_color.getRed(), 255-_color.getGreen(), 255-_color.getBlue()));
        //return negativeMap.getOrDefault(color, (byte)0);
    }

    private DrawMap(byte[] colors) { this.colors = colors; }
    private DrawMap(Color bcg) { this(new byte[128 * 128]); Arrays.fill(colors, to(bcg)); }

    public DrawMap fill(BufferedImage image) {
        if (image == null) return this;
        int size_x = image.getWidth();
        int size_y = image.getHeight();
        if (size_x != 128 || size_y != 128) image = MapPalette.resizeImage(image);
        int[] buff = new int[128*128];
        image.getRGB(0, 0, 128, 128, buff, 0, 128);
        int length = buff.length;
        if (image.getTransparency() == Transparency.OPAQUE) for (int i = 0; i < length; i++) colors[i] = to(buff[i]);
        else for (int i = 0; i < length; i++) colors[i] = to(buff[i], colors[i]);
        return this;
    }
    public void fill(Color color) {
        Arrays.fill(colors, to(color));
    }
    public Color pixel(int x, int y) { return to(colors[x % 128 + (y % 128) * 128]); }
    public DrawMap pixel(int x, int y, Color color) { return pixel(x,y,to(color)); }
    public DrawMap pixel(int x, int y, byte color) { colors[x % 128 + (y % 128) * 128] = color; return this; }
    public DrawMap pixel(system.Toast2<Integer, Integer> pos, byte color) { return pixel(pos.val0, pos.val1, color); }
    private static int in(int value, int min, int max) { return Math.max(Math.min(value, max), min); }
    private static int in(int value) { return in(value, 0, 128); }
    public DrawMap rectangle(int x, int y, int sizeX, int sizeY, Color color) {
        rectangle(x,y,sizeX,sizeY,color,true);
        return this;
    }
    public DrawMap rectangle(int x, int y, int sizeX, int sizeY, Color color, boolean fill) {
        int tX = in(x+sizeX);
        int tY = in(y+sizeY);
        Byte tC = to(color);
        if (fill) {
            for (int _x = in(x); _x < tX; _x++)
                for (int _y = in(y); _y < tY; _y++)
                    colors[_x + _y * 128] = tC == null ? negative(colors[_x + _y * 128]) : tC;
        } else {
            int __x = in(x);
            int __y = in(y);

            for (int _x = __x; _x < tX; _x++) {
                colors[_x + (tY - 1) * 128] = tC == null ? negative(colors[_x + (tY - 1) * 128]) : tC;
                colors[_x + __y * 128] = tC == null ? negative(colors[_x + __y * 128]) : tC;
            }
            for (int _y = __y; _y < tY; _y++) {
                colors[(tX - 1) + _y * 128] = tC == null ? negative(colors[(tX - 1) + _y * 128]) : tC;
                colors[__x + _y * 128] = tC == null ? negative(colors[__x + _y * 128]) : tC;
            }
        }
        return this;
    }
    public DrawMap rectangleFunc(int x, int y, int sizeX, int sizeY, system.Action1<Byte> func) {
        int _tX = in(x);
        int _tY = in(y);
        int tX = in(x+sizeX);
        int tY = in(y+sizeY);
        for (int _x = _tX; _x < tX; _x++)
            for (int _y = _tY; _y < tY; _y++)
                func.invoke(colors[_x + _y * 128]);
        return this;
    }
    public DrawMap rectangleFunc(int x, int y, int sizeX, int sizeY, system.Func1<Byte, Byte> func) {
        int _tX = in(x);
        int _tY = in(y);
        int tX = in(x+sizeX);
        int tY = in(y+sizeY);
        for (int _x = _tX; _x < tX; _x++)
            for (int _y = _tY; _y < tY; _y++)
                colors[_x + _y * 128] = func.invoke(colors[_x + _y * 128]);
        return this;
    }
    public DrawMap rectangle(int x, int y, int sizeX, int sizeY, byte color) {
        return rectangle(x,y,sizeX,sizeY,color,true);
    }
    public DrawMap rectangle(int x, int y, int sizeX, int sizeY, byte color, boolean fill) {
        int tX = in(x+sizeX);
        int tY = in(y+sizeY);
        if (fill) {
            for (int _x = in(x); _x < tX; _x++)
                for (int _y = in(y); _y < tY; _y++)
                    colors[_x + _y * 128] = color;
        } else {
            int __x = in(x);
            int __y = in(y);

            for (int _x = __x; _x < tX; _x++) {
                colors[_x + (tY - 1) * 128] = color;
                colors[_x + __y * 128] = color;
            }
            for (int _y = __y; _y < tY; _y++) {
                colors[(tX - 1) + _y * 128] = color;
                colors[__x + _y * 128] = color;
            }
        }
        return this;
    }
    public DrawMap rectangle(int x, int y, int sizeX, byte[] pixels, Map<Byte, Color> colors) {
        int tX = in(x+sizeX);
        int sizeY = pixels.length / sizeX;
        int tY = in(y+sizeY);
        Map<Byte, Byte> tCs = system.map.<Byte, Byte>of().add(colors.entrySet(), Map.Entry::getKey, kv -> to(kv.getValue())).build();
        for (int _x = in(x); _x < tX; _x++)
            for (int _y = in(y); _y < tY; _y++) {
                Byte color = tCs.getOrDefault(pixels[(_x - x) + (_y - y) * sizeX], null);
                if (color == null) continue;
                this.colors[_x + _y * 128] = color;
            }
        return this;
    }
    public DrawMap rectangle(int x, int y, int sizeX, Byte[] pixels) {
        int tX = in(x+sizeX);
        int sizeY = sizeX <= 0 ? 0 : (pixels.length / sizeX);
        int tY = in(y+sizeY);
        for (int _x = in(x); _x < tX; _x++)
            for (int _y = in(y); _y < tY; _y++) {
                Byte color = pixels[(_x - x) + (_y - y) * sizeX];
                if (color == null) continue;
                this.colors[_x + _y * 128] = color;
            }
        return this;
    }
    private static boolean inCircle(int x, int y, int radius, int px, int py) {
        return Math.pow(px - x + 0.5, 2) + Math.pow(py - y + 0.5, 2) <= Math.pow(radius + 0.5, 2);
    }
    public DrawMap circle(int x, int y, int radius, byte color) {
        int mx = x + radius;
        int my = y + radius;
        for (int _x = x - radius; _x <= mx; _x++)
            for (int _y = y - radius; _y <= my; _y++)
                if (inCircle(x, y, radius, _x, _y))
                    pixel(_x, _y, color);
        return this;
    }
    public DrawMap line(int from, int to, system.Func1<Integer, system.Toast2<Integer, Integer>> func, Color color) {
        byte tC = to(color);
        for (int i = from; i < to; i++) pixel(func.invoke(i), tC);
        return this;
    }
    public DrawMap draw(system.Action1<DrawMap> invoke) {
        invoke.invoke(this);
        return this;
    }
    public DrawMap draw(int offsetX, int offsetY, Images image) {
        image.draw(this, offsetX, offsetY);
        return this;
    }
    public byte[] save() { return colors; }
    public DrawMap copy() { return of(colors); }
    public static DrawMap of(Color bcg) { return new DrawMap(bcg); }
    public static DrawMap of() { return of(Color.WHITE); }
    public static DrawMap of(byte[] data) { return new DrawMap(Arrays.copyOf(data, data.length)); }

    public static void bufferReset() {
        CacheBuffer.cacheBuffer.clear();
    }
    public static void sendMap(Player player, int mapID, byte[] data) {
        system.Toast2<UUID, Integer> kv = system.toast(player.getUniqueId(), mapID);
        byte[] old = CacheBuffer.cacheBuffer.getOrDefault(kv, null);
        if (Arrays.compare(old, data) == 0) return;
        PacketManager.sendPacket(player, new PacketPlayOutMap(
                mapID,
                (byte)0,
                false,
                null,
                new WorldMap.b(0, 0, 128, 128, data)));
    }
}































