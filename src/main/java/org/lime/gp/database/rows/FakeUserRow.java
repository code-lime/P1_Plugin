package org.lime.gp.database.rows;

import net.minecraft.core.UUIDUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.lime.gp.database.mysql.MySql;
import org.lime.gp.database.mysql.MySqlRow;
import org.lime.gp.lime;
import org.lime.gp.module.npc.eplayer.Pose;
import org.lime.gp.player.module.DeathGame;
import org.lime.gp.player.module.Skins;
import org.lime.gp.player.module.TabManager;
import org.lime.system.utils.MathUtils;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FakeUserRow extends BaseRow {
    public final UUID unique;

    public final int id;
    public final int userID;
    public final int timedID;
    public final UUID uuid;
    public final String userName;
    public final Location location;
    public final UUID world;
    public final Pose pose;
    public final @Nullable String skin;
    public final String equipment;
    public final String serverIndex;
    public final int lifeTime;
    public final @Nullable String status;
    public final boolean autoRemove;

    public FakeUserRow(MySqlRow set) {
        super(set);
        this.id = MySql.readObject(set, "id", Integer.class);
        this.userID = MySql.readObject(set, "user_id", Integer.class);
        this.timedID = MySql.readObject(set, "timed_id", Integer.class);
        this.uuid = MySql.readObject(set, "uuid", UUID.class);
        this.userName = MySql.readObject(set, "user_name", String.class);
        this.world = MySql.readObject(set, "world", UUID.class);
        this.location = MathUtils.getLocation(Bukkit.getWorld(this.world), MySql.readObject(set, "location", String.class), false);
        this.pose = MySql.readObject(set, "pose", Pose.class);
        this.skin = MySql.readObject(set, "skin", String.class);
        this.equipment = MySql.readObject(set, "equipment", String.class);
        this.serverIndex = MySql.readObject(set, "server_index", String.class);
        this.lifeTime = MySql.readObject(set, "life_time", Integer.class);
        this.status = MySql.readObject(set, "status", String.class);
        this.autoRemove = MySql.readObject(set, "auto_remove", Boolean.class);
        this.unique = UUIDUtil.uuidFromIntArray(new int[] { this.id, this.userID, this.timedID, this.serverIndex.hashCode() });
    }
    public FakeUserRow(int id, Player player, String serverIndex, int lifeTime, boolean autoRemove) {
        super(null);
        this.id = id;
        this.userID = UserRow.getBy(player).map(v -> v.id).orElse(0);
        this.timedID = TabManager.getPayerID(player.getUniqueId());
        this.uuid = player.getUniqueId();
        this.userName = player.getName();
        this.location = player.getLocation();
        this.world = this.location.getWorld().getUID();
        if (lime.isSit(player)) {
            this.location.add(0, 6f/16f, 0);
            this.pose = Pose.SIT;
        } else if (lime.isCrawl(player)) {
            this.pose = Pose.CRAWL;
        } else {
            this.pose = Pose.NONE;
        }
        this.skin = Skins.getProperty(player).toJson().toString();
        this.equipment = DeathGame.saveEquipment(player.getInventory(), List.of());
        this.serverIndex = serverIndex;
        this.lifeTime = lifeTime;
        this.status = null;
        this.autoRemove = autoRemove;
        this.unique = UUIDUtil.uuidFromIntArray(new int[] { this.id, this.userID, this.timedID, this.serverIndex.hashCode() });
    }

    public Map<String, Object> rawColumns() {
        Map<String, Object> map = new HashMap<>();
        map.put("user_id", userID);
        map.put("timed_id", timedID);
        map.put("uuid", uuid);
        map.put("user_name", userName);
        map.put("world", world);
        map.put("location", MathUtils.getString(location));
        map.put("pose", pose.name());
        map.put("skin", skin);
        map.put("equipment", equipment);
        map.put("server_index", serverIndex);
        map.put("status", status);
        map.put("auto_remove", autoRemove);
        map.put("life_time", lifeTime);
        return map;
    }

    @Override public Map<String, String> appendToReplace(Map<String, String> map) {
        map = super.appendToReplace(map);
        map.put("id", String.valueOf(id));
        map.put("user_id", String.valueOf(userID));
        map.put("timed_id", String.valueOf(timedID));
        map.put("uuid", uuid.toString());
        map.put("user_name", userName);
        map.put("world", world.toString());
        map.put("location", MathUtils.getString(location));
        map.put("pose", pose.name());
        map.put("skin", skin);
        map.put("equipment", equipment);
        map.put("server_index", serverIndex);
        map.put("status", status);
        map.put("auto_remove", String.valueOf(autoRemove));
        map.put("life_time", String.valueOf(lifeTime));
        return map;
    }
}