package org.lime.gp.block.component.display.block;

import java.util.Optional;

import org.lime.display.Models;

import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.world.level.block.state.IBlockData;

public interface ITileModelBlock extends ITileBlock, IModelBlock {
    @Override default ITileModelBlock withModel(Models.Model model, int distanceChunk) { return this; }

    static ITileModelBlock of(IBlockData data, PacketPlayOutTileEntityData packet, Models.Model model, int distanceChunk) {
        return new ITileModelBlock() {
            @Override public Optional<PacketPlayOutTileEntityData> packet() { return Optional.ofNullable(packet); }
            @Override public int distanceChunk() { return distanceChunk; }
            @Override public Optional<IBlockData> data() { return Optional.ofNullable(data); }
            @Override public Optional<Models.Model> model() { return Optional.ofNullable(model); }
        };
    }
}