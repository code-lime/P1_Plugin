package org.limeold.gp.web;

import org.lime.gp.database.MySql;
import org.lime_old.gp.*;
import org.lime.system;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.lime.gp.extension.Zone;

import java.util.*;

public final class DataReader {



    public static void wantedModify(int user_id, int delta, system.Action0 callback) {
        SQL.Async.rawSql("UPDATE users SET users.wanted = GetByRange(0, users.wanted + ("+delta+"), 5) WHERE users.id = " + user_id, callback);
    }
    public static void wantedModify(int user_id, int delta, String reason, system.Action0 callback) {
        SQL.Async.rawSql("UPDATE users SET users.wanted = GetByRange(0, users.wanted + ("+delta+"), 5), users.wanted_reason = @wanted_reason WHERE users.id = " + user_id, MySql.args().add("wanted_reason", reason).build(), callback);
    }
    public static void AddUser(UUID uuid, String user_name, String first_name, String last_name, boolean isMale, Calendar birthday_date, Calendar create_date, system.Action1<UserRow> callback) {
        SQL.Async.rawSql(
                "INSERT INTO users (uuid,user_name,first_name,last_name,birthday_date,create_date,connect_date) VALUES (@uuid,@user_name,@first_name,@last_name,@birthday_date,@create_date,@create_date)",
                MySql.args(
                        system.toast("uuid", uuid.toString()),
                        system.toast("user_name", user_name),
                        system.toast("first_name", first_name),
                        system.toast("last_name", last_name),
                        system.toast("male", isMale ? 1 : 0),
                        system.toast("birthday_date", birthday_date),
                        system.toast("create_date", create_date)
                ), () -> SQL.Async.rawSqlOnce("SELECT * FROM users WHERE uuid = @uuid", MySql.args(system.toast("uuid", uuid)), UserRow::new, callback));
    }

    public static void SetRole(int user_id, int role_id, system.Action0 callback) {
        SQL.Async.rawSql("UPDATE users SET role = "+role_id+" WHERE id = " + user_id, callback);
    }

    public static void AddFriend(UUID uuid, int friend_id, String name, system.Action0 callback) {
        SQL.Async.rawSql("INSERT INTO friends (friends.user_id, friends.friend_id, friends.friend_name) VALUES (GetByUUID(@uuid), @friend_id, @name)", MySql.args(system.toast("uuid", uuid.toString()), system.toast("friend_id", friend_id), system.toast("name", name)), callback);
    }
    public static void DeleteFriend(int user_id, int friend_id, system.Action0 callback) {
        SQL.Async.rawSql("DELETE FROM friends WHERE friend_id = @friend_id AND user_id = @user_id", MySql.args(system.toast("friend_id", friend_id), system.toast("user_id", user_id)), callback);
    }
    public static void RenameFriend(int user_id, int friend_id, String name, system.Action0 callback) {
        SQL.Async.rawSql("UPDATE friends SET friend_name = @name WHERE friend_id = @friend_id AND user_id = @user_id", MySql.args(system.toast("friend_id", friend_id), system.toast("user_id", user_id), system.toast("name", name)), callback);
    }

    public static void AddTown(String type, String name, int id, system.Action1<String> callback) {
        SQL.Async.rawSqlOnce("SELECT AddTown(@type, @name, @id)", MySql.args(
                system.toast("type", type),
                system.toast("name", name),
                system.toast("id", String.valueOf(id))
        ), String.class, callback);
    }
    public static void AddTownHouse(String name, int id, boolean isRoom, Zone.Position pos1, Zone.Position pos2, Zone.Position posMain, BlockFace posFace, system.Action0 callback) {
        SQL.Async.rawSql("INSERT INTO house (street,is_room,name,pos1_x,pos1_y,pos1_z,pos2_x,pos2_y,pos2_z,posMain_x,posMain_y,posMain_z,posFace) VALUES (@street,@is_room,@name,@pos1_x,@pos1_y,@pos1_z,@pos2_x,@pos2_y,@pos2_z,@posMain_x,@posMain_y,@posMain_z,@posFace)", MySql.args(
                system.toast("street", id),
                system.toast("is_room", isRoom ? 1 : 0),
                system.toast("name", name),
                system.toast("pos1_x", pos1.X),
                system.toast("pos1_y", pos1.Y),
                system.toast("pos1_z", pos1.Z),
                system.toast("pos2_x", pos2.X),
                system.toast("pos2_y", pos2.Y),
                system.toast("pos2_z", pos2.Z),
                system.toast("posMain_x", posMain.X),
                system.toast("posMain_y", posMain.Y),
                system.toast("posMain_z", posMain.Z),
                system.toast("posFace", posFace.name())
        ), callback);
    }

    public static void RenameTown(String type, int id, String name, system.Action1<String> callback) {
        SQL.Async.rawSqlOnce("SELECT RenameTown(@type, @name, @id)", MySql.args(
                system.toast("type", type),
                system.toast("name", name),
                system.toast("id", String.valueOf(id))
        ), String.class, callback);
    }
    public static void DeleteTown(String type, int id, system.Action1<String> callback) {
        SQL.Async.rawSqlOnce("SELECT DeleteTown(@type, @id)", MySql.args(
                system.toast("type", type),
                system.toast("id", String.valueOf(id))
        ), String.class, callback);
    }

