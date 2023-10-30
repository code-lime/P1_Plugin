package org.lime.gp.entity.component.display.partial.list;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.entity.EntityLimeMarker;
import org.bukkit.entity.Player;
import org.lime.display.models.shadow.IBuilder;
import org.lime.gp.entity.component.display.EntityDisplay;
import org.lime.gp.entity.component.display.partial.Partial;
import org.lime.gp.entity.component.display.partial.PartialEnum;
import org.lime.gp.lime;

import java.util.Optional;
import java.util.UUID;

public class ModelPartial extends Partial {
    private final String model;
    private IBuilder generic = null;

    public ModelPartial(int distanceChunk, JsonObject json) {
        super(distanceChunk, json);
        this.model = parseModel(json.get("model"));
    }

    public ModelPartial(int distanceChunk, IBuilder model) {
        super(distanceChunk, new JsonObject());
        this.model = "#generic:" + UUID.randomUUID();
        this.generic = model;
    }

    private String parseModel(JsonElement json) {
        if (json.isJsonPrimitive()) return json.getAsString();
        generic = lime.models.builder().parse(json);
        return "#generic:" + UUID.randomUUID();
    }

    public Optional<IBuilder> model() { return Optional.ofNullable(generic).or(() -> lime.models.get(model)); }
    @Override public PartialEnum type() { return PartialEnum.Model; }
    @Override public String toString() { return model; }
}
