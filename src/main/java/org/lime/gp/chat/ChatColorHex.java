package org.lime.gp.chat;

import net.minecraft.network.chat.ChatHexColor;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.Color;

public class ChatColorHex {
    public int r;
    public int g;
    public int b;
    public ChatColorHex(float r, float g, float b) {
        this.r = (int)(r * 256);
        this.g = (int)(g * 256);
        this.b = (int)(b * 256);
    }
    public ChatColorHex(Color color) {
        this.r = color.getRed();
        this.g = color.getGreen();
        this.b = color.getBlue();
    }
    public ChatColorHex(String _hex) {
        Toast3<Integer, Integer, Integer> hex = fromHex(_hex);

        r = hex.val0;
        g = hex.val1;
        b = hex.val2;
    }
    public ChatColorHex(ChatColor color) {
        Toast3<Integer, Integer, Integer> hex;
        switch (color)
        {
            case DARK_RED: hex = fromHex("AA0000"); break;
            case RED: hex = fromHex("FF5555"); break;
            case GOLD: hex = fromHex("FFAA00"); break;
            case YELLOW: hex = fromHex("FFFF55"); break;
            case DARK_GREEN: hex = fromHex("00AA00"); break;
            case GREEN: hex = fromHex("55FF55"); break;
            case AQUA: hex = fromHex("55FFFF"); break;
            case DARK_AQUA: hex = fromHex("00AAAA"); break;
            case DARK_BLUE: hex = fromHex("0000AA"); break;
            case BLUE: hex = fromHex("5555FF"); break;
            case LIGHT_PURPLE: hex = fromHex("FF55FF"); break;
            case DARK_PURPLE: hex = fromHex("AA00AA"); break;
            case WHITE: hex = fromHex("FFFFFF"); break;
            case GRAY: hex = fromHex("AAAAAA"); break;
            case DARK_GRAY: hex = fromHex("555555"); break;
            case BLACK: hex = fromHex("000000"); break;
            default: throw new IllegalArgumentException("color");
        }

        r = hex.val0;
        g = hex.val1;
        b = hex.val2;
    }

    private static String format(byte r, byte g, byte b) {
        String hex = (String.format("%02X", r) + String.format("%02X", g) + String.format("%02X", b)).toLowerCase();
        StringBuilder tostring = new StringBuilder().append("§x");
        for (Character ch : hex.toCharArray()) tostring.append("§").append(ch);
        return tostring.toString();
    }
    public static Toast3<Integer, Integer, Integer> fromHex(String hex) {
        hex = hex.replace("#", "");
        int len = hex.length();
        if (len != 6) throw new IllegalArgumentException("hex");
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
            data[i / 2] =  (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i+1), 16));
        return new Toast3<>(data[0] & 0xFF, data[1] & 0xFF, data[2] & 0xFF);
    }

    @Override public String toString() {
        return format((byte)r, (byte)g, (byte)b);
    }
    public Color toBukkitColor() { return Color.fromRGB(r,g,b); }
    public ChatHexColor toNMS() { return ChatHexColor.fromRgb(toBukkitColor().asRGB()); }
    public TextColor toTextColor() {
        return TextColor.color(r,g,b);
    }
    public int toARGB() {
        int Red = r;
        int Green = g;
        int Blue = b;

        Red = (Red << 16) & 0x00FF0000;
        Green = (Green << 8) & 0x0000FF00;
        Blue = Blue & 0x000000FF;

        return 0xFF000000 | Red | Green | Blue;
    }

    public static ChatColorHex ofGray(float gray) { return new ChatColorHex(gray, gray, gray); }

    public static ChatColorHex of(String hex) {
        return new ChatColorHex(hex);
    }
    public static ChatColorHex of(ChatColor color) {
        return new ChatColorHex(color);
    }
    public static ChatColorHex of(Color color) {
        return new ChatColorHex(color);
    }
    public static String toHex(Color color) {
        return TextColor.color(color.asRGB()).asHexString();
    }
}
