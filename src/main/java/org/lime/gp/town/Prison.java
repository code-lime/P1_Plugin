package org.lime.gp.town;

import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.admin.Administrator;
import org.lime.gp.database.Methods;
import org.lime.gp.database.rows.HouseRow;
import org.lime.gp.database.rows.PrisonRow;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.lime;
import org.lime.gp.module.EntityPosition;
import org.lime.system.Time;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.MathUtils;

public class Prison implements Listener {
    public static CoreElement create() {
        return CoreElement.create(Prison.class)
                .withInit(Prison::init)
                .withInstance();
    }

    public static void init() {
        /*AnyEvent.AddEvent("prison.put", builder -> builder.CreateParam(Integer::parseInt, "[user_id]").CreateParam(Integer::parseInt, "[prison_id]").CreateParam(Integer::parseInt, "[time_min]"), (player, user_id, prison_id, time_min) -> {
            DataReader.UserRow user = DataReader.USER_TABLE.GetByID(user_id);
            if (user == null) return;
            DataReader.HouseRow house = DataReader.HOUSE_TABLE.GetByID(prison_id);
            if (house == null) return;
            DataReader.UserRow owner = DataReader.USER_TABLE.GetByUUID(player.getUniqueId());
            if (owner == null) return;
            DataReader.AddPrison(prison_id, user_id, owner.ID, new ZoneShow.Position(player.getLocation().toVector()), time_min, () -> {});
        });
        AnyEvent.AddEvent("prison.pick", builder -> builder.CreateParam(Integer::parseInt, "[user_id]").CreateParam(Integer::parseInt, "[prison_id]"), (player, user_id, prison_id) -> {
            DataReader.PrisonRow row = DataReader.PRISON_TABLE.GetByData(prison_id, user_id);
            if (row == null) return;
            DataReader.ResetPrison(row.ID, () -> {});
        });
        AnyEvent.addEvent("out.pos", AnyEvent.type.other, builder -> builder.createParam(Integer::parseInt, "[house_id]").createParam("set", "del"), (player, house_id, method) -> {
            switch (method) {
                case "set": MainSelector.create(b -> true, (p, f) -> {
                    LangMessages.Message.Phone_Prison_OutSelect.sendMessage(player);
                    DataReader.SetOutPos(house_id, p.Add(f).toVector());
                }).select(player); break;
                case "del": DataReader.SetOutPos(house_id, null); break;
            }
        });*/
        lime.repeat(Prison::update, 1);
    }
    private static Location getOutPos(PrisonRow prison, HouseRow house) {
        try {
            JsonObject data = house.data;
            if (data != null && data.has("out")) return MathUtils.getLocation(prison.outPos.world, data.get("out").getAsString());
            if (house.isRoom) {
                Location loc = Tables.HOUSE_TABLE.get(String.valueOf(house.street))
                        .map(v -> v.data)
                        .filter(v -> v.has("out"))
                        .map(v -> v.get("out").getAsString())
                        .map(v -> MathUtils.getLocation(prison.outPos.world, v))
                        .orElse(null);
                if (loc != null) return loc;
                /*Rows.HouseRow o_house = Tables.HOUSE_TABLE.get(house.Street + "");
                data = o_house == null ? null : o_house.Data;
                if (data != null && data.has("out")) return system.getLocation(prison.OutPos.world, data.get("out").getAsString());*/
            }
        } catch (Exception ignored) { }
        return prison.outPos.getLocation();
    }
    public static void update() {
        for (PrisonRow row : Tables.PRISON_TABLE.getRows()) {
            if (row.isLog) continue;
            Tables.HOUSE_TABLE.get(String.valueOf(row.houseID)).ifPresent(house -> Tables.USER_TABLE.get(String.valueOf(row.userID)).ifPresent(user -> {
                Player player = EntityPosition.onlinePlayers.getOrDefault(user.uuid, null);
                if (player == null) return;
                if (Administrator.inABan(user.uuid)) return;
                if (Time.compareCalendar(row.endTime, Time.moscowNow()) == -1) {
                    Methods.setIsLogPrison(row.id, true, () -> player.teleport(getOutPos(row, house)));
                } else {
                    Location location = player.getLocation();
                    if (house.inZone(location)) return;
                    player.teleport(house.center());
                }
            }));
        }
    }
}

































