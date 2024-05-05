package org.lime.gp.item.elemental.step.action;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.util.Vector;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.elemental.step.IStep;
import org.lime.gp.item.settings.use.target.ILocationTarget;
import org.lime.gp.module.mobs.IPopulateSpawn;
import org.lime.gp.module.mobs.spawn.ISpawn;
import org.lime.system.utils.MathUtils;

import java.util.Collections;

@Step(name = "entity")
public record EntityStep(ISpawn spawn) implements IStep<EntityStep> {
    @Override public void execute(ILocationTarget target, DataContext context, Transformation position) {
        if (!(target.getWorld() instanceof CraftWorld craftWorld)) return;
        WorldServer worldServer = craftWorld.getHandle();
        Vector pos = MathUtils.convert(position.getTranslation());
        spawn.generateMob(IPopulateSpawn.of(worldServer, Collections.emptyList()))
                .ifPresent(creator -> creator.spawn(worldServer, new Vec3D(pos.getX(), pos.getY(), pos.getZ())));
    }

    public EntityStep parse(JsonObject json) {
        return new EntityStep(ISpawn.parse(json.get("entity")));
    }
    @Override public JsonGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, JObject.of(
                JProperty.require(IName.raw("entity"), IJElement.field("SPAWN_TABLE_ENTITY"), IComment.text("Энтити, которое появится"))
        ), IComment.text("Спавнит энтити"));
    }
}
