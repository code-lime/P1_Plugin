package org.lime.gp.module.holiday;

import com.comphenix.protocol.events.PacketContainer;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.handler.codec.EncoderException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.EnumProtocol;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.EnumProtocolDirection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketListenerPlayOut;
import net.minecraft.network.protocol.game.PacketPlayOutLogin;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.World;
import net.minecraft.world.level.dimension.DimensionManager;
import org.lime.core;
import org.lime.gp.extension.PacketManager;
import org.lime.reflection;
import org.lime.system;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BiomeModify {
    public static core.element create() {
        return core.element.create(BiomeModify.class)
                .withInit(BiomeModify::init);
    }

    private static final ConcurrentHashMap<UUID, ModifyAction> actions = new ConcurrentHashMap<>();
    public static void init() {
        reflection.dynamic<?> flow = reflection.dynamic.ofValue(reflection.dynamic.ofValue(EnumProtocol.PLAY)
                .<Map<EnumProtocolDirection, Object>>getMojang("flows").value
                .get(EnumProtocolDirection.CLIENTBOUND));
        ProxyObject2IntOpenHashMap<Class<? extends Packet<?>>> classToId = ProxyObject2IntOpenHashMap.proxy(flow.<Object2IntMap<Class<? extends Packet<?>>>>getMojang("classToId").value);
        classToId.append.put(CustomPacketPlayOutLogin.class, classToId.getInt(PacketPlayOutLogin.class));
        flow.setMojang("classToId", classToId);

        PacketManager.adapter()
                .add(PacketPlayOutLogin.class, (packet, event) -> {
                    if (actions.isEmpty()) return;
                    event.setPacket(new PacketContainer(event.getPacketType(), new CustomPacketPlayOutLogin(packet)));
                })
                .listen();
    }

    public interface ModifyAction {
        void modify(int id, String name, NBTTagCompound element);
    }
    public interface ModifyActionCloseable extends Closeable {
        @Override void close();
    }

    private static final RegistryOps<NBTBase> BUILTIN_CONTEXT_OPS = RegistryOps.create(DynamicOpsNBT.INSTANCE, IRegistryCustom.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
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
            boolean isFlat
    ) implements Packet<PacketListenerPlayOut> {
        /*public CustomPacketPlayOutLogin(PacketDataSerializer buf) {
            this(buf.readInt(), buf.readBoolean(), EnumGamemode.byId(buf.readByte()), EnumGamemode.byNullableId(buf.readByte()), buf.readCollection(Sets::newHashSetWithExpectedSize, b2 -> ResourceKey.create(IRegistry.DIMENSION_REGISTRY, b2.readResourceLocation())), buf.readWithCodec(BuiltInRegistries.NETWORK_CODEC).freeze(), buf.readWithCodec(DimensionManager.CODEC), ResourceKey.create(IRegistry.DIMENSION_REGISTRY, buf.readResourceLocation()), buf.readLong(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
        }*/
        public CustomPacketPlayOutLogin(PacketPlayOutLogin other) {
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
                    other.isFlat());
        }
        @Override public void write(PacketDataSerializer buf) {
            buf.writeInt(this.playerId);
            buf.writeBoolean(this.hardcore);
            buf.writeByte(this.gameType.getId());
            buf.writeByte(EnumGamemode.getNullableId(this.previousGameType));
            buf.writeCollection(this.levels, PacketDataSerializer::writeResourceKey);

            NBTTagCompound registry = nbt(RegistrySynchronization.NETWORK_CODEC, this.registryHolder);
            NBTTagList list = registry.getCompound("minecraft:worldgen/biome").getList("value", NBTBase.TAG_COMPOUND);
            List<NBTTagCompound> globalList = new ArrayList<>();
            system.Toast1<Integer> maxID = system.toast(-1);
            list.forEach(_item -> {
                NBTTagCompound item = (NBTTagCompound)_item;
                maxID.val0 = Math.max(maxID.val0, item.getInt("id"));
                globalList.add(item);
            });
            list.clear();
            globalList.forEach(item -> {
                NBTTagCompound vannila = item.copy();
                NBTTagCompound lime = item.copy();

                int id = item.getInt("id");
                String[] args = vannila.getString("name").split(":", 2);
                String name = args[1];
                vannila.putInt("id", id + maxID.val0 + 1);
                lime.putString("name", "lime:" + name);
                NBTTagCompound element = lime.getCompound("element");
                actions.values().forEach(action -> action.modify(id, name, element));

                list.add(vannila);
                list.add(lime);
            });
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
        @Override public void handle(PacketListenerPlayOut packetListener) { }
        private static <T>NBTTagCompound nbt(Codec<T> codec, T object) {
            DataResult<NBTBase> dataresult = codec.encodeStart(DynamicOpsNBT.INSTANCE, object);
            dataresult.error().ifPresent(partialresult -> {
                String s2 = partialresult.message();
                throw new EncoderException("Failed to encode: " + s2 + " " + object);
            });
            return (NBTTagCompound)dataresult.result().get();
        }
    }
    private static class ProxyObject2IntOpenHashMap<T> extends Object2IntOpenHashMap<T> {
        public final HashMap<T, Integer> append = new HashMap<>();
        public ProxyObject2IntOpenHashMap(Object2IntMap<T> map) {
            super(map);
            defaultReturnValue(map.defaultReturnValue());
        }
        @Override public int getInt(Object k) {
            Integer ret = append.get(k);
            return ret == null ? super.getInt(k) : ret;
        }

        public static <T>ProxyObject2IntOpenHashMap<T> proxy(Object2IntMap<T> map) {
            return new ProxyObject2IntOpenHashMap<>(map);
        }
    }

    public static ModifyActionCloseable appendModify(ModifyAction action) {
        UUID uuid = UUID.randomUUID();
        actions.put(uuid, action);
        return () -> actions.remove(uuid);
    }
}































