package org.lime.gp.player.voice;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoderMode;
import de.maxhenkel.voicechat.api.packets.ConvertablePacket;
import de.maxhenkel.voicechat.api.packets.LocationalSoundPacket;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import de.maxhenkel.voicechat.api.packets.SoundPacket;
import de.maxhenkel.voicechat.plugins.PluginManager;
import de.maxhenkel.voicechat.plugins.impl.*;
import de.maxhenkel.voicechat.plugins.impl.packets.SoundPacketImpl;
import de.maxhenkel.voicechat.voice.common.LocationSoundPacket;
import de.maxhenkel.voicechat.voice.common.NetworkMessage;
import de.maxhenkel.voicechat.voice.common.PlayerState;
import de.maxhenkel.voicechat.voice.server.ClientConnection;
import de.maxhenkel.voicechat.voice.server.PlayerStateManager;
import de.maxhenkel.voicechat.voice.server.ServerWorldUtils;
import io.netty.buffer.Unpooled;
import net.kyori.adventure.text.Component;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.storage.WorldData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lime.core;
import org.lime.gp.player.module.Death;
import org.lime.plugin.CoreElement;
import org.lime.gp.player.menu.LangEnum;
import org.lime.system;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.block.component.data.voice.RadioInstance;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.extension.Cooldown;
import org.lime.gp.extension.MapUUID;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.list.MegaPhoneSetting;
import org.lime.gp.item.settings.list.RadioSetting;
import org.lime.gp.lime;
import org.lime.gp.module.TimeoutData;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.player.ui.CustomUI;
import org.lime.gp.player.ui.ImageBuilder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class Voice implements VoicechatPlugin {
    private static OpusEncoderMode MODE = OpusEncoderMode.VOIP;

    public static class Opus implements OpusEncoder, OpusDecoder {
        public final OpusEncoder encoder;
        public final OpusDecoder decoder;
        public Opus(VoicechatApi api) {
            encoder = api.createEncoder(MODE);
            decoder = api.createDecoder();
        }

        @Override public short[] decode(@Nullable byte[] bytes) { return decoder.decode(bytes); }
        @Override public byte[] encode(short[] shorts) { return encoder.encode(shorts); }
        @Override public void resetState() {
            decoder.resetState();
            encoder.resetState();
        }
        @Override public boolean isClosed() {
            return decoder.isClosed() || encoder.isClosed();
        }
        @Override public void close() {
            decoder.close();
            encoder.close();
        }
    }

    private static final Voice Instance = new Voice();
    private static PlayerStateManager PLAYER_STATE_MANAGER;
    public static CoreElement create() {
        return CoreElement.create(Voice.class)
                .withInit(Voice::init)
                .withUninit(Voice::uninit)
                .addCommand("voice", v -> v
                        .withTab((sender, args) -> switch (args.length) {
                            case 1 -> sender.isOp() ? List.of("reconnect", "reconnect-all") : List.of("reconnect");
                            case 2 -> switch (args[0]) {
                                case "reconnect" -> sender.isOp() ? Bukkit.getOnlinePlayers().stream().map(HumanEntity::getName).toList() : Collections.emptyList();
                                default -> Collections.emptyList();
                            };
                            default -> Collections.emptyList();
                        })
                        .withExecutor((sender, args) -> switch (args.length) {
                            case 1 -> switch (args[0]) {
                                case "reconnect" -> { reconnect(Collections.singleton((Player)sender)); yield true; }
                                case "reconnect-all" -> switch (sender.isOp() ? 1 : 0) {
                                    case 1 -> {
                                        sender.sendMessage("Reconnect all players...");
                                        reconnect(Bukkit.getOnlinePlayers());
                                        yield true;
                                    }
                                    default -> false;
                                };
                                default -> false;
                            };
                            case 2 -> switch (args[0]) {
                                case "reconnect" -> switch (sender.isOp() ? 1 : 0) {
                                    case 1 -> Optional.ofNullable(Bukkit.getPlayer(args[1]))
                                            .map(other -> {
                                                sender.sendMessage("Reconnect '"+other.getName()+"'...");
                                                reconnect(Collections.singleton(other));
                                                return true;
                                            })
                                            .orElseGet(() -> {
                                                sender.sendMessage("Player '"+args[1]+"' not founded!");
                                                return false;
                                            });
                                    default -> false;
                                };
                                default -> false;
                            };
                            default -> false;
                        })
                );
    }

    public static VoicechatApi API;
    //public static Opus OPUS;
    private static final MinecraftServer server = MinecraftServer.getServer();

    @SuppressWarnings("all")
    public static void reconnect(Collection<? extends Player> players) {
        Set<String> outgoingChannels = Bukkit.getMessenger().getOutgoingChannels(Voicechat.INSTANCE);

        WorldServer overworld = server.overworld();
        GameRules gamerules = overworld.getGameRules();
        WorldData worlddata = overworld.getLevelData();
        boolean RULE_DO_IMMEDIATE_RESPAWN = gamerules.getBoolean(GameRules.RULE_DO_IMMEDIATE_RESPAWN);
        boolean RULE_REDUCEDDEBUGINFO = gamerules.getBoolean(GameRules.RULE_REDUCEDDEBUGINFO);

        PlayerList playerList = server.getPlayerList();
        players.forEach(_player -> {
            if (!(_player instanceof CraftPlayer player)) return;
            for (String channel : outgoingChannels) player.addChannel(channel);
            EntityPlayer handle = player.getHandle();
            handle.connection.send(new PacketPlayOutLogin(
                    handle.getId(),
                    worlddata.isHardcore(),
                    handle.gameMode.getGameModeForPlayer(),
                    handle.gameMode.getPreviousGameModeForPlayer(),
                    server.levelKeys(),
                    server.registryAccess(),
                    overworld.dimensionTypeId(),
                    overworld.dimension(),
                    BiomeManager.obfuscateSeed(overworld.getSeed()),
                    server.getMaxPlayers(),
                    overworld.getChunkSource().chunkMap.playerChunkManager.getTargetSendDistance(),
                    overworld.getChunkSource().chunkMap.playerChunkManager.getTargetTickViewDistance(),
                    RULE_REDUCEDDEBUGINFO,
                    !RULE_DO_IMMEDIATE_RESPAWN,
                    overworld.isDebug(),
                    overworld.isFlat(),
                    Optional.empty())
            );
            Location loc = player.getLocation().clone();
            lime.nextTick(() -> {
                player.teleport(loc);
                WorldServer worldserver = handle.getLevel();

                //worldserver.addNewPlayer(handle);
                player.sendSupportedChannels();
                PlayerConnection playerconnection = handle.connection;
                playerconnection.send(new PacketPlayOutCustomPayload(PacketPlayOutCustomPayload.BRAND, new PacketDataSerializer(Unpooled.buffer()).writeUtf(server.getServerModName())));
                playerconnection.send(new PacketPlayOutServerDifficulty(worlddata.getDifficulty(), worlddata.isDifficultyLocked()));
                playerconnection.send(new PacketPlayOutAbilities(handle.getAbilities()));
                playerconnection.send(new PacketPlayOutHeldItemSlot(handle.getInventory().selected));
                playerconnection.send(new PacketPlayOutRecipeUpdate(server.getRecipeManager().getRecipes()));
                //playerconnection.send(new PacketPlayOutTags(TagNetworkSerialization.serializeTagsToNetwork(server.registryAccess())));
                playerList.sendPlayerPermissionLevel(handle);
                handle.getStats().markAllDirty();
                handle.getRecipeBook().sendInitialRecipeBook(handle);
                playerList.updateEntireScoreboard(worldserver.getScoreboard(), handle);

                playerList.respawn(handle, worldserver, true, loc, true, PlayerRespawnEvent.RespawnReason.PLUGIN);
            });
        });
    }

    public static int MAX_NOISE = 6000;
    public static int MIN_NOISE = 0;
    public static float VOICE_NOISE = 0.8f;
    public static float LERP_NOISE = 0.85f;

    public static void init() {
        AnyEvent.addEvent("opus.type", AnyEvent.type.owner_console, v -> v.createParam(OpusEncoderMode.values()), (p,v) -> {
            MODE = v;
            OPUS.clear();
        });
        AnyEvent.addEvent("voice.noise", AnyEvent.type.owner_console, v -> v.createParam("min","max","lerp","voice").createParam(Integer::parseInt, "[value]"), (p,type,value) -> {
            switch (type) {
                case "max": MAX_NOISE = value; break;
                case "min": MIN_NOISE = value; break;
                case "voice": VOICE_NOISE = value / 100.0f; break;
                case "lerp": LERP_NOISE = value / 100.0f; break;
            }
        });

        BukkitVoicechatService service = Bukkit.getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) service.registerPlugin(Instance);
        lime.once(() -> PLAYER_STATE_MANAGER = Voicechat.SERVER.getServer().getPlayerStateManager(), 5);
        CustomUI.addListener(new CustomUI.GUI(CustomUI.IType.ACTIONBAR) {
            @Override public Collection<ImageBuilder> getUI(Player player) {
                PlayerState state = PLAYER_STATE_MANAGER == null ? null : PLAYER_STATE_MANAGER.getState(player.getUniqueId());
                if ((state == null || state.isDisconnected()) && !player.getScoreboardTags().contains("voice.hide")) {
                    Component component = LangMessages.Message.Chat_NoVoiceActionBar.getSingleMessage();
                    return Collections.singletonList(ImageBuilder.of(component, ChatHelper.getTextSize(player, component), false));
                }
                return Collections.emptyList();
            }
        });
    }
    public static void uninit() {
        Bukkit.getServer().getServicesManager().unregister(Instance);
    }

    @Override public void initialize(VoicechatApi api) {
        API = api;
        //OPUS = new Opus(api);
    }
    @Override public String getPluginId() { return "lime"; }
    @Override public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, Voice::on);
    }

    public static void mute(UUID uuid) {
        Cooldown.setCooldown(uuid, "voice.mute", 5);
    }
    public static boolean isMute(UUID uuid) {
        return Cooldown.hasCooldown(uuid, "voice.mute") || Death.isDeathMute(uuid);
    }
    public static boolean isConnected(Player player) {
        return VoicechatConnectionImpl.fromPlayer(player) != null;
    }
    private static final ConcurrentHashMap<UUID, Opus> OPUS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Integer> NoiseModify = new ConcurrentHashMap<>();
    private static int lerp(int a, int b, float value) {
        return (int) (a + (b - a) * value);
    }
    public static byte[] modifyVolume(Radio.SenderInfo info, UUID id, byte[] audioSamples, int volume, double noise) {
        if (volume == 100 && noise == 0) return audioSamples;
        Opus opus = OPUS.computeIfAbsent(id, _id -> new Opus(API));

        if (volume < 0) volume = 0;
        else if (volume > 100) volume = 100;
        try {
            short[] shorts = opus.decode(audioSamples);
            for (int i = 0; i < shorts.length; i++)
            {
                int value = ((((int)shorts[i]) * volume) / 100);
                if (noise > 0) {
                    int delta = (int)(system.rand(MIN_NOISE, MAX_NOISE) * noise);
                    int ofDelta = NoiseModify.getOrDefault(id, delta);
                    delta = lerp(delta, ofDelta, LERP_NOISE);
                    NoiseModify.put(id, delta);
                    value -= (int)(value * VOICE_NOISE * noise);
                    value += delta;

                    shorts[i] = value < Short.MIN_VALUE ? Short.MIN_VALUE : value > Short.MAX_VALUE ? Short.MAX_VALUE : (short)value;
                } else {
                    shorts[i] = (short)value;
                }
            }
            return opus.encode(shorts);
        } catch (Throwable e) {
            Radio.logRadioError(info, e);
            return audioSamples;
        }
    }
    /*private static byte[] adjustVolume(byte[] audioSamples, float volume) {
        byte[] array = new byte[audioSamples.length];
        for (int i = 0; i < array.length; i+=2) {
            // convert byte pair to int
            short buf1 = audioSamples[i+1];
            short buf2 = audioSamples[i];

            buf1 = (short) ((buf1 & 0xff) << 8);
            buf2 = (short) (buf2 & 0xff);

            short res= (short) (buf1 | buf2);
            res = (short) (res * volume);

            // convert back
            array[i] = (byte) res;
            array[i+1] = (byte) (res >> 8);

        }
        return array;
    }*/

    public enum VoiceStatus {
        ACTIVE,
        OFFLINE,
        NONE
    }
    public static VoiceStatus voiceStatus(UUID uuid) {
        PlayerState state = PLAYER_STATE_MANAGER == null ? null : PLAYER_STATE_MANAGER.getState(uuid);
        if (state == null || state.isDisconnected()) return VoiceStatus.OFFLINE;
        return Cooldown.hasCooldown(uuid, "voice.active") ? VoiceStatus.ACTIVE : VoiceStatus.NONE;
    }

    public static void on(MicrophonePacketEvent event) {
        try {
            if (event.getSenderConnection() == null) return;
            if (!(event.getSenderConnection().getPlayer().getPlayer() instanceof Player player)) return;
            UUID uuid = player.getUniqueId();
            if (isMute(uuid)) {
                if (!Cooldown.hasCooldown(uuid, "voice.mute.display")) {
                    MenuCreator.showLang(player, LangEnum.CHAT, Apply.of().add("text", "."));
                    Cooldown.setCooldown(uuid, "voice.mute.display", 5);
                }
                event.cancel();
                return;
            }

            MicrophonePacket packet = event.getPacket();
            ItemStack item = player.getInventory().getItemInMainHand();
            byte[] bytes = packet.getOpusEncodedData();
            Cooldown.setCooldown(uuid, "voice.active", 0.25);
            system.Toast1<Boolean> use = system.toast(false);
            Location location = player.getLocation();
            Items.getOptional(RadioSetting.class, item)
                .ifPresent(setting -> RadioData.getData(item)
                    .filter(v -> v.enable)
                    .filter(v -> v.state.isInput)
                    .ifPresent(data -> {
                        use.val0 = true;
                        Radio.SenderInfo sender = Radio.SenderInfo.player(uuid, location.toVector(), setting.noise);
                        Radio.playRadio(sender, location, data.total_distance, data.level, modifyVolume(sender, MapUUID.of("radio.input", uuid), bytes, data.volume, 0));
                    }));
            TimeoutData.values(RadioInstance.RadioVoiceData.class)
                    .filter(v -> v.state.isInput)
                    .filter(v -> v.isDistance(location, 3))
                    .filter(v -> !TimeoutData.has(v.unique, Radio.RadioLockTimeout.class))
                    .forEach(v -> {
                        Radio.SenderInfo sender = Radio.SenderInfo.player(uuid, v.location.toVector(), v.component.noise);
                        Radio.playRadio(sender, v.location, v.total_distance, v.level, modifyVolume(sender, MapUUID.of("radio.block.input", uuid, v.unique), bytes, v.volume, 0));
                    });

            Items.getOptional(MegaPhoneSetting.class, item)
                    .flatMap(v -> MegaPhoneData.getData(item))
                    .ifPresent(data -> {
                        use.val0 = true;
                        event.cancel();

                        sendLocationPacket(
                                location.getWorld(),
                                packet.locationalSoundPacketBuilder()
                                        .opusEncodedData(modifyVolume(Radio.SenderInfo.player(uuid, location.toVector(), false), MapUUID.of("voice.megaphone", uuid), bytes, data.volume, 0))
                                        .position(new PositionImpl(location.getX(), location.getY(), location.getZ()))
                                        .distance(data.distance)
                                        .build(),
                                false
                        );
                    });
            
            if (use.val0 && !Cooldown.hasOrSetCooldown(uuid, "voice.use.lock", 1)) {
                lime.invokeSync(() -> {
                    ItemStack syncItem = player.getInventory().getItemInMainHand();
                    if (!syncItem.isSimilar(item)) return;
                    Items.hurt(syncItem, player, 1);
                    player.getInventory().setItemInMainHand(syncItem);
                });
            }
        } catch (Throwable e) {
            lime.logStackTrace(e);
        }
    }

    public static void playWithDistance(World world, ConvertablePacket packet, double distance) {
        withDistance(packet, distance).ifPresent(v -> v.send(world));
    }
    public static void playWithDistance(Collection<Player> players, ConvertablePacket packet, double distance) {
        withDistance(packet, distance).ifPresent(v -> v.send(players));
    }

    @SuppressWarnings("deprecation")
    private static Optional<DistanceAudioPacket> withDistance(ConvertablePacket packet, double distance) {
        return Optional.ofNullable(packet instanceof SoundPacket sound ? sound : null)
                .map(SoundPacket::getSender)
                .or(() -> Optional.ofNullable(packet instanceof MicrophonePacket microphone ? microphone : null)
                        .map(v -> v.toLocationalSoundPacket(new PositionImpl(0,0,0)))
                        .map(SoundPacket::getSender)
                )
                .flatMap(uuid -> packet instanceof LocationalSoundPacket locationSound
                        ? Optional.of(locationSound.getPosition())
                        : Optional.ofNullable(Bukkit.getEntity(uuid)).map(Entity::getLocation).map(PositionImpl::new)
                )
                .map(packet::toLocationalSoundPacket)
                .map(v -> new DistanceAudioPacket(v, distance));
    }

    public static Stream<ServerPlayer> getPlayersRange(ServerLevel level, Position pos, double range) {
        if (pos instanceof PositionImpl p) {
            return ServerWorldUtils.getPlayersInRange((World) level.getServerLevel(), p.getPosition(), range, player -> true)
                    .stream()
                    .map(ServerPlayerImpl::new);
        } else {
            throw new IllegalArgumentException("Position is not an instance of PositionImpl");
        }
    }
    public static Stream<ServerPlayer> getPlayersRange(World world, Location pos, double range) {
        return ServerWorldUtils.getPlayersInRange(world, pos, range, player -> true)
                .stream()
                .map(ServerPlayerImpl::new);
    }
    public static Optional<VoicechatConnection> getConnectionOf(UUID playerUuid) {
        return Optional.ofNullable(Bukkit.getPlayer(playerUuid))
                .map(VoicechatConnectionImpl::fromPlayer);
    }
    public static void sendPacket(VoicechatConnection connection, SoundPacket packet) {
        if (packet instanceof SoundPacketImpl impl) sendPacket(connection, impl.getPacket());
    }
    public static void sendPacket(VoicechatConnection connection, de.maxhenkel.voicechat.voice.common.SoundPacket<?> packet) {
        de.maxhenkel.voicechat.voice.server.Server server = Voicechat.SERVER.getServer();
        if (server == null) return;
        PlayerState state = server.getPlayerStateManager().getState(connection.getPlayer().getUuid());
        if (state == null) return;
        if (PluginManager.instance().onSoundPacket(null, null, (Player)connection.getPlayer().getPlayer(), state, packet, "plugin")) return;
        ClientConnection client = server.getConnection(connection.getPlayer().getUuid());
        if (client == null) return;
        try { client.send(server, new NetworkMessage(packet)); }
        catch (Exception var6) { var6.printStackTrace(); }
    }

    public static void sendPacket(UUID uuid, SoundPacket packet) { getConnectionOf(uuid).ifPresent(v -> sendPacket(v, packet)); }
    public static void sendPacket(UUID uuid, de.maxhenkel.voicechat.voice.common.SoundPacket<?> packet) { getConnectionOf(uuid).ifPresent(v -> sendPacket(v, packet)); }

    public static void sendLocationPacket(World world, LocationalSoundPacket packet, boolean send_owner) {
        UUID sender = packet.getSender();
        Voice.getPlayersRange(new ServerLevelImpl(world), packet.getPosition(), packet.getDistance()).forEach(player -> {
            if (!send_owner && sender.equals(player.getUuid())) return;
            Voice.getConnectionOf(player.getUuid()).ifPresent(connection -> sendPacket(connection, packet));
        });
    }
    public static void sendLocationPacket(World world, LocationSoundPacket packet, boolean send_owner) {
        UUID sender = packet.getSender();
        Voice.getPlayersRange(world, packet.getLocation(), packet.getDistance()).forEach(player -> {
            if (!send_owner && sender.equals(player.getUuid())) return;
            Voice.getConnectionOf(player.getUuid()).ifPresent(connection -> sendPacket(connection, packet));
        });
    }

    private static final ConcurrentHashMap<UUID, AtomicLong> sequences = new ConcurrentHashMap<>();
    public static long nextSequence(UUID sender) {
        return sequences.computeIfAbsent(sender, v -> new AtomicLong(0)).getAndIncrement();
    }
}




























