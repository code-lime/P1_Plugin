package org.lime.gp.entity.component.data;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLimeMarker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.lime.core;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.display.Model;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.Entities;
import org.lime.gp.entity.EntityInfo;
import org.lime.gp.entity.EntityInstance;
import org.lime.gp.entity.collision.Collider;
import org.lime.gp.entity.component.Components;
import org.lime.gp.entity.component.display.DisplayInstance;
import org.lime.gp.entity.component.display.EntityDisplay;
import org.lime.gp.entity.component.display.EntityModelDisplay;
import org.lime.gp.entity.event.EntityMarkerEventInteract;
import org.lime.gp.entity.event.EntityMarkerEventTick;
import org.lime.gp.extension.Input;
import org.lime.gp.lime;
import org.lime.gp.module.InputEvent;
import org.lime.system;

import java.util.Optional;
import java.util.UUID;

public class CarInstance extends EntityInstance implements CustomEntityMetadata.LazyTickable, EntityDisplay.Displayable, CustomEntityMetadata.Interactable {
    public static core.element create() {
        Entities.addDefaultEntities(new EntityInfo("car")
                        .add(v -> new Components.PhysicsComponent(v, new Collider(1, 1, 1, true)))
                /*.add(v -> new Components.CarComponent(v, 1.5, -1.5, 1.5, 100, Input.of(
                        Input.Speed.of(2, 1, 0),
                        Input.Speed.of(2, 1, 0),
                        Input.Speed.of(3, 3, 0),
                        10,
                        Input.Angle.of(45, 43),
                        null
                ), Model.parse(
                        system.json.object()
                                .addArray("childs", _v -> _v
                                        .addObject(__v -> __v
                                                .add("entity", "zombie")
                                                .add("local", "-0.75 0 1.25")
                                                .add("keys", List.of("driver.sit", "driver.sit.click"))
                                                .addObject("nbt", ___v -> ___v.add("IsBaby", true))
                                        )
                                        .addObject(__v -> __v
                                                .add("entity", "zombie")
                                                .add("local", "0.75 0 1.25")
                                                .addObject("nbt", ___v -> ___v.add("IsBaby", true))
                                        )
                                )
                                .build()
                )))*/
                /*.add(v -> new Components.DisplayComponent(v, Model.parse(
                                system.json.object()
                                        .add("entity", "zombie")
                                        .addObject("nbt", _v -> _v.add("IsBaby", true))
                                        .addArray("childs", _v -> _v
                                                .addObject(__v -> __v
                                                        .add("entity", "zombie")
                                                        .add("local", "-1 0 -2")
                                                        .addObject("nbt", ___v -> ___v.add("IsBaby", true))
                                                )
                                                .addObject(__v -> __v
                                                        .add("entity", "zombie")
                                                        .add("local", "1 0 -2")
                                                        .addObject("nbt", ___v -> ___v.add("IsBaby", true))
                                                )
                                                .addObject(__v -> __v
                                                        .add("entity", "zombie")
                                                        .add("local", "-1 0 2")
                                                        .addObject("nbt", ___v -> ___v.add("IsBaby", true))
                                                )
                                                .addObject(__v -> __v
                                                        .add("entity", "zombie")
                                                        .add("local", "1 0 2")
                                                        .addObject("nbt", ___v -> ___v.add("IsBaby", true))
                                                )
                                                .addObject(__v -> __v
                                                        .add("entity", "zombie")
                                                        .add("local", "-1 1 -2")
                                                        .addObject("nbt", ___v -> ___v.add("IsBaby", true))
                                                )
                                                .addObject(__v -> __v
                                                        .add("entity", "zombie")
                                                        .add("local", "1 1 -2")
                                                        .addObject("nbt", ___v -> ___v.add("IsBaby", true))
                                                )
                                                .addObject(__v -> __v
                                                        .add("entity", "zombie")
                                                        .add("local", "-1 1 2")
                                                        .addObject("nbt", ___v -> ___v.add("IsBaby", true))
                                                )
                                                .addObject(__v -> __v
                                                        .add("entity", "zombie")
                                                        .add("local", "1 1 2")
                                                        .addObject("nbt", ___v -> ___v.add("IsBaby", true))
                                                )
                                        )
                                        .build()
                        ))
                )*/
        );
        return core.element.create(CarInstance.class)
                .withInit(CarInstance::init);
    }
    public static void init() {
        AnyEvent.addEvent("tmp.driver", AnyEvent.type.owner, v -> v.createParam(uuid -> Bukkit.getEntity(UUID.fromString(uuid)), () -> Entities.all().map(Entity::getStringUUID).toList()), (player, entity) -> {
            if (!(entity instanceof Marker marker)) return;
            Entities.of(marker)
                    .flatMap(Entities::customOf)
                    .flatMap(v -> v.list(CarInstance.class).findAny())
                    .ifPresent(car -> {
                        lime.logOP("Drive!");
                        car.driver(player);
                    });
        });
    }

