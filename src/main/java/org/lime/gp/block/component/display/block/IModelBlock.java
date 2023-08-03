package org.lime.gp.block.component.display.block;

import java.util.Optional;

import net.minecraft.world.level.block.state.IBlockData;
import org.lime.display.models.Model;
import org.lime.system;

public interface IModelBlock extends IBlock {
    int distanceChunk();
    Optional<system.Toast2<Model, Double>> model();

    @Override default IModelBlock withModel(Model model, int distanceChunk, double distanceModel) { return this; }

    static IModelBlock of(IBlockData data, Model model, int distanceChunk, double distanceModel) {
        Optional<system.Toast2<Model, Double>> modelData = model == null ? Optional.empty() : Optional.of(system.toast(model, distanceModel));
        return new IModelBlock() {
            @Override public int distanceChunk() { return distanceChunk; }
            @Override public Optional<IBlockData> data() { return Optional.ofNullable(data); }
            @Override public Optional<system.Toast2<Model, Double>> model() { return modelData; }
        };
    }
}