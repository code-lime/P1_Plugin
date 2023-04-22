package org.lime.gp.chat;

import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.papermc.paper.adventure.AdventureComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.network.chat.IChatBaseComponent;
import org.lime.*;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.lime.gp.database.MySql;
import org.lime.gp.database.rows.BaseRow;
import org.lime.gp.database.tables.ITable;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.lime;
import org.lime.gp.module.JavaScript;

import java.util.*;
import java.util.stream.Collectors;

public class ChatHelper {
    public static String getHexColor(byte gray) {
        return getHexColor(gray, gray, gray);
    }
    public static String getHexColor(byte r, byte g, byte b) {
        return "#" + (String.format("%02X", r) + String.format("%02X", g) + String.format("%02X", b)).toLowerCase();
    }
    public static String getHexColor(float r, float g, float b) {
        return getHexColor((byte)(r * 255), (byte)(g * 255), (byte)(b * 255));
    }
    public static String getHsvColor(float hue, float saturation, float value) {
        int h = (int)(hue * 6);
        float f = hue * 6 - h;
        float p = value * (1 - saturation);
        float q = value * (1 - f * saturation);
        float t = value * (1 - (1 - f) * saturation);

        return switch (h) {
            case 0 -> getHexColor(value, t, p);
            case 1 -> getHexColor(q, value, p);
            case 2 -> getHexColor(p, value, t);
            case 3 -> getHexColor(p, q, value);
            case 4 -> getHexColor(t, p, value);
            case 5 -> getHexColor(value, p, q);
            default -> throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + hue + ", " + saturation + ", " + value);
        };
    }

    public static Component fromText(String text) {
        return fromText(text, NamedTextColor.WHITE);
    }
    public static Component fromText(String text, TextColor color) {
        return Component.text(text, createDefault().color(color).build());
    }
    public static Component formatComponent(String text, Style.Builder style) {
        return formatComponent(text, '<', '>', style);
    }
    public static Component formatComponent(String text) {
        return formatComponent(text,  createDefault());
    }
    public static List<Component> formatComponent(List<String> text) {
        return text.stream().map(ChatHelper::formatComponent).collect(Collectors.toList());
    }
    public static List<Component> formatComponent(List<String> text, Apply apply) {
        return text.stream().map(msg -> formatComponent(msg, apply)).collect(Collectors.toList());
    }

    public static Component formatComponent(String text, char start, char end, Style.Builder style) {
        return text.contains(String.valueOf(start)) || text.contains(String.valueOf(end)) ? IFormatComponent(text, start, end, style) : Component.text(text.replace("\u0000", ""), style.build());
    }
    private static String jsFix(String js) {
        String str = new JsonPrimitive(js)
            .toString()
            .replace("<", "<<")
            .replace(">", ">>")
            .replace("'", "\\'");
        return str.substring(1, str.length() - 1);
        //return js.replace("\\", "\\\\").replace("'", "\\'").replace("<", "<<").replace(">", ">>");
    }
    private static String txtFix(String txt) {
        return txt.replace("<", "<<").replace(">", ">>");
    }
    public static system.Func1<String, String> jsFix(Map<String, String> _args) {
        return _text -> {
            try {
                String key = _text;
                int prefixIndex = key.indexOf(':');
                if (prefixIndex != -1) {
                    String prefix = key.substring(0, prefixIndex);
                    key = key.substring(prefixIndex + 1);

                    switch (prefix) {
                        case "JS": {
                            if (!_args.containsKey(key)) return null;
                            String out = _args.get(key);
                            return jsFix(out == null ? "" : out);
                        }
                        case "JS.EXE": {
                            return JavaScript.getJsString(ChatHelper.replaceBy(key, '{', '}', jsFix(_args))).orElse("");
                        }
                        case "TXT": {
                            if (!_args.containsKey(key)) return null;
                            String out = _args.get(key);
                            return txtFix(out == null ? "" : out);
                        }
                        case "SQL": {
                            if (!_args.containsKey(key)) return null;
                            String out = _args.get(key);
                            return MySql.toSqlObject(out == null ? "" : out);
                        }
                        case "JS.SQL": {
                            if (!_args.containsKey(key)) return null;
                            String out = _args.get(key);
                            return jsFix(MySql.toSqlObject(out == null ? "" : out));
                        }
                    }
                }
                if (!_args.containsKey(_text)) return null;
                String out = _args.get(_text);
                return out == null ? "" : out;
            } catch (Exception e) {
                lime.logStackTrace(e);
                return _text;
            }
        };
        //return _text -> _text.startsWith("JS:") ? jsFix(_args.getOrDefault(_text.substring(3), "")) : _args.getOrDefault(_text, null);
    }

    public static String formatText(String text, Apply apply) {
        text = apply.apply(text);
        if (text.startsWith("!js ")) text = JavaScript.getJsString(text.substring(4)).orElse("");
        return text;
    }
    public static Component formatComponent(String text, Apply apply) {
        return formatComponent(formatText(text, apply), createDefault());
    }

    @SuppressWarnings({"deprecation", "removal"})
    public static String getLegacyText(Component component) {
        return Bukkit.getUnsafe().legacyComponentSerializer().serialize(component);
    }
    @SuppressWarnings({"deprecation", "removal"})
    public static Component getFromLegacyText(String text) {
        return Bukkit.getUnsafe().legacyComponentSerializer().deserialize(text);
    }

    public static JsonElement toJson(Component component) {
        return GsonComponentSerializer.gson().serializeToTree(component);
    }
    public static Component fromJson(JsonElement json) {
        if (json == null) return Component.empty();
        return GsonComponentSerializer.gson().deserializeFromTree(json);
    }

    public static WrappedChatComponent toWrapped(Component component) {
        return WrappedChatComponent.fromHandle(toNMS(component));
    }
    public static IChatBaseComponent toNMS(Component component) {
        return new AdventureComponent(component);
    }
    public static BaseComponent toMD5(Component component) {
        return toMD5(toJson(component));
    }
    public static BaseComponent toMD5(JsonElement json) {
        BaseComponent[] base = ComponentSerializer.parse(json.toString());
        int length = base.length;
        if (length == 1) return base[0];
        net.md_5.bungee.api.chat.TextComponent text = new net.md_5.bungee.api.chat.TextComponent();
        for (int i = 0; i < length; i++) text.addExtra(base[i]);
        return text;
    }
    public static IChatBaseComponent toNMS(JsonElement json) {
        return IChatBaseComponent.ChatSerializer.fromJson(json);
    }

    private static class IFormatter {
        public static List<IFormatter> formats = new ArrayList<>();

        static {
            /*STATIC*/        IFormatter.create(input -> {
                switch (input) {
                    case "R":
                    case "L":
                    case "RA":
                    case "LA":

                    case "BOLD":
                    case "ITALIC":
                    case "MAGIC":
                    case "UNDERLINE":
                    case "STRIKETHROUGH":

                    case "/":
                    case "/BOLD":
                    case "/ITALIC":
                    case "/MAGIC":
                    case "/UNDERLINE":
                    case "/STRIKETHROUGH":

                    case "/HOVER":
                    case "/INSERTION":
                    case "/FONT":
                    case "/CLICK": return true;
                }
                return false;
            }, (input, style) -> {
                switch (input) {
                    case "R": return Component.text(">", style.build());
                    case "L": return Component.text("<", style.build());
                    case "RA": return Component.text("}", style.build());
                    case "LA": return Component.text("{", style.build());

                    case "BOLD": style.decoration(TextDecoration.BOLD, true); break;
                    case "ITALIC": style.decoration(TextDecoration.ITALIC, true); break;
                    case "MAGIC": style.decoration(TextDecoration.OBFUSCATED, true); break;
                    case "UNDERLINE": style.decoration(TextDecoration.UNDERLINED, true); break;
                    case "STRIKETHROUGH": style.decoration(TextDecoration.STRIKETHROUGH, true); break;

                    case "/": resetStyle(style); break;
                    case "/BOLD": style.decoration(TextDecoration.BOLD, false); break;
                    case "/ITALIC": style.decoration(TextDecoration.ITALIC, false); break;
                    case "/MAGIC": style.decoration(TextDecoration.OBFUSCATED, false); break;
                    case "/UNDERLINE": style.decoration(TextDecoration.UNDERLINED, false); break;
                    case "/STRIKETHROUGH": style.decoration(TextDecoration.STRIKETHROUGH, false); break;

                    case "/HOVER": style.hoverEvent(null); break;
                    case "/INSERTION": style.insertion(null); break;
                    case "/FONT": style.font(null); break;
                    case "/CLICK": style.clickEvent(null); break;
                }
                return Component.empty();
            });
            /*COLOR*/         IFormatter.createEmpty(input -> {
                if (input.length() != 7 || input.charAt(0) != '#') return false;
                String color = input.substring(1).toUpperCase();
                for (int i = 0; i < 6; i++) {
                    char ch = color.charAt(i);
                    if (ch >= '0' && ch <= '9') continue;
                    if (ch >= 'A' && ch <= 'F') continue;
                    return false;
                }
                return true;
            }, (input, style) -> style.color(ChatColorHex.of(input.substring(1)).toTextColor()));
            /*OFFSET*/        IFormatter.create(input -> {
                if (input.length() <= 0) return false;
                switch (input.charAt(0)) {
                    case '+':
                    case '-': break;
                    default: return false;
                }
                return ExtMethods.parseUnsignedInt(input.substring(1)).isPresent();
            }, (input, style) -> {
                int s = 0;
                switch (input.charAt(0)) {
                    case '+': s = 1; break;
                    case '-': s = -1; break;
                }
                return Component.text(getSpaceSize(Integer.parseUnsignedInt(input.substring(1)) * s), style.build());
            });
            /*UNICODE*/       IFormatter.create(input -> {
                if (input.length() != 5 || input.charAt(0) != 'u') return false;
                return ExtMethods.parseUnsignedInt(input.substring(1), 16).isPresent();
            }, (input, style) -> Component.text((char)Integer.parseUnsignedInt(input.substring(1), 16), style.build()));
            /*FORMAT*/        IFormatter.createEmpty(input -> system.tryParse(ChatColor.class, input).isPresent(), (input, style) -> {
                ChatColor color = ChatColor.valueOf(input);
                if (color.isColor()) {
                    style.color(new ChatColorHex(color).toTextColor());
                } else {
                    switch (color) {
                        case BOLD: style.decoration(TextDecoration.BOLD, true); break;
                        case ITALIC: style.decoration(TextDecoration.ITALIC, true); break;
                        case MAGIC: style.decoration(TextDecoration.OBFUSCATED, true); break;
                        case RESET: resetStyle(style); break;
                        case UNDERLINE: style.decoration(TextDecoration.UNDERLINED, true); break;
                        case STRIKETHROUGH: style.decoration(TextDecoration.STRIKETHROUGH, true); break;
                        default: break;
                    }
                }
            });
            /*CHAR_FORMAT*/   IFormatter.create(input -> {
                if (input.length() < 1) return false;
                switch (input.charAt(0)) {
                    case '?':
                    case '@':
                    case '\'':
                    case 'L':
                    case '$': return true;
                    case '%': return input.indexOf('.', 1) != -1;
                }
                return false;
            }, (input, style) -> {
                switch (input.charAt(0)) {
                    case '?': return Component.translatable(input.substring(1), style.build());
                    case '@': return Component.selector(input.substring(1));
                    case '$': return Component.keybind(input.substring(1), style.build());
                    case 'L': {
                        int line = Integer.parseInt(input.substring(1));
                        int total = line / 5;
                        int add = line % 5;
                        String offset = ChatHelper.getSpaceSize(-1);
                        List<String> list = new ArrayList<>();
                        for (int i = 0; i < total; i++) list.add("-");
                        for (int i = 0; i < add; i++) list.add("·");
                        return Component.text(String.join(offset, list)).style(style.build());
                    }
                    case '\'': {
                        String[] args = input.substring(1).split("\\:");
                        int size = -(Integer.parseInt(args[0]) / 2);
                        int offset = Integer.parseInt(args[1]);

                        return Component.text(ChatHelper.getSpaceSize(size + offset))
                                .append(formatComponent(Arrays.stream(args).skip(2).collect(Collectors.joining(":"))))
                                .append(Component.text(ChatHelper.getSpaceSize(size - offset)));
                    }
                    case '%': {
                        String[] args = input.substring(1).split("\\.");
                        return Component.score(args[0], Arrays.stream(args).skip(1).collect(Collectors.joining(".")));
                    }
                }
                return Component.empty();
            });
            /*STRING_FORMAT*/ IFormatter.create(input -> {
                String[] args = input.split(":");
                if (args.length <= 1) return false;
                switch (args[0]) {
                    case "HOVER_ITEM":
                    case "HOVER_ENTITY":
                    case "HOVER_TEXT":
                    case "CLICK_URL":
                    case "CLICK_FILE":
                    case "CLICK_EXECUTE":
                    case "CLICK_SUGGEST":
                    case "CLICK_PAGE":
                    case "CLICK_COPY":
                    case "FONT":
                    case "INSERTION":
                    case "JS":
                    case "NICK":
                        return true;
                }
                return false;
            }, (input, style) -> {
                String[] args = input.split(":");
                String key = args[0];
                String value = Arrays.stream(args).skip(1).collect(Collectors.joining(":"));
                switch (key) {
                    case "HOVER_ITEM": style.hoverEvent(HoverEvent.showItem(itemOfString(value))); break;
                    case "HOVER_ENTITY": style.hoverEvent(HoverEvent.showEntity(entityOfString(value))); break;
                    case "HOVER_TEXT": style.hoverEvent(HoverEvent.showText(formatComponent(value))); break;
                    case "CLICK_URL": style.clickEvent(ClickEvent.openUrl(value)); break;
                    case "CLICK_FILE": style.clickEvent(ClickEvent.openFile(value)); break;
                    case "CLICK_EXECUTE": style.clickEvent(ClickEvent.runCommand(value)); break;
                    case "CLICK_SUGGEST": style.clickEvent(ClickEvent.suggestCommand(value)); break;
                    case "CLICK_PAGE": style.clickEvent(ClickEvent.changePage(value)); break;
                    case "CLICK_COPY": style.clickEvent(ClickEvent.copyToClipboard(value)); break;
                    case "FONT": style.font(Key.key(value)); break;
                    case "INSERTION": style.insertion(value); break;
                    case "JS": return JavaScript.getJsString(value).map(text -> formatComponent(text, style)).orElseGet(Component::empty);
                    case "NICK": {
                        String name = Bukkit.getOfflinePlayer(UUID.fromString(value)).getName();
                        return Component.text(name == null ? "" : name, style.build());
                    }
                }
                return Component.empty();
            });
        }

        private static HoverEvent.ShowItem itemOfString(String str) {
            String[] args = str.split(",");
            return switch (args.length) {
                case 1 -> HoverEvent.ShowItem.of(Key.key(args[0]), 1);
                case 2 -> HoverEvent.ShowItem.of(Key.key(args[0]), Integer.parseInt(args[1]));
                default -> HoverEvent.ShowItem.of(Key.key(args[0]), Integer.parseInt(args[1]), BinaryTagHolder.binaryTagHolder(Arrays.stream(args).skip(2).collect(Collectors.joining(","))));
            };
        }
        private static HoverEvent.ShowEntity entityOfString(String str) {
            String[] args = str.split(",");
            switch (args.length) {
                case 1: return HoverEvent.ShowEntity.of(Key.key(args[0]), new UUID(0, 0));
                case 2: return HoverEvent.ShowEntity.of(Key.key(args[0]), UUID.fromString(args[1]));
                default: return HoverEvent.ShowEntity.of(Key.key(args[0]), UUID.fromString(args[1]), formatComponent(Arrays.stream(args).skip(2).collect(Collectors.joining(","))));
            }
        }

        public final system.Func1<String, Boolean> check;
        public final system.Func2<String, Style.Builder, Component> tryParse;
        private IFormatter(system.Func1<String, Boolean> check, system.Func2<String, Style.Builder, Component> tryParse) {
            this.tryParse = tryParse;
            this.check = check;
        }
        public static void create(system.Func1<String, Boolean> check, system.Func2<String, Style.Builder, Component> tryParse) {
            formats.add(new IFormatter(check, tryParse));
        }
        public static void createEmpty(system.Func1<String, Boolean> check, system.Action2<String, Style.Builder> tryParse) {
            create(check, (a1, a2) -> {
                tryParse.invoke(a1,a2);
                return Component.empty();
            });
        }
        public static Component format(String tag, Style.Builder style) {
            for (IFormatter format : formats) {
                try
                {
                    if (!format.check.invoke(tag)) continue;
                    Component component = format.tryParse.invoke(tag, style);
                    if (component == null) continue;
                    return component;
                }
                catch (Exception ignored)
                {

                }
            }
            return Component.text("<"+tag+">");
        }
    }

    private static abstract class ITag {
        public String text;
        public ITag(String text) {
            this.text = text;
        }
        public abstract Component create(Style.Builder style);
    }
    private static class Tag extends ITag {
        public Tag(String text) {
            super(text);
        }
        @Override public Component create(Style.Builder style) {
            return IFormatter.format(text.replace("\u0000", ""), style);
        }
    }
    private static class Text extends ITag {
        public Text(String text) {
            super(text);
        }
        @Override public Component create(Style.Builder style) {
            return Component.text(text.replace("\u0000", ""), style.build());
        }
    }

    public static String applySqlJs(String table, String check, String filter, String output) {
        ITable<? extends BaseRow> _table = Tables.getLoadedTable(table);
        if (_table == null) return null;
        return _table.getBy(row -> filter.equals(row.applyToString(check, '(', ')'))).map(_row -> _row.applyToString(output, '(', ')')).orElse("");
    }
    public static String replaceBy(String text, char start, char end, HashMap<String, String> replace) {
        return replaceBy(text, start, end, v -> replace.getOrDefault(v, null));
    }
    public static String replaceBy(String text, char start, char end, system.Func1<String, String> invoke) {
        StringBuilder builder = new StringBuilder();
        List<ITag> tags = parseToTags(text, start, end);
        int length = tags.size();
        for (int i = 0; i < length; i++) {
            ITag tag = tags.get(i);
            if (tag instanceof Tag) {
                String ret = invoke.invoke(tag.text);
                if (ret == null) builder.append(start).append(tag.text).append(end);
                else builder.append(ret);
            } else {
                builder.append(tag.text);
            }
        }
        return builder.toString();
    }

    private static String reverse(String rev) {
        return new StringBuilder(rev).reverse().toString();
    }

    private static system.Toast3<String, String, Integer> getBetween(String str, int offset, char start, char end) {
        String _str = str;

        String doubleStart = start + "" + start;
        String doubleEnd = end + "" + end;
        String doubleEmpty = "\1\1";
        system.Func1<String, String> doubleReplace = _val -> _val.replace(doubleStart, String.valueOf(start)).replace(doubleEnd, String.valueOf(end));

        str = reverse(reverse(str.replace(doubleStart, doubleEmpty)).replace(doubleEnd, doubleEmpty));

        if (offset >= str.length()) return system.toast("", null, 0);
        int start_index = str.indexOf(start, offset);
        if (start_index == -1) return system.toast(doubleReplace.invoke(_str.substring(offset)), null, 0);
        int index = start_index;
        int count = 0;
        while (true)
        {
            int index_start = str.indexOf(start, index + 1);
            int index_end = str.indexOf(end, index + 1);
            if (index_end == -1)
            {
                index = str.length();
                break;
            }
            if (index_start != -1 && index_end > index_start)
            {
                count++;
                index = index_start;
            }
            else
            {
                count--;
                index = index_end;
                if (count < 0) break;
            }
        }
        return system.toast(doubleReplace.invoke(_str.substring(offset, start_index)), doubleReplace.invoke(_str.substring(start_index + 1, index)), index + 1);
    }

    public static List<ITag> parseToTags(String text, char start, char end) {
        List<ITag> tags = new ArrayList<>();
        int offset = 0;
        while (true) {
            system.Toast3<String, String, Integer> getter = getBetween(text, offset, start, end);
            if (getter.val1 == null)  {
                tags.add(new Text(getter.val0));
                return tags;
            }
            offset = getter.val2;
            tags.add(new Text(getter.val0));
            tags.add(new Tag(getter.val1));
        }
    }
    private static Style.Builder createDefault() {
        return resetStyle(Style.style());
    }
    private static Style.Builder resetStyle(Style.Builder builder) {
        builder.color(NamedTextColor.WHITE);
        for (TextDecoration decoration : TextDecoration.values()) builder.decoration(decoration, false);
        return builder.clickEvent(null).hoverEvent(null).font(Style.DEFAULT_FONT).insertion(null);
    }
    static Component IFormatComponent(String text, char start, char end, Style.Builder style) {
        TextComponent.Builder builder = Component.text();
        parseToTags(text, start, end).forEach(tag -> builder.append(tag.create(style)));
        return builder.build();
    }

    public static int getSymbolSize(char ch, int def) { return CharLib.sizeMap.getOrDefault(ch, def); }
    public static String getSpaceSize(int def) { return CharLib.spaceSize.getOrDefault(def, ""); }
    public static int getTextSize(String text) {
        int size = text.length();
        if (size > 0) size--;
        for (char ch : text.toCharArray()) {
            int length = CharLib.sizeMap.getOrDefault(ch, -1);
            if (length == -1) {
                length = 5;
                CharLib.sizeMap.put(ch, length);
                lime.logOP("SYMBOL SIZE '" + ch + "' NFND");
                lime.logToFile("symbol", "[{time}] Symbol size '" + ch + "' not founded");
            }
            size += length;
        }
        return size;
    }
    public static int getTextSize(Component text) {
        system.Toast1<String> onlyText = system.toast("");
        ComponentFlattener.textOnly().flatten(text, txt -> onlyText.val0 += txt);
        return getTextSize(onlyText.val0);
    }

    public static String padLeft(int pixels, String text) {
        pixels = pixels - getTextSize(text);
        if (pixels <= 0) return text;
        return CharLib.spaceSize.get(pixels) + text;
    }
    public static String padLeft(int pixels, char symbol) {
        pixels = pixels - CharLib.sizeMap.getOrDefault(symbol, 5);
        if (pixels <= 0) return symbol + "";
        return CharLib.spaceSize.get(pixels) + symbol;
    }
}
































