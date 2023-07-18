package org.lime.gp.block.component.display.block;

import java.util.Optional;

import net.minecraft.world.level.block.state.IBlockData;
import org.lime.display.models.Model;

public interface IModelBlock extends IBlock {
    int distanceChunk();
    Optional<Model> model();

    @Override default IModelBlock withModel(Model model, int distanceChunk) { return this; }

    static IModelBlock of(IBlockData data, Model model, int distanceChunk) {
        return new IModelBlock() {
            @Override public int distanceChunk() { return distanceChunk; }
            @Override public Optional<IBlockData> data() { return Optional.ofNullable(data); }
            @Override public Optional<Model> model() { return Optional.ofNullable(model); }
        };
    }
}