package org.lime.gp.player.module;

import com.comphenix.protocol.events.PacketContainer;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.lime.core;
import org.lime.display.EditedDataWatcher;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.item.Items;
import org.lime.gp.item.Settings;
import org.lime.gp.lime;
import org.lime.gp.module.EntityPosition;
import org.lime.gp.player.inventory.MainPlayerInventory;
import org.lime.gp.player.ui.Thirst;
import org.lime.system;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Drugs implements Listener {
    public enum EffectType {
        NONE((player, tick) -> {}),

        SATURATION_FULL(ofPotion(PotionEffectType.SATURATION.createEffect(80, 1).withIcon(false), 40)),
        SPEED_EFFECT(ofPotion(PotionEffectType.SPEED.createEffect(80, 1), 30)),
        REGENERATION_EFFECT(ofPotion(PotionEffectType.REGENERATION.createEffect(80, 0), 40)),
        STRENGTH_EFFECT(ofPotion(PotionEffectType.INCREASE_DAMAGE.createEffect(80, 0), 40)),
        NIGHT_VISION(ofPotion(PotionEffectType.NIGHT_VISION.createEffect(250, 0), 220)),
        BLINDNESS(ofPotion(PotionEffectType.BLINDNESS.createEffect(80, 0), 40)),
        JUMP(ofPotion(PotionEffectType.JUMP.createEffect(80, 0), 40)),

        THIRST_FULL((player, tick) -> {
            if (tick % 40 == 0) Thirst.thirstReset(player);
        }),
        SPEED_RANDOMIZE_EFFECT((player, tick) -> {
            switch (tick % 100) {
                case 0 -> PotionEffectType.SLOW.createEffect(80, 2).apply(player);
                case 70 -> PotionEffectType.SPEED.createEffect(80, 0).apply(player);
            }
        }),
        SATURATION_ZERO((player, tick) -> {
            if (tick % 20 != 0) return;
            ProxyFoodMetaData.ofPlayer(player).ifPresent(food -> food.setSaturation(0));
        }),
        HOTBAR_SORTER((player, tick) -> {
            if (tick % 300 != 0) return;
            PlayerInventory inventory = player.getInventory();
            List<ItemStack> items = new ArrayList<>();
            items.add(inventory.getItemInOffHand());
            for (int i = 0; i < 9; i++) {
                ItemStack item = inventory.getItem(i);
                if (MainPlayerInventory.checkBarrier(item)) break;
                items.add(item);
            }
            Collections.shuffle(items);
            inventory.setItemInOffHand(items.remove(0));
            int length = items.size();
            for (int i = 0; i < length; i++) inventory.setItem(i, items.get(i));
        }),
        ARMS_LOCK((player, tick) -> {
            PlayerInventory inventory = player.getInventory();
            regive(player, inventory, EquipmentSlot.OFF_HAND);
            regive(player, inventory, EquipmentSlot.HAND);
        }),
        TREMBLING((player, tick) -> {
            freezeList.compute(player.getEntityId(), (_id, _value) -> {
                if (_value == null) player.setFreezeTicks(player.getFreezeTicks() + 1);
                return 10;
            });
        });

        private final system.Action2<Player, Integer> tick;
        EffectType(system.Action2<Player, Integer> tick) { this.tick = tick; }
        public void tick(Player player, int time) { tick.invoke(player, time); }

        //private static system.Action2<Player, Integer> ofPotion(PotionEffect potionEffect) { return ofPotion(potionEffect, 0); }
        private static system.Action2<Player, Integer> ofPotion(PotionEffect potionEffect, int skip) {
            PotionEffectType effectType = potionEffect.getType();
            return (player, time) -> {
                PotionEffect current = player.getPotionEffect(effectType);
                if (current == null || current.getAmplifier() < potionEffect.getAmplifier() || current.getDuration() < skip)
                    player.addPotionEffect(potionEffect);
            };
        }
    }
    public static class GroupEffect {
        public final List<system.Toast2<ImmutableSet<EffectType>, Integer>> effects = new ArrayList<>();

        public GroupEffect() {

        }
        public GroupEffect(JsonObject json) {
            json.entrySet().forEach(kv -> {
                effects.add(system.toast(Arrays.stream(kv.getKey().split(" ")).map(EffectType::valueOf).collect(ImmutableSet.toImmutableSet()), kv.getValue().getAsInt()));
            });
        }
        public JsonObject save() {
            return system.json.object().add(effects, kv -> kv.val0.stream().map(Enum::name).collect(Collectors.joining(" ")), kv -> kv.val1).build();
        }

        public boolean tick(Player player, int tick) {
            if (effects.isEmpty()) return false;
            system.Toast2<ImmutableSet<EffectType>, Integer> effect = effects.get(0);
            effect.val1--;
            effect.val0.forEach(type -> type.tick(player, tick));
            if (effect.val1 <= 0) effects.remove(0);
            return true;
        }

        public void setup(List<system.Toast2<ImmutableSet<EffectType>, system.IRange>> effects) {
            this.effects.clear();
            effects.forEach(kv -> this.effects.add(system.toast(kv.val0, (int)kv.val1.getValue(0))));
        }
        public void modify(ImmutableSet<EffectType> types, int ticks, boolean set) {
            if (set) this.effects.clear();
            this.effects.add(system.toast(types, ticks));
        }
    }
    public static final HashMap<UUID, GroupEffect> players = new HashMap<>();

    public static core.element create() {
        return core.element.create(Drugs.class)
                .withInstance()
                .withInit(Drugs::init)
                .withUninit(Drugs::uninit);
    }
    private static void regive(Player player, PlayerInventory inventory, EquipmentSlot slot) {
        Optional.of(inventory.getItem(slot))
                .filter(v -> !v.getType().isAir())
                .ifPresent(item -> {
                    inventory.setItem(slot, new ItemStack(Material.STONE, 1));
                    Items.dropGiveItem(player, item, false);
                    inventory.setItem(slot, new ItemStack(Material.AIR));
                });
    }
    private static final ConcurrentHashMap<Integer, Integer> freezeList = new ConcurrentHashMap<>();
    private static final NamespacedKey DRUGS_EFFECTS = new NamespacedKey(lime._plugin, "drugs_effects");
    public static void init() {
        PacketManager.adapter()
                .add(PacketPlayOutEntityMetadata.class, (packet, event) -> {
                    int id = event.getPlayer().getEntityId();
                    if (packet.getId() == id) return;
                    if (freezeList.containsKey(packet.getId())) {
                        List<DataWatcher.Item<?>> data = new ArrayList<>(packet.getUnpackedData());
                        data.removeIf(item -> item.getAccessor().getId() == EditedDataWatcher.DATA_TICKS_FROZEN.getId());
                        data.add(new DataWatcher.Item<>(EditedDataWatcher.DATA_TICKS_FROZEN, 10000));
                        packet = new PacketPlayOutEntityMetadata(packet.getId(), new DataWatcher(null) { @Override public List<Item<?>> getAll() { return data; } @Override public void clearDirty() { } }, true);
                        event.setPacket(new PacketContainer(event.getPacketType(), packet));
                    }
                })
                .listen();
        AnyEvent.addEvent("drugs.effect.modify", AnyEvent.type.other, b -> b.createParam("add","set").createParam(EffectType.values()).createParam(Integer::parseUnsignedInt, "[ticks]"), (p, action, effect, ticks) -> {
            getGroupEffect(p.getUniqueId()).modify(ImmutableSet.of(effect), ticks, switch (action) {
                case "add" -> false;
                case "set" -> true;
                default -> throw new IllegalArgumentException("Action '"+action+"' not supported!");
            });
        });
        AnyEvent.addEvent("drugs.effect.reset", AnyEvent.type.other, p -> getGroupEffect(p.getUniqueId()).setup(Collections.emptyList()));
        lime.repeatTicks(() -> {
            int tick = MinecraftServer.currentTick;
            freezeList.entrySet().forEach(kv -> kv.setValue(kv.getValue() - 1));
            freezeList.entrySet().removeIf(kv -> {
                if (kv.getValue() > 0) return false;
                Bukkit.getOnlinePlayers()
                        .stream()
                        .filter(v -> v.getEntityId() == kv.getKey())
                        .findAny()
                        .ifPresent(player -> player.setFreezeTicks(player.getFreezeTicks() + 1));
                return true;
            });
            ConcurrentHashMap<UUID, Player> onlinePlayers = EntityPosition.onlinePlayers;
            players.entrySet().removeIf(kv -> {
                UUID uuid = kv.getKey();
                Player player = onlinePlayers.get(uuid);
                if (player == null) {
                    PlayerData.getPlayerData(uuid).setJson(DRUGS_EFFECTS, kv.getValue().save());
                    return true;
                }
                if (kv.getValue().tick(player, tick)) PlayerData.getPlayerData(uuid).setJson(DRUGS_EFFECTS, kv.getValue().save());
                return false;
            });
            onlinePlayers.keySet().forEach(Drugs::getGroupEffect);
        }, 1);
    }
    public static void uninit() {
        players.entrySet().removeIf(kv -> {
            PlayerData.getPlayerData(kv.getKey()).setJson(DRUGS_EFFECTS, kv.getValue().save());
            return true;
        });
    }
    public static GroupEffect getGroupEffect(UUID uuid) {
        return players.computeIfAbsent(uuid, _uuid -> Optional.ofNullable(PlayerData.getPlayerData(_uuid).getJson(DRUGS_EFFECTS))
                .map(JsonElement::getAsJsonObject)
                .map(GroupEffect::new)
                .orElseGet(GroupEffect::new));
    }

    @EventHandler(ignoreCancelled = true) public static void on(PlayerItemConsumeEvent e) {
        Items.getOptional(Settings.DrugsSetting.class, e.getItem())
                .ifPresent(drugs -> getGroupEffect(e.getPlayer().getUniqueId())
                        .setup(drugs.effects));
    }
}














