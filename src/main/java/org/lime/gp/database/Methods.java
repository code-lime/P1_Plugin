package org.lime.gp.database;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang.StringUtils;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.lime.Position;
import org.lime.plugin.CoreElement;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.block.component.data.voice.RecorderInstance;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.rows.BanListRow;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.player.module.TabManager;
import org.lime.gp.lime;
import org.lime.system.IJson;
import org.lime.system.Time;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.json;
import org.lime.system.map;
import org.lime.web;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

public class Methods {
    public static MySql SQL = null;

    public static CoreElement create() {
        return CoreElement.create(Methods.class)
                .withInit(Methods::init)
                .<JsonObject>addConfig("database", v -> v.withInvoke(json -> {
                    SQL = new MySql(
                            json.get("host").getAsString(),
                            json.get("port").getAsInt(),
                            json.get("name").getAsString(),
                            json.get("login").getAsString(),
                            json.get("password").getAsString());
                    if (!SQL.isValidMySQL()) throw new IllegalArgumentException("Could not establish database connection.");
                }).withDefault(() -> json.object()
                        .add("port", 3306)
                        .add("host", "localhost")
                        .add("name", "DATABASE_NAME")
                        .add("login", "LOGIN")
                        .add("password", "PASSWORD")
                        .add("pool_size", 10)
                        .build()));
    }

    private static final List<Thread> threads = new ArrayList<>();
    private static void threadAction() {
        while (true) {
            try { Thread.sleep(10); } catch (InterruptedException e) { throw new RuntimeException(e); }
            if (SQL != null) SQL.invokeNext();
        }
    }
    public static void init() {
        AnyEvent.addEvent("mysql.debug", AnyEvent.type.owner_console, (player) -> lime.logOP("Debug: " + (SQL.switchDebug() ? "ENABLE" : "DISABLE")));
        AnyEvent.addEvent("mysql.debug.filter", AnyEvent.type.owner_console, b -> b.createParam(Double::parseDouble, "[time:double]"), (player, time) -> lime.logOP("Debug filter: " + SQL.filterDebug(time)));
        AnyEvent.addEvent("mysql.calls", AnyEvent.type.owner_console, (player) -> lime.logOP("Calls: " + SQL.getCalls()));
        AnyEvent.addEvent("mysql.dump", AnyEvent.type.owner_console, (player) -> {
            List<String> dump = SQL.getDumpCalls();
            lime.logOP("Dump of " + dump.size() + " calls:\n   " + String.join("\n   ", dump));
        });
        lime.repeat(Methods::update, 1);
        lime.repeat(Methods::updateCityStatus, 1, 10);

        for (int i = 0; i < 5; i++)
            threads.add(new Thread(Methods::threadAction));
        threads.forEach(Thread::start);
    }
    @SuppressWarnings("deprecation")
    public static void uninit() {
        SQL.close();
        threads.forEach(Thread::stop);
    }
    private static boolean CITY_ENABLE = false;
    public static void update() {
        SQL.Async.rawSql("SELECT OnUpdate()", () -> {});
    }
    public static void updateCityStatus() {
        SQL.Async.rawSqlOnce(
                "SELECT COUNT(1) FROM information_schema.tables WHERE `table_schema` = DATABASE() AND `table_name` = 'city'",
                Integer.class,
                status -> CITY_ENABLE = status > 0
        );
    }

    public static Vector readPosition(ResultSet set, String prefix) {
        prefix = prefix != null ? (prefix + "_") : "";
        int x = MySql.readObject(set, prefix + "x", Integer.class);
        int y = MySql.readObject(set, prefix + "y", Integer.class);
        int z = MySql.readObject(set, prefix + "z", Integer.class);
        return new Vector(x,y,z);
    }
    public static Position readPosition(ResultSet set, World world, String prefix) {
        return new Position(world, readPosition(set, prefix));
    }

    public enum CallType {
        PHONE((world) -> world == lime.MainWorld || world == lime.EndWorld),
        GLOBAL((world) -> world == lime.MainWorld),
        POLICE((world) -> world == lime.MainWorld),
        MEDIC((world) -> world == lime.MainWorld),
        MEDIC_DIE((world) -> world == lime.MainWorld),
        ADMIN((world) -> true);

