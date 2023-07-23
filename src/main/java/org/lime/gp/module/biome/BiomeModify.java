package org.lime.gp.module.biome;

import com.comphenix.protocol.events.PacketContainer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.handler.codec.EncoderException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.EnumProtocol;
import net.minecraft.network.protocol.EnumProtocolDirection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutLogin;
import net.minecraft.server.MinecraftServer;
import org.lime.core;
import org.lime.gp.access.ReflectionAccess;
import org.lime.gp.extension.PacketManager;
import org.lime.reflection;
import org.lime.system;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class BiomeModify {
    public static core.element create() {
        return core.element.create(BiomeModify.class)
                .withInit(BiomeModify::init);
    }

    private static final ConcurrentHashMap<UUID, IAction> actions = new ConcurrentHashMap<>();
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
                    event.setPacket(new PacketContainer(event.getPacketType(), new CustomPacketPlayOutLogin(packet, actions.values())));
                })
                .listen();
    }

    public interface IAction { }
    public interface ModifyAction extends IAction { void modify(int id, String name, NBTTagCompound element); }
    public interface GenerateAction extends IAction { Stream<system.Toast3<Integer, NBTTagCompound, String>> generate(int id, String key, NBTTagCompound element); }
    public interface ActionCloseable extends Closeable { @Override void close(); }

    public static ActionCloseable appendModify(ModifyAction action) {
        UUID uuid = UUID.randomUUID();
        actions.put(uuid, action);
        return () -> actions.remove(uuid);
    }
    public static ActionCloseable appendGenerate(GenerateAction action) {
        UUID uuid = UUID.randomUUID();
        actions.put(uuid, action);
        return () -> actions.remove(uuid);
    }
    public static <T>NBTTagCompound nbt(Codec<T> codec, T object) {
        DataResult<NBTBase> dataresult = codec.encodeStart(DynamicOpsNBT.INSTANCE, object);
        dataresult.error().ifPresent(partialresult -> {
            String s2 = partialresult.message();
            throw new EncoderException("Failed to encode: " + s2 + " " + object);
        });
        return (NBTTagCompound)dataresult.result().get();
    }
    public static Stream<NBTTagCompound> getRawVanillaBiomes() {
        return nbt(RegistrySynchronization.NETWORK_CODEC,
                ReflectionAccess.synchronizedRegistries_PlayerList.get(MinecraftServer.getServer().getPlayerList()))
                .getCompound("minecraft:worldgen/biome")
                .getList("value", NBTBase.TAG_COMPOUND)
                .stream()
                .map(v -> (NBTTagCompound)v);
    }
}































