package org.lime.gp.module.biome.holiday;

import com.google.gson.JsonObject;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatHexColor;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftVex;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.util.Vector;
import org.lime.core;
import org.lime.gp.item.Items;
import org.lime.gp.lime;
import org.lime.gp.module.biome.BiomeModify;
import org.lime.gp.module.damage.EntityDamageByPlayerEvent;
import org.lime.system;

import java.util.*;

public class Halloween implements Listener {
    public static core.element create() {
        return core.element.create(Halloween.class)
                .withInit(Halloween::init)
                .withInstance()
                .<JsonObject>addConfig("config", v -> v
                        .withParent("halloween")
                        .withDefault(system.json.object()
                                .add("enable", false)
                                .addObject("biome", _v -> _v
                                        .add("sky", "200000")
                                        .add("water", "600000")
                                        .add("water_fog", "600000")
                                        .add("fog", "600000")
                                        .add("grass", "605000")
                                        .add("foliage", "600000")
                                )
                                .addObject("vex", _v -> _v
                                        .add("life_ticks", 2400)
                                        .add("limit_to_player", 1)
                                        .add("radius", 30)
                                        .addObject("loot", __v -> __v
                                                .add("Eat.Candy*1:3", 10)
                                                .add("Eat.Candy_2*1:3", 10)
                                                .add("Eat.Candy_3*1:3", 10)
                                        )
                                )
                                .build())
                        .withInvoke(Halloween::config)
                );
    }

    private static boolean ENABLE = false;
    private static int TICKS_LIFE = 2400;
    private static int LIMIT_TO_PLAYER = 2;
    private static int RADIUS = 30;
    private static final List<system.Toast2<String, system.IRange>> LOOT = new ArrayList<>();

    private static system.Func1<NBTTagCompound, NBTTagCompound> appendEffects = v -> v;

    private static BiomeModify.ActionCloseable closeable = null;
    public static void config(JsonObject json) {
        ENABLE = json.get("enable").getAsBoolean();
        JsonObject vex = json.getAsJsonObject("vex");
        TICKS_LIFE = vex.get("life_ticks").getAsInt();
        LIMIT_TO_PLAYER = vex.get("limit_to_player").getAsInt();
        RADIUS = vex.get("radius").getAsInt();
        LOOT.clear();
        vex.getAsJsonObject("loot").entrySet().forEach(kv -> {
            String[] key = kv.getKey().split("\\*", 2);
            int count = kv.getValue().getAsInt();
            system.Toast2<String, system.IRange> element = system.toast(key[0], key.length > 1 ? system.IRange.parse(key[1]) : new system.OnceRange(1));
            for (int i = 0; i < count; i++) LOOT.add(element);
        });

        JsonObject biome = json.getAsJsonObject("biome");
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
            BiomeModify.ActionCloseable _closeable = BiomeModify.appendModify(Halloween::modify);
            if (closeable != null) closeable.close();
            closeable = _closeable;
        } else {
            closeable.close();
            closeable = null;
        }
    }
    public static void modify(int id, String name, NBTTagCompound element) {
        NBTTagCompound effects = appendEffects.invoke(element.contains("effects") ? element.getCompound("effects") : new NBTTagCompound());
        element.put("effects", effects);
    }
    public static void init() {
        lime.repeat(Halloween::update, 1);
    }
    public static void update() {
        List<Player> players = lime.MainWorld.getPlayers();
        system.Toast1<Integer> add_count = system.toast(players.size() * LIMIT_TO_PLAYER);
        Bukkit.getWorlds().forEach(world -> world.getEntitiesByClass(Vex.class).forEach(vex -> {
            Set<String> tags = vex.getScoreboardTags();
            if (!tags.contains("halloween")) return;
            int ticks = vex.getTicksLived();
            if (ticks > TICKS_LIFE) vex.remove();
            add_count.val0--;
        }));
        if (ENABLE) {
            for (int i = 0; i < add_count.val0; i++) {
                Player player = system.rand(players);
                Location location = player.getLocation().add(Vector.getRandom().multiply(RADIUS));
                location.getWorld().spawn(location, CraftVex.class, vex -> {
                    vex.getHandle().setItemSlot(EnumItemSlot.MAINHAND, ItemStack.EMPTY);
                    vex.getScoreboardTags().add("halloween");
                });
            }
        }
    }
    @EventHandler public static void on(EntityDamageByPlayerEvent event) {
        if (event.getEntity() instanceof CraftVex vex && vex.getScoreboardTags().contains("halloween")) {
            event.setCancelled(true);
            vex.getHandle().kill();
            system.rand(LOOT).invoke((item_key, count) -> Items.createItem(item_key, v -> v.setCount((int)count.getValue(64)))
                    .ifPresent(item -> Items.dropGiveItem(event.getDamageOwner(), item, true))
            );
        }
    }
    @EventHandler public static void on(EntityTargetLivingEntityEvent e) {
        if (e.getEntity() instanceof Vex vex && vex.getScoreboardTags().contains("halloween"))
            e.setCancelled(true);
    }
}
































