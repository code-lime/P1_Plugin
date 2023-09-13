package org.lime.gp.module.biome.holiday;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftEgg;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftRabbit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;
import org.lime.plugin.CoreElement;
import org.lime.system;
import org.lime.gp.lime;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.Checker;
import org.lime.gp.module.damage.EntityDamageByPlayerEvent;
import com.google.gson.JsonObject;

import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;

public class Easter implements Listener {
    public static CoreElement create() {
        return CoreElement.create(Easter.class)
                .withInit(Easter::init)
                .withInstance()
                .<JsonObject>addConfig("config", v -> v
                        .withParent("easter")
                        .withDefault(system.json.object()
                                .add("enable", false)
                                /*.addObject("biome", _v -> _v
                                        .add("sky", "200000")
                                        .add("water", "600000")
                                        .add("water_fog", "600000")
                                        .add("fog", "600000")
                                        .add("grass", "605000")
                                        .add("foliage", "600000")
                                )*/
                                .addObject("rabbit", _v -> _v
                                        .add("life_ticks", 2400)
                                        .add("limit_to_player", 1)
                                        .add("radius", 30)
                                        .addObject("loot", __v -> __v
                                                .add("Eat.pasha_egg.(.*)*1", 10)
                                        )
                                )
                                .build())
                        .withInvoke(Easter::config)
                );
    }
    
    private static boolean ENABLE = false;
    private static int TICKS_LIFE = 2400;
    private static int LIMIT_TO_PLAYER = 2;
    private static int RADIUS = 30;
    private static final List<system.Toast2<Checker, system.IRange>> LOOT = new ArrayList<>();

