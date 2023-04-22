package org.lime.gp.block.component.data;

import net.minecraft.network.chat.ChatHexColor;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.lime.gp.block.BlockInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.ComponentDynamic;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatColorHex;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.database.rows.HouseRow;
import org.lime.gp.item.CartographyBrush;
import org.lime.gp.item.CartographyBucket;
import org.lime.gp.module.DrawMap;
import org.lime.json.JsonObjectOptional;
import org.lime.system;

import java.util.UUID;

public class PaintingInstance extends BlockInstance implements CustomTileMetadata.Interactable {
    public PaintingInstance(ComponentDynamic<?, ?> component, CustomTileMetadata metadata) {
        super(component, metadata);
    }

    @Override public void read(JsonObjectOptional json) { }
    @Override public system.json.builder.object write() { return system.json.object(); }
    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        EntityHuman human = event.player();
        UUID uuid = human.getUUID();
        if (HouseRow.useType(HouseRow.getInHouse(metadata.location()), uuid) == HouseRow.UseType.Deny) return EnumInteractionResult.PASS;
        if (human.getBukkitEntity() instanceof Player player) return CartographyBrush.getData(uuid).flatMap(brush -> metadata.list(DisplayInstance.class).findFirst().map(display -> {
            Color display_color = Color.fromRGB(metadata.list(DisplayInstance.class)
                    .findFirst()
                    .flatMap(v -> v.get("display.color"))
                    .map(v -> ChatHexColor.parseColor("#" + v).getValue())
                    .orElse(0xFFFFFF));
            Color brush_color = DrawMap.to(brush.color);
            system.Toast3<Integer, Integer, Integer> delta = system.toast(
                    Math.abs(display_color.getRed() - brush_color.getRed()),
                    Math.abs(display_color.getGreen() - brush_color.getGreen()),
                    Math.abs(display_color.getBlue() - brush_color.getBlue()));

            delta.val0 *= 50;
            delta.val1 *= 50;
            delta.val2 *= 50;

            system.Toast3<Integer, Integer, Integer> bucket = system.toast(0,0,0);
            CartographyBucket.modifyData(player.getInventory().getItemInOffHand(), data -> {
                bucket.val0 = delta.val0;
                bucket.val1 = delta.val1;
                bucket.val2 = delta.val2;
                if (delta.val0 > data.r || delta.val1 > data.g || delta.val2 > data.b) return false;
                data.r -= delta.val0;
                data.g -= delta.val1;
                data.b -= delta.val2;
                return true;
            }).ifPresentOrElse(state -> {
                if (state) {
                    display.set("display.color", ChatColorHex.toHex(brush_color).substring(1));
                    display.reshow();
                } else {
                    LangMessages.Message.Brush_Bucket_Empty.sendMessage(player, Apply.of()
                            .add("total_r", String.valueOf(delta.val0))
                            .add("total_g", String.valueOf(delta.val1))
                            .add("total_b", String.valueOf(delta.val2))

                            .add("bucket_r", String.valueOf(bucket.val0))
                            .add("bucket_g", String.valueOf(bucket.val1))
                            .add("bucket_b", String.valueOf(bucket.val2))
                    );
                }
            }, () -> LangMessages.Message.Brush_Bucket_NotFound.sendMessage(player));
            return EnumInteractionResult.CONSUME;
        })).orElse(EnumInteractionResult.PASS);
        return EnumInteractionResult.PASS;
    }
}
