package org.lime.gp.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import org.lime.display.models.shadow.BaseBuilder;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.block.Blocks;
import org.lime.gp.craft.Crafts;
import org.lime.gp.item.Items;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;
import org.lime.system.execute.Func0;
import org.lime.system.execute.Func1;
import org.lime.system.json;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class ExportJson {
    public static CoreElement create() {
        return CoreElement.create(ExportJson.class)
                .withInit(ExportJson::init);
    }

    private static void register(String event, String name, Func0<Collection<String>> elements, Func1<String, JsonElement> getter) {
        AnyEvent.addEvent(event,
                AnyEvent.type.owner_console,
                b -> b.createParam(v -> v, elements),
                (p, key) -> lime.logOP(Component.text(name)
                        .append(Component.text(" '" + key + "':"))
                        .appendNewline()
                        .append(Component.text("   "))
                        .append(json.formatComponent(getter.invoke(key), null))));
    }
    private static void register(String event, String name, Map<String, ? extends JsonElement> elements) {
        register(event, name, elements::keySet, v -> Objects.requireNonNullElseGet(elements.get(v), JsonObject::new));
    }

    private static void init() {
        register("models.json", "Model", lime.models::keys, v -> lime.models.get(v).map(BaseBuilder::source).orElse(new JsonObject()));
        register("blocks.json", "Block", Blocks.overrides);
        register("items.json", "Item", Items.overrides);
        register("crafts.json", "Craft", Crafts.overrides);
    }
}
