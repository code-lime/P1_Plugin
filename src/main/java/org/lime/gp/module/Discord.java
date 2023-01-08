package org.lime.gp.module;

import com.google.gson.JsonObject;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;
import github.scarsz.discordsrv.dependencies.jda.api.events.message.guild.GuildMessageReceivedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction;
import github.scarsz.discordsrv.util.DiscordUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.lime.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.block.component.data.voice.RecorderInstance;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.database.Methods;
import org.lime.gp.database.Rows;
import org.lime.gp.database.Tables;
import org.lime.gp.lime;
import org.lime.gp.player.module.TabManager;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Discord implements Listener {
    private static class ListenDS extends ListenerAdapter {
        private static final ListenDS discordsrvListener = new ListenDS();
        private static final Consumer<? super Message> TIMED_MESSAGE = msg -> msg.delete().queueAfter(8, TimeUnit.SECONDS);

        @Override public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent e) {
            if (e.getAuthor().isBot()) return;
            Rows.DiscordRow row = Tables.DISCORD_TABLE.get(e.getChannel().getUser().getIdLong() + "").orElse(null);
            if (row == null) return;
            Message message = e.getMessage();
            Message ref = message.getReferencedMessage();
            if (ref == null) return;
            if (!ref.getAuthor().isBot()) return;
            List<MessageEmbed> embeds = ref.getEmbeds();
            List<Message.Attachment> attachments = message.getAttachments();
            if (embeds.isEmpty() || attachments.isEmpty()) return;
            MessageEmbed embed = embeds.get(0);
            Message.Attachment attachment = attachments.get(0);
            Optional.ofNullable(embed.getFooter())
                        .map(MessageEmbed.Footer::getText)
                        .ifPresent(footer -> {
                            switch (footer) {
                                case "gp:voice":
                                    Optional.ofNullable(attachment.getFileExtension())
                                            .ifPresent(ext -> {
                                                RecorderInstance.AudioType type;
                                                switch (ext) {
                                                    case "mp3" -> type = RecorderInstance.AudioType.MP3;
                                                    case "wav" -> type = RecorderInstance.AudioType.WAV;
                                                    default -> {
                                                        message.reply("Ошибка! Поддерживаемые форматы музыкальных файлов только `*.mp3` и `*.wav`").queue();
                                                        return;
                                                    }
                                                }
                                                system.LockToast2<Boolean, Message> convertBIF = system.<Boolean, Message>toast(false, null).lock();
                                                Methods.recorderFill(row.uuid, type, attachment.getUrl(), attachment.getFileName(), (action, reason) -> {
                                                    if (action == Methods.SoundFillStart.CONVERT_BIF) {
                                                        convertBIF.invoke(kv -> {
                                                            if (kv.val1 == null) {
                                                                if (kv.val0) return;
                                                                kv.val0 = true;
                                                                message.reply(reason).queue(convertBIF::set1);
                                                            } else {
                                                                kv.val1.editMessage(reason).queue();
                                                            }
                                                        });
                                                        return;
                                                    }
                                                    message.reply(switch (action) {
                                                        case ERROR -> "Ошибка! " + reason;
                                                        case CHECK -> "Проверка активных запросов... 0 / 5";
                                                        case DOWNLOAD -> "Скачивание файла... 1 / 5";
                                                        case CONVERT -> "Конвертация файла... 2 / 5";
                                                        case SAVE -> "Сохранение файла... 4 / 5";
                                                        case DONE -> "Успешно! 5 / 5";
                                                        default -> "";
                                                    }).queue();
                                                });
                                            });
                                    break;
                            }
                        });
        }
        @Override public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent e) {
            if (e.getChannel().getIdLong() != auth_channel) return;
            if (e.getAuthor().isBot()) return;
            e.getMessage().delete().queueAfter(1, TimeUnit.SECONDS);
            int id;
            try {
                id = Integer.parseInt(e.getMessage().getContentRaw());
            }
            catch (Exception ignored) {
                e.getChannel().sendMessage("Ошибка! Введите просто число!").queue(TIMED_MESSAGE);
                return;
            }
            Player _player;
            try {
                UUID uuid = TabManager.getUUIDorNull(id);
                _player = uuid == null ? null : Bukkit.getPlayer(uuid);
            } catch (Exception ignored) {
                _player = null;
            }
            Player player = _player;

            if (player == null) {
                e.getChannel().sendMessage("Данный пользователь не подключен к серверу").queue(TIMED_MESSAGE);
                return;
            }
            String nickName = player.getName();
            UUID uuid = player.getUniqueId();
            long discord_id = e.getAuthor().getIdLong();
            String discord_name = e.getAuthor().getAsTag();
            if (auth_callback.containsKey(uuid)) {
                e.getChannel().sendMessage("Данный пользователь уже ожидает подтверждения").queue(TIMED_MESSAGE);
                return;
            }
            Tables.DISCORD_TABLE.getBy(v -> v.discordID == discord_id || v.uuid.equals(uuid)).ifPresentOrElse(row -> e.getChannel().sendMessage(row.discordID == discord_id
                    ? "К данному дискорду уже привязан аккаунт"
                    : "К данному аккаунту уже привязан дискорд").queue(TIMED_MESSAGE),
                    () -> {
                        e.getChannel().sendMessage("Подтвердите аккаунт '"+nickName+"' в игре").queue(TIMED_MESSAGE);
                        Apply args = Apply.of()
                                .add("discord_name", discord_name)
                                .add("discord_id", String.valueOf(discord_id));
                        LangMessages.Message.Discord_Check.sendMessage(player, args);

                        auth_callback.put(uuid, system.toast(() -> Methods.addDiscord(discord_id, uuid, () -> {
                            e.getChannel().sendMessage("<@"+discord_id+"> связан с ником " + nickName).queue(TIMED_MESSAGE);

                            Player __player = Bukkit.getPlayer(uuid);
                            if (__player == null) return;
                            LangMessages.Message.Discord_Done.sendMessage(__player, args);
                        }), System.currentTimeMillis() + 2 * 60 * 1000));
                    });
        }

        @Subscribe public void discordReadyEvent(DiscordReadyEvent event) {
            DiscordUtil.getJda().addEventListener(this);
        }
    }

    private static boolean debug;
    private static int update;
    private static long auth_channel;
    private static long main_guild;
    private static long confirmed_role;
    private static long online_role;
    private static long gift_role;
    private static final ConcurrentHashMap<Long, Object> role_list = new ConcurrentHashMap<>();

    public static core.element create() {
        return core.element.create(Discord.class)
                .withInstance()
                .withInit(Discord::init)
                .withUninit(Discord::uninit)
                .withInstance()
                .<JsonObject>addConfig("discord", v -> v
                        .withInvoke(json -> {
                            debug = json.has("debug") && json.get("debug").getAsBoolean();
                            update = json.get("update").getAsInt();
                            main_guild = json.get("main_guild").getAsLong();
                            auth_channel = json.get("auth_channel").getAsLong();
                            confirmed_role = json.get("confirmed_role").getAsLong();
                            online_role = json.get("online_role").getAsLong();
                            gift_role = json.get("gift_role").getAsLong();
                        })
                        .withDefault(system.json.object()
                                .add("debug", false)
                                .add("update", 5 * 20)
                                .add("main_guild", 870190631824289863L)
                                .add("auth_channel", 0)
                                .add("confirmed_role", 870190631824289866L)
                                .add("online_role", 870190632101093384L)
                                .add("gift_role", 968684859222544408L)
                                .build()
                        ));
    }

    public static void uninit() {
        DiscordSRV.api.unsubscribe(ListenDS.discordsrvListener);
    }
    public static void init() {
        DiscordSRV.api.subscribe(ListenDS.discordsrvListener);
        AnyEvent.addEvent("discord.auth", AnyEvent.type.none, player -> {
            system.Toast2<system.Action0, Long> callback = auth_callback.remove(player.getUniqueId());
            if (callback == null) return;
            callback.val0.invoke();
            UUID uuid = player.getUniqueId();
            lime.once(() -> updateSingle(uuid), 3);
        });
        AnyEvent.addEvent("discord.cancel", AnyEvent.type.none, player -> {
            system.Toast2<system.Action0, Long> callback = auth_callback.remove(player.getUniqueId());
            if (callback == null) return;
            LangMessages.Message.Discord_Cancel.sendMessage(player);
        });
        AnyEvent.addEvent("discord.reset", AnyEvent.type.none, player -> Methods.delDiscord(player.getUniqueId(), id -> {
            LangMessages.Message.Discord_Reset.sendMessage(player);
            reset(id);
        }));
        lime.repeat(() -> {
            long now = System.currentTimeMillis();
            auth_callback.entrySet().removeIf(kv -> {
                Player player = Bukkit.getPlayer(kv.getKey());
                if (player == null) return true;
                if (kv.getValue().val1 >= now) return false;
                LangMessages.Message.Discord_Timeout.sendMessage(player);
                return true;
            });
        }, 5);
        lime.once(Discord::nextUpdate, 5);
    }
    private static void nextUpdate() {
        Methods.discordRoleList(list -> {
            role_list.putAll(list);
            role_list.entrySet().removeIf(v -> !list.containsKey(v.getKey()));
        });
        updateAll();
        lime.once(Discord::nextUpdate, update);
    }

    public static void sendRecord(long discord_id) {
        if (!DiscordSRV.getPlugin().isEnabled()) return;
        DiscordSRV.getPlugin()
                .getJda()
                .getGuilds()
                .stream()
                .map(v -> v.getMemberById(discord_id))
                .flatMap(Stream::ofNullable)
                .findAny()
                .ifPresent(member -> member.getUser().openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(new EmbedBuilder()
                                .setTitle("Добавление музыкальной композиции")
                                .addField("Инструкция по использованию", String.join("\n",
                                        "Добавление музыкаольной композиции происходит путем отправки **файла** с музыкой в **ответ** на **это сообщение**.",
                                        "Простая отправка сообщения в диалог с ботом не даст никакого результата, требуется именно **ответ** на **это сообщение**.",
                                        "После отправки сообщения бот начнет загружать данный музыкальный файл на сервер. Данная процедура может занять некоторое время.",
                                        "Во время процедуры загрузки бот будет уведомлять вас об этапах загрузки файла на сервер."
                                ), false)
                                .addField("P.S.", "Если у вас остались вопросы по работе загрузки файла - пишите в тикет: <#956273468691865631>", false)
                                .setFooter("gp:voice")
                                .build()
                        )
                        .queue()));
    }

    private static Map<String, RestAction<Void>> update(long discord_id, String user_name, List<Long> discord_roles, UUID uuid) {
        HashMap<String, RestAction<Void>> actions = new HashMap<>();
        if (!DiscordSRV.getPlugin().isEnabled()) return actions;
        String main_prefix = main_guild + ":";
        DiscordSRV.getPlugin().getJda().getGuilds().forEach(guild -> {
            String prefix = guild.getId() + ":";
            update(guild, discord_id, user_name, prefix.equals(main_prefix) ? discord_roles : Collections.emptyList(), uuid)
                    .forEach((k,v) -> actions.put(prefix + k, v));
        });
        return actions;
    }
    private static Map<String, RestAction<Void>> update(Guild guild, long discord_id, String user_name, List<Long> discord_roles, UUID uuid) {
        boolean log = debug && discord_id == 291179227754266624L && discord_roles.size() > 0;
        if (log) lime.logOP("UPDATE." +discord_id + ": " + user_name + "[" + discord_roles.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]");
        Member member = guild.getMemberById(discord_id);
        if (member == null) return Collections.emptyMap();
        if (log) lime.logOP("UPDATE.0");

        List<Role> roles = member.getRoles();
        List<Long> rolesIds = roles.stream().map(ISnowflake::getIdLong).toList();
        boolean hasOnline = rolesIds.contains(online_role);
        boolean hasGift = rolesIds.contains(gift_role);
        boolean hasConfirm = rolesIds.contains(confirmed_role);

        boolean isOnline = Bukkit.getPlayer(uuid) != null;
        boolean isGift = TabManager.hasDonateID(uuid);

        HashMap<Long, Boolean> user_roles = new HashMap<>();

        if (hasOnline != isOnline) user_roles.put(online_role, isOnline);
        if (hasGift != isGift) user_roles.put(gift_role, isGift);
        if (!hasConfirm) user_roles.put(confirmed_role, true);

        if (log) lime.logOP("UPDATE.1:" + (hasOnline ? 1 : 0) + "|"+(hasConfirm ? 1 : 0) + "|" + (isOnline ? 1 : 0));

        role_list.keySet().forEach(_roleId -> {
            boolean granted = discord_roles.contains(_roleId);
            Boolean state = user_roles.getOrDefault(_roleId, null);
            if (log) lime.logOP("UPDATE.1.ROLE-" + _roleId + ": " + (granted ? 1 : 0) + "|" + (state == null ? "NULL" : (state ? 1 : 0)));
            user_roles.put(_roleId, state == null ? granted : (state || granted));
        });

        List<Role> addRoles = new ArrayList<>();
        List<Role> delRoles = new ArrayList<>();

        if (log) lime.logOP("UPDATE.2");
        user_roles.forEach((_roleId,_state) -> {
            Role _role = guild.getRoleById(_roleId);
            if (log) lime.logOP("UPDATE.2.0: " + _roleId + " : " + guild.getId());
            if (_role == null) return;
            if (log) lime.logOP("UPDATE.2.1");
            if (_state == rolesIds.contains(_roleId)) return;
            if (log) lime.logOP("UPDATE.2.2");
            (_state ? addRoles : delRoles).add(_role);
            if (log) lime.logOP("UPDATE.2.3." + (_state ? "ADD" : "DEL") + " " + _role.getName());
        });

        HashMap<String, RestAction<Void>> actions = new HashMap<>();
        if (log) lime.logOP("UPDATE.3: " + addRoles.size() + " | " + delRoles.size());
        if (addRoles.size() != 0 || delRoles.size() != 0) actions.put("m."+discord_id+".roles", guild.modifyMemberRoles(member, addRoles, delRoles));
        if (log) lime.logOP("UPDATE.4: " + member.isOwner() + " | " + member.getNickname());
        if (
                !(member.isOwner() || discord_id == 291179227754266624L) &&
                !user_name.equals(member.getNickname()) &&
                !user_name.equals(member.getUser().getName()) &&
                guild.getSelfMember().canInteract(member)
        )
            actions.put("m."+discord_id+".name", member.modifyNickname(user_name));
        if (log) lime.logOP("UPDATE.5");
        return actions;
    }
    private static void reset(long discord_id) {
        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        Member member = guild.getMemberById(discord_id);
        if (member == null) return;

        List<Role> delRoles = new ArrayList<>();
        delRoles.add(guild.getRoleById(online_role));
        delRoles.add(guild.getRoleById(gift_role));
        delRoles.add(guild.getRoleById(confirmed_role));

        role_list.keySet().forEach(_roleId -> delRoles.add(guild.getRoleById(_roleId)));
        guild.modifyMemberRoles(member, new ArrayList<>(), delRoles).queue();
        if (!member.isOwner()) member.modifyNickname(null).queue();
    }

    private static RestAction<Void> combine(Collection<RestAction<Void>> actions) {
        RestAction<Void> action = null;
        for (RestAction<Void> item : actions) action = action == null ? item : action.and(item);
        return action;
    }
    private static List<Long> roleList(Long... roles) {
        List<Long> list = new ArrayList<>();
        for (Long role : roles) {
            if (role != null)
                list.add(role);
        }
        return list;
    }
    public static void updateAll() {
        if (debug) lime.logOP("UPDATE.ALL");
        Map<String, RestAction<Void>> queue = new HashMap<>();
        Methods.discordUpdate(
                (discord_id, user_name, discord_role, discord_group_role, uuid) -> queue.putAll(Discord.update(discord_id, user_name, roleList(discord_role, discord_group_role), uuid)),
                () -> {
                    if (debug) lime.logOP("UPDATE.ALL.COUNT: " + queue.size());
                    RestAction<Void> action = combine(queue.values());
                    if (debug) queue.keySet().forEach(key -> lime.logOP(" - UPDATE.ALL.ITEM: " + key));
                    if (action == null) return;
                    action.queue((v) -> {
                        if (debug) lime.logOP("UPDATE.ALL.QUEUE: DONE");
                    }, (error) -> {
                        lime.logOP("UPDATE.ALL.QUEUE: ERROR");
                        lime.logStackTrace(error);
                    });
                }
        );
    }
    public static void updateSingle(UUID uuid) {
        if (debug) lime.logOP("UPDATE.SINGLE");
        Map<String, RestAction<Void>> queue = new HashMap<>();
        Methods.discordUpdateSingle(uuid,
                (discord_id, user_name, discord_role, discord_group_role, _uuid) -> queue.putAll(Discord.update(discord_id, user_name, roleList(discord_role, discord_group_role), _uuid)),
                () -> {
                    if (debug) lime.logOP("UPDATE.SINGLE.COUNT: " + queue.size());
                    RestAction<Void> action = combine(queue.values());
                    if (debug) queue.keySet().forEach(key -> lime.logOP(" - UPDATE.SINGLE.ITEM: " + key));
                    if (action == null) return;
                    action.queue((v) -> {
                        if (debug) lime.logOP("UPDATE.SINGLE.QUEUE: DONE");
                    }, (error) -> {
                        lime.logOP("UPDATE.SINGLE.QUEUE: ERROR");
                        lime.logStackTrace(error);
                    });
                }
        );
    }

    @EventHandler public static void onJoin(PlayerJoinEvent e) { updateSingle(e.getPlayer().getUniqueId()); }
    @EventHandler public static void onLeave(PlayerQuitEvent e) { updateSingle(e.getPlayer().getUniqueId()); }

    private static final HashMap<UUID, system.Toast2<system.Action0, Long>> auth_callback = new HashMap<>();
}




























