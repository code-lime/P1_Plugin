package org.lime.gp.block.component.display.block;

import java.util.Optional;

import net.minecraft.world.level.block.state.IBlockData;
import org.lime.display.models.Model;

public interface IBlock {
    Optional<IBlockData> data();

    default IModelBlock withModel(Model model, int distanceChunk, double distanceModel) { return IModelBlock.of(data().orElse(null), model, distanceChunk, distanceModel); }

    static IBlock of(IBlockData data) { return () -> Optional.ofNullable(data); }
}