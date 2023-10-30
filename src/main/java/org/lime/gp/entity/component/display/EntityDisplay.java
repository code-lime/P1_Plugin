package org.lime.gp.entity.component.display;

import net.minecraft.world.entity.EntityLimeMarker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.lime.gp.block.component.display.event.PlayerChunkMoveEvent;
import org.lime.gp.entity.component.display.display.EntityModelDisplay;
import org.lime.gp.entity.component.display.instance.DisplayInstance;
import org.lime.plugin.CoreElement;
import org.lime.display.Displays;
import org.lime.display.models.shadow.IBuilder;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.Entities;

import java.util.Optional;
import java.util.UUID;

public class EntityDisplay implements Listener {
    public static CoreElement create() {
        return CoreElement.create(EntityDisplay.class)
                .withInit(EntityDisplay::init)
                .withInstance();
    }
    public static final EntityModelDisplay.EntityModelManager MODEL_MANAGER = EntityModelDisplay.manager();
    public static void init() {
        Displays.initDisplay(MODEL_MANAGER);
        AnyEvent.addEvent("entity.display.variable", AnyEvent.type.other, v -> v
                        .createParam(UUID::fromString, "[entity_uuid:uuid]")
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
        int distanceChunk();
        Optional<IBuilder> model();
        static IEntity of(IBuilder model, int distanceChunk) {
            return new IEntity() {
                @Override public int distanceChunk() { return distanceChunk; }
                @Override  public Optional<IBuilder> model() { return Optional.ofNullable(model); }
            };
        }
    }
    public interface Displayable extends CustomEntityMetadata.Uniqueable {
        Optional<IEntity> onDisplayAsync(Player player, EntityLimeMarker marker);
    }

    @EventHandler public static void move(PlayerChunkMoveEvent e) {
        DisplayInstance.appendDirtyQueue(e.getPlayer().getUniqueId());
    }
}


























