package org.lime.gp.block.component.display.partial.list;

import com.google.gson.JsonElement;
import org.lime.display.models.shadow.IBuilder;
import org.lime.gp.lime;
import org.lime.system.toast.*;

import javax.annotation.Nullable;
import java.util.Optional;

public interface IModelPartial {
    void generic(IBuilder generic);
    IBuilder generic();

    @Nullable String modelKey();
    double modelDistance();

    default Optional<Toast2<IBuilder, Double>> model() {
        return Optional.ofNullable(generic()).or(() -> Optional.ofNullable(modelKey()).flatMap(lime.models::get)).map(v -> Toast.of(v, modelDistance()));
    }
    default String parseModel(JsonElement json) {
        if (json.isJsonPrimitive()) return json.getAsString();
        generic(lime.models.builder().parse(json));
        return "#generic";
    }
}
