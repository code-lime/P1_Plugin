package org.lime.gp.module;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.block.component.data.voice.RecorderInstance;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.database.Methods;
import org.lime.gp.database.rows.DeathRow;
import org.lime.gp.database.rows.DiscordRow;
import org.lime.gp.database.rows.QuentaRow;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.lime;
import org.lime.gp.player.module.FakeUsers;
import org.lime.gp.player.module.TabManager;
import org.lime.plugin.CoreElement;
import org.lime.system.execute.Action0;
import org.lime.system.execute.Action1;
import org.lime.system.execute.Func1;
import org.lime.system.json;
import org.lime.system.toast.LockToast2;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;
import org.lime.web;

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

        private void onPrivateMessage(MessageReceivedEvent e) {
            if (e.getAuthor().isBot()) return;
            DiscordRow row = Tables.DISCORD_TABLE.get(String.valueOf(e.getChannel().asPrivateChannel().getUser().getIdLong())).orElse(null);
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
                                            LockToast2<Boolean, Message> convertBIF = Toast.lock(false, null);
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
        private void onGuildMessage(MessageReceivedEvent e) {
            if (e.getChannel().getIdLong() != auth_channel) return;
            if (e.getAuthor().isBot()) return;
            e.getMessage().delete().queueAfter(1, TimeUnit.SECONDS);
            int id;
            try {
                id = Integer.parseInt(e.getMessage().getContentRaw());
            }
            catch (Exception ignored) {
                e.getMessage().reply("Ошибка! Введите просто число!").queue(TIMED_MESSAGE);
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
                e.getMessage().reply("Данный пользователь не подключен к серверу").queue(TIMED_MESSAGE);
                return;
            }
            String nickName = player.getName();
            UUID uuid = player.getUniqueId();
            long discord_id = e.getAuthor().getIdLong();
            String discord_name = e.getAuthor().getName();
            String[] discord_split = discord_name.split("#");
            String discord_display_name = discord_split.length == 2 && discord_split[1].equals("0000") ? discord_split[0] : discord_name;
            if (auth_callback.containsKey(uuid)) {
                e.getMessage().reply("Данный пользователь уже ожидает подтверждения").queue(TIMED_MESSAGE);
                return;
            }
            Tables.DISCORD_TABLE.getBy(v -> v.discordID == discord_id || v.uuid.equals(uuid)).ifPresentOrElse(row -> e.getMessage().reply(row.discordID == discord_id
                            ? "К данному дискорду уже привязан аккаунт"
                            : "К данному аккаунту уже привязан дискорд").queue(TIMED_MESSAGE),
                    () -> {
                        e.getMessage().reply("Подтвердите аккаунт '"+nickName+"' в игре").queue(TIMED_MESSAGE);
                        Apply args = Apply.of()
                                .add("discord_name", discord_display_name)
                                .add("discord_id", String.valueOf(discord_id));
                        LangMessages.Message.Discord_Check.sendMessage(player, args);

                        auth_callback.put(uuid, Toast.of(() -> Methods.addDiscord(discord_id, uuid, () -> {
                            e.getMessage().reply("<@"+discord_id+"> связан с ником " + nickName).queue(TIMED_MESSAGE);

                            Player __player = Bukkit.getPlayer(uuid);
                            if (__player == null) return;
                            LangMessages.Message.Discord_Done.sendMessage(__player, args);
                        }), System.currentTimeMillis() + 2 * 60 * 1000));
                    });
        }

        @Override public void onMessageReceived(@NotNull MessageReceivedEvent e) {
            if (e.isFromType(ChannelType.PRIVATE)) onPrivateMessage(e);
            else if (e.isFromType(ChannelType.TEXT) && e.isFromGuild()) onGuildMessage(e);
        }
    }

    private static boolean debug;
    private static int update;
    private static long auth_channel;
    private static long main_guild;

    private static DiscordRole confirmedRole = new DiscordRole("confirmed_role", 0, uuid -> true, v -> Discord.confirmedRole = v);
    private static DiscordRole onlineRole = new DiscordRole("online_role", 0, uuid -> Bukkit.getPlayer(uuid) != null, v -> Discord.onlineRole = v);
    private static DiscordRole giftRole = new DiscordRole("gift_role", 0, TabManager::hasDonate, v -> Discord.giftRole = v);
    private static DiscordRole quentaRole = new DiscordRole("quenta_role", 0, QuentaRow::hasQuenta, v -> Discord.quentaRole = v);
    private static DiscordRole deathRole = new DiscordRole("death_role", 0, DeathRow::hasDeath, v -> Discord.deathRole = v);

    private record DiscordRole(String name, long roleID, Func1<UUID, Boolean> check, Action1<DiscordRole> update) {
        public boolean isCheck(UUID uuid) {
            return check.invoke(uuid);
        }
        public void readUpdate(JsonObject json) {
            if (!json.has(name)) {
                lime.logOP("Discord role '"+name+"' not set! Skip...");
                update.invoke(new DiscordRole(name, 0, check, update));
                return;
            }
            update.invoke(new DiscordRole(name, json.get(name).getAsLong(), check, update));
        }
    }
    private static Stream<DiscordRole> getAllRoles() {
        return Stream.of(confirmedRole, onlineRole, giftRole, quentaRole, deathRole);
    }

    private static final ConcurrentHashMap<Long, Object> role_list = new ConcurrentHashMap<>();

    public static CoreElement create() {
        return CoreElement.create(Discord.class)
                .withInstance()
                .withInit(Discord::init)
                .withUninit(Discord::uninit)
                .withInstance()
                .<JsonObject>addConfig("discord", v -> v
                        .withInvoke(json -> {
                            debug = json.has("debug") && json.get("debug").getAsBoolean();
                            update = json.get("update").getAsInt();
                            String token = json.get("token").getAsString();
                            main_guild = json.get("main_guild").getAsLong();
                            auth_channel = json.get("auth_channel").getAsLong();

                            getAllRoles().forEach(role -> role.readUpdate(json));

                            if (jda != null) jda.shutdown();
                            if (token.isEmpty()) return;
                            jda = JDABuilder.createDefault(token)
                                    .enableCache(
                                            CacheFlag.ACTIVITY,
                                            CacheFlag.VOICE_STATE,
                                            CacheFlag.EMOJI,
                                            CacheFlag.STICKER,
                                            CacheFlag.CLIENT_STATUS,
                                            CacheFlag.MEMBER_OVERRIDES,
                                            CacheFlag.ROLE_TAGS,
                                            CacheFlag.FORUM_TAGS,
                                            CacheFlag.ONLINE_STATUS,
                                            CacheFlag.SCHEDULED_EVENTS
                                    )
                                    .setChunkingFilter(ChunkingFilter.ALL)
                                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                                    .enableIntents(
                                            GatewayIntent.GUILD_MEMBERS,
                                            GatewayIntent.GUILD_MODERATION,
                                            GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
                                            GatewayIntent.GUILD_WEBHOOKS,
                                            GatewayIntent.GUILD_INVITES,
                                            GatewayIntent.GUILD_VOICE_STATES,
                                            GatewayIntent.GUILD_PRESENCES,
                                            GatewayIntent.GUILD_MESSAGES,
                                            GatewayIntent.GUILD_MESSAGE_REACTIONS,
                                            GatewayIntent.GUILD_MESSAGE_TYPING,

                                            GatewayIntent.DIRECT_MESSAGES,
                                            GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                                            GatewayIntent.DIRECT_MESSAGE_TYPING,

                                            GatewayIntent.MESSAGE_CONTENT,
                                            GatewayIntent.SCHEDULED_EVENTS,

                                            GatewayIntent.AUTO_MODERATION_CONFIGURATION,
                                            GatewayIntent.AUTO_MODERATION_EXECUTION
                                    ).build();
                            jda.addEventListener(ListenDS.discordsrvListener);
                        })
                        .withDefault(json.object()
                                .add("debug", false)
                                .add("update", 5 * 20)
                                .add("token", "")
                                .add("main_guild", 870190631824289863L)
                                .add("auth_channel", 0)
                                .add("confirmed_role", 870190631824289866L)
                                .add("online_role", 870190632101093384L)
                                .add("gift_role", 968684859222544408L)
                                .add("quenta_role", 968684859222544409L)
                                .add("death_role", 0L)
                                .build()
                        ));
    }

    private static JDA jda = null;
    private static JDA getJDA() {
        return jda;
    }

    public static void uninit() {
        if (jda != null) jda.shutdown();
    }
    private static String lastStatus = "";
    public static void init() {
        AnyEvent.addEvent("discord.auth", AnyEvent.type.none, player -> {
            Toast2<Action0, Long> callback = auth_callback.remove(player.getUniqueId());
            if (callback == null) return;
            callback.val0.invoke();
            UUID uuid = player.getUniqueId();
            lime.once(() -> updateSingle(uuid), 3);
        });
        AnyEvent.addEvent("discord.cancel", AnyEvent.type.none, player -> {
            Toast2<Action0, Long> callback = auth_callback.remove(player.getUniqueId());
            if (callback == null) return;
            LangMessages.Message.Discord_Cancel.sendMessage(player);
        });
        AnyEvent.addEvent("discord.reset", AnyEvent.type.none, player -> Methods.findDiscord(player.getUniqueId(), id -> {
            if (id == null) return;
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
            JDA jda = getJDA();
            if (jda == null) return;
            String status = "Онлайн: " + (Bukkit.getOnlinePlayers().size() + FakeUsers.getCount());
            if (status.equals(lastStatus)) return;
            lastStatus = status;
            jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.watching(status));
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
        JDA jda = getJDA();
        if (jda == null) return;
        jda.getGuilds()
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
        JDA jda = getJDA();
        if (jda == null) return actions;
        String main_prefix = main_guild + ":";
        jda.getGuilds().forEach(guild -> {
            String prefix = guild.getId() + ":";
            boolean isMain = prefix.equals(main_prefix);
            update(guild, discord_id, user_name, isMain, isMain ? discord_roles : Collections.emptyList(), uuid)
                    .forEach((k,v) -> actions.put(prefix + k, v));
        });
        return actions;
    }
    private static Map<String, RestAction<Void>> update(Guild guild, long discord_id, String user_name, boolean isMain, List<Long> discord_roles, UUID uuid) {
        boolean log = debug && discord_id == 291179227754266624L && isMain;
        if (log) lime.logOP("UPDATE." +discord_id + ": " + user_name + "[" + discord_roles.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]");
        Member member = guild.getMemberById(discord_id);
        if (member == null) return Collections.emptyMap();
        if (log) lime.logOP("UPDATE.0");

        List<Role> roles = member.getRoles();
        List<Long> rolesIds = roles.stream().map(ISnowflake::getIdLong).toList();
        HashMap<Long, Boolean> user_roles = new HashMap<>();
        getAllRoles().forEach(role -> {
            if (role.roleID == 0) return;
            boolean hasRole = rolesIds.contains(role.roleID);
            boolean isRole = role.isCheck(uuid);
            if (hasRole != isRole) user_roles.put(role.roleID, isRole);
        });

        //if (log) lime.logOP("UPDATE.1:" + (hasOnline ? 1 : 0) + "|"+(hasConfirm ? 1 : 0) + "|" + (isOnline ? 1 : 0));

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
                !member.isOwner() &&
                !user_name.equals(member.getNickname()) &&
                !user_name.equals(member.getUser().getName()) &&
                guild.getSelfMember().canInteract(member)
        )
            actions.put("m."+discord_id+".name", member.modifyNickname(user_name));
        if (log) lime.logOP("UPDATE.5");
        return actions;
    }
    public static void reset(long discord_id) {
        boolean log = debug && discord_id == 291179227754266624L;
        if (log) lime.logOP("RESET." +discord_id);
        Methods.discordClear(discord_id, () -> Optional.ofNullable(getJDA()).ifPresent(jda -> jda.getGuilds().forEach(guild -> {
            Member member = guild.getMemberById(discord_id);
            if (member == null) return;

            if (log) lime.logOP("RESET.MEMBER: " +member);
            List<Role> delRoles = new ArrayList<>();
            getAllRoles().forEach(v -> {
                Role role = guild.getRoleById(v.roleID);
                if (log) lime.logOP("RESET.ROLE: " + v.roleID + " : " + role);
                delRoles.add(role);
            });
            role_list.keySet().forEach(_roleId -> {
                Role role = guild.getRoleById(_roleId);
                if (log) lime.logOP("RESET.ROLE: " + _roleId + " : " + role);
                delRoles.add(role);
            });

            delRoles.removeIf(Objects::isNull);
            guild.modifyMemberRoles(member, new ArrayList<>(), delRoles).queue();
            if (!member.isOwner() && guild.getSelfMember().canInteract(member))
                member.modifyNickname(null).queue();
        })));
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
                (discord_id, user_name, discord_roles, uuid) -> queue.putAll(Discord.update(discord_id, user_name, roleList(discord_roles), uuid)),
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
                (discord_id, user_name, discord_roles, _uuid) -> queue.putAll(Discord.update(discord_id, user_name, roleList(discord_roles), _uuid)),
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

    private static final HashMap<UUID, Toast2<Action0, Long>> auth_callback = new HashMap<>();

    public static void sendMessageToChannel(long channelID, String message) {
        JDA jda = getJDA();
        if (jda == null) return;
        jda.getTextChannelById(channelID).sendMessage(message).queue();
    }

    public static void sendMessageToWebhook(String webhook, String message) {
        sendMessageToWebhook(webhook, message, true);
    }
    public static void sendMessageToWebhook(String webhook, String message, boolean async) {
        var executor = web.method.POST.create(webhook, json.object().add("content", message).build().toString())
                .header("Content-Type", "application/json")
                .none();
        if (async) executor.executeAsync((v,a) -> {});
        else executor.execute();
    }
}




