    /*private static system.Func1<NBTTagCompound, NBTTagCompound> appendEffects = v -> v;

    private static BiomeModify.ModifyActionCloseable closeable = null;*/
    public static void config(JsonObject json) {
        ENABLE = json.get("enable").getAsBoolean();
        JsonObject rabbit = json.getAsJsonObject("rabbit");
        TICKS_LIFE = rabbit.get("life_ticks").getAsInt();
        LIMIT_TO_PLAYER = rabbit.get("limit_to_player").getAsInt();
        RADIUS = rabbit.get("radius").getAsInt();
        LOOT.clear();
        rabbit.getAsJsonObject("loot").entrySet().forEach(kv -> {
            String[] key = kv.getKey().split("\\*");

            String type = Arrays.stream(key).limit(key.length - 1).collect(Collectors.joining("*"));

            int count = kv.getValue().getAsInt();
            system.Toast2<Checker, system.IRange> element = system.toast(Checker.createCheck(type), key.length > 1 ? system.IRange.parse(key[key.length - 1]) : new system.OnceRange(1));
            for (int i = 0; i < count; i++) LOOT.add(element);
        });

        /*JsonObject biome = json.getAsJsonObject("biome");
        int sky = ChatHexColor.parseColor("#"+biome.get("sky").getAsString()).getValue();
        int water = ChatHexColor.parseColor("#"+biome.get("water").getAsString()).getValue();
        int water_fog = ChatHexColor.parseColor("#"+biome.get("water_fog").getAsString()).getValue();
        int fog = ChatHexColor.parseColor("#"+biome.get("fog").getAsString()).getValue();
        int grass = ChatHexColor.parseColor("#"+biome.get("grass").getAsString()).getValue();
        int foliage = ChatHexColor.parseColor("#"+biome.get("foliage").getAsString()).getValue();

        appendEffects = compound -> {
            compound.putInt("fog_color", fog);
            compound.putInt("water_color", water);
            compound.putInt("water_fog_color", water_fog);
            compound.putInt("sky_color", sky);

            compound.putInt("foliage_color", foliage);
            compound.putInt("grass_color", grass);
            return compound;
        };
        if ((closeable != null) == ENABLE) return;
        if (ENABLE) {
            BiomeModify.ModifyActionCloseable _closeable = BiomeModify.appendModify(Easter::modify);
            if (closeable != null) closeable.close();
            closeable = _closeable;
        } else {
            closeable.close();
            closeable = null;
        }*/
    }
    /*public static void modify(int id, String name, NBTTagCompound element) {
        NBTTagCompound effects = appendEffects.invoke(element.contains("effects") ? element.getCompound("effects") : new NBTTagCompound());
        element.put("effects", effects);
    }*/
    public static void init() {
        lime.repeat(Easter::update, 1);
    }
    public static void update() {
        List<Player> players = lime.MainWorld.getPlayers();
        system.Toast1<Integer> add_count = system.toast(players.size() * LIMIT_TO_PLAYER);
        Bukkit.getWorlds().forEach(world -> world.getEntitiesByClass(Rabbit.class).forEach(rabbit -> {
            Set<String> tags = rabbit.getScoreboardTags();
            if (!tags.contains("easter")) return;
            int ticks = rabbit.getTicksLived();
            if (ticks > TICKS_LIFE) rabbit.remove();
            add_count.val0--;
        }));
        if (ENABLE) {
            for (int i = 0; i < add_count.val0; i++) {
                Player player = system.rand(players);
                Location location = player.getLocation().add(Vector.getRandom().add(new Vector(-0.5, -0.5, -0.5)).multiply(RADIUS * 2));
                Block top = location.getWorld().getHighestBlockAt(location);
                Block bottom;
                if (top.isEmpty()) {
                    bottom = top.getRelative(BlockFace.DOWN);
                    //lime.logOP("Check if top - empty: " + top.getType() + " with bottom: " + bottom.getType());
                } else {
                    if (!(top = top.getRelative(BlockFace.UP)).isEmpty()) continue;
                    bottom = top.getRelative(BlockFace.DOWN);
                    //lime.logOP("Check if top: " + top.getType() + " with bottom: " + bottom.getType());
                }
                switch (bottom.getType()) {
                    case GRASS_BLOCK: 
                    case DIRT: 
                    case DIRT_PATH: 
                    case FARMLAND:
                        //lime.logOP("Spawn in " + system.getString(top.getLocation().toVector()));
                        break;
                    default:
                        //lime.logOP("Error spawn in " + system.getString(top.getLocation().toVector()) + ". Bottom: " + bottom.getType());
                        continue;
                }
                spawnRabbit(top.getLocation().add(0.5, 0.5, 0.5));
            }
        }
    }
    private static void spawnRabbit(Location location) {
        location.getWorld().spawn(location, CraftRabbit.class, rabbit -> {
            rabbit.setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
            rabbit.customName(Component.text("Пасхальный кролик"));
            rabbit.getHandle().setItemSlot(EnumItemSlot.MAINHAND, ItemStack.EMPTY);
            rabbit.getScoreboardTags().add("easter");
        });
    }
    @EventHandler public static void on(EntityDamageByPlayerEvent event) {
        if (event.getEntity() instanceof CraftRabbit rabbit && rabbit.getScoreboardTags().contains("easter")) {
            event.setCancelled(true);
            rabbit.getHandle().kill();
            system.rand(LOOT).invoke((item_key, count) -> Items.createItem(system.rand(item_key.getWhitelistKeys().toList()), v -> v.setCount(count.getIntValue(64)))
                    .ifPresent(item -> Items.dropGiveItem(event.getDamageOwner(), item, true))
            );
        }
    }
    @EventHandler public static void on(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof CraftRabbit rabbit && rabbit.getScoreboardTags().contains("easter")) {
            event.setCancelled(true);
        }
    }
    @EventHandler public static void on(ProjectileHitEvent e) {
        if (e.getEntity() instanceof CraftEgg egg && LOOT.stream().anyMatch(v -> v.val0.check(egg.getItem()))) {
            if (system.rand_is(0.01))
            spawnRabbit(e.getEntity().getLocation());
        }
    }
    /*@EventHandler public static void on(EntityTargetLivingEntityEvent e) {
        if (e.getEntity() instanceof rabbit rabbit && rabbit.getScoreboardTags().contains("easter"))
            e.setCancelled(true);
    }*/
}