        public final Func1<World, Boolean> checkWorldFunc;
        public boolean check(World world) {
            return checkWorldFunc.invoke(world);
        }
        CallType(Func1<World, Boolean> checkWorldFunc) {
            this.checkWorldFunc = checkWorldFunc;
        }
    }
    public static void callLog(int from, CallType type, String message, Action1<Integer> callback) {
        SQL.Async.rawSqlOnce("SELECT CallLog(@from,@type,@message,@now)", MySql.args().add("from", from).add("type", type.name()).add("message", message).add("now", Time.moscowNow()).build(), Integer.class, callback);
    }
    public static void smsLog(int from_id, int to_id, String message, Action1<Integer> callback) {
        SQL.Async.rawSqlOnce("SELECT Sms(@from_id,@to_id,@msg)", MySql.args().add("from_id", from_id).add("to_id", to_id).add("msg", message).build(), Integer.class, callback);
    }
    public static void isIgnore(int player, int other, Action1<Boolean> callback_ignore) {
        SQL.Async.rawSqlOnce("SELECT EXISTS(SELECT * FROM other_flags WHERE FlagGetIndex(other_flags.flag, 0) = 1 AND other_flags.user_id = "+player+" AND other_flags.other_id = " + other + ")",
                Integer.class,
                state -> callback_ignore.invoke(state == 1));
    }

    public static class WarnInfo {
        public final UUID uuid;
        public final String reason;
        public final UUID owner;
        public final Calendar create_time;
        public final Optional<Calendar> end_time;

        public boolean isEnd() {
            return end_time.map(v -> v.getTimeInMillis() < System.currentTimeMillis()).orElse(false);
        }

