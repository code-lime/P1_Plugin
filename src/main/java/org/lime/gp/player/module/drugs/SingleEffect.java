package org.lime.gp.player.module.drugs;

import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.list.DrugsSetting;
import org.lime.system;

public class SingleEffect {
    public int id;
    public double effect_timer = 0;

    public SingleEffect(int id, JsonObject json) {
        this(id);
        this.effect_timer = json.get("effect_timer").getAsDouble();
    }

    public JsonObject save() {
        return system.json.object()
                .add("effect_timer", effect_timer)
                .build();
    }

    public SingleEffect(int id) {
        this.id = id;
    }

    public void applyEffect(Player player, int minecraftTick) {
        if (Items.creators.get(id) instanceof ItemCreator creator) {
            creator.getOptional(DrugsSetting.class).ifPresent(drugs -> {
                (effect_timer > drugs.first ? drugs.first_effects : drugs.last_effects).forEach(effect -> {
                    effect.tick(player, minecraftTick);
                });
            });
        }
    }

    public boolean tickRemove(Player player, double delta) {
        effect_timer -= delta;
        return effect_timer <= 0;
    }
}