    @Override public Components.CarComponent component() { return (Components.CarComponent)super.component(); }
    public CarInstance(Components.CarComponent component, CustomEntityMetadata metadata) {
        super(component, metadata);
        this.input = component.input.deepClone(this::driver);
    }

    public double fuel = 0;
    public double inWater = 0;
    public Input input;
    @Override public void read(JsonObject json) {
        fuel = json.get("fuel").getAsDouble();
        inWater = json.get("inWater").getAsDouble();
    }

    @Override public system.json.builder.object write() {
        return system.json.object()
                .add("fuel", fuel)
                .add("inWater", inWater);
    }

    public EntityModelDisplay.EntityModelKey modelKey() {
        return new EntityModelDisplay.EntityModelKey(metadata().marker.getUUID(), component().controller.unique, unique());
    }

    public Optional<Player> driver() {
        return EntityModelDisplay.of(modelKey())
                .map(v -> v.model)
                .flatMap(v -> v.singleOfKey("driver.sit"))
                .flatMap(Model.ChildDisplay::sitter);
    }
    public void modifyDriver(system.Func1<Optional<Player>, Player> player) {
        EntityModelDisplay.of(modelKey())
                .map(v -> v.model)
                .flatMap(v -> v.singleOfKey("driver.sit"))
                .ifPresent(v -> v.sit(player.invoke(v.sitter().map(_v -> _v))));
    }
    public void driver(Player player) {
        EntityModelDisplay.of(modelKey())
                .map(v -> v.model)
                .flatMap(v -> v.singleOfKey("driver.sit"))
                .ifPresent(v -> v.sit(player));
    }
    public boolean hasDriver() {
        return EntityModelDisplay.of(modelKey())
                .map(v -> v.model)
                .flatMap(v -> v.singleOfKey("driver.sit"))
                .map(Model.ChildDisplay::hasSitter)
                .orElse(false);
    }

    @Override public void onLazyTick(CustomEntityMetadata metadata, EntityMarkerEventTick event) {
        double delta = event.getDelta();
        Components.CarComponent component = component();

        inWater = event.getMarker().getFeetBlockState().getFluidState().isEmpty()
                ? Math.max(0, inWater - delta)
                : Math.min(component.water_timeout, inWater + delta);

        double isWater = Math.min(1, (Math.min(component.water_timeout, Math.max(0, inWater)) / component.water_timeout) * 2);
        boolean isFuel = true;//fuel > 0;
        boolean isDisable = isWater >= 1;
        if (!isFuel) isDisable = true;
        if (isDisable) this.input.stop();
        double add = (isDisable ? 0 : 1) * (1 - isWater) * this.input.update(delta);
        double rotation = this.input.getAngle(this.input.getRotation());
        //lime.logOP("Input: " + driver().map(HumanEntity::getName).orElse("NO_DRIVER") + " - " + this.input.isUnmount() + " / " + this.input.getRotation() + " / " + this.input.getSpeed());
        double wheel_rotation = component.wheel_length * Math.tan(Math.toRadians(90 - rotation));

        metadata.list(DisplayInstance.class)
                .findAny()
                .ifPresent(display -> {
                    display.set("rotation.wheel", system.getDouble(rotation));
                    display.set("rotation.steering", system.getDouble(this.input.getRotation()));
                });

        Location location = metadata.location();

        double rot = location.getYaw();
        double a = wheel_rotation == 0 ? 0 : (add / wheel_rotation);
        rot += Math.toDegrees(a);

        location.setYaw((float) rot);
        location.add(location.getDirection().multiply(add));

        metadata.moveTo(location);

        EntityModelDisplay.of(modelKey())
                .map(v -> v.model)
                .map(v -> v.ofKey("sit"))
                .ifPresent(v -> v.forEach(model -> model.sitter()
                        .map(InputEvent::last)
                        .filter(InputEvent::isUnmount)
                        .ifPresent(sitter -> model.unsit()))
                );
    }
    @Override public Optional<EntityDisplay.IEntity> onDisplay(Player player, EntityLimeMarker marker) { return Optional.of(EntityDisplay.IEntity.of(component().controller)); }
    @Override public void onInteract(CustomEntityMetadata metadata, EntityMarkerEventInteract event) {
        if (!event.getParentDisplay().key.element_uuid().equals(unique())) return;
        if (event.getClickDisplay().keys().contains("sit.click"))
            event.getClickDisplay().singleOfKey("sit")
                    .filter(v -> v.sitter().isEmpty())
                    .ifPresent(model -> model.sit(event.getPlayer()));
    }
}


















