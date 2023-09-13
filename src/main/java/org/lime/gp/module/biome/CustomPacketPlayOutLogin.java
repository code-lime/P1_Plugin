package org.lime.gp.module.biome;

import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketListenerPlayOut;
import net.minecraft.network.protocol.game.PacketPlayOutLogin;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.World;
import net.minecraft.world.level.dimension.DimensionManager;
import org.bukkit.NamespacedKey;
import org.lime.gp.extension.JsonNBT;
import org.lime.gp.lime;
import org.lime.system;

import java.util.*;

//private static final RegistryOps<NBTBase> BUILTIN_CONTEXT_OPS = RegistryOps.create(DynamicOpsNBT.INSTANCE, IRegistryCustom.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
public record CustomPacketPlayOutLogin(
        int playerId,
        boolean hardcore,
        EnumGamemode gameType,
        EnumGamemode previousGameType,
        Set<ResourceKey<World>> levels,
        IRegistryCustom.Dimension registryHolder,
        ResourceKey<DimensionManager> dimensionType,
        ResourceKey<World> dimension,
        long seed,
        int maxPlayers,
        int chunkRadius,
        int simulationDistance,
        boolean reducedDebugInfo,
        boolean showDeathScreen,
        boolean isDebug,
        boolean isFlat,
        Iterable<BiomeModify.IAction> actions
) implements Packet<PacketListenerPlayOut> {
    /*public CustomPacketPlayOutLogin(PacketDataSerializer buf) {
        this(buf.readInt(), buf.readBoolean(), EnumGamemode.byId(buf.readByte()), EnumGamemode.byNullableId(buf.readByte()), buf.readCollection(Sets::newHashSetWithExpectedSize, b2 -> ResourceKey.create(IRegistry.DIMENSION_REGISTRY, b2.readResourceLocation())), buf.readWithCodec(BuiltInRegistries.NETWORK_CODEC).freeze(), buf.readWithCodec(DimensionManager.CODEC), ResourceKey.create(IRegistry.DIMENSION_REGISTRY, buf.readResourceLocation()), buf.readLong(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }*/
    public CustomPacketPlayOutLogin(PacketPlayOutLogin other, Iterable<BiomeModify.IAction> actions) {
        this(other.playerId(),
                other.hardcore(),
                other.gameType(),
                other.previousGameType(),
                other.levels(),
                other.registryHolder(),
                other.dimensionType(),
                other.dimension(),
                other.seed(),
                other.maxPlayers(),
                other.chunkRadius(),
                other.simulationDistance(),
                other.reducedDebugInfo(),
                other.showDeathScreen(),
                other.isDebug(),
                other.isFlat(),
                actions);
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeInt(this.playerId);
        buf.writeBoolean(this.hardcore);
        buf.writeByte(this.gameType.getId());
        buf.writeByte(EnumGamemode.getNullableId(this.previousGameType));
        buf.writeCollection(this.levels, PacketDataSerializer::writeResourceKey);

        NBTTagCompound registry = BiomeModify.nbt(RegistrySynchronization.NETWORK_CODEC, this.registryHolder);
        NBTTagList list = registry.getCompound("minecraft:worldgen/biome").getList("value", NBTBase.TAG_COMPOUND);
        List<NBTTagCompound> globalList = new ArrayList<>();
        system.Toast1<Integer> maxID = system.toast(-1);
        list.forEach(_item -> {
            NBTTagCompound item = (NBTTagCompound) _item;
            maxID.val0 = Math.max(maxID.val0, item.getInt("id"));
            globalList.add(item);
        });
        list.clear();
        globalList.forEach(item -> {
            NBTTagCompound vannila = item.copy();
            NBTTagCompound lime = item.copy();

            int id = item.getInt("id");
            String key = vannila.getString("name");
            String[] args = key.split(":", 2);
            String name = args[1];
            vannila.putInt("id", id + maxID.val0 + 1);
            lime.putString("name", "lime:" + name);
            NBTTagCompound element = lime.getCompound("element");
            NBTTagCompound original = element.copy();
            actions.forEach(action -> {
                if (action instanceof BiomeModify.GenerateAction generate) {
                    generate
                            .generate(id, key, original)
                            .forEach(kv -> kv.invoke((index, _element, group) -> {
                                NBTTagCompound generated = item.copy();
                                generated.putInt("id", index);
                                generated.putString("name", "lime" + ":" + group + "/" + name);
                                generated.put("element", _element);
                                list.add(generated);
                            }));
                }
                if (action instanceof BiomeModify.ModifyAction modify)
                    modify.modify(id, name, element);
            });

            list.add(vannila);
            list.add(lime);
        });
        lime.writeAllConfig("tmp.codec", system.toFormat(JsonNBT.toJson(registry)));
        buf.writeNbt(registry);


        buf.writeResourceKey(this.dimensionType);
        buf.writeResourceKey(this.dimension);
        buf.writeLong(this.seed);
        buf.writeVarInt(this.maxPlayers);
        buf.writeVarInt(this.chunkRadius);
        buf.writeVarInt(this.simulationDistance);
        buf.writeBoolean(this.reducedDebugInfo);
        buf.writeBoolean(this.showDeathScreen);
        buf.writeBoolean(this.isDebug);
        buf.writeBoolean(this.isFlat);
        buf.writeOptional(Optional.empty(), PacketDataSerializer::writeGlobalPos);
    }

    @Override
    public void handle(PacketListenerPlayOut packetListener) {
    }
}
