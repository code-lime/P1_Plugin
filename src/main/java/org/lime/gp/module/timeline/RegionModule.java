package org.lime.gp.module.timeline;

import com.google.gson.JsonPrimitive;
import net.minecraft.core.SectionPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.ChunkCoordIntPair;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.permissions.ServerOperator;
import org.bukkit.util.Vector;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.lime;
import org.lime.gp.player.ui.CustomUI;
import org.lime.gp.player.ui.ImageBuilder;
import org.lime.plugin.CoreElement;
import org.lime.system.Time;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class RegionModule {
    private static ChunkCoordIntPair getChunkID(Location location) {
        return getChunkID(location.toVector());
    }
    private static ChunkCoordIntPair getChunkID(Vector pos) {
        return new ChunkCoordIntPair(SectionPosition.blockToSectionCoord(pos.getBlockX()), SectionPosition.blockToSectionCoord(pos.getBlockZ()));
    }

    private static Toast2<Integer, Integer> getRegionID(Location location) {
        return getRegionID(location.toVector());
    }
    private static Toast2<Integer, Integer> getRegionID(Vector pos) {
        ChunkCoordIntPair chunk = getChunkID(pos);
        return Toast.of(chunk.getRegionX(), chunk.getRegionZ());
    }

    private static final WorldServer worldLoader = lime.MainWorld.getHandle();
    private static @Nullable RegionTimeline timeline = null;
    private static @Nullable String drawInfo = null;

    private static boolean ENABLE = false;

    public static CoreElement create() {
        return CoreElement.create(RegionModule.class)
                .addCommand("timeline", v -> v
                        .withCheck(ServerOperator::isOp)
                        .withTab((s, args) -> ENABLE ? switch (args.length) {
                            case 1 -> List.of("select", "time", "clean");
                            case 2 -> switch (args[0]) {
                                case "select" -> Stream.concat(
                                        RegionDownloader.hasLast() ? Stream.of("last") : Stream.<String>empty(),
                                        s instanceof Player player
                                                ? Stream.of("last", "[x] [z]", getRegionID(player.getLocation()).invokeGet((x,z) -> x + " " + z))
                                                : Stream.of("last", "[x] [z]")
                                ).toList();
                                case "time" -> List.of("step", "start", "stop", "speed", "set");
                                default -> Collections.emptyList();
                            };
                            case 3 -> switch (args[0]) {
                                case "select" -> args[1].equals("last")
                                        ? Collections.emptyList()
                                        : s instanceof Player player
                                            ? List.of("[z]", String.valueOf(getRegionID(player.getLocation()).val1))
                                            : List.of("[z]");
                                case "time" -> switch (args[1]) {
                                    case "step" -> List.of("[time(s,m,h,d,w)]");
                                    case "speed" -> List.of("[time(s,m,h,d,w)_per_sec]");
                                    case "set" -> timeline == null
                                            ? List.of("!!!NOT_SELECTED!!!")
                                            : Stream.concat(
                                                    timeline.getTimes()
                                                            .stream()
                                                            .map(_v -> Time.formatCalendar(_v, true, true)),
                                                    Stream.of("[dd.MM.yyyy] [HH:mm:ss]", "[dd.MM.yyyy]")
                                            ).toList();
                                    default -> Collections.emptyList();
                                };
                                default -> Collections.emptyList();
                            };
                            case 4 -> switch (args[0]) {
                                case "time" -> switch (args[1]) {
                                    case "set" -> List.of("[dd.MM.yyyy]");
                                    default -> Collections.emptyList();
                                };
                                default -> Collections.emptyList();
                            };
                            default -> Collections.emptyList();
                        } : Collections.singletonList("!!!NOT ENABLED!!!"))
                        .withExecutor((s, args) -> ENABLE ? switch (args.length) {
                            case 1 -> switch (args[0]) {
                                case "clean" -> {
                                    if (timeline == null) {
                                        s.sendMessage("ERROR! Timeline already cleaned! Select it first");
                                        yield true;
                                    }
                                    timeline.loadTime(worldLoader, timeline.getLastTime());
                                    timeline = null;
                                    yield true;
                                }
                                default -> false;
                            };
                            case 2 -> switch (args[0]) {
                                case "select" -> switch (args[1]) {
                                    case "last" -> {
                                        s.sendMessage("Begin load last region");
                                        RegionDownloader.loadLastRegion(
                                                worldLoader,
                                                s instanceof Player p ? p.getUniqueId() : null,
                                                dat -> dat.ifPresentOrElse(timeline -> {
                                                    if (timeline.isEmpty()) {
                                                        s.sendMessage("WARNING! Last region in '"+timeline.getX()+" "+timeline.getZ()+"' founded, but empty! Skipped!");
                                                        return;
                                                    }
                                                    s.sendMessage("Region '"+timeline.getX()+" "+timeline.getZ()+"' loaded!");
                                                    RegionModule.timeline = timeline;
                                                }, () -> s.sendMessage("ERROR! Error load region '"+timeline.getX()+" "+timeline.getZ()+"'! Skipped!")));
                                        yield true;
                                    }
                                    default -> false;
                                };
                                case "time" -> {
                                    if (timeline == null) {
                                        s.sendMessage("ERROR! Timeline not selected! Select it first");
                                        yield true;
                                    }
                                    yield switch (args[1]) {
                                        case "start" -> {
                                            timeline.setEnable(true);
                                            s.sendMessage("Timeline started");
                                            yield true;
                                        }
                                        case "stop" -> {
                                            timeline.setEnable(false);
                                            s.sendMessage("Timeline stopped");
                                            yield true;
                                        }
                                        default -> false;
                                    };
                                }
                                default -> false;
                            };
                            case 3 -> switch (args[0]) {
                                case "select" -> {
                                    if (timeline != null) {
                                        s.sendMessage("ERROR! Timeline already selected! Clean it first");
                                        yield true;
                                    }
                                    ExtMethods.parseInt(args[1]).flatMap(x -> ExtMethods.parseInt(args[2]).map(z -> Toast.of(x,z)))
                                            .ifPresentOrElse(region -> {
                                                s.sendMessage("Begin download region '"+region.val0+" "+region.val1+"'");
                                                RegionDownloader.downloadRegion(
                                                        worldLoader,
                                                        region.val0, region.val1,
                                                        s instanceof Player p ? p.getUniqueId() : null,
                                                        dat -> dat.ifPresentOrElse(timeline -> {
                                                            if (timeline.isEmpty()) {
                                                                s.sendMessage("WARNING! Region '"+region.val0+" "+region.val1+"' downloaded, but empty! Skipped!");
                                                                return;
                                                            }
                                                            s.sendMessage("Region '"+region.val0+" "+region.val1+"' downloaded!");
                                                            RegionModule.timeline = timeline;
                                                        }, () -> s.sendMessage("ERROR! Error download and load region '"+region.val0+" "+region.val1+"'! Skipped!")));
                                            }, () -> s.sendMessage("Key '"+args[1]+" "+args[2]+"' not region ID"));
                                    yield true;
                                }
                                case "time" -> {
                                    if (timeline == null) {
                                        s.sendMessage("ERROR! Timeline not selected! Select it first");
                                        yield true;
                                    }
                                    yield switch (args[1]) {
                                        case "step" -> {
                                            timeline.addFrame(worldLoader, Time.formattedTime(args[2]));
                                            s.sendMessage("Timeline step");
                                            yield true;
                                        }
                                        case "speed" -> {
                                            timeline.setTimePerSec(Time.formattedTime(args[2]));
                                            s.sendMessage("Timeline step");
                                            yield true;
                                        }
                                        case "set" -> {
                                            timeline.setFrame(worldLoader, Time.parseCalendar(args[2]));
                                            s.sendMessage("Timeline set");
                                            yield true;
                                        }
                                        default -> false;
                                    };
                                }
                                default -> false;
                            };
                            case 4 -> switch (args[0]) {
                                case "time" -> {
                                    if (timeline == null) {
                                        s.sendMessage("ERROR! Timeline not selected! Select it first");
                                        yield true;
                                    }
                                    yield switch (args[1]) {
                                        case "set" -> {
                                            timeline.setFrame(worldLoader, Time.parseCalendar(args[2] + " " + args[3], true));
                                            s.sendMessage("Timeline set");
                                            yield true;
                                        }
                                        default -> false;
                                    };
                                }
                                default -> false;
                            };
                            default -> false;
                        } : false)
                )
                .withInit(RegionModule::init)
                .<JsonPrimitive>addConfig("timeline", v -> v
                        .withParent("enable")
                        .withInvoke(json -> ENABLE = json.getAsBoolean())
                        .withDefault(new JsonPrimitive(ENABLE))
                );
    }
    private static void init() {
        lime.repeat(RegionModule::update, 1);

        CustomUI.addListener(new CustomUI.GUI(CustomUI.IType.ACTIONBAR) {
            @Override public Collection<ImageBuilder> getUI(Player player) {
                return drawInfo == null
                        ? Collections.emptyList()
                        : Collections.singletonList(ImageBuilder.of(player, drawInfo));
            }
        });
    }
    private static void update() {
        if (timeline == null) {
            drawInfo = null;
            return;
        }
        if (!ENABLE) {
            timeline = null;
            return;
        }
        timeline.onTick(worldLoader);
        drawInfo = timeline.drawInfo();
    }
}
