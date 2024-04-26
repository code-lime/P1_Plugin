package org.lime.gp.block.component.list;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.data.BombInstance;
import org.lime.gp.docs.IDocsLink;
import org.lime.system.Regex;
import org.lime.system.toast.Toast3;
import org.lime.system.utils.MathUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@InfoComponent.Component(name = "bomb") public class BombComponent extends ComponentDynamic<JsonObject, BombInstance> {

    public final List<Toast3<Integer, Integer, Integer>> blocks = new ArrayList<>();
    public final List<Material> blacklist = new ArrayList<>();
    public final int seconds;
    public final @Nullable String sound_beep;
    public final @Nullable String sound_boom;

    public BombComponent(BlockInfo info, JsonObject json) {
        super(info, json);
        json.getAsJsonArray("blocks")
                        .forEach(v -> blocks.add(MathUtils.getPosToast(v.getAsString())));
        if (json.has("blacklist"))
            json.getAsJsonArray("blacklist")
                    .asList()
                    .stream()
                    .map(JsonElement::getAsString)
                    .flatMap(regex -> Arrays.stream(Material.values()).filter(v -> Regex.compareRegex(v.name(), regex)))
                    .forEach(blacklist::add);
        seconds = json.get("seconds").getAsInt();
        if (json.has("sound")) {
            JsonObject sound = json.getAsJsonObject("sound");
            sound_beep = sound.has("beep") ? sound.get("beep").getAsString() : null;
            sound_boom = sound.has("boom") ? sound.get("boom").getAsString() : null;
        } else {
            sound_beep = null;
            sound_boom = null;
        }
    }

    @Override public BombInstance createInstance(CustomTileMetadata metadata) {
        return new BombInstance(this, metadata);
    }
    @Override public Class<BombInstance> classInstance() { return BombInstance.class; }
    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, JObject.of(
                JProperty.require(IName.raw("blocks"), IJElement.anyList(IJElement.link(docs.vectorInt())), IComment.text("Список локальных координат блоков, которые будут уничтожены")),
                JProperty.require(IName.raw("seconds"), IJElement.raw(10), IComment.text("Время, через которое произойдет взрыв")),
                JProperty.optional(IName.raw("blacklist"), IJElement.anyList(IJElement.raw("REGEX")), IComment.text("Список запрещенных блоков для ломания")),
                JProperty.optional(IName.raw("sound"), JObject.of(
                        JProperty.optional(IName.raw("beep"), IJElement.link(docs.sound()), IComment.text("Ежесекундный звук активности")),
                        JProperty.optional(IName.raw("boom"), IJElement.link(docs.sound()), IComment.text("Звук взрыва"))
                ))
        ), IComment.text("Уничтожает блоки через определенное время"));
    }
}
