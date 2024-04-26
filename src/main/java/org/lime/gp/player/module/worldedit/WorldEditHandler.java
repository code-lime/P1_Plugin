package org.lime.gp.player.module.worldedit;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.LimeKey;
import net.minecraft.world.level.border.WorldBorder;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinStringTag;
import org.enginehub.linbus.tree.LinTag;
import org.enginehub.linbus.tree.LinTagType;
import org.lime.gp.block.component.data.MultiBlockInstance;
import org.lime.gp.block.component.data.OtherGenericInstance;
import org.lime.system.execute.Func1;
import org.lime.system.json;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldEditHandler extends AbstractDelegateExtent {
    private final @Nullable WorldBorder border;
    private final boolean modeLock;
    private final CraftPlayer player;
    private final Map<Toast2<BlockPosition, UUID>, UUID> map = new HashMap<>();

    private boolean singleWarning = true;

    protected WorldEditHandler(CraftWorld world, CraftPlayer player, Extent extent) {
        super(extent);
        this.player = player;
        this.modeLock = !player.isOp() && player.getGameMode() != GameMode.CREATIVE;
        this.border = player.isOp() ? null : world.getHandle().getWorldBorder();
    }

    private static LimeKey getKey(LinStringTag data) {
        String[] args = data.value().split(" ", 2);
        return new LimeKey(UUID.fromString(args[0]), args[1]);
    }
    private static LinStringTag getKey(LimeKey data) {
        return LinStringTag.of(data.uuid() + " " + data.type());
    }

    private UUID updateUuid(BlockPosition position, UUID uuid) {
        return map.computeIfAbsent(Toast.of(position, uuid), v -> UUID.randomUUID());
    }

    private static <Tag extends LinTag<?>, T>void tryUpdateTag(
            LinCompoundTag nbt,
            LinCompoundTag.Builder builder,
            String name,
            LinTagType<Tag> tagType,
            Func1<Tag, Tag> map
    ) {
        Tag nbtValue = nbt.findTag(name, tagType);
        if (nbtValue != null)
            builder.put(name, map.invoke(nbtValue));
    }

    private BaseBlock tryModifyNbt(BlockPosition position, BaseBlock block) {
        if (block.getBlockType() != BlockTypes.SKELETON_SKULL) return block;
        LinCompoundTag nbt = block.getNbt();
        if (nbt == null) return block;
        LinCompoundTag nbtBukkitValues = nbt.findTag("PublicBukkitValues", LinTagType.compoundTag());
        if (nbtBukkitValues == null) return block;
        LinStringTag nbtKey = nbtBukkitValues.findTag("lime:custom_block", LinTagType.stringTag());
        if (nbtKey == null) return block;
        LimeKey key = getKey(nbtKey);

        LinCompoundTag.Builder builderBukkitValues = nbtBukkitValues.toBuilder();

        builderBukkitValues.put("lime:custom_block", getKey(new LimeKey(updateUuid(position, key.uuid()), key.type())));

        tryUpdateTag(nbtBukkitValues, builderBukkitValues,
                "lime:multiblock", LinTagType.stringTag(),
                v -> LinStringTag.of(MultiBlockInstance.mapBlockUuids(json.parse(v.value()).getAsJsonObject(), position, this::updateUuid).toString()));
        tryUpdateTag(nbtBukkitValues, builderBukkitValues,
                "lime:other.generic", LinTagType.stringTag(),
                v -> LinStringTag.of(OtherGenericInstance.mapBlockUuids(json.parse(v.value()).getAsJsonObject(), position, this::updateUuid).toString()));

        nbt = nbt.toBuilder().put("PublicBukkitValues", builderBukkitValues.build()).build();
        return block.toBaseBlock(nbt);
    }

    @Override public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws WorldEditException {
        if (modeLock) {
            if (singleWarning) {
                player.sendMessage(Component.text("Невозможно размещать блоки в этом игровом режиме").color(NamedTextColor.RED));
                singleWarning = false;
            }
            return false;
        }

        BlockPosition position = new BlockPosition(location.x(), location.y(), location.z());
        if (border != null && !border.isWithinBounds(position)) {
            if (singleWarning) {
                player.sendMessage(Component.text("Невозможно размещать блоки за границей мира").color(NamedTextColor.RED));
                singleWarning = false;
            }
            return false;
        }

        return block instanceof BaseBlock baseBlock
                ? super.setBlock(location, tryModifyNbt(position, baseBlock))
                : super.setBlock(location, block);
    }
}