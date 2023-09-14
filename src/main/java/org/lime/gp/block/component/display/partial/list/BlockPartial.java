package org.lime.gp.block.component.display.partial.list;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftMagicNumbers;
import org.jetbrains.annotations.Nullable;
import org.lime.docs.IIndexDocs;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.system.map;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.block.component.display.block.IBlock;
import org.lime.gp.block.component.display.block.ITileBlock;
import org.lime.gp.block.component.display.partial.Partial;
import org.lime.gp.block.component.display.partial.PartialEnum;
import org.lime.gp.extension.JsonNBT;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.network.chat.ChatHexColor;
import net.minecraft.world.level.block.BlockSkullShapeInfo;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockPartial extends Partial implements CustomTileMetadata.Shapeable {
    private final Material material;
    private final IBlockData blockData;
    private final TileEntityTypes<?> type;
    private final JsonNBT.DynamicNBT<NBTTagCompound> nbt;

    private final boolean hasCollision;

    public Material material() { return this.material; }
    public IBlockData blockData() { return this.blockData; }
    public TileEntityTypes<?> entity() { return this.type; }
    public JsonNBT.DynamicNBT<NBTTagCompound> nbt() { return this.nbt; }
    public boolean hasCollision() { return this.hasCollision; }

    public BlockPartial(int distanceChunk, IBlockData blockData) {
        super(distanceChunk, new JsonObject());
        this.material = blockData.getBukkitMaterial();
        this.blockData = blockData;
        this.type = null;
        this.nbt = null;
        this.hasCollision = blockData.getBlock().hasCollision;
    }
    public BlockPartial(int distanceChunk, JsonObject json) {
        super(distanceChunk, json);
        this.material = Material.valueOf(json.get("material").getAsString());
        IBlockData blockData = CraftMagicNumbers
                .getBlock(material)
                .defaultBlockState();
        if (json.has("states")) {
            HashMap<String, IBlockState<?>> states = map.<String, IBlockState<?>>of()
                    .add(blockData.getProperties(), IBlockState::getName, v -> v)
                    .build();
            for (Map.Entry<String, JsonElement> kv : json.get("states").getAsJsonObject().entrySet()) {
                IBlockState<?> state = states.getOrDefault(kv.getKey(), null);
                if (state == null) continue;
                blockData = BlockInfo.setValue(blockData, state, kv.getValue().getAsString());
            }
        }
        this.blockData = blockData;
        this.nbt = json.has("nbt") ? JsonNBT.toDynamicNBT(json.getAsJsonObject("nbt"), List.of("{color}")) : null;
        this.type = this.nbt == null ? null : BuiltInRegistries.BLOCK_ENTITY_TYPE.stream().filter(v -> v.isValid(this.blockData)).findAny().orElse(null);
        this.hasCollision = !json.has("has_collision") || json.get("has_collision").getAsBoolean();
    }

    @Override public PartialEnum type() { return PartialEnum.Block; }
    @Override public String toString() { return blockData + (nbt == null ? "" : GameProfileSerializer.structureToSnbt(nbt.build(Collections.emptyMap())))+super.toString(); }

    @Override public @Nullable VoxelShape onShape(CustomTileMetadata metadata, BlockSkullShapeInfo event) {
        return this.hasCollision ? blockData.getShape(event.world(), event.pos(), event.context()) : VoxelShapes.empty();
    }

    private final ConcurrentHashMap<String, NBTTagCompound> variableConvert = new ConcurrentHashMap<>();
    private NBTTagCompound ofVariable(Map<String, String> variable) {
        String color = variable.getOrDefault("display.color", "FFFFFF");
        String key = "display.color=" + color;
        return variableConvert.computeIfAbsent(key, (_key) -> {
            HashMap<String, NBTBase> map = new HashMap<>();
            map.put("{color}", NBTTagInt.valueOf(ChatHexColor.parseColor("#" + color).getValue()));
            return nbt.build(map);
        });
    }
    public IBlock getDynamicDisplay(BlockPosition position, Map<String, String> variable) {
        return type == null
                ? IBlock.of(blockData)
                : ITileBlock.of(blockData, ReflectionAccess.init_PacketPlayOutTileEntityData.newInstance(position, type, ofVariable(variable)));
    }

    public static JObject docs(IDocsLink docs, IIndexDocs variable) {
        return Partial.docs(docs, variable).addFirst(
                JProperty.require(IName.raw("material"), IJElement.link(docs.vanillaMaterial()), IComment.raw("Отображаемый блок")),
                JProperty.optional(IName.raw("states"), IJElement.anyObject(
                        JProperty.require(IName.raw("BLOCK_STATE_KEY"), IJElement.raw("BLOCK_STATE_VALUE"))
                ), IComment.text("Параметры отображаемого блока. Узнать параметры блока можно в F3")),
                JProperty.optional(IName.raw("nbt"), IJElement.link(docs.dynamicNbt()), IComment.empty()
                        .append(IComment.text("NBT блока с передаваемыми параметрами: "))
                        .append(IComment.raw("{color}"))),
                JProperty.optional(IName.raw("has_collision"), IJElement.bool(), IComment.text("Указывает, просчитывать ли колизию для блока или оставить просчет только на стороне клиента"))
        );
    }
}











