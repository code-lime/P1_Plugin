package org.lime.gp.block.component.display.block;

import java.util.Optional;

import org.lime.display.Models;

import net.minecraft.world.level.block.state.IBlockData;

public interface IBlock {
    Optional<IBlockData> data();

    default IModelBlock withModel(Models.Model model, int distanceChunk) { return IModelBlock.of(data().orElse(null), model, distanceChunk); }

    static IBlock of(IBlockData data) { return () -> Optional.ofNullable(data); }
}