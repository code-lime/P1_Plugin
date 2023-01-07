package org.lime.gp.player.ui;

import com.google.common.collect.Streams;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;
import org.lime.core;
import org.lime.gp.lime;
import org.lime.system;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.extension.JManager;
import org.lime.gp.player.module.Death;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Infection implements Listener, CustomUI.IUI {
    @Override public CustomUI.IType getType() { return CustomUI.IType.ACTIONBAR; }

    private static ImageBuilder[] whole;
    private static ImageBuilder[] half;
    private static ImageBuilder[] empty;
    private static double _default;

    public static final Infection Instance = new Infection();
    public static core.element create() {
        return core.element.create(Infection.class)
                .withInit(Infection::init)
                .withInstance(Instance)
                .<JsonObject>addConfig("config", v -> v
                        .withDefault(system.json.object()
                                .addObject("images", _v -> _v
                                        .add("whole", Arrays.asList("E517:7", "E514:7"))
                                        .add("half", Arrays.asList("E516:7", "E513:7"))
                                        .add("empty", Arrays.asList("E518:7", "E515:7"))
                                )
                                .add("default_sec", 20 * 60)
                                .build()
                        )
                        .withInvoke(Infection::config)
                        .withParent("infection")
                );
    }
    /*public static class TagData {
        public static final ConcurrentHashMap<String, TagData> tags = new ConcurrentHashMap<>();
        public final double per_sec;
        public TagData(JsonElement json) {
            if (json.isJsonPrimitive()) {
                per_sec = json.getAsDouble();
            } else {
                throw new IllegalArgumentException("Error parse tag data: "+json+"!");
            }
        }
        public static void update(Player player, InfectionData infection) {
            double delta = 0.1;
            for (String tag : player.getScoreboardTags()) {
                TagData data = tags.getOrDefault(tag, null);
                if (data == null) continue;
                delta += data.per_sec;
            }
            infection.update(player, delta);
        }
    }*/
    private static final ConcurrentHashMap<String, IData> datas = new ConcurrentHashMap<>();
    public interface IData {
        double tick(Player player);
    }
    public static void add(String key, IData data) {
        datas.put(key, data);
    }
    public static void init() {
        AnyEvent.addEvent("infection", AnyEvent.type.other, builder -> builder.createParam("set","add","del").createParam(Double::parseDouble, "[value]"), (player, method, value) -> {
            switch (method) {
                case "set": infection(player, infection(player).invoke(v -> v.value = value)); break;
                case "add": infection(player, infection(player).invoke(v -> v.value += value)); break;
                case "del": infection(player, infection(player).invoke(v -> v.value -= value)); break;
            }
        });
        add("default", player -> Infection._default);
        CustomUI.addListener(Instance);
        lime.repeat(Infection::update, 1);
    }
    public static void update() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            switch (player.getGameMode()) {
                case CREATIVE:
                case SPECTATOR: return;
            }
            InfectionData data = infection(player);

            double delta = 0;
            for (IData iData : datas.values()) delta += iData.tick(player);
            data.update(player, delta);

            infection(player, data);
        });
    }
    public static void config(JsonObject json) {
        JsonObject images = json.getAsJsonObject("images");

        double _default = json.get("default_sec").getAsDouble();
        ImageBuilder[] whole = Streams.stream(images.getAsJsonArray("whole"))
                .map(v -> v.getAsString().split(":"))
                .map(args -> ImageBuilder.of(Integer.parseInt(args[0], 16), Integer.parseInt(args[1])))
                .toArray(ImageBuilder[]::new);
        ImageBuilder[] half = Streams.stream(images.getAsJsonArray("half"))
                .map(v -> v.getAsString().split(":"))
                .map(args -> ImageBuilder.of(Integer.parseInt(args[0], 16), Integer.parseInt(args[1])))
                .toArray(ImageBuilder[]::new);
        ImageBuilder[] empty = Streams.stream(images.getAsJsonArray("empty"))
                .map(v -> v.getAsString().split(":"))
                .map(args -> ImageBuilder.of(Integer.parseInt(args[0], 16), Integer.parseInt(args[1])))
                .toArray(ImageBuilder[]::new);

        Infection._default = 20 / _default;
        Infection.whole = whole;
        Infection.half = half;
        Infection.empty = empty;
    }

    private static class InfectionData {
        private static final int THIRST_VERSION = 1;

        public static InfectionData empty() { return new InfectionData(); }
        private InfectionData() {}

        public double value = 20;

        public static InfectionData parse(JsonObject json) {
            InfectionData dat = empty();
            try {
                if (json == null) return dat;
                if (json.get("v").getAsInt() != THIRST_VERSION) return dat;
                dat.value = json.get("value").getAsDouble();
                return dat;
            } catch (Exception e) {
                lime.logStackTrace(e);
                return dat;
            }
        }
        public JsonObject toJson() {
            return system.json
                    .object()
                    .add("v", THIRST_VERSION)
                    .add("value", value)
                    .build();
        }
        public void update(Player player, double delta) {
            value = Math.min(20, Math.max(0, value + delta));
            double state = 20 - value;
            if (state >= 11) player.addPotionEffect(PotionEffectType.CONFUSION.createEffect(120, 0));
            if (state >= 13) player.addPotionEffect(PotionEffectType.SLOW.createEffect(120, 0));
            if (state >= 15 && !player.hasPotionEffect(PotionEffectType.POISON)) player.addPotionEffect(PotionEffectType.POISON.createEffect(120, 0));
            if (state >= 17) player.addPotionEffect(PotionEffectType.BLINDNESS.createEffect(120, 0));
            if (state >= 19) player.addPotionEffect(PotionEffectType.SLOW.createEffect(120, 2));
            if (state >= 20) {
                player.addPotionEffect(PotionEffectType.WITHER.createEffect(120, 10));
                if (Death.isDamageLay(player.getUniqueId())) Death.kill(player, Death.Reason.INFECTION);
            }
        }
        public InfectionData invoke(system.Action1<InfectionData> func) {
            func.invoke(this);
            return this;
        }
    }
    private static InfectionData infection(Player player) {
        return InfectionData.parse(JManager.get(JsonObject.class, player.getPersistentDataContainer(), "infection", null));
    }
    private static void infection(Player player, InfectionData value) {
        JManager.set(player.getPersistentDataContainer(), "infection", value.toJson());
    }
    public static void reset(Player player) {
        JManager.del(player.getPersistentDataContainer(), "infection");
    }
    public static void clear_kill(Player player) {
        infection(player, infection(player).invoke(v -> v.value = Math.max(10, v.value)));
    }

    @Override public Collection<ImageBuilder> getUI(Player player) {
        switch (player.getGameMode()) {
            case CREATIVE:
            case SPECTATOR: return Collections.emptyList();
        }
        InfectionData infection = infection(player);
        if (infection.value >= 20) return Collections.emptyList();
        List<ImageBuilder> images = new ArrayList<>();

        int level = 0;
        if (player.getRemainingAir() < player.getMaximumAir()) level++;
        int value = 20 - (int)Math.round(infection.value);

        boolean half = value % 2 == 1;
        if (half) {
            int i = (value + 1) / 2 - 1;
            images.add(Infection.half[level].addOffset(85 - i * 8));
        }
        value = (value / 2) - 1;
        for (int i = 0; i <= value; i++) images.add(Infection.whole[level].addOffset(85 - i * 8));
        for (int i = value + (half ? 2 : 1); i < 10; i++) images.add(Infection.empty[level].addOffset(85 - i * 8));

        return images;
    }
}
























