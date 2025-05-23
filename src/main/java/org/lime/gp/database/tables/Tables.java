package org.lime.gp.database.tables;

import org.lime.gp.player.inventory.TownInventory;
import org.lime.gp.player.module.DeathGame;
import org.lime.plugin.CoreElement;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.admin.BanList;
import org.lime.gp.craft.book.RecipesBook;
import org.lime.gp.database.Methods;
import org.lime.gp.database.mysql.ExecuteData;
import org.lime.gp.database.rows.*;
import org.lime.gp.module.JavaScript;
import org.lime.gp.lime;
import org.lime.gp.player.module.PredonateWhitelist;
import org.lime.gp.player.perm.Perms;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.*;
import java.util.stream.Collectors;

public class Tables {
    public static String valueOfInt(Integer v) {
        return v == null ? "" : String.valueOf(v);
    }

    public static CoreElement create() {
        return CoreElement.create(Tables.class)
                .withInit(Tables::init);
    }

    public static void init() {
        AnyEvent.addEvent("tables.resync", AnyEvent.type.owner_console, player -> {
            KeyedTable.resyncAll();
            lime.logOP("All tables resynced!");
        });
        lime.repeat(Tables::update, 1);
    }

    public static void update() {
        KeyedTable.updateAll();
    }

    public static ExecuteData getTable(String table, Action1<ITable<? extends BaseRow>> callback) {
        if (table.startsWith("!sql "))
            return Methods.SQL.Async.rawSqlQuery(table.substring(5), AnyRow::new,
                    rows -> callback.invoke(new StaticTable<>(rows)));
        if (table.startsWith("!json ")) {
            JavaScript.getJsString(table.substring(6))
                .map(json::parse)
                .ifPresent(json -> {
                    List<AnyRow> rows = new ArrayList<>();
                    json.getAsJsonArray().forEach(item -> {
                        HashMap<String, String> columns = new HashMap<>();
                        item.getAsJsonObject().asMap().forEach((key, value) -> {
                            columns.put(key, value.getAsString());
                        });
                        rows.add(new AnyRow(columns));
                    });
                    callback.invoke(new StaticTable<>(rows));
                });
            return new ExecuteData();
        }
        callback.invoke(KeyedTable.tables.get(table));
        return new ExecuteData();
    }

    public static KeyedTable<? extends BaseRow> getLoadedTable(String table) {
        return KeyedTable.tables.getOrDefault(table, null);
    }

    public static List<Toast2<String, String>> getListRow(String prefix, BaseRow row) {
        return row.appendToReplace(new HashMap<>()).entrySet().stream()
                .map(kv -> Toast.of(prefix + kv.getKey(), kv.getValue())).collect(Collectors.toList());
    }

    public static final KeyedTable<AAnyRow> ABAN_TABLE = KeyedTable.of("aban", AAnyRow::new)
            .keyed("id", v -> String.valueOf(v.id))
            .build();
    public static final KeyedTable<AAnyRow> AMUTE_TABLE = KeyedTable.of("amute", AAnyRow::new)
            .keyed("id", v -> String.valueOf(v.id))
            .build();
    public static final KeyedTable<RolesRow> ROLES_TABLE = KeyedTable.of("roles", RolesRow::new)
            .keyed("id", v -> String.valueOf(v.id))
            .build();
    public static final KeyedTable<GroupRow> GROUPS_TABLE = KeyedTable.of("role_groups", GroupRow::new)
            .keyed("id", v -> String.valueOf(v.id))
            .build();
    public static final KeyedTable<UserRow> USER_TABLE = KeyedTable.of("users", UserRow::new)
            .keyed("id", v -> String.valueOf(v.id))
            .other("uuid", v -> v.uuid.toString())
            .other("user_name", v -> v.userName)
            .event(KeyedTable.Event.Removed, RecipesBook::editRow)
            .event(KeyedTable.Event.Updated, RecipesBook::editRow)
            .build();
    public static final KeyedTable<HouseRow> HOUSE_TABLE = KeyedTable.of("house", HouseRow::new)
            .keyed("id", v -> String.valueOf(v.id))
            .build();
    public static final KeyedTable<HouseSubsRow> HOUSE_SUBS_TABLE = KeyedTable.of("house_subs", HouseSubsRow::new)
            .keyed("id", v -> String.valueOf(v.id))
            .build();
    public static final KeyedTable<FriendRow> FRIEND_TABLE = KeyedTable.of("friends", FriendRow::new)
            .where("friends.friend_name IS NOT NULL")
            .keyed("id", v -> String.valueOf(v.id))
            .build();

