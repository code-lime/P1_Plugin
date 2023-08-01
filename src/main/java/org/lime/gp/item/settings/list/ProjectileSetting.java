package org.lime.gp.item.settings.list;

import com.google.gson.JsonObject;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;

import java.util.ArrayList;
import java.util.List;

@Setting(name = "projectile") public class ProjectileSetting extends ItemSetting<JsonObject> {
    public final float speed;
    public final float divergence;
    public final float height;
    public final int cooldown;
    public final boolean pickupOwner;
    public final byte loyalty;
    public final float damage;
    public final List<String> tags = new ArrayList<>();

    public ProjectileSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        this.speed = json.get("speed").getAsFloat();
        this.divergence = json.get("divergence").getAsFloat();
        this.height = json.get("height").getAsFloat();
        this.cooldown = json.get("cooldown").getAsInt();
        this.pickupOwner = json.has("pickup_owner") && json.get("pickup_owner").getAsBoolean();
        this.loyalty = json.has("loyalty") ? json.get("loyalty").getAsByte() : 0;
        this.damage = json.has("damage") ? json.get("damage").getAsFloat() : 0;
        if (json.has("tags")) json.getAsJsonArray("tags").forEach(item -> tags.add(item.getAsString()));
    }
}
