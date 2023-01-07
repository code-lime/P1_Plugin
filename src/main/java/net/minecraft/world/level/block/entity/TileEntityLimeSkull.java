package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.LimeKey;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class TileEntityLimeSkull extends TileEntitySkull {
    public TileEntityLimeSkull(BlockPosition pos, IBlockData state) {
        super(pos, state);
    }

    @Nullable private LimeKey key = null;

    public Optional<LimeKey> customKey() { return Optional.ofNullable(key); }
    public Optional<UUID> customUUID() { return customKey().map(LimeKey::uuid); }
    public Optional<String> customType() { return customKey().map(LimeKey::type); }

    @Override public void setRemoved() {
        TileEntitySkullEventRemove.execute(this);
        super.setRemoved();
    }
    @Override public void load(NBTTagCompound nbt) {
        super.load(nbt);
        metadata = null;
        key = LimeKey.getKey(persistentDataContainer, LimeKey.KeyType.CUSTOM_BLOCK).orElse(null);
    }

    @Nullable private ITickMetadata metadata = null;
    public @Nullable ITickMetadata getMetadata() { return metadata; }
    public void setMetadata(@Nullable ITickMetadata value) { metadata = value; }
    public void removeMetadata() { metadata = null; }

    public void onTick(World world, BlockPosition pos, IBlockData state) {
        try {
            if (metadata != null && metadata.isLoaded()) metadata.onTick(new TileEntitySkullTickInfo(world, pos, state, this));
            else TileEntitySkullPreTickEvent.execute(world, pos, state, this);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void onDestroy(BlockSkullDestroyInfo info) {
        try {
            if (metadata != null) metadata.onDestroy(info);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public @Nullable EnumInteractionResult onInteract(BlockSkullInteractInfo info) {
        try {
            return metadata == null ? null : metadata.onInteract(info);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
    public @Nullable IBlockData onPlace(BlockSkullPlaceInfo info) {
        try {
            return metadata == null ? null : metadata.onPlace(info);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
    public @Nullable VoxelShape onShape(BlockSkullShapeInfo info) {
        try {
            return metadata == null ? null : metadata.onShape(info);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
    public @Nullable IBlockData onState(BlockSkullStateInfo info) {
        try {
            return metadata == null ? null : metadata.onState(info);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
}













