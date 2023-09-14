package org.lime.gp.player.module.pets;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.display.ObjectDisplay;
import org.lime.display.models.display.BaseChildDisplay;
import org.lime.gp.database.rows.PetsRow;
import org.lime.gp.lime;
import org.lime.gp.module.DrawText;
import org.lime.gp.module.EntityPosition;
import org.lime.system.utils.RandomUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PetDisplay extends ObjectDisplay<PetsRow, Marker> {

    @Override public double getDistance() { return 30; }

    private final PetsRow row;

    private final AbstractPet pet;
    private final BaseChildDisplay<?, ?, ?> model;
    private final Map<String, Object> data = new HashMap<>();
    private int step;
    private final double speed;
    private final int maxSteps;
    private Vector last_to = null;

    private boolean teleport = false;

    @Override public Location location() {
        EntityPosition.PositionInfo data = EntityPosition.playerInfo.get(row.uuid);
        if (data == null) return new Location(lime.LoginWorld, 0, -100, 0);
        Location location = data.location;
        if (location == null) return new Location(lime.LoginWorld, 0, -100, 0);
        Location loc = location.clone();
        loc.setPitch(0);

        Vector forward = loc.getDirection();
        Vector right = forward.getCrossProduct(new Vector(0, 1, 0));
        Vector pos = location.toVector();

        double value = Math.sin(((step * 2.0 / maxSteps) - 1) * Math.PI);
        Vector to = new Vector().add(pos).add(forward.multiply(-1.5)).add(new Vector(0, 1.5, 0)).add(right.multiply(value * 2));

        if (last_to == null) last_to = to;

        Vector delta = to.clone().subtract(last_to);

        double curr_speed = to.distance(last_to);

        if (curr_speed > speed && curr_speed < 100) {
            double _speed = speed;
            if (curr_speed > 80) _speed = 10;
            else if (curr_speed > 40) _speed = 5;
            else if (curr_speed > 20) _speed = 3;
            else if (curr_speed > 10) _speed = 1.5;
            if (delta.lengthSquared() == 0) delta = new Vector(0, 0, 0);
            else delta.normalize().multiply(_speed);
            to = delta.clone().add(last_to);
        } else if (curr_speed >= 100) {
            teleport = true;
        }
        last_to = to;
        return loc.set(to.getX(), to.getY(), to.getZ()).setDirection(delta.lengthSquared() == 0 ? new Vector(0, 0, 0) : delta.normalize());
    }

    protected PetDisplay(AbstractPet pet, PetsRow row) {
        this.pet = pet;
        this.row = row;
        this.speed = pet.speed == 0 ? 0 : (pet.speed + RandomUtils.rand(0, pet.speed * 0.05) * (RandomUtils.rand() ? 1 : -1));
        this.maxSteps = pet.steps;
        this.step = RandomUtils.rand(0, maxSteps);

        this.model = preInitDisplay(pet.model().<PetsRow>display(this));
        postInit();
    }

    private UUID lastWorldId = null;

    @Override public void update(PetsRow row, double delta) {
        if (this.row != row) {
            discard();
            return;
        }
        step += 1;
        step %= maxSteps;
        super.update(row, delta);
        if (last_location == null) return;
        UUID worldId = last_location.getWorld().getUID();
        if (!worldId.equals(lastWorldId)) {
            if (lastWorldId != null) teleport = true;
            lastWorldId = worldId;
        }

        int id = row.id;

        if (row.name != null) {
            Location location = last_location;
            Component text = Component.text(row.name).color(row.color == null ? NamedTextColor.WHITE : TextColor.fromHexString("#" + row.color));
            int modelID = this.model.entityID;
            DrawText.show(new DrawText.IShowTimed(0.5) {
                @Override public Optional<Integer> parent() { return Optional.of(modelID); }
                @Override public String getID() { return "Pet[" + id + "].NickName"; }
                @Override public boolean filter(Player player) { return true; }
                @Override public Component text(Player player) { return text; }
                @Override public Location location() { return location; }
                @Override public double distance() { return 5; }
            });
        }

        pet.tick(model, data);
        invokeAll(this::sendData);

        if (teleport) {
            discard();
            teleport = false;
        }
    }

    private void discard() {
        Pets.MANAGER.remove(row.id, this::enqueuePacket);
    }

    @Override protected Marker createEntity(Location location) {
        return new Marker(EntityTypes.MARKER, ((CraftWorld) location.getWorld()).getHandle());
    }
}