        private WarnInfo(ResultSet set) {
            uuid = UUID.fromString(MySql.readObject(set, "uuid", String.class));
            reason = MySql.readObject(set, "reason", String.class);
            owner = Optional.ofNullable(MySql.readObject(set, "owner", String.class)).map(UUID::fromString).orElse(null);
            create_time = MySql.readObject(set, "create_time", Calendar.class);
            end_time = MySql.readObjectOptional(set, "end_time", Calendar.class);
        }
        public Component toLine(String prefix) {
            return toLine(Component.text(prefix));
        }
        public Component toLine(Component prefix) {
            Optional<UserRow> user = Optional.ofNullable(this.uuid).flatMap(UserRow::getBy);
            Optional<UserRow> owner = Optional.ofNullable(this.owner).flatMap(UserRow::getBy);
            String createTime = Time.formatCalendar(this.create_time, true);
            String endTime = this.end_time.map(v -> Time.formatCalendar(v, true)).orElse("Неограничено");
            Component display = Component.empty()
                    .append(Component.text("Дата выдачи: ").append(Component.text(createTime).color(NamedTextColor.GRAY)).append(Component.newline()))
                    .append(Component.text("Действителен до: ").append(Component.text(endTime).color(this.end_time.isPresent() ? NamedTextColor.GRAY : NamedTextColor.RED)).append(Component.newline()))
                    .append(ChatHelper.formatComponent("Кем выдано: " + (owner.map(v -> "<GRAY>"+v.firstName +" "+v.lastName +"</> (<GOLD>"+v.userName +"</>)").orElse("<AQUA>CONSOLE</>") + "\n")))
                    .append(ChatHelper.formatComponent("Кому выдано: " + (user.map(v -> "<GRAY>"+v.firstName +" "+v.lastName +"</> (<GOLD>"+v.userName +"</>)").orElse("<AQUA>Неизвестно</>") + "\n")))
                    .append(Component.text("Причина: ").append(Component.text(reason).color(NamedTextColor.GRAY)).append(Component.newline()))
                    .append(Component.text(" Нажми для копирования...").color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC));
            return Component.empty()
                    .append(prefix)
                    .append(Component.text("[" + createTime + "] ").color(NamedTextColor.GRAY))
                    .append(Component.text("✉")
                            .color(NamedTextColor.RED)
                            .hoverEvent(HoverEvent.showText(display))
                            .clickEvent(ClickEvent.copyToClipboard(String.join("\n",
                                    "Дата выдачи: " + createTime,
                                            "Действителен до: " + endTime,
                                            "Кем выдано: " + (owner.map(v -> v.firstName +" "+v.lastName + " (" + v.userName + ")").orElse("CONSOLE")),
                                            "Кому выдано: " + (user.map(v -> v.firstName +" "+v.lastName + " (" + v.userName + ")").orElse("Неизвестно")),
                                            "Причина: " + reason
                                    )
                            ))
                    );
        }
    }
    public static void aBanAdd(UUID uuid, String reason, Integer time, UUID owner, Action0 callback) {
        String sql = owner == null
                ? "INSERT INTO aban (aban.uuid, aban.reason, aban.time) VALUES (@uuid, @reason, @time) ON DUPLICATE KEY UPDATE aban.reason = @reason, aban.time = @time, aban.owner = NULL"
                : "INSERT INTO aban (aban.uuid, aban.reason, aban.owner, aban.time) VALUES (@uuid, @reason, @owner, @time) ON DUPLICATE KEY UPDATE aban.reason = @reason, aban.time = @time, aban.owner = @owner";
        SQL.Async.rawSql(sql,
                MySql.args().add("uuid", uuid.toString()).add("owner", owner == null ? "" : owner.toString()).add("reason", reason).add("time", time).build(),
                callback);
    }
    public static void aMuteAdd(UUID uuid, String reason, Integer time, UUID owner, Action0 callback) {
        String sql = owner == null
                ? "INSERT INTO amute (amute.uuid, amute.reason, amute.time) VALUES (@uuid, @reason, @time) ON DUPLICATE KEY UPDATE amute.reason = @reason, amute.time = @time, amute.owner = NULL"
                : "INSERT INTO amute (amute.uuid, amute.reason, amute.owner, amute.time) VALUES (@uuid, @reason, @owner, @time) ON DUPLICATE KEY UPDATE amute.reason = @reason, amute.time = @time, amute.owner = @owner";
        SQL.Async.rawSql(sql,
                MySql.args().add("uuid", uuid.toString()).add("owner", owner == null ? "" : owner.toString()).add("reason", reason).add("time", time).build(),
                callback);
    }
    public static void aWarnAdd(UUID uuid, String reason, Integer time, UUID owner, Action1<List<WarnInfo>> callback) {
        SQL.Async.rawSql("SELECT AWarnCreate(@uuid, @reason, @time, @owner)",
                MySql.args().add("uuid", uuid.toString()).add("reason", reason).add("time", time).add("owner", owner == null ? null : owner.toString()).build(),
                () -> aWarnList(uuid, callback));
    }
    public static void aWarnList(UUID uuid, Action1<List<Methods.WarnInfo>> callback) {
        SQL.Async.rawSqlQuery("SELECT * FROM awarn WHERE awarn.uuid = @uuid",
                MySql.args().add("uuid", uuid.toString()).build(),
                WarnInfo::new,
                callback);
    }
    public static void aBanDel(UUID uuid, Action0 callback) {
        SQL.Async.rawSql("DELETE FROM aban WHERE aban.uuid = '"+uuid+"'", callback);
    }
    public static void aAnyUpdate(Iterable<UUID> online) {
        SQL.Async.rawSql("UPDATE aban SET aban.time = aban.time - 1 WHERE aban.time IS NOT NULL AND aban.uuid IN @online", MySql.args().add("online", online).build(), () -> {});
        SQL.Async.rawSql("UPDATE amute SET amute.time = amute.time - 1 WHERE amute.time IS NOT NULL AND amute.uuid IN @online", MySql.args().add("online", online).build(), () -> {});
        SQL.Async.rawSql("DELETE FROM amute WHERE amute.time < 0", () -> {});
    }

    public static void addIP(String ip, UUID uuid) {
        SQL.Async.rawSql("INSERT INTO ip_list (ip,uuid) VALUES (@ip,@uuid)", MySql.args().add("ip", ip).add("uuid", uuid.toString()).build(), null);
    }
    public static void updateUser(UUID uuid, String user_name, Calendar connect_date) {
        SQL.Async.rawSql("UPDATE users SET connect_date = @connect_date, user_name = @user_name WHERE uuid = @uuid", MySql.args().add("uuid", uuid.toString()).add("user_name", user_name).add("connect_date", connect_date).build(), null);
    }
    public static void renameUser(UUID uuid, String first_name, String last_name, Action0 callback) {
        SQL.Async.rawSql("UPDATE users SET users.first_name = @first_name, users.last_name = @last_name WHERE users.uuid = @uuid", MySql.args()
                .add("first_name", first_name)
                .add("last_name", last_name)
                .add("uuid", uuid)
                .build()
        , callback);
    }

    public static void commandExecute(UUID uuid, String command, Action0 callback) {
        SQL.Async.rawSqlOnce("SELECT CommandExecute(@uuid, @command)",
                MySql.args().add("uuid", uuid == null ? "NULL" : uuid.toString()).add("command", command).build(),
                Integer.class,
                (id) -> callback.invoke());
    }

    public static void addDiscord(long discord_id, UUID uuid, Action0 callback) {
        SQL.Async.rawSql("INSERT INTO discord (discord_id,uuid) VALUES ("+discord_id+", '"+uuid+"')", callback);
    }
    public static void delDiscord(UUID uuid, Action1<Long> callback) {
        SQL.Async.rawSqlQuery("SELECT discord_id FROM discord WHERE discord.uuid = '"+uuid+"'", Long.class, ids -> ids.forEach(id -> SQL.Async.rawSql("DELETE FROM discord WHERE discord_id = '"+id+"'", () -> callback.invoke(id))));
    }
    public static void findDiscord(UUID uuid, Action1<Long> callback) {
        SQL.Async.rawSqlOnce("SELECT discord_id FROM discord WHERE discord.uuid = '"+uuid+"'", Long.class, callback);
    }
    public static void discordRoleList(Action1<Map<Long, Object>> callback) {
        List<String> unions = new ArrayList<>();
        unions.add("SELECT roles.discord_role FROM roles WHERE roles.discord_role IS NOT NULL GROUP BY roles.discord_role");
        unions.add("SELECT role_groups.discord_role FROM role_groups WHERE role_groups.discord_role IS NOT NULL GROUP BY role_groups.discord_role");
        if (CITY_ENABLE) unions.add("SELECT city.discord_role FROM city WHERE city.discord_role IS NOT NULL GROUP BY city.discord_role");
        SQL.Async.rawSqlQuery(String.join(" UNION ", unions), Long.class, list -> callback.invoke(map.<Long, Object>of().add(list, new Object()).build()));
    }

    private static final String discordUpdateSQL = String.join(" ",
            "SELECT",
                    String.join(", ",
                            "discord.discord_id",
                            "users.uuid",
                            "CONCAT(users.first_name, ' ', users.last_name) AS user_name",
                            "roles.discord_role AS discord_role",
                            "role_groups.discord_role AS discord_group_role",
                            "{CITY_FIELDS}"
                    ),
                    "FROM users",
                    "INNER JOIN discord ON users.uuid = discord.uuid",
                    "LEFT JOIN roles ON roles.id = users.role",
                    "LEFT JOIN role_groups ON role_groups.id = roles.id_group"
    );
    private static final String discordUpdateSQL_City = " LEFT JOIN city ON city.id = role_groups.id_city";
    private static final String discordUpdateSQL_City_Fields = String.join(",",
            "city.discord_role AS discord_city_role"
    );
    private static final String discordUpdateSQL_City_None_Fields = String.join(",",
            "NULL AS discord_city_role"
    );

    public static void discordUpdate(Action4<Long, String, Long[], UUID> callback, Action0 end) {
        discordUpdate(discordUpdateSQL
                .replace("{CITY_FIELDS}", CITY_ENABLE ? discordUpdateSQL_City_Fields : discordUpdateSQL_City_None_Fields)
                + (CITY_ENABLE ? discordUpdateSQL_City : ""), callback, end);
    }
    public static void discordUpdateSingle(UUID uuid, Action4<Long, String, Long[], UUID> callback, Action0 end) {
        discordUpdate(discordUpdateSQL
                .replace("{CITY_FIELDS}", CITY_ENABLE ? discordUpdateSQL_City_Fields : discordUpdateSQL_City_None_Fields)
                + (CITY_ENABLE ? discordUpdateSQL_City : "")
                + " WHERE users.uuid = '" + uuid + "'", callback, end);
    }
    private static void discordUpdate(String sql, Action4<Long, String, Long[], UUID> callback, Action0 end) {
        SQL.Async.rawSqlQuery(sql,
                v -> Toast.of(
                        MySql.readObject(v, "discord_id", Long.class),
                        MySql.readObject(v, "uuid", String.class),
                        MySql.readObject(v, "user_name", String.class),
                        MySql.readObject(v, "discord_role", Long.class),
                        MySql.readObject(v, "discord_group_role", Long.class),
                        MySql.readObject(v, "discord_city_role", Long.class)
                ),
                list -> {
                    list.forEach(t -> callback.invoke(t.val0, t.val2, new Long[] { t.val3, t.val4, t.val5 }, UUID.fromString(t.val1)));
                    end.invoke();
                });
    }

    public static void discordFind(UUID uuid, Action1<Long> callback) {
        SQL.Async.rawSqlOnce("SELECT discord.discord_id FROM discord WHERE discord.uuid = '" + uuid + "'", Long.class, callback);
    }
    public static void discordFind(long dsid, Action1<UUID> callback) {
        SQL.Async.rawSqlOnce("SELECT discord.`uuid` FROM discord WHERE discord.discord_id = " + dsid, String.class, uuid -> callback.invoke(ExtMethods.parseUUID(uuid).orElse(null)));
    }
    public static void discordClear(long discordId, Action0 callback) {
        SQL.Async.rawSql("DELETE FROM discord WHERE discord.discord_id = " + discordId, callback);
    }

    public enum SoundFillStart {
        CHECK,
        DOWNLOAD,
        CONVERT,
        CONVERT_BIF,
        SAVE,
        DONE,
        ERROR
    }
    public static void recorderReset(int id, Action0 action) {
        SQL.Async.rawSql("UPDATE recorder SET recorder.url = @url WHERE recorder.id = @id",
                MySql.args()
                        .add("url", null)
                        .add("id", id)
                        .build(),
                action);
    }
    public static void recorderFill(UUID uuid, RecorderInstance.AudioType type, String url, String name, Action2<SoundFillStart, String> action) {
        action.invoke(SoundFillStart.CHECK, "");
        SQL.Async.rawSqlOnce("SELECT RecorderFirst('"+uuid+"', @url)", MySql.args().add("url", url).build(), Integer.class, id -> {
            if (id < 0) {
                action.invoke(SoundFillStart.ERROR, "У вас нету активных запросов на установку музыки");
                return;
            }
            action.invoke(SoundFillStart.DOWNLOAD, "");
            web.method.GET.create(url)
                    .data()
                    .executeAsync((bytes, code) -> {
                        if (bytes == null) {
                            recorderReset(id, () -> {});
                            action.invoke(SoundFillStart.ERROR, "Ошибка скачивания #" + code);
                            return;
                        }
                        action.invoke(SoundFillStart.CONVERT, "");
                        Toast1<Integer> lastProgress = Toast.of(-1);
                        lime.invokeAsync(() -> RecorderInstance.createSoundFile(type, (current, total) -> {
                            int currentProgress = ((current * 100 / total) / 5) * 5;
                            if (currentProgress == lastProgress.val0) return;
                            lastProgress.val0 = currentProgress;
                            action.invoke(SoundFillStart.CONVERT_BIF, "[" + StringUtils.leftPad(currentProgress + "", 3, ' ') + "%] Обработка файла 3 / 5 | " + current + " / " + total + "...");
                        }, bytes).goodOrError(good -> {
                            action.invoke(SoundFillStart.SAVE, "");
                            SQL.Async.rawSql("UPDATE recorder SET recorder.sound = @sound, recorder.time = @time, recorder.name = @name WHERE recorder.id = @id",
                                    MySql.args()
                                            .add("sound", good.soundUUID())
                                            .add("time", good.totalSec())
                                            .add("name", name)
                                            .add("id", id)
                                            .build(),
                                    () -> action.invoke(SoundFillStart.DONE, ""));
                        }, error -> {
                            recorderReset(id, () -> {});
                            action.invoke(SoundFillStart.ERROR, "Ошибка чтения файла: " + error.text());
                        }), () -> { });
                    });
        });
    }

    public static class PayDayInput implements IJson<JsonObject> {
        private final JsonObject json;
        private PayDayInput(JsonObject json) { this.json = json; }
        public JsonObject toJson() { return json; }

        public static PayDayInput create() { return new PayDayInput(org.lime.system.json.object().build()); }
    }
    public static class PayDayOutput extends IJson.ILoad<JsonObject> {
        public final int level;
        public final int exp;
        public final int next;
        protected PayDayOutput(JsonObject json) {
            super(json);
            level = json.get("level").getAsInt();
            exp = json.get("exp").getAsInt();
            next = json.get("next").getAsInt();
        }

        public HashMap<String, String> args() {
            return map.<String, String>of()
                    .add("level", String.valueOf(level))
                    .add("exp", String.valueOf(exp))
                    .add("next", String.valueOf(next))
                    .build();
        }
    }
    public static void payDay(HashMap<UUID, PayDayInput> input, Action1<HashMap<UUID, PayDayOutput>> callback) {
        SQL.Async.rawSqlOnce("SELECT PayDay(@json)",
                MySql.args().add("json", IJson.toJson(input, UUID::toString).toString()).build(),
                String.class,
                v -> callback.invoke(PayDayOutput.parse(PayDayOutput::new, json.parse(v).getAsJsonObject(), UUID::fromString))
        );
    }

    private static final String DonateVIP_SQL = "SELECT * FROM donate_vip";
    public static void donateVip(Action1<HashMap<UUID, TabManager.DonateInfo>> callback) {
        SQL.Async.rawSqlQuery(
                DonateVIP_SQL,
                set -> Toast.of(UUID.fromString(MySql.readObject(set, "uuid", String.class)), MySql.readObject(set, "static_id", Integer.class)),
                list -> callback.invoke(map.<UUID, TabManager.DonateInfo>of().add(list, kv -> kv.val0, kv -> new TabManager.DonateInfo(Optional.ofNullable(kv.val1))).build())
        );
    }

    public static void setIsLogPrison(int id, boolean isLog, Action0 callback) {
        SQL.Async.rawSql("UPDATE prison SET is_log = "+(isLog ? 1 : 0)+" WHERE id = " + id, callback);
    }

    public static void syncUserCrafts(int craftID, int useCount) {
        SQL.Async.rawSql("UPDATE user_crafts SET use_count = "+useCount+" WHERE id = " + craftID, () -> {});
    }

    private static void banUser(BanListRow.Type type, String user, String reason, String owner, Action0 callback) {
        SQL.Async.rawSql("INSERT INTO ban_list (ban_list.`type`, ban_list.user, ban_list.reason, ban_list.owner) VALUES (@type, @user, @reason, @owner)",
                MySql.args()
                        .add("type", type.name())
                        .add("user", user)
                        .add("reason", reason)
                        .add("owner", owner)
                        .build(),
                callback);
    }
    public static void banUser(UUID uuid, String reason, String owner, Action0 callback) {
        banUser(BanListRow.Type.UUID, uuid.toString(), reason, owner, callback);
    }
    public static void banUser(InetAddress ip, String reason, String owner, Action0 callback) {
        banUser(BanListRow.Type.IP, ip.getHostAddress(), reason, owner, callback);
    }

    private static void pardonUser(BanListRow.Type type, String user, Action0 callback) {
        SQL.Async.rawSql("DELETE FROM ban_list WHERE ban_list.`type` = @type AND ban_list.user = @user",
                MySql.args()
                        .add("type", type.name())
                        .add("user", user)
                        .build(),
                callback);
    }
    public static void pardonUser(UUID uuid, Action0 callback) {
        pardonUser(BanListRow.Type.UUID, uuid.toString(), callback);
    }
    public static void pardonUser(InetAddress ip, Action0 callback) {
        pardonUser(BanListRow.Type.IP, ip.getHostAddress(), callback);
    }

    public static void ipListByUUIDs(Collection<UUID> uuids, Action1<Set<InetAddress>> ips) {
        if (uuids.isEmpty()) {
            ips.invoke(Collections.emptySet());
            return;
        }
        SQL.Async.rawSqlQuery("SELECT ip_list.ip FROM ip_list WHERE ip_list.uuid IN ('"+uuids.stream().map(UUID::toString).collect(Collectors.joining("','"))+"') GROUP BY ip_list.ip",
                set -> Execute.funcEx(()->InetAddress.getByName(MySql.readObject(set, "ip", String.class))).optional().invoke(),
                list -> ips.invoke(list.stream().flatMap(Optional::stream).collect(Collectors.toSet())));
    }
    public static void predonateWhitelist(Collection<String> append, Collection<String> ignored) {
        SQL.Async.rawSql("UPDATE predonate SET predonate.whitelist = 'GIVED' WHERE predonate.name IN @append", MySql.args().add("append", new ArrayList<>(append)).build(), () -> {});
        SQL.Async.rawSql("UPDATE predonate SET predonate.whitelist = 'ERROR' WHERE predonate.name IN @ignored", MySql.args().add("ignored", new ArrayList<>(ignored)).build(), () -> {});
    }

    public static void bankOPG(int totalSafe, Action1<Integer> callback) {
        SQL.Async.rawSqlOnce("SELECT BankSafeBoxOPG("+totalSafe+")", Integer.class, callback);
    }
    public static void bankReturnOPG(int count, Action0 callback) {
        SQL.Async.rawSql("SELECT BankSafeBoxReturn("+count+")", callback);
    }

    public static void addDonateSPCoin(UUID uuid, int count) {
        SQL.Async.rawSql("SELECT DonateSPCoinUUID('"+uuid+"', "+count+")", () -> {});
    }
    
    public static void appendDeltaLevel(int user_id, int work, double deltaExp) {
        SQL.Async.rawSql(
            String.join(" ",
                "INSERT INTO level (level.user_id, level.work, level.level, level.exp)",
                "VALUES (@user_id, @work, IF(@exp >= 1, 1, 0), IF(@exp >= 1, 0, @exp))",
                "ON DUPLICATE KEY UPDATE",
                "level.level = IF (level.exp + @exp >= 1, level.level + 1, level.level),",
                "level.exp = IF (level.exp + @exp >= 1, 0, level.exp + @exp)"
            ),
            MySql.args()
                .add("user_id", user_id)
                .add("work", work)
                .add("exp", deltaExp)
                .build(),
            () -> {}
        );
    }
    public static void removeDeltaLevel(int user_id, int work, double deltaExp) {
        SQL.Async.rawSql(
            String.join(" ",
                "INSERT INTO level (level.user_id, level.work, level.level, level.exp)",
                "VALUES (@user_id, @work, 0, 0)",
                "ON DUPLICATE KEY UPDATE",
                "level.level = GREATEST(0, FLOOR(level.level + level.exp - @exp)),",
                "level.exp = GREATEST(0, level.level + level.exp - @exp - FLOOR(level.level + level.exp - @exp))"
            ),
            MySql.args()
                .add("user_id", user_id)
                .add("work", work)
                .add("exp", deltaExp)
                .build(),
            () -> {}
        );
    }

    public static void rejoinCreate(String name, @Nullable UUID owner, Action0 callback) {
        SQL.Async.rawSql(
                "INSERT INTO rejoin (name, owner) VALUES (@name, @owner)",
                MySql.args().add("name", name).add("owner", owner).build(),
                callback);
    }
    public static void rejoinDelete(String identifier, Action0 callback) {
        SQL.Async.rawSql(
                "DELETE FROM rejoin WHERE CONCAT(rejoin.name,':',rejoin.index) = @identifier",
                MySql.args().add("identifier", identifier).build(),
                callback);
    }
    public static void rejoinSet(String identifier, @Nullable UUID owner, Action0 callback) {
        SQL.Async.rawSql(
                "UPDATE rejoin SET rejoin.owner = @owner WHERE CONCAT(rejoin.name,':',rejoin.index) = @identifier",
                MySql.args().add("identifier", identifier).add("owner", owner).build(),
                callback);
    }
    public static void rejoinRename(String identifier, String name, Action0 callback) {
        SQL.Async.rawSql(
                "UPDATE rejoin SET rejoin.name = @name WHERE CONCAT(rejoin.name,':',rejoin.index) = @identifier",
                MySql.args().add("identifier", identifier).add("name", name).build(),
                callback);
    }
    public static void rejoinSelect(UUID owner, @Nullable String identifier, Action0 callback) {
        SQL.Async.rawSql(
                "UPDATE rejoin SET rejoin.select = IF(CONCAT(rejoin.name,':',rejoin.index) = @identifier,1,0) WHERE rejoin.owner = @owner",
                MySql.args().add("identifier", identifier).add("owner", owner).build(),
                callback);
    }

    public static void hasteDonate(Action1<Calendar> callback) {
        SQL.Async.rawSqlOnce(
                String.join(" ",
                "SELECT TIMESTAMPADD(SECOND, SUM(dl.time), donate_list.date) AS `end_time`",
                        "FROM donate_list",
                        "JOIN donate_list dl",
                        "WHERE dl.`type` = 'HASTE_RTS' AND donate_list.type = 'HASTE_RTS' AND dl.date >= donate_list.date",
                        "GROUP BY donate_list.id",
                        "ORDER BY TIMESTAMPADD(SECOND, SUM(dl.time), donate_list.date) DESC",
                        "LIMIT 1"),
                Calendar.class,
                callback);
    }
}




















