package org.lime.gp.block.component.display.block;

import java.util.Optional;

import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.world.level.block.state.IBlockData;
import org.lime.display.models.shadow.IBuilder;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

public interface ITileModelBlock extends ITileBlock, IModelBlock {
    @Override default ITileModelBlock withModel(IBuilder model, int distanceChunk, double distanceModel) { return this; }

    static ITileModelBlock of(IBlockData data, PacketPlayOutTileEntityData packet, IBuilder model, int distanceChunk, double distanceModel) {
        Optional<Toast2<IBuilder, Double>> modelData = model == null ? Optional.empty() : Optional.of(Toast.of(model, distanceModel));
        return new ITileModelBlock() {
            @Override public Optional<PacketPlayOutTileEntityData> packet() { return Optional.ofNullable(packet); }
            @Override public int distanceChunk() { return distanceChunk; }
            @Override public Optional<IBlockData> data() { return Optional.ofNullable(data); }
            @Override public Optional<Toast2<IBuilder, Double>> model() { return modelData; }
        };
    }
}