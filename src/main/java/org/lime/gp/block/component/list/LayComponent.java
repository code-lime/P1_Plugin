package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import dev.geco.gsit.api.GSitAPI;
import dev.geco.gsit.objects.IGPoseSeat;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.util.Vector;
import org.lime.display.transform.LocalLocation;
import org.lime.display.transform.Transform;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentStatic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.lime;
import org.lime.gp.module.SingleModules;
import org.lime.system.utils.MathUtils;

@InfoComponent.Component(name = "lay")
public final class LayComponent extends ComponentStatic<JsonObject> implements CustomTileMetadata.Interactable {
    public final double rotation;
    public final Vector offset;
    public final boolean sitAtBlock;

    public LayComponent(BlockInfo creator, JsonObject json) {
        super(creator, json);
        rotation = json.has("rotation") ? json.get("rotation").getAsDouble() : 0;
        offset = json.has("offset") ? MathUtils.getVector(json.get("offset").getAsString()) : new Vector();
        sitAtBlock = !json.has("sit_at_block") || json.get("sit_at_block").getAsBoolean();
    }

    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        if (!(event.player() instanceof EntityPlayer eplayer)) return EnumInteractionResult.PASS;
        Player bukkitPlayer = eplayer.getBukkitEntity();
        if (!GSitAPI.getSeats(metadata.block()).isEmpty() || lime.isSit(bukkitPlayer) || lime.isLay(bukkitPlayer))
            return EnumInteractionResult.PASS;
        double rotation = metadata.list(DisplayInstance.class)
                .findAny()
                .flatMap(DisplayInstance::getRotation)
                .orElse(InfoComponent.Rotation.Value.ANGLE_0)
                .angle / 360.0;
        rotation += this.rotation / 360.0;
        rotation = (rotation % 1) * 360;
        if (rotation > 180) rotation -= 360;
        Vector rotated_offset = Transform.toWorld(new Location(null, 0, 0, 0, (float) rotation, 0), new LocalLocation(offset)).toVector();

        String bedLoc = MathUtils.getString(metadata.location());
        if (SingleModules.beds.containsKey(bedLoc)) return EnumInteractionResult.PASS;
        SingleModules.beds.put(bedLoc, bukkitPlayer.getUniqueId());

        IGPoseSeat pose = GSitAPI.createPose(metadata.block(), eplayer.getBukkitEntity(), Pose.SLEEPING, rotated_offset.getX(), rotated_offset.getY(), rotated_offset.getZ(), (float) rotation, sitAtBlock);
        return pose == null ? EnumInteractionResult.PASS : EnumInteractionResult.CONSUME;
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, JObject.of(
                JProperty.optional(IName.raw("rotation"), IJElement.raw(10.0), IComment.text("Относительный угол поворота")),
                JProperty.optional(IName.raw("offset"), IJElement.link(docs.vector()), IComment.text("Относительная позиция")),
                JProperty.optional(IName.raw("sit_at_block"), IJElement.bool(), IComment.text("Привязываться ли к высоте блока"))
        ), "На блок можно лечь");
    }
}
