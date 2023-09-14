package org.lime.gp.extension;

import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.lime.gp.module.InputEvent;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.RandomUtils;

import java.util.Collections;
import java.util.Optional;

public abstract class Input {
    private final static class StaticInput extends Input {
        private final Speed forward;
        private final Speed backward;
        private final Speed steering;
        private final double max_speed;
        private final Angle angle;
        private final Func0<Optional<Player>> driver;
        private StaticInput(Speed forward, Speed backward, Speed steering, double max_speed, Angle angle, Func0<Optional<Player>> driver) {
            this.forward = forward;
            this.backward = backward;
            this.steering = steering;
            this.max_speed = max_speed;
            this.angle = angle;
            this.driver = driver;
        }
        @Override protected Optional<Player> getDriver() { return driver.invoke(); }
        @Override protected double getMaxSpeed() { return max_speed; }
        @Override protected Speed getForward() { return forward; }
        @Override protected Speed getBackward() { return backward; }
        @Override protected Speed getSteering() { return steering; }
        @Override protected Angle getAngle() { return angle; }

        @Override public Input deepClone(Func0<Optional<Player>> driver) {
            return of(forward.deepClone(), backward.deepClone(), steering == null ? null : steering.deepClone(), max_speed, angle.deepClone(), driver);
        }
        @Override public JsonObject toJson() {
            Speed steering = getSteering();
            return json.object()
                    .add("max_speed", getMaxSpeed())
                    //.add("angle", getAngle())
                    .add("forward", getForward().toJson())
                    .add("backward", getBackward().toJson())
                    .add(steering == null ? Collections.emptyList() : Collections.singletonList(steering), v -> "steering", Speed::toJson)
                    .build();
        }
    }

    protected abstract Optional<Player> getDriver();
    protected abstract double getMaxSpeed();

    public abstract static class Speed {
        private final static class StaticSpeed extends Speed {
            private final double up;
            private final double down;
            private final double def;

            private StaticSpeed(double up, double down, double def) {
                this.up = up;
                this.down = down;
                this.def = def;
            }

            @Override public double getDefault() { return def; }
            @Override public double getUp() { return up; }
            @Override public double getDown() { return down; }
            @Override public Speed deepClone() { return new StaticSpeed(up, down, def); }
            @Override public JsonObject toJson() {
                return json.object()
                        .add("up", up)
                        .add("down", down)
                        .add("default", def)
                        .build();
            }
        }

        public abstract double getDefault();
        public abstract double getUp();
        public abstract double getDown();

        public static Speed of(double up, double down, double def) { return new StaticSpeed(up, down, def); }
        public static Speed of(JsonObject json) { return of(json.get("up").getAsDouble(),json.get("down").getAsDouble(),json.get("default").getAsDouble()); }

        public abstract Speed deepClone();
        public abstract JsonObject toJson();
    }
    public abstract static class Angle {
        private final static class StaticAngle extends Angle {
            private final double min;
            private final double max;

            private StaticAngle(double min, double max) {
                this.min = min;
                this.max = max;
            }

            @Override public double getMax() { return max; }
            @Override public double getMin() { return min; }

            @Override public Angle deepClone() { return new StaticAngle(min, max); }
            @Override public JsonObject toJson() {
                return json.object()
                        .add("min", min)
                        .add("max", max)
                        .build();
            }
        }

        public abstract double getMax();
        public abstract double getMin();

        /*public double getAngle(double speed, double max_speed) {
            double max = getMax();
            double min = getMin();

            return (max - min) * (speed / max_speed) + min;
        }*/
        public double getAngle(double rotation) {
            return RandomUtils.rand(getMin(), getMax()) * rotation;
            /*double max = getMax();
            double min = getMin();

            return (max - min) * (speed / max_speed) + min;*/
        }

        public static Angle of(double min, double max) { return new StaticAngle(min, max); }
        public static Angle of(JsonObject json) { return of(json.get("min").getAsDouble(),json.get("max").getAsDouble()); }

        public abstract Angle deepClone();
        public abstract JsonObject toJson();
    }

    protected abstract Speed getForward();
    protected abstract Speed getBackward();
    protected abstract Speed getSteering();
    protected abstract Angle getAngle();

    protected double speed;
    protected double rotation;
    protected boolean unmount;
    protected Boolean last_input = null;

    public double getSpeed() { return speed; }
    public double getAngle(double rotation) { return getAngle().getAngle(rotation); }
    public double getRotation() { return rotation; }
    public boolean isUnmount() { return unmount; }

    public void stop() {
        speed = 0;
    }

    public double update(double delta) {
        Speed forward = getForward();
        Speed backward = getBackward();
        Speed steering = getSteering();

        InputEvent input = input();
        unmount = input.isUnmount();
        double delta_speed = switch (input.getVertical().delta()) {
            case 1 -> 0.1 * (Boolean.FALSE.equals(last_input) ? forward.getDown() : backward.getUp());
            case -1 -> -0.1 * (Boolean.TRUE.equals(last_input) ? forward.getDown() : backward.getUp());
            default -> Math.abs(speed) < 0.5 ? 0 : (speed > 0 ? (-0.1 * forward.getDefault()) : (0.1 * backward.getDefault()));
        };

        double delta_rotate = steering == null ? 0 : switch (input.getHorizontal().delta()) {
            case 1 -> -(rotation > 0 ? steering.getDown() : steering.getUp());
            case -1 -> (rotation > 0 ? steering.getUp() : steering.getDown());
            default -> rotation == 0 ? 0 : ((rotation > 0 ? -1 : 1) * steering.getDefault());
        };
        rotation += delta_rotate * delta;
        if (rotation > 1) rotation = 1;
        else if (rotation < -1) rotation = -1;
        else if (Math.abs(rotation) < 0.005) rotation = 0;

        double max_speed = getMaxSpeed();
        double _speed = speed / max_speed;
        _speed += delta_speed * delta;
        double abs = Math.abs(_speed);
        _speed = abs < 0.005 || (delta_speed == 0 && abs < 1) ? 0 : _speed;

        if (last_input == null) last_input = _speed == 0 ? null : (_speed > 0);
        else {
            if (_speed == 0 && delta_speed == 0) last_input = null;
            else if (_speed > 0 && !last_input) _speed = 0;
            else if (_speed < 0 && last_input) _speed = 0;
        }

        speed = max_speed * Math.min(1, Math.max(-1, _speed));

        return speed * delta;
    }
    protected InputEvent input() {
        return getDriver().map(InputEvent::last).orElse(InputEvent.EMPTY);
    }

    public static Input of(Speed forward, Speed backward, Speed steering, double max_speed, Angle angle, Func0<Optional<Player>> driver) {
        return new StaticInput(forward, backward, steering, max_speed, angle, driver);
    }
    public static Input of(JsonObject json, Func0<Optional<Player>> driver) {
        return of(
                Speed.of(json.get("forward").getAsJsonObject()),
                Speed.of(json.get("backward").getAsJsonObject()),
                json.has("steering") ? Speed.of(json.get("steering").getAsJsonObject()) : null,
                json.get("max_speed").getAsDouble(),
                Angle.of(json.get("angle").getAsJsonObject()),
                driver
        );
    }

    public abstract JsonObject toJson();
    public abstract Input deepClone(Func0<Optional<Player>> driver);
}
















