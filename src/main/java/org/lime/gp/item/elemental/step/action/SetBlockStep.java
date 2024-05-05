package org.lime.gp.item.elemental.step.action;

import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.util.Vector;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Step;
import org.lime.gp.item.settings.use.target.ILocationTarget;
import org.lime.system.utils.MathUtils;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Step(name = "block.set")
public final class SetBlockStep extends IBlockStep<SetBlockStep> {
    private final boolean force;
    public SetBlockStep(IBlockData block, boolean force) {
        super(block);
        this.force = force;
    }
    public SetBlockStep(Material material, Map<String, String> states, boolean force) {
        super(material, states);
        this.force = force;
    }

    @Override public void execute(ILocationTarget target, DataContext context, Transformation location) {
        if (!(target.getWorld() instanceof CraftWorld handle)) return;
        Vector point = MathUtils.convert(location.getTranslation());
        BlockPosition pos = new BlockPosition(point.getBlockX(), point.getBlockY(), point.getBlockZ());
        World world = handle.getHandle();
        IBlockData data = world.getBlockState(pos);
        if (!force && !data.canBeReplaced()) return;
        world.setBlock(pos, block, Block.UPDATE_ALL);
    }

    public SetBlockStep parse(JsonObject json) {
        return new SetBlockStep(
                Material.valueOf(json.get("material").getAsString()),
                json.has("states")
                        ? json.getAsJsonObject("states")
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, kv -> kv.getValue().getAsString()))
                        : Collections.emptyMap(),
                json.get("force").getAsBoolean()
        );
    }
    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, JObject.of(
                JProperty.require(IName.raw("material"), IJElement.link(docs.vanillaMaterial()), IComment.text("Тип блока")),
                JProperty.optional(IName.raw("states"), IJElement.anyObject(
                        JProperty.require(IName.raw("KEY"), IJElement.raw("VALUE"))
                ), IComment.text("Параметры блока")),
                JProperty.require(IName.raw("force"), IJElement.bool(), IComment.text("Влияет ли изменение на незаменяемые блоки (камень, земля)"))
        ), IComment.text("Заменяет блок"));
    }
}
