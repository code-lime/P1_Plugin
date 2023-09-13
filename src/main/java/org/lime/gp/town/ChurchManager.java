package org.lime.gp.town;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.database.Methods;
import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.lime;
import org.lime.gp.module.damage.EntityDamageByPlayerEvent;
import org.lime.system;
import org.lime.gp.player.ui.CustomUI;
import org.lime.gp.player.ui.ImageBuilder;

import java.sql.ResultSet;
import java.util.*;

public class ChurchManager implements Listener {
    private static double func_pow;
    private static int func_div;

    private static double global_distance;

    public static double applyAdder(int count) {
        return Math.pow(count, func_pow) / func_div;
    }
    public static CoreElement create() {
        return CoreElement.create(ChurchManager.class)
                .withInit(ChurchManager::init)
                .withInstance()
                .<JsonObject>addConfig("config", v -> v
                        .withParent("church")
                        .withInvoke(json -> {
                            JsonObject func = json.getAsJsonObject("func");
                            func_pow = func.get("pow").getAsDouble();
                            func_div = func.get("div").getAsInt();
                            global_distance = json.get("global_distance").getAsDouble();
                        })
                        .withDefault(system.json.object()
                                .addObject("func", _v -> _v
                                        .add("pow", 1.75)
                                        .add("div", 5)
                                )
                                .add("global_distance", 300.0)
                                .build()
                        )
                );
    }

    public static void init() {
        lime.repeat(ChurchManager::update, 0.2);
        CustomUI.addListener(new EffectUI());
    }
    public static void update() {
        Methods.SQL.Async.rawSqlQuery("SELECT * FROM church", IEffect::parse, list -> list.forEach(IEffect::sync));
        effect_list.entrySet().removeIf(kv -> kv.getValue().isRemove());
        effects.forEach((uuid, data) -> data.entrySet().removeIf(kv -> !effect_list.containsKey(kv.getKey())));
    }

    public static class EffectUI implements CustomUI.IUI {
        private static void appendNumber(List<ImageBuilder> builders, Integer num, int offset) {
            builders.add((num == null ? ImageBuilder.of(0xE61B, 3) : ImageBuilder.of(0xE610 + num, num == 1 ? 1 : 3)).withOffset(offset));
        }
        private static void appendTime(List<ImageBuilder> builders, Integer total_sec, int offset) {
            if (total_sec == null || total_sec >= 3600) total_sec = null;
            else if (total_sec < 0) total_sec = 0;
            Integer min;
            Integer sec;
            if (total_sec == null) {
                min = null;
                sec = null;
            } else {
                sec = (total_sec % 60);
                total_sec /= 60;
                min = total_sec;
            }

            appendNumber(builders, min == null ? null : (min / 10), offset);
            offset += 4;
            appendNumber(builders, min == null ? null : (min % 10), offset);
            offset += 3;

            builders.add(ImageBuilder.of(0xE61A, 1).withOffset(offset));
            offset += 3;

            appendNumber(builders, sec == null ? null : (sec / 10), offset);
            offset += 4;
            appendNumber(builders, sec == null ? null : (sec % 10), offset);
        }
        private static void appendSingle(List<ImageBuilder> builders, Integer total_sec, List<ImageBuilder> icons, int offset) {
            builders.add(ImageBuilder.of(0xEfe5, 29).withOffset(offset));
            icons.forEach(icon -> builders.add(icon.withOffset(offset - 9)));
            appendTime(builders, total_sec, offset - 3);
        }
        @Override public Collection<ImageBuilder> getUI(Player player) {
            HashMap<Integer, IEffect> effects = ChurchManager.effects.getOrDefault(player.getUniqueId(), null);
            if (effects == null) return Collections.emptyList();
            List<ImageBuilder> builders = new LinkedList<>();
            int i = -1;
            for (IEffect effect : effects.values()) {
                i++;
                appendSingle(builders, effect.getTime(), effect.icon(), 120 + i * 28);
            }
            return builders;
        }
        @Override public CustomUI.IType getType() { return CustomUI.IType.BOSSBAR; }
    }

    private static final HashMap<Integer, IEffect> effect_list = new HashMap<>();
    private static final HashMap<UUID, HashMap<Integer, IEffect>> effects = new HashMap<>();
    public static boolean hasLocalEffect(Player player, EffectType type) {
        HashMap<Integer, IEffect> map = effects.getOrDefault(player.getUniqueId(), null);
        if (map == null) return false;
        for (IEffect effect : map.values()) {
            if (effect.type == type && effect.isLocal()) return true;
        }
        return false;
    }
    public static boolean hasGlobalEffect(Player player, EffectType type) {
        HashMap<Integer, IEffect> map = effects.getOrDefault(player.getUniqueId(), null);
        if (map == null) return false;
        for (IEffect effect : map.values()) {
            if (effect.type == type && !effect.isLocal()) return true;
        }
        return false;
    }
    public static boolean hasGlobalEffect(EffectType type, Location location) {
        for (IEffect effect : effect_list.values()) {
            if (effect.type != type || effect.isLocal()) continue;
            boolean inHouse = Tables.HOUSE_TABLE.get(String.valueOf(effect.getHouseID()))
                    .map(house -> house.center().distance(location) < global_distance)
                    .orElse(false);
            if (inHouse) return true;
        }
        return false;
    }
    public static boolean hasAnyEffect(Player player, EffectType type) {
        HashMap<Integer, IEffect> map = effects.getOrDefault(player.getUniqueId(), null);
        if (map == null) return false;
        for (IEffect effect : map.values()) {
            if (effect.type == type) return true;
        }
        return false;
    }

