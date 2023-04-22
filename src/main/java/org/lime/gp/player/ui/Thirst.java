package org.lime.gp.player.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.lime.core;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.extension.JManager;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.Checker;
import org.lime.gp.lime;
import org.lime.gp.town.ChurchManager;
import org.lime.system;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class Thirst implements Listener, CustomUI.IUI {
    private static final Thirst Instance = new Thirst();
    private Thirst() {}

    public static core.element create() {
        return core.element.create(Thirst.class)
                .withInstance(Instance)
                .withInit(Thirst::init)
                .<JsonObject>addConfig("thirst", v -> v.withInvoke(Thirst::config).withDefault(new JsonObject()));
    }

    public static void init() {
        AnyEvent.addEvent("thirst.value", AnyEvent.type.other, builder -> builder.createParam(Double::parseDouble, "[value:0-20]"), Thirst::thirstValue);
        AnyEvent.addEvent("thirst.state.add", AnyEvent.type.other, builder -> builder.createParam(ThirstData.StateData.dats::get, ThirstData.StateData.dats::keySet), Thirst::thirstState);

        HashMap<UUID, Vector> actualData = new HashMap<>();
        lime.repeat(() -> {
            HashMap<UUID, Vector> lastData = new HashMap<>();
            Bukkit.getOnlinePlayers().forEach(player -> {
                switch (player.getGameMode()) {
                    case CREATIVE:
                    case SPECTATOR: return;
                    default:
                        break;
                }
                UUID uuid = player.getUniqueId();

                Vector last = actualData.getOrDefault(uuid, new Vector());
                Vector now = player.getLocation().toVector();
                lastData.put(uuid, now);
                boolean moved = last.distance(now) > 0.1;

                ThirstData data = getThirst(player);
                data.update(player, moved, 0.15);
                setThirst(player, data);
            });
            actualData.clear();
            actualData.putAll(lastData);
        }, 0.15);
        lime.repeat(() -> Bukkit.getOnlinePlayers().forEach(player -> {
            switch (player.getGameMode()) {
                case CREATIVE:
                case SPECTATOR: return;
                default:
                    break;
            }
            ThirstData data = getThirst(player);
            data.updateDesert(player, 5);
            setThirst(player, data);
        }), 5);

        CustomUI.addListener(Instance);
    }
    public static void config(JsonObject json) {
        ThirstData.StateData.parseAll(json);
    }
    public static void thirstValue(Player player, double value) {
        ThirstData data = getThirst(player);
        data.value = value;
        if (data.value < 0) data.value = 0;
        else if (data.value > 20) data.value = 20;
        setThirst(player, data);
    }
    public static void thirstState(Player player, ThirstData.StateData state) {
        ThirstData data = getThirst(player);
        data.times.put(state.key, 0.0);
        setThirst(player, data);
    }
    public static void thirstStateByKey(Player player, String stateKey) {
        ThirstData data = getThirst(player);
        data.times.put(stateKey, 0.0);
        setThirst(player, data);
    }
    public static void thirstStateReset(Player player) {
        ThirstData data = getThirst(player);
        data.times.clear();
        setThirst(player, data);
    }
    public static void thirstReset(Player player) {
        ThirstData data = getThirst(player);
        data.times.clear();
        data.value = 20;
        setThirst(player, data);
    }

    private static boolean isDesert(Location location) {
        return switch (location.getWorld().getBiome(location)) {
            case BADLANDS, ERODED_BADLANDS, WOODED_BADLANDS, DESERT, SAVANNA, SAVANNA_PLATEAU, WINDSWEPT_SAVANNA -> true;
            default -> false;
        };
    }

    private static class ThirstData {
        private static final int THIRST_VERSION = 1;

        public static class StateColor {
            public final List<ImageBuilder> whole;
            public final List<ImageBuilder> half;
            public final List<ImageBuilder> empty;

            private static List<ImageBuilder> parsePart(JsonObject json, int offset) {
                int baseOffset = json.has("offset") ? json.get("offset").getAsInt() : offset;
                TextColor color = TextColor.fromHexString("#" + json.get("color").getAsString());
                List<ImageBuilder> list = new ArrayList<>();
                json.get("image").getAsJsonArray().forEach(v -> {
                    String[] args = v.getAsString().split(":");
                    int image = Integer.parseInt(args[0], 16);
                    int size = Integer.parseInt(args[1]);
                    list.add(ImageBuilder.of(image, size).withColor(color).addOffset(baseOffset));
                });
                return list;
            }
            public static StateColor parse(JsonObject json) {
                int offset = json.has("offset") ? json.get("offset").getAsInt() : 0;
                return new StateColor(
                        parsePart(json.get("whole").getAsJsonObject(), offset),
                        parsePart(json.get("half").getAsJsonObject(), offset),
                        parsePart(json.get("empty").getAsJsonObject(), offset)
                );
            }

            public enum Type {
                Whole,
                Half,
                Empty
            }

            public ImageBuilder get(Type type, int level) {
                List<ImageBuilder> list;
                switch (type) {
                    case Whole: list = whole; break;
                    case Half: list = half; break;
                    case Empty: list = empty; break;
                    default: return ImageBuilder.empty;
                }
                return system.getOrDefault(list, level, ImageBuilder.empty);
            }

            public StateColor(List<ImageBuilder> whole, List<ImageBuilder> half, List<ImageBuilder> empty) {
                this.whole = whole;
                this.half = half;
                this.empty = empty;
            }
        }
        /*public enum StateColor {
            Desert(LangMessages.Message.UI_Thirst_Desert_Whole, LangMessages.Message.UI_Thirst_Desert_Half, LangMessages.Message.UI_Thirst_Desert_Empty),
            Drunk(LangMessages.Message.UI_Thirst_Drunk_Whole, LangMessages.Message.UI_Thirst_Drunk_Half, LangMessages.Message.UI_Thirst_Drunk_Empty),
            Saturation(LangMessages.Message.UI_Thirst_Saturation_Whole, LangMessages.Message.UI_Thirst_Saturation_Half, LangMessages.Message.UI_Thirst_Saturation_Empty),
            Thirst(LangMessages.Message.UI_Thirst_Thirst_Whole, LangMessages.Message.UI_Thirst_Thirst_Half, LangMessages.Message.UI_Thirst_Thirst_Empty),
            Wither(LangMessages.Message.UI_Thirst_Wither_Whole, LangMessages.Message.UI_Thirst_Wither_Half, LangMessages.Message.UI_Thirst_Wither_Empty),
            Default(LangMessages.Message.UI_Thirst_Default_Whole, LangMessages.Message.UI_Thirst_Default_Half, LangMessages.Message.UI_Thirst_Default_Empty);

            public final LangMessages.Message whole;
            public final LangMessages.Message half;
            public final LangMessages.Message empty;

            StateColor(LangMessages.Message whole, LangMessages.Message half, LangMessages.Message empty) {
                this.whole = whole;
                this.half = half;
                this.empty = empty;
            }
        }*/
        public static class StateData {
            private static final HashMap<String, StateData> dats = new HashMap<>();
            public static final List<system.Toast2<Checker, StateData>> includes = new ArrayList<>();
            private static double getDelta(double des, double def, double perc) {
                return ((des - def) * (1 - perc)) + def;
            }
            public static StateData getDefault(boolean desert, double desertPerc) {
                StateData _default = dats.get("default");
                if (!desert) return _default;
                StateData _desert = dats.get("desert");
                double delta = getDelta(_desert.delta, _default.delta, desertPerc);
                return new StateData("default", delta, 0, _desert.color, "default", _desert.effects, _desert.commands);
            }

            public final String key;

            public final double delta;
            public final double wait;
            public final StateColor color;
            public final String next;
            public final List<PotionEffect> effects = new ArrayList<>();
            public final List<String> commands = new ArrayList<>();

            private StateData(String key, double delta, double wait, StateColor color, String next, List<PotionEffect> effects, List<String> commands) {
                this.key = key;
                this.delta = delta;
                this.wait = wait;
                this.color = color;
                this.next = next;
                this.effects.addAll(effects);
                this.commands.addAll(commands);
            }
            private StateData(String key, JsonObject json, HashMap<String, StateColor> colors, String next) {
                this.key = key;
                delta = json.get("delta").getAsDouble();
                wait = json.get("wait").getAsDouble();
                color = colors.get(json.get("color").getAsString());
                this.next = next != null ? next : json.has("next") && json.get("next").isJsonPrimitive() ? json.get("next").getAsString() : "default";
                if (json.has("effects")) json.get("effects").getAsJsonArray().forEach(obj -> effects.add(Items.parseEffect(obj.getAsJsonObject())));
                if (json.has("commands")) json.get("commands").getAsJsonArray().forEach(obj -> commands.add(obj.getAsString()));
            }
            private StateData drink(double single) {
                return new StateData(key + ".drink", single, 0, color, key, effects, commands);
            }
            private double getDelta(Player player, boolean moving) {
                double delta = this.delta;
                if (delta >= 0) return delta;
                if (player.isSprinting()) delta *= 5;
                else if (moving) delta *= 2;
                return delta;
            }
            private String update(Player player, boolean moving, ThirstData data, double time) {
                double delta = getDelta(player, moving);
                if (delta < 0 && ChurchManager.hasEffect(player, ChurchManager.EffectType.SATURATION)) delta *= 0.5;
                data.value += delta;
                if (data.value < 0) data.value = 0;
                else if (data.value > 20) data.value = 20;
                effects.forEach(player::addPotionEffect);
                commands.forEach(cmd -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{name}", player.getName()).replace("{uuid}", player.getUniqueId().toString())));
                if (key.equals("default")) return "default";
                if (time >= wait) return next;
                return null;
            }
            private static List<StateData> parse(String key, JsonObject value, HashMap<String, StateColor> colors, String next) {
                List<StateData> list = new ArrayList<>();
                StateData data = new StateData(key, value, colors, next);
                list.add(data);

                if (value.has("single")) {
                    JsonElement single = value.get("single");
                    if (single.isJsonObject()) list.addAll(parse(key + ".drink", single.getAsJsonObject(), colors, key));
                    else list.add(data.drink(single.getAsDouble()));
                }

                return list;
            }
            public static void parseAll(JsonObject json) {
                HashMap<String, StateColor> colors = new HashMap<>();
                json.get("colors").getAsJsonObject().entrySet().forEach(kv -> colors.put(kv.getKey(), StateColor.parse(kv.getValue().getAsJsonObject())));
                HashMap<String, StateData> dats = new HashMap<>();
                JsonObject effects = json.get("effects").getAsJsonObject();
                effects.entrySet().forEach(kv -> parse(kv.getKey(), kv.getValue().getAsJsonObject(), colors, null).forEach(dat -> dats.put(dat.key, dat)));
                if (!dats.containsKey("default")) throw new IllegalArgumentException("StateData.default not founded!");

                List<system.Toast2<Checker, StateData>> includes = new ArrayList<>();
                json.get("invoke").getAsJsonObject().entrySet().forEach(kv -> {
                    String _data = kv.getValue().getAsString();
                    StateData data = dats.getOrDefault(_data, null);
                    if (data == null) throw new IllegalArgumentException("StateData."+_data+" not founded!");
                    includes.add(system.toast(Checker.createCheck(kv.getKey()), data));
                });

                StateData.dats.clear();
                StateData.dats.putAll(dats);

                StateData.includes.clear();
                StateData.includes.addAll(includes);
            }
        }

        public static ThirstData GetEmpty() { return new ThirstData(); }
        private ThirstData() {}

        public double value = 20;
        public double desertTime = 0;
        public boolean desert = false;

        public static final int MAX_DESERT_TIME = 10 * 60 * 60;

        public HashMap<String, Double> times = new HashMap<>();
        public StateColor getStateColor() {
            String state = times.entrySet()
                    .stream()
                    .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
            StateData data = state == null ? null : StateData.dats.getOrDefault(state, null);
            if (data == null) data = StateData.getDefault(desert, desertTime / MAX_DESERT_TIME);
            return data.color;
        }

        public static ThirstData parse(JsonObject json) {
            try {
                ThirstData dat = ThirstData.GetEmpty();
                if (json == null) return dat;
                if (json.get("v").getAsInt() != THIRST_VERSION) return GetEmpty();
                dat.value = json.get("value").getAsDouble();
                JsonObject desert = json.get("desert").getAsJsonObject();
                dat.desertTime = desert.get("time").getAsDouble();
                dat.desert = desert.get("desert").getAsBoolean();
                json.get("times").getAsJsonObject().entrySet().forEach(kv -> dat.times.put(kv.getKey(), kv.getValue().getAsDouble()));
                return dat;
            } catch (Exception e) {
                lime.logStackTrace(e);
                return GetEmpty();
            }
        }
        public JsonObject toJson() {
            return system.json
                    .object()
                    .add("v", THIRST_VERSION)
                    .add("value", value)
                    .addObject("desert", v -> v
                            .add("time", desertTime)
                            .add("desert", desert)
                    )
                    .add("times", times).build();
        }
        public void updateDesert(Player player, double delta_time) {
            desert = isDesert(player.getLocation());
            desertTime += delta_time * (desert ? 1 : -1);
            if (desertTime > MAX_DESERT_TIME) desertTime = MAX_DESERT_TIME;
            else if (desertTime < 0) desertTime = 0;
        }
        public void update(Player player, boolean moving, double delta_time) {
            Set<String> nextList = new HashSet<>();
            times.entrySet().removeIf(v -> {
                String key = v.getKey();
                if (key.equals("default")) return true;
                StateData data = StateData.dats.getOrDefault(key, null);
                if (data == null) return true;
                double time = v.getValue() + delta_time;
                v.setValue(time);
                String next = data.update(player, moving, this, time);
                if (next == null) return false;
                if (!next.equals("default")) nextList.add(next);
                return true;
            });
            nextList.forEach(k -> times.put(k, 0.0));
            if (times.size() == 0) StateData.getDefault(desert, desertTime / MAX_DESERT_TIME).update(player, moving, this, 0);
            if (value <= 3) player.addPotionEffect(PotionEffectType.CONFUSION.createEffect((int)(delta_time * 20) + 100, 0));
            if (value <= 0) player.addPotionEffect(PotionEffectType.SLOW.createEffect((int)(delta_time * 20) + 100, 1));
        }
    }
    private static ThirstData getThirst(Player player) {
        return ThirstData.parse(JManager.get(JsonObject.class, player.getPersistentDataContainer(), "thirst", null));
    }
    private static void setThirst(Player player, ThirstData value) {
        JManager.set(player.getPersistentDataContainer(), "thirst", value.toJson());
    }

    @Override public Collection<ImageBuilder> getUI(Player player) {
        switch (player.getGameMode()) {
            case CREATIVE:
            case SPECTATOR: return Collections.emptyList();
            default:
                break;
        }
        ThirstData data = getThirst(player);
        List<ImageBuilder> images = new ArrayList<>();
        int level = 0;
        if (player.getRemainingAir() < player.getMaximumAir()) level++;
        ThirstData.StateColor color = data.getStateColor();
        int value = (int)Math.round(data.value);

        boolean half = value % 2 == 1;
        if (half)
        {
            int i = (value + 1) / 2 - 1;
            images.add(color.get(ThirstData.StateColor.Type.Half, level).addOffset(86 - i * 8));
        }
        value = (value / 2) - 1;
        for (int i = 0; i <= value; i++) images.add(color.get(ThirstData.StateColor.Type.Whole, level).addOffset(86 - i * 8));
        for (int i = value + (half ? 2 : 1); i < 10; i++) images.add(color.get(ThirstData.StateColor.Type.Empty, level).addOffset(86 - i * 8));

        return images;
    }
    @Override public CustomUI.IType getType() {
        return CustomUI.IType.ACTIONBAR;
    }
    @EventHandler public static void onUse(PlayerItemConsumeEvent e) {
        ItemStack item = e.getItem();
        ThirstData.StateData state = ThirstData.StateData.includes.stream().filter(kv -> kv.val0.check(item)).map(kv -> kv.val1).findFirst().orElse(null);
        if (state == null) return;
        Player player = e.getPlayer();
        ThirstData data = getThirst(player);
        data.times.put(state.key, 0.0);
        setThirst(player, data);
    }
}















