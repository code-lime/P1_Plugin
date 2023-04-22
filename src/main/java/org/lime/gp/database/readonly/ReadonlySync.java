package org.lime.gp.database.readonly;

import com.google.gson.JsonArray;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.lime.core;
import org.lime.gp.admin.Administrator;
import org.lime.gp.lime;
import org.lime.gp.module.EntityPosition;
import org.lime.gp.player.module.Skins;
import org.lime.gp.player.perm.Works;
import org.lime.gp.player.selector.ISelector;
import org.lime.gp.player.selector.SelectorType;
import org.lime.gp.player.voice.Radio;
import org.lime.system;
import org.lime.gp.extension.JManager;
import org.lime.gp.item.settings.list.*;
import org.lime.gp.player.selector.UserSelector;
import org.lime.gp.player.module.TabManager;
import org.lime.gp.player.module.Death;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class ReadonlySync {
    public static core.element create() {
        return core.element.create(ReadonlySync.class)
                .withInit(ReadonlySync::init);
    }
    public static void init() {
        lime.repeat(ReadonlySync::update, 1);
    }
    public static void update() {
        ReadonlyTable.sync();
    }

    private static final ReadonlyTable<String> ONLINE_READONLY = ReadonlyTable.Builder.<String>of("online", "uuid")
            .withKey(String.class)
            .withKeys("uuid","x","y","z","world","timed_id","data_icon","data_name","is_op","zone_selector","die","gpose","hide","skin_url")
            .withData(() -> EntityPosition.onlinePlayers.entrySet().stream().collect(Collectors.toMap(kv -> kv.getKey().toString(), kv -> {
                UUID uuid = kv.getKey();
                Player player = kv.getValue();
                Location location = player.getLocation();
                int world = Bukkit.getWorlds().indexOf(location.getWorld());
                Optional<Works.WorkData> data = Works.getWorkData(uuid);
                String selector_name = UserSelector.getSelector(player).map(ISelector::getType).map(SelectorType::getName).orElse(null);

                return Arrays.asList(
                        uuid,
                        system.round(location.getX(), 3),
                        system.round(location.getY(), 3),
                        system.round(location.getZ(), 3),
                        world,
                        TabManager.getPayerIDorNull(uuid),
                        data.map(v -> v.icon).orElse(null),
                        data.map(v -> v.name).orElse(null),
                        player.isOp(),
                        selector_name,
                        Death.isDamageLay(uuid),
                        lime.isLay(player) ? "LAY" : lime.isSit(player) ? "SIT" : "NONE",
                        HideNickSetting.isHide(player) ? 1 : 0,
                        Skins.getSkinURL(player)
                );
            })))
            .build();

    private static final ReadonlyTable<String> ABAN_TARGET_READONLY = ReadonlyTable.Builder.<String>of("aban_target", "CONCAT(IFNULL(data,''),'^',room_id,'^',type)")
            .withKey(String.class)
            .withKeys("data","room_id","type")
            .withData(() -> {
                HashMap<String, List<Object>> list = new HashMap<>();

                List<system.Toast3<Double, Location, String>> _rooms = new ArrayList<>(Administrator.rooms);
                int size = _rooms.size();
                for (int i = 0; i < size; i++) {
                    system.Toast3<Double, Location, String> kv = _rooms.get(i);
                    list.put((kv.val2 == null ? "" : kv.val2) + "^" + i + "^ROOM", Arrays.<Object>asList(kv.val2, i, "ROOM"));
                }
                Administrator.target_list.forEach((k, v) -> list.put(k + "^" + v + "^UUID", Arrays.<Object>asList(k.toString(), v, "UUID")));

                return list;
            })
            .build();

    private static final ReadonlyTable<String> WORK_READONLY = ReadonlyTable.Builder.<String>of("work_readonly", "CONCAT(id,'^',type)")
            .withKey(String.class)
            .withKeys("id","type","icon","name")
            .withData(() ->
                    Works.works.values().stream().map(v -> system.toast(v.id + "^WORK", Arrays.<Object>asList(v.id, "WORK", v.icon, v.name)))
                            .collect(Collectors.toMap(kv -> kv.val0, kv -> kv.val1))
            )
            .build();

    @SuppressWarnings("deprecation")
    private static final ReadonlyTable<String> ENTITY_READONLY = ReadonlyTable.Builder.<String>of("entity", "uuid")
            .withKey(String.class)
            .withKeys("uuid","x","y","z","name","owner_uuid","type")
            .withData(() -> EntityPosition.getEntitiyRows().entrySet().stream().collect(Collectors.toMap(kv -> kv.getKey().toString(), kv -> {
                Tameable tameable = kv.getValue();
                Location location = tameable.getLocation();
                return Arrays.asList(
                        kv.getKey(),
                        system.round(location.getX(), 3),
                        system.round(location.getY(), 3),
                        system.round(location.getZ(), 3),
                        tameable.getCustomName(),
                        tameable.getOwnerUniqueId(),
                        tameable.getType().name()
                );
            })))
            .build();

    private static final ReadonlyTable<String> ENTITY_SUBS_READONLY = ReadonlyTable.Builder.<String>of("entity_subs", "CONCAT(uuid,'^',sub_uuid)")
            .withKey(String.class)
            .withKeys("uuid","sub_uuid")
            .withData(() -> EntityPosition.getEntitiyRows().values().stream().flatMap(j -> {
                JsonArray data = JManager.get(JsonArray.class, j.getPersistentDataContainer(), "sub_owners", null);
                if (data == null) return Stream.empty();
                List<system.Toast2<String, List<Object>>> list = new ArrayList<>();
                String uuid = j.getUniqueId().toString();
                data.forEach(v -> {
                    String other = v.getAsString();
                    list.add(system.toast(uuid + "^" + other, Arrays.<Object>asList(uuid, other)));
                });
                return list.stream();
            }).collect(Collectors.toMap(kv -> kv.val0, kv -> kv.val1)))
            .build();

    private static final ReadonlyTable<String> RADIO_READONLY = ReadonlyTable.Builder.<String>of("radio_readonly", "CONCAT(uuid,'^',level)")
            .withKey(String.class)
            .withKeys("uuid", "level")
            .withData(() -> Radio.elements()
                    .map(v -> v instanceof Radio.PlayerRadioElement p ? p : null)
                    .filter(Objects::nonNull)
                    .flatMap(radio -> {
                        String uuid = radio.player().getUniqueId().toString();
                        return radio.levels().stream().map(level -> system.toast(uuid + "^" + level, Arrays.<Object>asList(uuid, level + "")));
                    }).collect(Collectors.toMap(kv -> kv.val0, kv -> kv.val1))
            )
            .build();

}