    public static final KeyedTable<PrisonRow> PRISON_TABLE = KeyedTable.of("prison", PrisonRow::new)
            .where("prison.is_log = 0")
            .keyed("id", v -> String.valueOf(v.id))
            .build();
    public static final KeyedTable<Variable> VARIABLE_TABLE = KeyedTable.of("variable", Variable::new)
            .keyed("tmp", v -> "0")
            .build();
    public static final KeyedTable<DiscordRow> DISCORD_TABLE = KeyedTable.of("discord", DiscordRow::new)
            .keyed("discord_id", v -> String.valueOf(v.discordID))
            .build();
    public static final KeyedTable<CompassTargetRow> COMPASS_TARGET_TABLE = KeyedTable
            .of("compass_target", CompassTargetRow::new)
            .keyed("id", v -> String.valueOf(v.id))
            .build();
    public static final KeyedTable<PetsRow> PETS_TABLE = KeyedTable.of("pets", PetsRow::new)
            .keyed("id", v -> String.valueOf(v.id))
            .build();
    public static final KeyedTable<PermissionRow> PERMISSIONS_TABLE = KeyedTable.of("permissions", PermissionRow::new)
            .keyed("uuid", v -> v.rawUuid)
            .event(KeyedTable.Event.Removed, PermissionRow::removed)
            .build();
    public static final KeyedTable<UserFlagsRow> USERFLAGS_TABLE = KeyedTable.of("user_flags", UserFlagsRow::new)
            .where("user_flags.backpack_id > 0")
            .keyed("id", v -> String.valueOf(v.id))
            .other("uuid", v -> v.uuid.toString())
            .build();
    public static final KeyedTable<UserCraftsRow> USERCRAFTS_TABLE = KeyedTable.of("user_crafts", UserCraftsRow::new)
            .keyed("id", v -> String.valueOf(v.id))
            .event(KeyedTable.Event.Removed, row -> UserRow.getBy(row.uuid).ifPresent(RecipesBook::editRow))
            .event(KeyedTable.Event.Updated, row -> UserRow.getBy(row.uuid).ifPresent(RecipesBook::editRow))
            .event(KeyedTable.Event.Removed, Perms::onUserCraftUpdate)
            .event(KeyedTable.Event.Updated, Perms::onUserCraftUpdate)
            .build();
    public static final KeyedTable<BanListRow> BANLIST_TABLE = KeyedTable.of("ban_list", BanListRow::new)
            .keyed("id", v -> String.valueOf(v.id))
            .other("user", v -> v.user)
            .event(KeyedTable.Event.Removed, BanList::onBanUpdate)
            .event(KeyedTable.Event.Updated, BanList::onBanUpdate)
            .build();
    public static final KeyedTable<PreDonateRow> PREDONATE_TABLE = KeyedTable.of("predonate", PreDonateRow::new)
            .keyed("id", v -> String.valueOf(v.id))
            .event(KeyedTable.Event.Removed, PredonateWhitelist::onUpdate)
            .event(KeyedTable.Event.Updated, PredonateWhitelist::onUpdate)
            .build();
    public static final KeyedTable<PreDonateItemsRow> PREDONATE_ITEMS_TABLE = KeyedTable
            .of("predonate_items", PreDonateItemsRow::new)
            .where("predonate_items.amount > 0")
            .keyed("id", v -> String.valueOf(v.id))
            .build();
    public static final KeyedTable<SmsPresetRow> SMSPRESET_TABLE = KeyedTable.of("sms_preset", SmsPresetRow::new)
            .keyed("id", v -> String.valueOf(v.id))
            .build();
    public static final KeyedTable<LevelRow> LEVEL_TABLE = KeyedTable.of("level", LevelRow::new)
            .keyed("id", v -> String.valueOf(v.id))
            .other("work", v -> v.userID + "^" + v.work)
            .build();
    public static final KeyedTable<ReJoinRow> REJOIN_TABLE = KeyedTable.of("rejoin", ReJoinRow::new)
            .keyed("index", v -> String.valueOf(v.index))
            .build();
    public static final KeyedTable<DeathRow> DEATH_TABLE = KeyedTable.of("death", DeathRow::new)
            .optional()
            .keyed("id", v -> String.valueOf(v.id))
            .other("user_id", v -> String.valueOf(v.userID))
            .event(KeyedTable.Event.Removed, DeathGame::onUpdate)
            .event(KeyedTable.Event.Updated, DeathGame::onUpdate)
            .build();
    public static final KeyedTable<CityRow> CITY_TABLE = KeyedTable.of("city", CityRow::new)
            .optional()
            .keyed("id", v -> String.valueOf(v.id))
            .build();
    public static final KeyedTable<QuentaRow> QUENTA_TABLE = KeyedTable.of("quenta", QuentaRow::new)
            .optional()
            .keyed("id", v -> String.valueOf(v.id))
            .other("user_id", v -> String.valueOf(v.userID))
            .build();

    public static final KeyedTable<PrivateHouseRow> PRIVATE_HOUSE = KeyedTable.of("private_house", PrivateHouseRow::new)
            .optional()
            .keyed("id", v -> String.valueOf(v.id))
            .event(KeyedTable.Event.Removed, TownInventory::onUpdate)
            .event(KeyedTable.Event.Updated, TownInventory::onUpdate)
            .build();
    public static final KeyedTable<PrivatePatternRow> PRIVATE_PATTERN = KeyedTable.of("private_pattern", PrivatePatternRow::new)
            .optional()
            .keyed("id", v -> String.valueOf(v.id))
            .event(KeyedTable.Event.Removed, TownInventory::onUpdate)
            .event(KeyedTable.Event.Updated, TownInventory::onUpdate)
            .build();
}
