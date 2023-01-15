package org.lime.gp.block.component.display.block;

import java.util.Optional;

import org.lime.display.Models;

import net.minecraft.world.level.block.state.IBlockData;

public interface IModelBlock extends IBlock {
    int distanceChunk();
    Optional<Models.Model> model();

    @Override default IModelBlock withModel(Models.Model model, int distanceChunk) { return this; }

    static IModelBlock of(IBlockData data, Models.Model model, int distanceChunk) {
        return new IModelBlock() {
            @Override public int distanceChunk() { return distanceChunk; }
            @Override public Optional<IBlockData> data() { return Optional.ofNullable(data); }
            @Override public Optional<Models.Model> model() { return Optional.ofNullable(model); }
        };
    }
}