    public static void GetATM(Vector location, system.Action2<Integer, Integer> callback) {
        SQL.Async.rawSqlOnce("SELECT atm.id AS 'id', atm.state AS 'state' FROM atm WHERE atm.pos_x = "+location.getBlockX()+" AND atm.pos_y = "+location.getBlockY()+" AND atm.pos_z = "+location.getBlockZ(),
                set -> system.toast(MySql.readObject(set, "id", Integer.class), MySql.readObject(set, "state", Integer.class)),
                data -> {
                    if (data == null) return;
                    callback.invoke(data.val0, data.val1);
                }
        );
    }
    public static void DeleteATM(int id, system.Action0 callback) {
        SQL.Async.rawSql("DELETE FROM atm WHERE id = @id;", MySql.args(system.toast("id", id)), callback);
    }
    public static void ExistATM(int id, system.Action1<Boolean> callback) {
        SQL.Async.rawSqlQuery("SELECT * FROM atm WHERE id = @id", MySql.args(system.toast("id", id)), AtmRow::new, rows -> callback.invoke(rows.size() > 0));
    }
    public static void AddATM(Zone.Position pos, system.Action0 callback) {
        SQL.Async.rawSql("INSERT INTO atm (pos_x,pos_y,pos_z) VALUES (@x,@y,@z)", MySql.args(system.toast("x", pos.X), system.toast("y", pos.Y), system.toast("z", pos.Z)), callback);
    }
    public static void WithdrawATM(int id, int atm_id, int cost, system.Action1<Integer> state) {
        SQL.Async.rawSqlOnce("SELECT WithdrawATM(@id,@atm_id,@cost)", MySql.args(system.toast("id", id), system.toast("atm_id", atm_id), system.toast("cost", cost)), Integer.class, state);
    }
    public static void PutATM(int id, int atm_id, int cost, system.Action1<Integer> state)  {
        SQL.Async.rawSqlOnce("SELECT PutATM(@id,@atm_id,@cost)", MySql.args(system.toast("id", id), system.toast("atm_id", atm_id), system.toast("cost", cost)), Integer.class, state);
    }
    public static void InsertATM(int atm_id, int cost, system.Action0 state)  {
        SQL.Async.rawSql("UPDATE atm SET atm.balance = atm.balance + " + cost + " WHERE atm.id = " + atm_id, state);
    }
    public static void PutTownDistrict(int atm_id, int district_id, int cost, system.Action0 state) {
        SQL.Async.rawSql("UPDATE district SET district.balance = district.balance + " + cost + " WHERE district.id = " + district_id, () -> {
            SQL.Async.rawSql("UPDATE atm SET atm.balance = atm.balance + " + cost + " WHERE atm.id = " + atm_id, state);
        });
    }
    public static void WithdrawTownDistrict(int atm_id, int district_id, int cost, system.Action1<Integer> state) {
        SQL.Async.rawSqlOnce("SELECT WithdrawDistrict(@id,@atm_id,@cost)", MySql.args(system.toast("id", district_id), system.toast("atm_id", atm_id), system.toast("cost", cost)), Integer.class, state);
    }
    public static void StateATM(int atm_id, int state, system.Action0 callback) {
        SQL.Async.rawSql("UPDATE atm SET atm.state = " + state + " WHERE atm.id = " + atm_id, callback);
    }

    public static void ModifyTown(String arg, int value, int id, system.Action1<String> callback) {
        SQL.Async.rawSqlOnce("SELECT ModifyTown(@arg,@value,@id)", MySql.args(
                system.toast("arg", arg),
                system.toast("value", value),
                system.toast("id", String.valueOf(id))
        ), String.class, callback);
    }
    public static void PrivateTown(int house_id, long value, system.Action0 callback) {
        SQL.Async.rawSql("UPDATE house SET house.private = " + value + " WHERE house.id = " + house_id, callback);
    }
    public static void BuyTown(int user_id, int house_id, system.Action1<String> callback) {
        SQL.Async.rawSqlOnce("SELECT BuyTown(@user_id,@house_id)", MySql.args(
                system.toast("user_id", user_id),
                system.toast("house_id", house_id)
        ), String.class, callback);
    }
    public static void AddTownSub(int house_id, int sub, system.Action0 callback) {
        SQL.Async.rawSql("INSERT INTO house_subs (user_id,house_id) VALUES (@user_id,@house_id)", MySql.args(system.toast("user_id", sub), system.toast("house_id", house_id)), callback);
    }
    public static void DelTownSub(int house_id, int sub, system.Action0 callback) {
        SQL.Async.rawSql("DELETE FROM house_subs WHERE user_id = @user_id AND house_id = @house_id", MySql.args(system.toast("user_id", sub), system.toast("house_id", house_id)), callback);
    }

    public static void AddPrison(int house_id, int user_id, int owner_id, Zone.Position out_pos, int prisonTime, system.Action0 callback) {
        Calendar calendar = system.getMoscowNow();
        calendar.add(Calendar.MINUTE, prisonTime);
        SQL.Async.rawSql("INSERT INTO prison " +
                "(house_id,user_id,owner_id,out_pos_x,out_pos_y,out_pos_z,end_time) VALUES " +
                "(@house_id,@user_id,@owner_id,@out_pos_x,@out_pos_y,@out_pos_z,@end_time)",
                MySql.args(
                        system.toast("house_id", house_id),
                        system.toast("user_id", user_id),
                        system.toast("owner_id", owner_id),
                        system.toast("out_pos_x", out_pos.X),
                        system.toast("out_pos_y", out_pos.Y),
                        system.toast("out_pos_z", out_pos.Z),
                        system.toast("end_time", calendar)
                ), callback);
    }
    public static void ResetPrison(int id, system.Action0 callback) {
        SQL.Async.rawSql("UPDATE prison SET prison.end_time = '2000-01-01 00:00:00' WHERE id = " + id, callback);
    }

    public static void SetOutPos(int id, Vector pos) {
        SQL.Async.rawSql("SELECT SetOutPos(@id,@pos)", MySql.args(
                system.toast("id", id),
                system.toast("pos", pos == null ? null : system.getString(pos))
        ), () -> {});
    }

}





























