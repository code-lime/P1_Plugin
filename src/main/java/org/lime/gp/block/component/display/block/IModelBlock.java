package org.lime.gp.block.component.display.block;

import java.util.Optional;

import net.minecraft.world.level.block.state.IBlockData;
import org.lime.display.models.shadow.IBuilder;
import org.lime.system;

public interface IModelBlock extends IBlock {
    int distanceChunk();
    Optional<system.Toast2<IBuilder, Double>> model();

    @Override default IModelBlock withModel(IBuilder model, int distanceChunk, double distanceModel) { return this; }

    static IModelBlock of(IBlockData data, IBuilder model, int distanceChunk, double distanceModel) {
        Optional<system.Toast2<IBuilder, Double>> modelData = model == null ? Optional.empty() : Optional.of(system.toast(model, distanceModel));
        return new IModelBlock() {
            @Override public int distanceChunk() { return distanceChunk; }
            @Override public Optional<IBlockData> data() { return Optional.ofNullable(data); }
            @Override public Optional<system.Toast2<IBuilder, Double>> model() { return modelData; }
        };
    }
}