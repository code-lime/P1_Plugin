package org.lime.gp.item.settings.list;

import com.google.gson.JsonObject;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
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

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("speed"), IJElement.raw(1.5), IComment.text("Скорость бросаемого предмета")),
                JProperty.require(IName.raw("divergence"), IJElement.raw(1.5), IComment.text("Разброс")),
                JProperty.require(IName.raw("height"), IJElement.raw(1.5), IComment.text("Относительная высота места вылетания")),
                JProperty.require(IName.raw("cooldown"), IJElement.raw(10), IComment.text("Время подготовки броска в тиках")),
                JProperty.optional(IName.raw("pickup_owner"), IJElement.bool(), IComment.text("Возможность поднять бросаемый предмет только бросившим")),
                JProperty.optional(IName.raw("loyalty"), IJElement.raw(2), IComment.empty()
                        .append(IComment.text("Уровень зачарования "))
                        .append(IComment.raw("возврат"))),
                JProperty.optional(IName.raw("damage"), IJElement.raw(1.5), IComment.text("Урон бросаемого предмета")),
                JProperty.optional(IName.raw("tags"), IJElement.anyList(IJElement.raw("TAG")), IComment.text("Тэги добавляемые в бросаемый предмет"))
        ), "Бросаемый предмет");
    }
}








