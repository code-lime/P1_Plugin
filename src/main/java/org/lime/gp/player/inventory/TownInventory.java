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
import org.bukkit.block.Lectern;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;
import org.bukkit.Material;
import org.lime.Position;
import org.lime.display.DisplayManager;
import org.lime.display.Displays;
import org.lime.display.ObjectDisplay;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.block.Blocks;
import org.lime.gp.block.component.data.OtherGenericInstance;
import org.lime.gp.chat.Apply;
import org.lime.gp.database.Methods;
import org.lime.gp.database.rows.*;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.extension.Zone;
import org.lime.gp.lime;
import org.lime.gp.module.DrawMap;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.player.selector.ZoneSelector;
import org.lime.gp.player.ui.EditorUI;
import org.lime.plugin.CoreElement;
import org.lime.system.map;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.RandomUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TownInventory implements Listener {
    public interface IPrivatePattern {
        boolean isCantBlock(String block);
        boolean isCantBreaks(String block);
        boolean isCantEntity(EntityType entity);

        boolean isCheck(String block);
        boolean isCheck(EntityType entity);

        String info();

        static IPrivatePattern combine(List<? extends IPrivatePattern> patterns) {
            return new IPrivatePattern() {
                @Override public boolean isCantBlock(String block) {
                    for (IPrivatePattern pattern : patterns)
                        if (pattern.isCantBlock(block))
                            return true;
                    return false;
                }
                @Override public boolean isCantBreaks(String block) {
                    for (IPrivatePattern pattern : patterns)
                        if (pattern.isCantBreaks(block))
                            return true;
                    return false;
                }
                @Override public boolean isCantEntity(EntityType entity) {
                    for (IPrivatePattern pattern : patterns)
                        if (pattern.isCantEntity(entity))
                            return true;
                    return false;
                }

                @Override public boolean isCheck(String block) {
                    for (IPrivatePattern pattern : patterns)
                        if (pattern.isCheck(block))
                            return true;
                    return false;
                }
                @Override public boolean isCheck(EntityType entity) {
                    for (IPrivatePattern pattern : patterns)
                        if (pattern.isCheck(entity))
                            return true;
                    return false;
                }

                @Override public String info() { return "["+patterns.stream().map(IPrivatePattern::info).collect(Collectors.joining(","))+"]"; }
            };
        }
        IPrivatePattern EMPTY = new IPrivatePattern() {
            @Override public boolean isCantBlock(String block) { return true; }
            @Override public boolean isCantBreaks(String block) { return true; }
            @Override public boolean isCantEntity(EntityType entity) { return true; }

            @Override public boolean isCheck(String block) { return false; }
            @Override public boolean isCheck(EntityType entity) { return false; }

            @Override public String info() { return "EMPTY"; }
        };
    }

    private static boolean debug = false;

    private record ImagePoint(int x, int y) {
        public static ImagePoint parse(String text) {
            String[] args = text.split(" ", 2);
            return new ImagePoint(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        }
    }
    private static class DisplayHtml {
        public final String sql;
        public final List<Toast2<String, String>> args = new ArrayList<>();
        public final String file;
        public final String html;
        public final String loadingUrl;
        public final ImagePoint htmlSize;
        public final ImagePoint htmlOffset;
        public final ImagePoint resultSize;

        private static final String HTML_EMPTY = "!EMPTY";
        public static final String DEFAULT_LOADING_URL = "https://cdn.discordapp.com/attachments/853050024099577866/864120999301873674/1f3d9-fe0f.png";

        public DisplayHtml(JsonObject json) {
            sql = json.get("sql").getAsString();
            if (json.has("args")) json.get("args").getAsJsonObject().entrySet().forEach(kv -> args.add(Toast.of(kv.getKey(), kv.getValue().getAsString())));
            file = json.get("file").getAsString();
            if (lime.existConfig(file, ".html")) {
                HashMap<String, String> params = new HashMap<>();
                this.html = lime.readAllConfig(file, ".html")
                        .lines()
                        .dropWhile(line -> {
                            if (!line.startsWith("#")) return false;
                            String[] args = line.substring(1).split("=", 2);
                            params.put(args[0].trim(), args.length > 1 ? args[1].trim() : "true");
                            return true;
                        })
                        .collect(Collectors.joining("\n"));
                this.loadingUrl = params.getOrDefault("loading_url", DEFAULT_LOADING_URL);
                this.htmlSize = ImagePoint.parse(params.getOrDefault("html.size", "128 128"));
                this.htmlOffset = ImagePoint.parse(params.getOrDefault("html.offset", "0 0"));
                this.resultSize = ImagePoint.parse(params.getOrDefault("result.size", "128 128"));

                lime.logOP("Params: " + params.entrySet().stream().map(v -> v.getKey() + "=" + v.getValue()).collect(Collectors.joining(", ")));

            } else {
                this.html = HTML_EMPTY;
                this.loadingUrl = DEFAULT_LOADING_URL;
                this.htmlSize = new ImagePoint(128, 128);
                this.htmlOffset = new ImagePoint(0, 0);
                this.resultSize = new ImagePoint(128, 128);
            }
        }

        public void generate(Action1<HashMap<Integer, String>> callback) {
            Methods.SQL.Async.rawSqlQuery(sql, AnyRow::of, list -> lime.invokeAsync(() -> {
                HashMap<Integer, String> map = new HashMap<>();
                list.forEach(v -> {
                    int house_id = Integer.parseInt(v.columns.get("house_id"));
                    String _html = html;
                    for (Toast2<String, String> arg : args) _html = _html.replace("{" + arg.val0 + "}", v.applyToString(arg.val1));
                    map.put(house_id, v.applyToString(_html));
                });
                return map;
            }, callback));
        }
        public void generateMap(Action1<HashMap<Integer, byte[]>> callback) {
            generate(html -> generateMap(html, callback, htmlSize, htmlOffset, resultSize));
        }
        private static final ConcurrentHashMap<String, Toast2<byte[], Integer>> buffer = new ConcurrentHashMap<>();
        @SuppressWarnings("unused")
        public static void generateMap(String html, Action1<byte[]> callback, ImagePoint htmlSize, ImagePoint offset, ImagePoint resultSize) {
            if (!HTML_RENDER) return;
            Toast2<byte[], Integer> data = buffer.getOrDefault(html, null);
            if (data != null) {
                if (data.val1-- <= 0) buffer.remove(html);
                callback.invoke(data.val0);
                return;
            }
            lime.invokeAsync(() -> {
                BufferedImage image = new BufferedImage(htmlSize.x, htmlSize.y, BufferedImage.TYPE_INT_ARGB);
                Graphics graphics = image.createGraphics();
                JEditorPane jep = new JEditorPane("text/html", html);
                jep.setSize(htmlSize.x, htmlSize.y);
                jep.print(graphics);
                return DrawMap.of().fillPart(image, offset.x, offset.y, resultSize.x, resultSize.y).save();
            }, bytes -> {
                buffer.put(html, Toast.of(bytes, RandomUtils.rand(15, 20)));
                callback.invoke(bytes);
            });
        }
        public static void generateMap(HashMap<Integer, String> htmls, Action1<HashMap<Integer,byte[]>> callback, ImagePoint htmlSize, ImagePoint offset, ImagePoint resultSize) {
            if (!HTML_RENDER) return;
            HashMap<Integer, byte[]> _data = new HashMap<>();
            htmls.entrySet().removeIf(kv -> {
                String html = kv.getValue();
                Toast2<byte[], Integer> data = buffer.getOrDefault(html, null);
                if (data == null) return false;
                if (data.val1-- <= 0) buffer.remove(html);
                _data.put(kv.getKey(), data.val0);
                return true;
            });

            lime.invokeAsync(() -> htmls.forEach((house_id, html) -> {
                BufferedImage image = new BufferedImage(htmlSize.x, htmlSize.y, BufferedImage.TYPE_INT_ARGB);
                Graphics graphics = image.createGraphics();
                JEditorPane jep = new JEditorPane("text/html", html);
                jep.setSize(htmlSize.x, htmlSize.y);
                jep.print(graphics);
                byte[] bytes = DrawMap.of().fillPart(image, offset.x, offset.y, resultSize.x, resultSize.y).save();
                _data.put(house_id, bytes);
                buffer.put(html, Toast.of(bytes, RandomUtils.rand(15, 20)));
            }), () -> callback.invoke(_data));
        }
        public static void generateAllMap(Action1<HashMap<Integer, byte[]>> callback) {
            if (!HTML_RENDER) return;
            ImmutableList.copyOf(displays.values()).forEach(displayHtml -> displayHtml.generateMap(callback));
        }
    }
    private static final ConcurrentHashMap<HtmlType, DisplayHtml> displays = new ConcurrentHashMap<>();
    private static List<String> htmlPages() {
        List<String> files = new ArrayList<>();
        displays.values().forEach(value -> files.add(value.file + ".html"));
        return files;
    }

    public static CoreElement create() {
        return CoreElement.create(TownInventory.class)
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
                })));
                //.<JsonObject>addConfig("private_pattern", v -> v.withInvoke(TownInventory::patternConfig).withDefault(new JsonObject()));
    }

    public enum HtmlType {
        HOME("home", row -> true);

        private final Func1<HouseRow, Boolean> isUse;
        public final String key;
        HtmlType(String key, Func1<HouseRow, Boolean> isUse) {
            this.isUse = isUse;
            this.key = key;
        }
        public boolean IsUse(HouseRow row) {
            return isUse.invoke(row);
        }

        public static HtmlType getPageType(HouseRow row) {
            for (HtmlType type : HtmlType.values()) {
                if (type.IsUse(row))
                    return type;
            }
            return null;
        }
    }
    private static final HomeManager HOME_MANAGER = new HomeManager();

    private static final ConcurrentHashMap<Integer, IPrivatePattern> housePatterns = new ConcurrentHashMap<>();
    public static void onUpdate(int houseId) {
        lime.onceTicks(() -> {
            List<PrivatePatternRow> lockPatterns = new ArrayList<>(Tables.PRIVATE_PATTERN.getRows());

            Set<Integer> unlockPatterns = PrivateHouseRow
                    .getBy(v -> v.houseId == houseId && !v.isLock)
                    .stream()
                    .map(v -> v.patternId)
                    .collect(Collectors.toSet());

            lockPatterns.removeIf(v -> unlockPatterns.contains(v.id));

            housePatterns.put(houseId, IPrivatePattern.combine(lockPatterns));
        }, 2);
    }
    public static void onUpdate(PrivateHouseRow row) {
        onUpdate(row.houseId);
    }
    public static void onUpdate(PrivatePatternRow row) {
        housePatterns.keySet().forEach(TownInventory::onUpdate);
    }

    private static boolean HTML_RENDER = true;

    public static void init() {
        AnyEvent.addEvent("html.render", AnyEvent.type.owner_console, v -> v.createParam("enable", "disable"), (p, v)-> {
            HTML_RENDER = v.equals("enable");
            Displays.uninitDisplay(HOME_MANAGER);
            DisplayHtml.buffer.clear();
            HomeDisplay.house_list.clear();
            DrawMap.bufferReset();
            Displays.initDisplay(HOME_MANAGER);
        });
        Displays.initDisplay(HOME_MANAGER);

        /*lime.repeat(() -> {
            if (!debug) return;
            List<? extends Player> players = Bukkit.getOnlinePlayers().stream().filter(ServerOperator::isOp).toList();
            if (players.size() == 0) return;
            List<HomePerm> homes = HomePerm.getAll();
            players.forEach(player -> {
                Location loc = player.getLocation();
                List<String> map = new ArrayList<>();
                UserRow.getBy(player.getUniqueId()).ifPresent(user -> {
                    Boolean state = null;
                    for (HomePerm home : homes) {
                        Toast2<List<String>, Boolean> _state = home.display(PrivatePattern.patternBreaks.get(Material.CHEST.name()), loc, user, true);
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
        }, 1);*/
        lime.repeat(() -> {
            openList.values().removeIf(uuid -> {
                Player player = Bukkit.getPlayer(uuid);
                return player == null || player.getOpenInventory().getTopInventory().getType() != InventoryType.CHEST;
            });
        }, 0.5);
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
    /*
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
    */

    private static class HomeDisplay extends ObjectDisplay<HouseRow, EntityItemFrame> {
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

        private static final Map<String, byte[]> loadingMapIcons = new HashMap<>();

        private static byte[] getLoadingMapIcon(String url) {
            byte[] bytes = loadingMapIcons.computeIfAbsent(url, _url -> DrawMap.of().draw(draw -> {
                try { draw.fill(ImageIO.read(new URL(_url))); }
                catch (Exception e) { draw.pixel(0, 0, Color.RED); }
            }).save());
            return Arrays.copyOf(bytes, bytes.length);
        }
        
        @SuppressWarnings("deprecation")
        protected HomeDisplay(HouseRow row) {
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
            DisplayHtml html = displays.get(HtmlType.getPageType(row));
            mapData = getLoadingMapIcon(html == null ? DisplayHtml.DEFAULT_LOADING_URL : html.loadingUrl);
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
        private void updateMap(HouseRow row) {
            if (timeRedraw > System.currentTimeMillis()) return;
            byte[] data = house_list.getOrDefault(row.id, null);
            if (data != null) mapData = data;
            invokeAll(this::sendData);
            timeRedraw = System.currentTimeMillis() + 5 * 1000;
        }
        @Override public void update(HouseRow row, double delta) {
            super.update(row, delta);
            displayPreview.removeIf(p -> {
                if (!p.isOnline()) return true;
                Zone.showBox(p, pos1, pos2, ZoneSelector.ShowParticle);
                return false;
            });
            updateMap(row);
        }

        public void onClick(Player player, boolean isShift) {
            if (openList.containsKey(houseRowID)) return;
            UUID uuid = player.getUniqueId();
            openList.values().remove(uuid);
            openList.put(houseRowID, uuid);
            Tables.HOUSE_TABLE.get(String.valueOf(houseRowID)).ifPresent(row -> MenuCreator.show(player, "town.house.open", Apply.of().add("house_", row).add("is_shift", isShift ? "true" : "false")));
        }

        public static HomeDisplay create(Integer integer, HouseRow houseRow) {
            return new HomeDisplay(houseRow);
        }
    }

    private final static ConcurrentHashMap<Integer, UUID> openList = new ConcurrentHashMap<>();

    private static class HomeManager extends DisplayManager<Integer, HouseRow, HomeDisplay> {
        @Override public HashMap<Integer, HouseRow> getData() { return Tables.HOUSE_TABLE.getMap(v -> v.id); }
        @Override public HomeDisplay create(Integer integer, HouseRow houseRow) { return HomeDisplay.create(integer, houseRow); }
    }

    private static HomeDisplay getClick(int entityID) {
        for (HomeDisplay display : HOME_MANAGER.getDisplays().values()) {
            if (display.entityID == entityID)
                return display;
        }
        return null;
    }
    private static boolean isCantBreak(Block block, Player player) {
        //PrivatePattern privateType = PrivatePattern.patternBreaks.getOrDefault(Blocks.getBlockKey(block), null);
        //if (privateType == null) return false;
        String blockKey = Blocks.getBlockKey(block);
        if (!Tables.PRIVATE_PATTERN.hasBy(row -> row.isCheck(blockKey))) return false;
        return isCant(v -> v.isCantBlock(blockKey), block.getLocation().add(0.5, 0.5, 0.5), player);
    }
    private static boolean isCantBlock(Block block, Player player) {
        //PrivatePattern privateType = PrivatePattern.patternBlocks.getOrDefault(Blocks.getBlockKey(block), null);
        //if (privateType == null) return false;
        String blockKey = Blocks.getBlockKey(block);
        if (!Tables.PRIVATE_PATTERN.hasBy(row -> row.isCheck(blockKey))) return false;
        return isCant(v -> v.isCantBlock(blockKey), block.getLocation().add(0.5, 0.5, 0.5), player);
    }
    private static boolean isCantDamage(EntityType entity, Location pos, Player player) {
        //Func1<IPrivatePattern, Boolean> privateCant
        //PrivatePattern privateType = PrivatePattern.patternEntities.getOrDefault(entity, null);
        //if (privateType == null) return false;
        if (!Tables.PRIVATE_PATTERN.hasBy(row -> row.isCheck(entity))) return false;
        return isCant(v -> v.isCantEntity(entity), pos, player);
    }

    private static class HomePerm {
        private final HouseRow row;
        private final List<HomePerm> perms = new ArrayList<>();
        private HomePerm(HouseRow row) { this.row = row; }
        private void child(HomePerm room) { perms.add(room); }
        public Boolean isCan(Func1<IPrivatePattern, Boolean> privateCant, Location pos, UserRow user) {
            return display(privateCant, pos, user, false).val1;
        }
        public Toast2<List<String>, Boolean> display(Func1<IPrivatePattern, Boolean> privateCant, Location pos, UserRow user, boolean createList) {
            String prefix = StringUtils.leftPad(String.valueOf(row.id), 2, '0');
            List<String> list = createList ? new ArrayList<>() : null;
            Boolean state = null;
            for (HomePerm perm : perms) {
                Toast2<List<String>, Boolean> _state = perm.display(privateCant, pos, user, createList);
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
                else if (privateCant.invoke(housePatterns.getOrDefault(row.id, IPrivatePattern.EMPTY))) {
                    state = state != null && state;
                    if (createList) list.add(ChatColor.RED + prefix + ": PRIVATE");
                }
                else if (createList) list.add(prefix + ": NONE");
            }
            return Toast.of(list, state);
        }
        public static List<HomePerm> getAll() {
            HashMap<Integer, HouseRow> rows = map.<Integer, HouseRow>of()
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
        public static boolean isCanAll(Func1<IPrivatePattern, Boolean> privateCant, Location pos, UserRow user) {
            List<HomePerm> homes = HomePerm.getAll();
            Boolean state = null;
            for (HomePerm home : homes) {
                Boolean _state = home.isCan(privateCant, pos, user);
                if (state == null || state) state = _state == null ? state : _state;
                else state = false;
            }
            return state == null || state;
        }
    }

    private static boolean isCant(Func1<IPrivatePattern, Boolean> privateCant, Location pos, Player player) {
        return UserRow.getBy(player.getUniqueId())
                .map(user -> !user.isOwner() && !HomePerm.isCanAll(privateCant, pos, user))
                .orElse(true);
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
            if (block.getType() == Material.LECTERN) {
                Lectern lectern = (Lectern) block.getState();
                Inventory inventory = lectern.getSnapshotInventory();
                if (!inventory.isEmpty()) {
                    ItemStack book = inventory.getItem(0);
                    if (book != null && book.getItemMeta() instanceof BookMeta bookMeta) {
                        EditorUI.openBook(player, bookMeta.pages());
                        e.setCancelled(true);
                        return;
                    }
                }
            }

            Apply data = Apply.of()
                    .add("block_type", block.getType().name());
            Blocks.of(block)
                    .flatMap(Blocks::customOf)
                    .map(metadata -> metadata.list(OtherGenericInstance.class).findFirst().flatMap(OtherGenericInstance::owner).flatMap(Blocks::customOf).orElse(metadata))
                    .ifPresentOrElse(
                            metadata -> {
                                Position position = metadata.position();
                                data
                                        .add("block_pos_x", String.valueOf(position.x))
                                        .add("block_pos_y", String.valueOf(position.y))
                                        .add("block_pos_z", String.valueOf(position.z))
                                        .add("block_pos", position.toSave())

                                        .add("block_key", metadata.key.type())
                                        .add("block_uuid", metadata.key.uuid().toString());
                            },
                            () -> data
                                    .add("block_pos_x", String.valueOf(block.getX()))
                                    .add("block_pos_y", String.valueOf(block.getY()))
                                    .add("block_pos_z", String.valueOf(block.getZ()))
                                    .add("block_pos", block.getX() + " " + block.getY() + " " + block.getZ()));

            MenuCreator.show(player, "interact.lock", data);
            e.setCancelled(true);
            /*
                            .add("block_uuid", metadata.key.uuid().toString())
                .add("block_pos", position.toSave())
            */
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






























































