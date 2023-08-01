package org.lime.gp.item.settings.list;

import java.util.HashSet;

import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.gson.JsonObject;
import org.lime.gp.player.module.drugs.EffectType;

@Setting(name = "drugs") public class DrugsSetting extends ItemSetting<JsonObject> {
    public final HashSet<EffectType> first_effects = new HashSet<>();
    public final HashSet<EffectType> last_effects = new HashSet<>();

    public double addiction;
    public double first;
    public double last;

    public DrugsSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        json.getAsJsonArray("first_effects")
                .forEach(item -> first_effects.add(EffectType.valueOf(item.getAsString())));
        json.getAsJsonArray("last_effects")
                .forEach(item -> last_effects.add(EffectType.valueOf(item.getAsString())));

        this.addiction = json.get("addiction").getAsDouble();
        this.first = json.get("first").getAsDouble();
        this.last = json.get("last").getAsDouble();
    }
}