package org.lime.gp.block.component.data;

import net.minecraft.world.level.block.entity.TileEntitySkullTickInfo;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R1.block.CraftBlock;
import org.lime.gp.block.BlockComponentInstance;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.display.instance.DisplayInstance;
import org.lime.gp.block.component.list.BombComponent;
import org.lime.gp.sound.Sounds;
import org.lime.json.JsonObjectOptional;
import org.lime.system.json;
import org.lime.system.toast.Toast3;

public class BombInstance extends BlockComponentInstance<BombComponent> implements CustomTileMetadata.Tickable {
    public BombInstance(BombComponent component, CustomTileMetadata metadata) {
        super(component, metadata);
    }

    @Override public void read(JsonObjectOptional json) {}
    @Override public json.builder.object write() { return json.object(); }

    private int ticks = 0;
    @Override public void onTick(CustomTileMetadata metadata, TileEntitySkullTickInfo event) {
        ticks++;
        if (ticks % 20 == 0) Sounds.playSound(component().sound_beep, metadata.location());
        if (ticks / 20 >= component().seconds) executeDestroy();
    }

    public void executeDestroy() {
        BombComponent component = component();
        CustomTileMetadata metadata = metadata();

        InfoComponent.Rotation.Value rotation = metadata
                .list(DisplayInstance.class)
                .map(DisplayInstance::getRotation)
                .findAny()
                .flatMap(v -> v)
                .orElse(InfoComponent.Rotation.Value.ANGLE_0);

        Block block = metadata.block();
        metadata.setAir();

        Sounds.playSound(component.sound_boom, metadata.location());
        for (Toast3<Integer, Integer, Integer> k : component.blocks) {
            Toast3<Integer, Integer, Integer> p = rotation.rotate(k);
            if (block.getRelative(p.val0, p.val1, p.val2) instanceof CraftBlock target && target.getNMS().isDestroyable())
                target.breakNaturally();
        }
    }
}
