package org.lime.gp.entity.component;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.lime.display.Models;
import org.lime.gp.entity.CustomEntityMetadata;
import org.lime.gp.entity.EntityInfo;
import org.lime.gp.entity.component.data.BackPackInstance;
import org.lime.gp.entity.component.display.DisplayInstance;
import org.lime.gp.entity.component.display.DisplayPartial;
import org.lime.gp.module.JavaScript;
import org.lime.system;

import java.util.*;

public class Components {
    @InfoComponent.Component(name = "display") public static final class DisplayComponent extends ComponentDynamic<JsonObject, DisplayInstance> {
        public final List<DisplayPartial.Partial> partials = new LinkedList<>();
        public final Map<UUID, DisplayPartial.Partial> partialMap = new HashMap<>();
        public final double maxDistanceSquared;
        public final String animation_tick;
        public final HashMap<String, Object> animation_args = new HashMap<>();

        public void animationTick(Map<String, Object> variable, Map<String, Object> data) {
            if (animation_tick == null) return;
            JavaScript.invoke(animation_tick,
                    system.map.<String, Object>of()
                            .add(animation_args, k -> k, v -> v)
                            .add("variable", variable)
                            .add("data", data)
                            .build()
            );
        }

        private static Object toObj(JsonPrimitive json) {
            return json.isNumber() ? json.getAsNumber() : json.isBoolean() ? json.getAsBoolean() : json.getAsString();
        }

        public DisplayComponent(EntityInfo info, JsonObject json) {
            super(info, json);
            if (json.has("animation")) {
                JsonObject animation = json.getAsJsonObject("animation");
                animation_tick = animation.has("tick") ? animation.get("tick").getAsString() : null;
                if (animation.has("args")) animation.getAsJsonObject("args").entrySet().forEach(kv -> animation_args.put(kv.getKey(), toObj(kv.getValue().getAsJsonPrimitive())));
            } else {
                animation_tick = null;
            }
            maxDistanceSquared = DisplayPartial.load(info, json.getAsJsonObject("partial"), partials, partialMap);
        }
        public DisplayComponent(EntityInfo info, Models.Model model) {
            super(info);
            animation_tick = null;
            DisplayPartial.ModelPartial partial = new DisplayPartial.ModelPartial(-1, model);
            partials.add(partial);
            partialMap.put(partial.uuid, partial);
            maxDistanceSquared = -1;
        }
        @Override public DisplayInstance createInstance(CustomEntityMetadata metadata) {
            return new DisplayInstance(this, metadata);
        }
    }
    @InfoComponent.Component(name = "backpack") public static final class BackPackComponent extends ComponentDynamic<JsonNull, BackPackInstance> {
        public BackPackComponent(EntityInfo info) {
            super(info);
        }

        @Override public BackPackInstance createInstance(CustomEntityMetadata metadata) {
            return new BackPackInstance(this, metadata);
        }
    }

    /*@InfoComponent.Component(name = "car") public static final class CarComponent extends ComponentDynamic<JsonObject, CarInstance> {
        public final double wheel_fore;
        public final double wheel_back;
        public final double wheel_length;
        public final double wheel_width;

        public final double water_timeout;

        public final Input input;

        public final Model controller;

        public CarComponent(EntityInfo info, JsonObject json) {
            super(info, json);
            JsonObject wheel = json.getAsJsonObject("wheel");
            wheel_fore = Math.max(wheel.get("fore").getAsDouble(), wheel.get("back").getAsDouble());
            wheel_back = Math.min(wheel.get("fore").getAsDouble(), wheel.get("back").getAsDouble());
            wheel_length = wheel_fore - wheel_back;
            wheel_width = wheel.get("width").getAsDouble();

            water_timeout = json.get("water_timeout").getAsDouble();

            input = Input.of(json.getAsJsonObject("input"), null);

            controller = Model.parse(json.getAsJsonObject("controller"));
        }
        public CarComponent(EntityInfo info, double wheel_fore, double wheel_back, double wheel_width, double water_timeout, Input input, Model controller) {
            super(info);
            this.wheel_fore = wheel_fore;
            this.wheel_back = wheel_fore;
            this.wheel_length = wheel_fore - wheel_back;
            this.wheel_width = wheel_width;

            this.water_timeout = water_timeout;

            this.input = input;

            this.controller = controller;
        }

        @Override public CarInstance createInstance(CustomEntityMetadata metadata) {
            return new CarInstance(this, metadata);
        }
    }
    @InfoComponent.Component(name = "physics") public static final class PhysicsComponent extends ComponentDynamic<JsonObject, PhysicsInstance> {
        public final Collider collider;
        public final double velocity;
        public final double gravity;

        public PhysicsComponent(EntityInfo info, JsonObject json) {
            super(info, json);

            this.velocity = json.get("velocity").getAsDouble();
            this.gravity = json.get("gravity").getAsDouble();

            JsonObject collider = json.getAsJsonObject("collider");
            this.collider = new Collider(collider.get("width").getAsDouble(), collider.get("height").getAsDouble(), collider.get("length").getAsDouble(), collider.has("display") && collider.get("display").getAsBoolean());
        }
        public PhysicsComponent(EntityInfo info, Collider collider) {
            super(info);
            this.collider = collider;
            this.velocity = 0.25;
            this.gravity = 0.1;
        }

        @Override public PhysicsInstance createInstance(CustomEntityMetadata metadata) {
            return new PhysicsInstance(this, metadata);
        }
    }*/
}






