    public enum EffectType {
        //L - Убирает агр мобов пока моба не ударишь
        //G - Убирает мобов в радиусе 300 блоков от церкви
        MOBS(TextColor.color(0xAA0000)),
        //L - Снижает скорость уменьшения голода, насыщения и жажды в 2 раза
        //G - Снижает скорость уменьшения голода, насыщения и жажды в 2 раза в радиусе 300 блоков от церкви
        SATURATION(TextColor.color(0x00AA00)),
        //L - Дает эффект повышеного урона на 15%
        //G - Уменьшает входящий урон на 15%
        DAMAGE(TextColor.color(0x0000AA));

        public final TextColor color;
        EffectType(TextColor color) { this.color = color; }
    }

    private static abstract class IEffect {
        public static IEffect parse(ResultSet set) {
            int id = MySql.readObject(set, "id", Integer.class);
            int house_id = MySql.readObject(set, "house_id", Integer.class);
            String target = MySql.readObject(set, "target", String.class);
            EffectType type = EffectType.valueOf(MySql.readObject(set, "type", String.class));
            int time = MySql.readObject(set, "time", Integer.class);
            IEffect effect = target == null ? new GlobalEffect(house_id, type) : new PlayerEffect(UUID.fromString(target), house_id, type);
            effect.id = id;
            effect.time = time;
            return effect;
        }

        private int ticks = 3;
        private boolean isRemove() {
            ticks--;
            return ticks <= 0;
        }

        private int id;
        private int time;

        private final EffectType type;
        private final int house_id;

        protected IEffect(int house_id, EffectType type) {
            this.type = type;
            this.house_id = house_id;
        }

        public int getTime() { return time; }
        public int getID() { return id; }
        public int getHouseID() { return house_id; }
        public EffectType getType() { return type; }
        public abstract boolean isLocal();
        public abstract void sync();
        public abstract List<ImageBuilder> icon();
    }
    private static class GlobalEffect extends IEffect {
        protected GlobalEffect(int house_id, EffectType type) { super(house_id, type); }

        @Override public void sync() {
            effect_list.put(getID(), this);
            Tables.HOUSE_TABLE.get(String.valueOf(getHouseID())).ifPresent(house -> house.center().getNearbyPlayers(global_distance).forEach(player -> effects.compute(player.getUniqueId(), (k, v) -> {
                if (v == null) v = new HashMap<>();
                v.put(getID(), this);
                return v;
            })));
        }
        @Override public List<ImageBuilder> icon() {
            return Arrays.asList(
                    ImageBuilder.of(0xEfe6, 7).withColor(getType().color),
                    ImageBuilder.of(0xEfe7, 5).withColor(NamedTextColor.WHITE)
            );
        }
        /*
        public boolean inZone(Location pos) {
            return Tables.HOUSE_TABLE.get(String.valueOf(getHouseID())).map(v -> v.inZone(pos)).orElse(false);
        }
        */
        @Override public boolean isLocal() { return false; }
    }
    private static class PlayerEffect extends IEffect {
        private final UUID uuid;
        protected PlayerEffect(UUID uuid, int house_id, EffectType type) {
            super(house_id, type);
            this.uuid = uuid;
        }

        @Override public void sync() {
            effect_list.put(getID(), this);
            effects.compute(uuid, (k, v) -> {
                if (v == null) v = new HashMap<>();
                v.put(getID(), this);
                return v;
            });
        }
        @Override public List<ImageBuilder> icon() {
            return Arrays.asList(
                    ImageBuilder.of(0xEfe6, 7).withColor(getType().color),
                    ImageBuilder.of(0xEfe8, 5).withColor(NamedTextColor.WHITE)
            );
        }
        @Override public boolean isLocal() { return true; }
    }

    private static String getTargetKey(UUID uuid) {
        return "target:" + uuid;
    }
    @EventHandler public static void on(EntityTargetLivingEntityEvent e) {
        if (!(e.getEntity() instanceof Monster monster)) return;
        if (!(e.getTarget() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (monster.getScoreboardTags().contains(getTargetKey(uuid))) return;
        if (hasLocalEffect(player, EffectType.MOBS))
            e.setCancelled(true);
    }
    @EventHandler public static void on(EntityDamageByPlayerEvent e) {
        Player player = e.getDamageOwner();
        if (hasLocalEffect(player, EffectType.DAMAGE)) e.setDamage(e.getDamage() * 1.15);
        Entity entity = e.getEntity();
        if (entity instanceof Player target && hasGlobalEffect(target, EffectType.DAMAGE)) e.setDamage(e.getDamage() * 0.85);
        else if (entity instanceof Monster monster) {
            monster.getScoreboardTags().add(getTargetKey(player.getUniqueId()));
            if (monster.getTarget() != null) return;
            monster.setTarget(player);
        }
    }
    @EventHandler public static void on(CreatureSpawnEvent e) {
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        if (!(e.getEntity() instanceof Monster)) return;
        Location location = e.getLocation();
        if (location.getWorld() != lime.MainWorld) return;
        if (!hasGlobalEffect(EffectType.MOBS, location)) return;
        e.setCancelled(true);
    }
}



























