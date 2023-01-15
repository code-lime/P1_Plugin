package org.lime.gp.block.component.display.block;

import java.util.Optional;

import org.lime.display.Models;

import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.world.level.block.state.IBlockData;

public interface ITileBlock extends IBlock {
    Optional<PacketPlayOutTileEntityData> packet();

    @Override default ITileModelBlock withModel(Models.Model model, int distanceChunk) { return ITileModelBlock.of(data().orElse(null), packet().orElse(null), model, distanceChunk); }

    static ITileBlock of(IBlockData data, PacketPlayOutTileEntityData packet) {
        return new ITileBlock() {
            @Override public Optional<IBlockData> data() { return Optional.ofNullable(data); }
            @Override public Optional<PacketPlayOutTileEntityData> packet() { return Optional.ofNullable(packet); }
        };
    }
}