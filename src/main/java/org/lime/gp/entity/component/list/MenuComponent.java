package org.lime.gp.entity.component.list;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.world.EnumInteractionResult;
import org.lime.Position;
import org.lime.gp.chat.Apply;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.EntityInfo;
import org.lime.gp.entity.component.ComponentStatic;
import org.lime.gp.entity.component.InfoComponent;
import org.lime.gp.entity.component.display.instance.DisplayInstance;
import org.lime.gp.entity.event.EntityMarkerEventInteract;
import org.lime.gp.player.menu.MenuCreator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@InfoComponent.Component(name = "menu")
public class MenuComponent extends ComponentStatic<JsonPrimitive> implements
        CustomEntityMetadata.Interactable
{
    private final boolean enable;
    public MenuComponent(EntityInfo creator, JsonPrimitive json) {
        super(creator, json);
        enable = json.getAsBoolean();
    }

    public static Apply argsOf(CustomEntityMetadata metadata) {
        Position position = Position.of(metadata.location());
        return Apply.of()
                .add(metadata.list(DisplayInstance.class).findAny().map(DisplayInstance::getAll).orElseGet(Collections::emptyMap))
                .add("entity_uuid", metadata.key.uuid().toString())
                .add("entity_pos", position.toSave())
                .add("entity_pos_x", String.valueOf(position.x))
                .add("entity_pos_y", String.valueOf(position.y))
                .add("entity_pos_z", String.valueOf(position.z));
    }
    @Override public EnumInteractionResult onInteract(CustomEntityMetadata metadata, EntityMarkerEventInteract event) {
        if (!enable) return EnumInteractionResult.PASS;
        String menu = null;
        Map<String, String> args = new HashMap<>();
        for (String key : event.getClickDisplay().keys()) {
            if (key.startsWith("menu:")) {
                menu = key.substring(5);
            } else if (key.startsWith("menu.arg:")) {
                String[] parts = key.substring(9).split("=", 2);
                if (parts.length > 1) args.put(parts[0], parts[1]);
                else args.put(parts[0], "true");
            }
        }
        if (menu == null) return EnumInteractionResult.PASS;
        MenuCreator.show(event.getPlayer(), menu, argsOf(metadata).add(args));
        return EnumInteractionResult.CONSUME;
    }
}






