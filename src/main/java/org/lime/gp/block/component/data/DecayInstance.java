package org.lime.gp.block.component.data;

import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockSkullInteractInfo;
import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import org.lime.gp.block.BlockComponentInstance;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.display.IDisplayVariable;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.list.DecayComponent;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.DecayMutateSetting;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;

public class DecayInstance extends BlockComponentInstance<DecayComponent> implements CustomTileMetadata.Tickable, CustomTileMetadata.Interactable, IDisplayVariable {
    public DecayInstance(DecayComponent component, CustomTileMetadata metadata) {
        super(component, metadata);

        int displayValue = (int)Math.round(decayValue * component().displayCount);
        if (displayValue != lastDisplayValue) {
            lastDisplayValue = displayValue;
            syncDisplayVariable(metadata);
        }
    }

    private double decayValue = 0;

    public int decayPercent() {
        return (int)Math.round(decayValue * 100);
    }
    public void decayPercent(int percent) {
        decayValue = percent / 100.0;
    }

    @Override public void read(JsonObjectOptional json) {
        decayPercent(json.getAsInt("decay").orElse(0));

        int displayValue = (int)Math.round(decayValue * component().displayCount);
        if (displayValue != lastDisplayValue) {
            lastDisplayValue = displayValue;
            syncDisplayVariable(metadata());
        }
    }

    @Override public json.builder.object write() {
        return json.object()
                .add("decay", decayPercent());
    }

    private int lastDisplayValue = -1;

    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        DecayComponent component = component();
        decayValue += component.tickDecayModify();
        int displayValue = (int)Math.round(decayValue * component.displayCount);
        if (displayValue != lastDisplayValue) {
            lastDisplayValue = displayValue;
            syncDisplayVariable(metadata);
        }
        if (decayValue < 1) return;
        Blocks.setBlock(metadata.position(), component.replace, InfoComponent.Rotation.Value.ANGLE_0);
        //metadata.block().setType(component.replace);
    }

    @Override public EnumInteractionResult onInteract(CustomTileMetadata metadata, BlockSkullInteractInfo event) {
        EnumHand hand = event.hand();
        EntityHuman player = event.player();
        World world = event.world();

        return Items.getOptional(DecayMutateSetting.class, player.getItemInHand(hand)).map(data -> {
            DecayComponent component = component();
            decayValue -= component.valueDecayModify(data.decayDelta(component.totalDecay()));
            if (decayValue < 0) decayValue = 0;

            int displayValue = (int)Math.round(decayValue * component.displayCount);
            if (displayValue != lastDisplayValue) {
                lastDisplayValue = displayValue;
                syncDisplayVariable(metadata);
            }

            return EnumInteractionResult.sidedSuccess(world.isClientSide);
        }).orElse(EnumInteractionResult.PASS);
    }

    @Override public final void syncDisplayVariable(CustomTileMetadata metadata) {
        metadata().list(DisplayInstance.class).findAny().ifPresent(display -> {
            display.modify(map -> {
                map.put("decay", String.valueOf(lastDisplayValue));
                return true;
            });
        });
    }
}













