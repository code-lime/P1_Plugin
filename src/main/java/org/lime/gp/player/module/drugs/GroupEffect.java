package org.lime.gp.player.module.drugs;

import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.lime.gp.player.module.needs.thirst.Thirst;
import org.lime.system;

import java.util.HashMap;

public class GroupEffect {

    public double addiction_timer = 0;
    public boolean addiction_state = false;
    public double health_timer = 0;
    public final HashMap<Integer, SingleEffect> effects = new HashMap<>();

    private static final int DATA_VERSION = 1;

    public GroupEffect() {

    }

    public GroupEffect(JsonObject json) {
        int version = json.has("v") ? json.get("v").getAsInt() : -1;
        if (version != DATA_VERSION) return;
        json.entrySet().forEach(kv -> {
            if (kv.getKey().equals("v")) return;
            int id = Integer.parseInt(kv.getKey());
            effects.put(id, new SingleEffect(id, kv.getValue().getAsJsonObject()));
        });
    }

    public JsonObject save() {
        return system.json.object()
                .add("v", DATA_VERSION)
                .add(effects, String::valueOf, SingleEffect::save)
                .build();
    }

    public void addEffect(int id, double addiction, double first, double last) {
        SingleEffect effect = effects.computeIfAbsent(id, SingleEffect::new);
        effect.effect_timer = first + last;
        addiction_timer += addiction + first + last;
        if (addiction_timer > 120) addiction_timer = 120;
        setHealthTimer(0);
    }

    public void setHealthTimer(double time) {
        health_timer = time;
    }

    public void applyEffect(Player player, int minecraftTick) {
        if (health_timer <= 0) effects.values().forEach(type -> type.applyEffect(player, minecraftTick));
        if (health_timer <= 0 && addiction_timer > 90 || (addiction_timer > 15 && effects.isEmpty())) {
            if (minecraftTick % 1000 == 0)
                player.addPotionEffect(PotionEffectType.WITHER.createEffect(5 * 20, 0).withIcon(false).withParticles(false));
            if (minecraftTick % 20 == 0)
                player.addPotionEffect(PotionEffectType.DARKNESS.createEffect(2 * 20, 0).withIcon(false).withParticles(false));
        } else if (health_timer > 0) {
            if (minecraftTick % 20 == 0)
                Thirst.thirstStateByKey(player, "drugs");
        }
    }

    public boolean tickRemove(Player player, double delta) {
        double modifyDelta = delta * (health_timer > 0 ? 4 : 1);
        effects.values().removeIf(type -> type.tickRemove(player, modifyDelta));
        if (addiction_timer > 0) addiction_timer -= modifyDelta;

        if (health_timer > 0) health_timer -= delta;
        else if (health_timer < 0) health_timer = 0;

        return addiction_timer <= 0 && effects.isEmpty();
    }

    /*

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
    */
    public void reset() {
        addiction_timer = 0;
        addiction_state = false;
        health_timer = 0;
        effects.clear();
    }
}
