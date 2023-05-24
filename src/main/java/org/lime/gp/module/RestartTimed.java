package org.lime.gp.module;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.ServerOperator;
import org.lime.core;
import org.lime.gp.lime;
import org.lime.gp.player.ui.CustomUI;
import org.lime.gp.player.ui.ImageBuilder;
import org.lime.system;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class RestartTimed extends CustomUI.GUI {
    private static final RestartTimed Instance = new RestartTimed();
    private RestartTimed() { super(CustomUI.IType.ACTIONBAR); }
    @Override public Collection<ImageBuilder> getUI(Player player) {
        if (restart_time == null || restart_message == null) return Collections.emptyList();
        return Collections.singleton(ImageBuilder.of(player, restart_message.val0).withColor(restart_message.val1));
    }

    public static core.element create() {
        return core.element.create(RestartTimed.class)
                .withInit(RestartTimed::init)
                .addCommand("restart.timed", v -> v
                        .withCheck(ServerOperator::isOp)
                        .withTab((sender, args) -> switch (args.length) {
                            case 1 -> Arrays.asList("time", "sec", "cancel");
                            case 2 -> switch (args[0]) {
                                case "time" -> Collections.singletonList("00:00:00");
                                case "sec" -> Collections.singletonList("[sec]");
                                default -> Collections.emptyList();
                            };
                            default -> Collections.emptyList();
                        })
                        .withExecutor((sender, args) -> {
                            switch (args.length) {
                                case 1 -> {
                                    if ("cancel".equals(args[0])) restart_time = null;
                                    return true;
                                }
                                case 2 -> {
                                    switch (args[0]) {
                                        case "time" -> {
                                            system.Toast1<Integer> sec = system.toast(0);
                                            Arrays.stream(args[1].split(":")).forEach(_v -> sec.val0 = sec.val0 * 60 + Integer.parseUnsignedInt(_v));
                                            int time = (int) ((System.currentTimeMillis() / 1000) % 24 * 60 * 60);
                                            while (time < 0) time += 24 * 60 * 60;
                                            restart_time = (double)time;
                                            return true;
                                        }
                                        case "sec" -> {
                                            restart_time = (double)Integer.parseUnsignedInt(args[1]);
                                            return true;
                                        }
                                    }
                                    return true;
                                }
                            }
                            return false;
                        })
                );
    }
    public static Double restart_time = null;
    public static system.Toast2<String, TextColor> restart_message = null;
    private static boolean color = false;
    public static void init() {
        double delta = 0.5;
        CustomUI.addListener(Instance);
        lime.repeat(() -> {
            if (restart_time == null) return;
            restart_time -= delta;
            color = !color;

            int total = (int)(double)restart_time;
            if (total == 0) {
                MinecraftServer minecraftServer = MinecraftServer.getServer();
                minecraftServer.getPlayerList().saveAll();
                minecraftServer.saveAllChunks(true, true, true);
                lime.once(Bukkit::shutdown, 1);
                return;
            }

            int sec = total % 60;
            total /= 60;
            int min = total % 60;
            total /= 60;
            int hour = total;

            restart_message = system.toast("Рестарт через: " +
                org.apache.commons.lang.StringUtils.leftPad(String.valueOf(hour), 2, '0') + ":" +
                org.apache.commons.lang.StringUtils.leftPad(String.valueOf(min), 2, '0') + ":" +
                org.apache.commons.lang.StringUtils.leftPad(String.valueOf(sec), 2, '0'),
                color ? NamedTextColor.GOLD : NamedTextColor.YELLOW);
            /*
            ImageBuilder.of(player, "Рестарт через: " +
                    org.apache.commons.lang.StringUtils.leftPad(String.valueOf(hour), 2, '0') + ":" +
                    org.apache.commons.lang.StringUtils.leftPad(String.valueOf(min), 2, '0') + ":" +
                    org.apache.commons.lang.StringUtils.leftPad(String.valueOf(sec), 2, '0')
            ).withColor(color ? NamedTextColor.GOLD : NamedTextColor.YELLOW);
            */
        }, delta);
    }
}
