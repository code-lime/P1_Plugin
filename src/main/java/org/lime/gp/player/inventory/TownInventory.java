package org.lime.gp.player.inventory;

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.entity.decoration.EntityItemFrame;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Color;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.permissions.ServerOperator;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;
import org.lime.core;
import org.lime.display.DisplayManager;
import org.lime.display.Displays;
import org.lime.display.ObjectDisplay;
import org.lime.gp.block.Blocks;
import org.lime.gp.chat.Apply;
import org.lime.gp.database.Methods;
import org.lime.gp.database.Rows;
import org.lime.gp.database.Tables;
import org.lime.gp.lime;
import org.lime.gp.extension.Zone;
import org.lime.gp.module.DrawMap;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.player.selector.ZoneSelector;
import org.lime.gp.player.ui.ScoreboardUI;
import org.lime.system;
import org.lime.timings.lib.MCTiming;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TownInventory implements Listener {
    private static boolean debug = false;

    private static class DisplayHtml {
        public final String sql;
        public final List<system.Toast2<String, String>> args = new ArrayList<>();
        public final String file;
        public final String html;

        private static final String HTML_EMPTY = "!EMPTY";

        public DisplayHtml(JsonObject json) {
            sql = json.get("sql").getAsString();
            if (json.has("args")) json.get("args").getAsJsonObject().entrySet().forEach(kv -> args.add(system.toast(kv.getKey(), kv.getValue().getAsString())));
            file = json.get("file").getAsString();
            html = lime.existConfig(file, ".html") ? lime.readAllConfig(file, ".html") : HTML_EMPTY;
        }

        public void generate(system.Action1<HashMap<Integer, String>> callback) {
            Methods.SQL.Async.rawSqlQuery(sql, Rows.AnyRow::of, list -> lime.invokeAsync(() -> {
                HashMap<Integer, String> map = new HashMap<>();
                list.forEach(v -> {
                    int house_id = Integer.parseInt(v.columns.get("house_id"));
                    String _html = html;
                    for (system.Toast2<String, String> arg : args) _html = _html.replace("{" + arg.val0 + "}", v.applyToString(arg.val1));
                    map.put(house_id, v.applyToString(_html));
                });
                return map;
            }, callback));
        }
        public void generateMap(system.Action1<HashMap<Integer, byte[]>> callback) {
            generate(html -> generateMap(html, callback));
        }
        private static final ConcurrentHashMap<String, system.Toast2<byte[], Integer>> buffer = new ConcurrentHashMap<>();
        @SuppressWarnings("unused")
        public static void generateMap(String html, system.Action1<byte[]> callback) {
            system.Toast2<byte[], Integer> data = buffer.getOrDefault(html, null);
            if (data != null) {
                if (data.val1-- <= 0) buffer.remove(html);
                callback.invoke(data.val0);
                return;
            }
            lime.invokeAsync(() -> {
                BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
                Graphics graphics = image.createGraphics();
                JEditorPane jep = new JEditorPane("text/html", html);
                jep.setSize(128, 128);
                jep.print(graphics);
                return DrawMap.of().fill(image).save();
            }, bytes -> {
                buffer.put(html, system.toast(bytes, system.rand(15, 20)));
                callback.invoke(bytes);
            });
        }
        public static void generateMap(HashMap<Integer, String> htmls, system.Action1<HashMap<Integer,byte[]>> callback) {
            HashMap<Integer, byte[]> _data = new HashMap<>();
            htmls.entrySet().removeIf(kv -> {
                String html = kv.getValue();
                system.Toast2<byte[], Integer> data = buffer.getOrDefault(html, null);
                if (data == null) return false;
                if (data.val1-- <= 0) buffer.remove(html);
                _data.put(kv.getKey(), data.val0);
                return true;
            });

            lime.invokeAsync(() -> htmls.forEach((house_id, html) -> {
                BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
                Graphics graphics = image.createGraphics();
                JEditorPane jep = new JEditorPane("text/html", html);
                jep.setSize(128, 128);
                jep.print(graphics);
                byte[] bytes = DrawMap.of().fill(image).save();
                _data.put(house_id, bytes);
                buffer.put(html, system.toast(bytes, system.rand(15, 20)));
            }), () -> callback.invoke(_data));
        }
        public static void generateAllMap(system.Action1<HashMap<Integer, byte[]>> callback) {
            ImmutableList.copyOf(displays.values()).forEach(displayHtml -> displayHtml.generateMap(callback));
        }
    }
    private static final ConcurrentHashMap<HtmlType, DisplayHtml> displays = new ConcurrentHashMap<>();
    private static List<String> htmlPages() {
        List<String> files = new ArrayList<>();
        displays.values().forEach(value -> files.add(value.file + ".html"));
        return files;
    }

    public static core.element create() {
        return core.element.create(TownInventory.class)
                .withInit(TownInventory::init)
                .withInstance()
                .<JsonPrimitive>addConfig("config", v -> v
                        .withParent("debug-town")
                        .withDefault(new JsonPrimitive(false))
                        .withInvoke(j -> debug = j.getAsBoolean())
                )
                .<JsonObject>addConfig("town_data", v -> v
                        .withDefault(new JsonObject())
                        .withInvoke(j -> {
                            HashMap<HtmlType, DisplayHtml> displays = new HashMap<>();
                            j.entrySet().forEach(kv -> displays.put(HtmlType.valueOf(kv.getKey()), new DisplayHtml(kv.getValue().getAsJsonObject())));
                            TownInventory.displays.clear();
                            TownInventory.displays.putAll(displays);
                        })
                )
                .addEmptyInit("town-html", () -> lime.autodownload.updateConfigAsync(htmlPages(), () -> lime.autodownload.updateConfigAsync(Collections.singleton("town_data"), () -> {
                    Displays.uninitDisplay(HOME_MANAGER);
                    DisplayHtml.buffer.clear();
                    DrawMap.bufferReset();
                    Displays.initDisplay(HOME_MANAGER);
                })))
                .<JsonObject>addConfig("private_pattern", v -> v.withInvoke(TownInventory::patternConfig).withDefault(new JsonObject()));
    }

    public enum HtmlType {
        HOME("home", row -> true);

        private final system.Func1<Rows.HouseRow, Boolean> isUse;
        public final String key;
        HtmlType(String key, system.Func1<Rows.HouseRow, Boolean> isUse) {
            this.isUse = isUse;
            this.key = key;
        }
        public boolean IsUse(Rows.HouseRow row) {
            return isUse.invoke(row);
        }

        public static HtmlType getPageType(Rows.HouseRow row) {
            for (HtmlType type : HtmlType.values()) {
                if (type.IsUse(row))
                    return type;
            }
            return null;
        }
    }
    private static final HomeManager HOME_MANAGER = new HomeManager();

    public static class PrivatePattern extends system.Enum {
        private static final HashMap<String, PrivatePattern> patternBreaks = new HashMap<>();
        private static final HashMap<String, PrivatePattern> patternBlocks = new HashMap<>();
        private static final HashMap<EntityType, PrivatePattern> patternEntities = new HashMap<>();
        public final int index;
        public final String name;
        public final List<String> blocks;
        public final List<String> breaks;
        public final List<EntityType> entities;
        private PrivatePattern(int index, String name, List<String> breaks, List<String> blocks, List<EntityType> entities) {
            this.index = index;
            this.name = name;
            this.breaks = breaks;
            this.blocks = blocks;
            this.entities = entities;
        }
        public static PrivatePattern parse(String name, JsonObject json) {
            int index = json.get("index").getAsInt();

            return new PrivatePattern(
                    index,
                    name,
                    system.parseGet(json, "breaks", Stream.concat(Blocks.creators.keySet().stream(), Arrays.stream(Material.values()).map(Enum::name)).collect(Collectors.toSet()), v -> v),
                    system.parseGet(json, "blocks", Stream.concat(Blocks.creators.keySet().stream(), Arrays.stream(Material.values()).map(Enum::name)).collect(Collectors.toSet()), v -> v),
                    system.parseGet(json, "entities", Arrays.asList(EntityType.values()), Enum::name));
        }
        private void addToPatterns() {
            this.breaks.forEach(m -> patternBreaks.put(m, this));
            this.blocks.forEach(m -> patternBlocks.put(m, this));
            this.entities.forEach(m -> patternEntities.put(m, this));
        }

        @Override protected long getBit() {
            return 1L << index;
        }
    }

    public static final HashMap<String, PrivatePattern> patterns = new HashMap<>();

    public static void init() {
        Displays.initDisplay(HOME_MANAGER);

        lime.repeat(() -> {
            if (!debug) return;
            List<? extends Player> players = Bukkit.getOnlinePlayers().stream().filter(ServerOperator::isOp).toList();
            if (players.size() == 0) return;
            List<HomePerm> homes = HomePerm.getAll();
            players.forEach(player -> {
                Location loc = player.getLocation();
                List<String> map = new ArrayList<>();
                Rows.UserRow.getBy(player.getUniqueId()).ifPresent(user -> {
                    Boolean state = null;
                    for (HomePerm home : homes) {
                        system.Toast2<List<String>, Boolean> _state = home.display(PrivatePattern.patternBreaks.get(Material.CHEST.name()), loc, user, true);
                        map.addAll(_state.val0);
                        if (state == null || state) state = _state.val1 == null ? state : _state.val1;
                        else state = false;
                    }
                    ChatColor color;
                    if (state == null) color = ChatColor.WHITE;
                    else color = state ? ChatColor.GREEN : ChatColor.RED;
                    ScoreboardUI.SendFakeScoreboard(player, color + "Home private", map);
                });
            });
        }, 1);
        lime.repeat(() -> DisplayHtml.generateAllMap(HomeDisplay.house_list::putAll), 5);
    }
    @EventHandler public static void on(PlayerUseUnknownEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.isAttack()) return;
        HomeDisplay display = getClick(e.getEntityId());
        if (display == null) return;
        Player player = e.getPlayer();
        boolean isShift = player.isSneaking();
        display.onClick(player, isShift);
    }
    public static void patternConfig(JsonObject json) {
        HashMap<String, PrivatePattern> patterns = new HashMap<>();
        json.entrySet().forEach(kv -> patterns.put(kv.getKey(), PrivatePattern.parse(kv.getKey(), kv.getValue().getAsJsonObject())));
        TownInventory.patterns.clear();
        PrivatePattern.patternEntities.clear();
        PrivatePattern.patternBlocks.clear();
        TownInventory.patterns.putAll(patterns);
        TownInventory.patterns.put("ALL", null);
        TownInventory.patterns.put("NONE", null);
        patterns.values().forEach(PrivatePattern::addToPatterns);
    }

    private static class HomeDisplay extends ObjectDisplay<Rows.HouseRow, EntityItemFrame> {
        private static final ConcurrentHashMap<Integer, byte[]> house_list = new ConcurrentHashMap<>();

        @Override public double getDistance() {
            return 40;
        }

        private final List<Player> displayPreview = new ArrayList<>();
        public final BlockFace showFace;

        public final Vector pos1;
        public final Vector pos2;

        private final int mapID;
        private final int houseRowID;
        private byte[] mapData;

        private static final byte[] loadingMapIcon;
        static {
            loadingMapIcon = DrawMap.of().draw(draw -> {
                try {
                    draw.fill(ImageIO.read(new URL("https://cdn.discordapp.com/attachments/853050024099577866/864120999301873674/1f3d9-fe0f.png")));
                } catch (Exception e) {
                    draw.pixel(0,0,Color.RED);
                }
            }).save();
        }
        private static byte[] getLoadingMapIcon() { return Arrays.copyOf(loadingMapIcon, loadingMapIcon.length); }

        
        @SuppressWarnings("deprecation")
        protected HomeDisplay(Rows.HouseRow row) {
            super(row.posMain.getLocation(row.posFace.getDirection()));
            pos1 = row.posMin;
            pos2 = row.posMax;
            showFace = row.posFace;
            postInit();
            houseRowID = row.id;
            mapID = DrawMap.getNextMapID();
            ItemStack map = new ItemStack(Material.FILLED_MAP);
            MapMeta meta = (MapMeta)map.getItemMeta();
            meta.setMapId(mapID);
            mapData = getLoadingMapIcon();
            map.setItemMeta(meta);
            entity.setItem(CraftItemStack.asNMSCopy(map), true, false);
        }
        @Override protected void sendData(Player player, boolean child) {
            DrawMap.sendMap(player, mapID, mapData);
            super.sendData(player, child);
        }
        @Override protected EntityItemFrame createEntity(Location location) {
            return new EntityItemFrame(
                    ((CraftWorld)location.getWorld()).getHandle(),
                    new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                    EnumDirection.byName(showFace.name().toLowerCase()));
        }

        private long timeRedraw = 0;
        private void updateMap(Rows.HouseRow row) {
            if (timeRedraw > System.currentTimeMillis()) return;
            byte[] data = house_list.getOrDefault(row.id, null);
            if (data != null) mapData = data;
            invokeAll(this::sendData);
            timeRedraw = System.currentTimeMillis() + 5 * 1000;
        }
        @Override public void update(Rows.HouseRow row, double delta) {
            super.update(row, delta);
            displayPreview.removeIf(p -> {
                if (!p.isOnline()) return true;
                Zone.showBox(p, pos1, pos2, ZoneSelector.ShowParticle);
                return false;
            });
            updateMap(row);
        }
        public void onClick(Player player, boolean isShift) {
            Tables.HOUSE_TABLE.get(houseRowID + "").ifPresent(row -> MenuCreator.show(player, "town.house.open", Apply.of().add("house_", row).add("is_shift", isShift ? "true" : "false")));
        }

        public static HomeDisplay create(Integer integer, Rows.HouseRow houseRow) {
            return new HomeDisplay(houseRow);
        }
    }
    private static class HomeManager extends DisplayManager<Integer, Rows.HouseRow, HomeDisplay> {
        @Override public HashMap<Integer, Rows.HouseRow> getData() { return Tables.HOUSE_TABLE.getMap(v -> v.id); }
        @Override public HomeDisplay create(Integer integer, Rows.HouseRow houseRow) { return HomeDisplay.create(integer, houseRow); }
    }

    private static HomeDisplay getClick(int entityID) {
        for (HomeDisplay display : HOME_MANAGER.getDisplays().values()) {
            if (display.entityID == entityID)
                return display;
        }
        return null;
    }
    private static boolean isCantBreak(Block block, Player player) {
        PrivatePattern privateType = PrivatePattern.patternBreaks.getOrDefault(Blocks.getBlockKey(block), null);
        if (privateType == null) return false;
        return isCant(privateType, block.getLocation().add(0.5, 0.5, 0.5), player);
    }
    private static boolean isCantBlock(Block block, Player player) {
        PrivatePattern privateType = PrivatePattern.patternBlocks.getOrDefault(Blocks.getBlockKey(block), null);
        if (privateType == null) return false;
        return isCant(privateType, block.getLocation().add(0.5, 0.5, 0.5), player);
    }
    private static boolean isCantDamage(EntityType entity, Location pos, Player player) {
        PrivatePattern privateType = PrivatePattern.patternEntities.getOrDefault(entity, null);
        if (privateType == null) return false;
        return isCant(privateType, pos, player);
    }

    private static class HomePerm {
        private final Rows.HouseRow row;
        private final List<HomePerm> perms = new ArrayList<>();
        private HomePerm(Rows.HouseRow row) { this.row = row; }
        private void child(HomePerm room) { perms.add(room); }
        public Boolean isCan(PrivatePattern privateType, Location pos, Rows.UserRow user) {
            return display(privateType, pos, user, false).val1;
        }
        public system.Toast2<List<String>, Boolean> display(PrivatePattern privateType, Location pos, Rows.UserRow user, boolean createList) {
            String prefix = StringUtils.leftPad(String.valueOf(row.id), 2, '0');
            List<String> list = createList ? new ArrayList<>() : null;
            Boolean state = null;
            for (HomePerm perm : perms) {
                system.Toast2<List<String>, Boolean> _state = perm.display(privateType, pos, user, createList);
                if (createList) _state.val0.forEach(line -> list.add(" - " + line));
                if (state == null || state) state = _state.val1 == null ? state : _state.val1;
                else state = false;
            }

            if (row.inZone(pos)) {
                if (row.ownerID != null && row.ownerID == user.id) {
                    state = true;
                    if (createList) list.add(ChatColor.GREEN + prefix + ": OWNER");
                }
                else if (row.isSub(user.id)) {
                    state = true;
                    if (createList) list.add(ChatColor.GREEN + prefix + ": SUB");
                }
                else if (!PrivatePattern.has(row.private_flags, privateType)) {
                    state = state != null && state;
                    if (createList) list.add(ChatColor.RED + prefix + ": PRIVATE");
                }
                else if (createList) list.add(prefix + ": NONE");
            }
            return system.toast(list, state);
        }
        public static List<HomePerm> getAll() {
            HashMap<Integer, Rows.HouseRow> rows = system.map.<Integer, Rows.HouseRow>of()
                    .add(Tables.HOUSE_TABLE.getRows(), kv -> kv.id, kv -> kv)
                    .build();

            HashMap<Integer, HomePerm> map = new HashMap<>();
            rows.forEach((k,v) -> {
                if (v.isRoom) return;
                map.put(v.id, new HomePerm(v));
            });
            rows.forEach((k,v) -> {
                if (!v.isRoom) return;
                HomePerm room = new HomePerm(v);
                HomePerm owner = map.getOrDefault(v.street, null);
                if (owner == null) map.put(v.id, room);
                else owner.child(room);
            });
            return new ArrayList<>(map.values());
        }
        public static boolean isCanAll(PrivatePattern privateType, Location pos, Rows.UserRow user) {
            List<HomePerm> homes = HomePerm.getAll();
            Boolean state = null;
            for (HomePerm home : homes) {
                Boolean _state = home.isCan(privateType, pos, user);
                if (state == null || state) state = _state == null ? state : _state;
                else state = false;
            }
            return state == null || state;
        }
    }

    private static boolean isCant(PrivatePattern privateType, Location pos, Player player) {
        return Rows.UserRow.getBy(player.getUniqueId()).map(user -> !user.isOwner() && !HomePerm.isCanAll(privateType, pos, user)).orElse(true);
    }
    private static Player getOwner(Entity damager) {
        if (damager instanceof Player) return (Player)damager;
        else if (damager instanceof Projectile) {
           ProjectileSource source = ((Projectile)damager).getShooter();
           if (source instanceof Player) return (Player)source;
        }
        return null;
    }

    @EventHandler public static void onBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        Player player = e.getPlayer();
        if (player.getWorld() != lime.MainWorld) return;
        if (isCantBreak(block, player)) e.setCancelled(true);
    }
    @EventHandler public static void onClick(PlayerInteractEvent e) {
        switch (e.getAction()) {
            case LEFT_CLICK_BLOCK:
            case LEFT_CLICK_AIR:
            case RIGHT_CLICK_AIR: return;
            default:
                break;
        }
        Block block = e.getClickedBlock();
        if (block == null) return;
        Player player = e.getPlayer();
        if (player.getWorld() != lime.MainWorld) return;
        if (isCantBlock(block, player)) {
            e.setCancelled(true);
        }
    }
    @EventHandler public static void onSwap(PlayerArmorStandManipulateEvent e) {
        if (e.getRightClicked().getWorld() != lime.MainWorld) return;
        if (isCantDamage(e.getRightClicked().getType(), e.getRightClicked().getLocation(), e.getPlayer())) e.setCancelled(true);
    }
    @EventHandler public static void onClick(PlayerInteractEntityEvent e) {
        if (e.getRightClicked().getWorld() != lime.MainWorld) return;
        if (isCantDamage(e.getRightClicked().getType(), e.getRightClicked().getLocation(), e.getPlayer())) e.setCancelled(true);
    }
    @EventHandler public static void onDamage(EntityDamageByEntityEvent e) {
        Player damager = getOwner(e.getDamager());
        if (damager == null) return;
        if (damager.getWorld() != lime.MainWorld) return;
        if (isCantDamage(e.getEntity().getType(), e.getEntity().getLocation(), damager)) e.setCancelled(true);
    }
    @EventHandler public static void onDamage(HangingBreakByEntityEvent e) {
        Player remover = getOwner(e.getRemover());
        if (remover == null) return;
        if (remover.getWorld() != lime.MainWorld) return;
        if (isCantDamage(e.getEntity().getType(), e.getEntity().getLocation(), remover)) e.setCancelled(true);
    }
    @EventHandler public static void onDestroy(VehicleDestroyEvent e) {
        Player attacker = getOwner(e.getAttacker());
        if (attacker == null) return;
        if (attacker.getWorld() != lime.MainWorld) return;
        if (isCantDamage(e.getVehicle().getType(), e.getVehicle().getLocation(), attacker)) e.setCancelled(true);
    }
}






























































