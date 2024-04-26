package org.lime.gp.database.readonly;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftMarker;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.lime.gp.admin.Administrator;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.entity.Entities;
import org.lime.gp.entity.component.list.OwnerComponent;
import org.lime.gp.item.settings.list.HideNickSetting;
import org.lime.gp.lime;
import org.lime.gp.module.EntityOwner;
import org.lime.gp.module.EntityPosition;
import org.lime.gp.player.module.Death;
import org.lime.gp.player.module.Ghost;
import org.lime.gp.player.module.Skins;
import org.lime.gp.player.module.TabManager;
import org.lime.gp.player.perm.Works;
import org.lime.gp.player.selector.ISelector;
import org.lime.gp.player.selector.SelectorType;
import org.lime.gp.player.selector.UserSelector;
import org.lime.gp.player.voice.Radio;
import org.lime.plugin.CoreElement;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast3;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class ReadonlySync {
    public static CoreElement create() {
        return CoreElement.create(ReadonlySync.class)
                .withInit(ReadonlySync::init);
    }
    private static boolean ONLINE_POSITIONS_READONLY_ENABLE = false;
    public static void init() {
        lime.repeat(ReadonlySync::update, 1);
        ONLINE_POSITIONS_READONLY_ENABLE = lime.existConfig("__online_position") && lime.readAllConfig("__online_position").equals("true");
        AnyEvent.addEvent("use_online_positions", AnyEvent.type.owner_console, p -> {
            ONLINE_POSITIONS_READONLY_ENABLE = !ONLINE_POSITIONS_READONLY_ENABLE;
            lime.logOP("Use table online_positions: " + (ONLINE_POSITIONS_READONLY_ENABLE ? "enable" : "disable"));
            lime.writeAllConfig("__online_position", ONLINE_POSITIONS_READONLY_ENABLE ? "true" : "false");
        });
    }
    public static void update() {
        ReadonlyTable.sync();
    }

    private static final ReadonlyTable<String> ONLINE_READONLY = ReadonlyTable.Builder.<String>of("online", "uuid")
            .withKey(String.class)
            .withKeys("uuid",
                    "x","y","z", "world",
                    "timed_id",
                    "data_icon","data_name",
                    "is_op", "zone_selector","die",
                    "gpose","hide","skin_url","ghost")
            .withData(() -> EntityPosition.onlinePlayers.entrySet().stream().collect(Collectors.toMap(kv -> kv.getKey().toString(), kv -> {
                UUID uuid = kv.getKey();
                Player player = kv.getValue();
                Location location = player.getLocation();
                int world = Bukkit.getWorlds().indexOf(location.getWorld());
                Optional<Works.WorkData> data = Works.getWorkData(uuid);
                String selector_name = UserSelector.getSelector(player).map(ISelector::getType).map(SelectorType::getName).orElse(null);

                return Arrays.asList(
                        uuid,

                        ONLINE_POSITIONS_READONLY_ENABLE ? 0 : (int)location.getBlockX(), //system.round(location.getX(), 3),
                        ONLINE_POSITIONS_READONLY_ENABLE ? 0 : (int)location.getBlockY(), //system.round(location.getY(), 3),
                        ONLINE_POSITIONS_READONLY_ENABLE ? 0 : (int)location.getBlockZ(), //system.round(location.getZ(), 3),
                        world,

                        TabManager.getPayerID(uuid),

                        data.map(v -> v.icon).orElse(null),
                        data.map(v -> v.name).orElse(null),

                        player.isOp(), selector_name, Death.getDamageState(uuid).index,

                        lime.isLay(player) ? "LAY" : lime.isSit(player) ? "SIT" : "NONE",
                        HideNickSetting.isHide(player) ? 1 : 0,
                        Skins.getSkinURL(player),
                        Ghost.getGhostTarget(player).orElse(null)
                );
            })))
            //.withDebug(text -> lime.logOP("[SQL] " + text))
            .build();
    private static final ReadonlyTable<String> ONLINE_POSITIONS_READONLY = ReadonlyTable.Builder.<String>of("online_positions", "uuid")
            .withKey(String.class)
            .withKeys("uuid","x","y","z","world")
            .withData(() -> EntityPosition.onlinePlayers.entrySet().stream().collect(Collectors.toMap(kv -> kv.getKey().toString(), kv -> {
                UUID uuid = kv.getKey();
                Player player = kv.getValue();
                Location location = player.getLocation();
                int world = Bukkit.getWorlds().indexOf(location.getWorld());

                return Arrays.asList(
                        uuid,
                        (int)location.getBlockX(),//system.round(location.getX(), 3),
                        (int)location.getBlockY(),//system.round(location.getY(), 3),
                        (int)location.getBlockZ(),//system.round(location.getZ(), 3),
                        world
                );
            })))
            .build();

    private static final ReadonlyTable<String> ABAN_TARGET_READONLY = ReadonlyTable.Builder.<String>of("aban_target", "CONCAT(IFNULL(data,''),'^',room_id,'^',type)")
            .withKey(String.class)
            .withKeys("data","room_id","type")
            .withData(() -> {
                HashMap<String, List<Object>> list = new HashMap<>();

                List<Toast3<Double, Location, String>> _rooms = new ArrayList<>(Administrator.rooms);
                int size = _rooms.size();
                for (int i = 0; i < size; i++) {
                    Toast3<Double, Location, String> kv = _rooms.get(i);
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
                    Works.works.values().stream().map(v -> Toast.of(v.id + "^WORK", Arrays.<Object>asList(v.id, "WORK", v.icon, v.name)))
                            .collect(Collectors.toMap(kv -> kv.val0, kv -> kv.val1))
            )
            .build();

    @SuppressWarnings("deprecation")
    private static final ReadonlyTable<String> ENTITY_READONLY = ReadonlyTable.Builder.<String>of("entity", "uuid")
            .withKey(String.class)
            .withKeys("uuid","x","y","z","name","owner_uuid","owner_death","type")
            .withData(() -> EntityOwner.getAllEntities()
                    .flatMap(entityOwner -> entityOwner.getOwner()
                            .flatMap(EntityOwner.UserInfo::row)
                            .map(owner -> {
                                Entity entity = entityOwner.entity();
                                UUID entityUniqueId = entity.getUniqueId();
                                Location location = entity.getLocation();

                                return Toast.of(entityUniqueId.toString(), Arrays.<Object>asList(
                                        entityUniqueId,
                                        (int)location.getBlockX(),//system.round(location.getX(), 3),
                                        (int)location.getBlockY(),//system.round(location.getY(), 3),
                                        (int)location.getBlockZ(),//system.round(location.getZ(), 3),
                                        entity.getCustomName(),
                                        owner.uuid,
                                        owner.userName.startsWith("RES:") ? 1 : 0,
                                        Optional.of(entity)
                                                .map(v -> v instanceof CraftMarker m ? m.getHandle() : null)
                                                .flatMap(Entities::customOf)
                                                .flatMap(v -> v.list(OwnerComponent.class).findAny())
                                                .map(v -> v.entityType)
                                                .orElseGet(() -> entity.getType().name())
                                ));
                            })
                            .stream())
                    .collect(Collectors.toMap(kv -> kv.val0, kv -> kv.val1)))
            .build();

    private static final ReadonlyTable<String> ENTITY_SUBS_READONLY = ReadonlyTable.Builder.<String>of("entity_subs", "CONCAT(uuid,'^',sub_uuid)")
            .withKey(String.class)
            .withKeys("uuid","sub_uuid")
            .withData(() -> EntityOwner.getAllEntities()
                    .flatMap(entityOwner -> {
                        UUID uuid = entityOwner.entity().getUniqueId();
                        return entityOwner.getSubs()
                                .flatMap(v -> v.row().stream())
                                .map(sub -> Toast.of(uuid + "^" + sub.uuid, Arrays.<Object>asList(uuid, sub.uuid)));
                    })
                    .collect(Collectors.toMap(kv -> kv.val0, kv -> kv.val1)))
            /*
            .withData(() -> EntityPosition.getEntitiyRows().values().stream().flatMap(j -> {
                JsonArray data = JManager.get(JsonArray.class, j.getPersistentDataContainer(), "sub_owners", null);
                if (data == null) return Stream.empty();
                List<Toast2<String, List<Object>>> list = new ArrayList<>();
                String uuid = j.getUniqueId().toString();
                data.forEach(v -> {
                    String other = v.getAsString();
                    list.add(Toast.of(uuid + "^" + other, Arrays.<Object>asList(uuid, other)));
                });
                return list.stream();
            }).collect(Collectors.toMap(kv -> kv.val0, kv -> kv.val1)))
            */
            .build();

    private static final ReadonlyTable<String> RADIO_READONLY = ReadonlyTable.Builder.<String>of("radio_readonly", "CONCAT(uuid,'^',level)")
            .withKey(String.class)
            .withKeys("uuid", "level")
            .withData(() -> Radio.elements()
                    .map(v -> v instanceof Radio.PlayerRadioElement p ? p : null)
                    .filter(Objects::nonNull)
                    .flatMap(radio -> {
                        String uuid = radio.player().getUniqueId().toString();
                        return radio.levels().stream().map(level -> Toast.of(uuid + "^" + level, Arrays.<Object>asList(uuid, level + "")));
                    }).collect(Collectors.toMap(kv -> kv.val0, kv -> kv.val1))
            )
            .build();

}
