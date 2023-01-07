package org.lime.gp.entity.component.display;

import net.minecraft.world.entity.EntityLimeMarker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.lime.core;
import org.lime.display.Displays;
import org.lime.display.Models;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.Entities;

import java.util.Optional;
import java.util.UUID;

public class EntityDisplay {
    public static core.element create() {
        return core.element.create(EntityDisplay.class)
                .withInit(EntityDisplay::init);
    }
    public static final EntityModelDisplay.EntityModelManager MODEL_MANAGER = EntityModelDisplay.manager();
    public static void init() {
        Displays.initDisplay(MODEL_MANAGER);
        AnyEvent.addEvent("entity.display.variable", AnyEvent.type.other, v -> v
                        .createParam(UUID::fromString, "[block_uuid:uuid]")
                        .createParam("[key:text]")
                        .createParam("[value:text]"),
                (p, entity_uuid, key, value) -> Optional.ofNullable(Bukkit.getEntity(entity_uuid))
                        .map(v -> v instanceof Marker marker ? marker : null)
                        .flatMap(Entities::of)
                        .flatMap(Entities::customOf)
                        .flatMap(v -> v.list(DisplayInstance.class).findAny())
                        .ifPresent(instance -> instance.set(key, value))
        );
    }

    public interface IEntity {
        Optional<Models.Model> data();
        static IEntity of(Models.Model model) {
            return () -> Optional.ofNullable(model);
        }
    }
    public interface Displayable extends CustomEntityMetadata.Uniqueable {
        Optional<IEntity> onDisplay(Player player, EntityLimeMarker marker);
    }
}


























