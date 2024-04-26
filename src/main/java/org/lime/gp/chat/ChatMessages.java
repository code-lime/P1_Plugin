package org.lime.gp.chat;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.permissions.ServerOperator;
import org.lime.gp.database.Methods;
import org.lime.gp.database.rows.FriendRow;
import org.lime.gp.database.rows.QuentaRow;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.entity.component.data.ActionInstance;
import org.lime.gp.extension.Cooldown;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.lime;
import org.lime.gp.module.JavaScript;
import org.lime.gp.player.menu.LangEnum;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.player.module.Death;
import org.lime.gp.player.module.Ghost;
import org.lime.gp.player.voice.RadioData;
import org.lime.gp.player.voice.Voice;
import org.lime.plugin.CoreElement;
import org.lime.system.Time;
import org.lime.system.execute.Action1;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChatMessages implements Listener {
    public static CoreElement create() {
        return CoreElement.create(ChatMessages.class)
                .withInstance()
                .withInit(ChatMessages::init)
                .addCommand("sms", v -> v
                        .withExecutor(ChatMessages::sms)
                        .withCheckCast(Player.class)
                        .addCheck(_v -> !Ghost.isGhost(_v))
                        .withTab((sender, args) -> {
                            Player player = sender;
                            if (args.length > 1) return getSmsPresets(args[0]);
                            List<String> list = FriendRow.getFriendsByUUID(player.getUniqueId()).stream().map(f -> f.friendName).filter(Objects::nonNull).collect(Collectors.toList());
                            list.add("101");
                            list.add("103");
                            return list;
                        })
                )
                .addCommand("do", v -> v
                        .withCheckCast(Player.class)
                        .withExecutor((sender, args) -> MenuCreator.showLang(sender, LangEnum.DO, Apply.of().add("text", String.join(" ", args))))
                        .withTab("[действие]")
                )
                .addCommand("me", v -> v
                        .withCheckCast(Player.class)
                        .withExecutor((sender, args) -> MenuCreator.showLang(sender, LangEnum.ME, Apply.of().add("text", String.join(" ", args))))
                        .withTab("[действие]")
                )
                .addCommand("news", v -> v
                        .withCheckCast(Player.class)
                        .withExecutor((sender, args) -> MenuCreator.showLang(sender, LangEnum.NEWS, Apply.of().add("text", String.join(" ", args))))
                        .withTab("[сообщение]")
                )
                .addCommand("news.test", v -> v
                        .withCheckCast(Player.class)
                        .withExecutor((sender, args) -> MenuCreator.showLang(sender, LangEnum.NEWS_TEST, Apply.of().add("text", String.join(" ", args))))
                        .withTab("[сообщение]")
                )
                .addCommand("now", v -> v
                        .withCheckCast(Player.class)
                        .withExecutor((sender, args) -> MenuCreator.showLang(sender, LangEnum.TIME))
                )
                .addCommand("try", v -> v
                        .withCheckCast(Player.class)
                        .withExecutor((sender, args) -> MenuCreator.showLang(sender, LangEnum.TRY, Apply.of().add("text", String.join(" ", args))))
                        .withTab("[действие]")
                )
                .addCommand("trydo", v -> v
                        .withCheckCast(Player.class)
                        .withExecutor((sender, args) -> MenuCreator.showLang(sender, LangEnum.TRYDO, Apply.of().add("text", String.join(" ", args))))
                        .withTab("[действие]")
                )
                .addCommand("dice", v -> v
                        .withCheckCast(Player.class)
                        .withExecutor((sender, args) -> args.length >= 2 && MenuCreator.showLang(sender, LangEnum.DICE, Apply.of().add("scale", args[0]).add("text", Stream.of(args).skip(1).collect(Collectors.joining(" ")))))
                        .withTab((sender, args) -> args.length == 1 ? List.of("6", "20", "100") : Collections.singletonList("[действие]"))
                )
                .addCommand("stats", v -> v
                        .withCheckCast(Player.class)
                        .withExecutor(sender -> MenuCreator.showLang(sender, LangEnum.STATS))
                )
                .addCommand("action", v -> v
                        .withCheckCast(Player.class)
                        .withTab((sender, args) -> Collections.singletonList(args.length == 1 ? "[время]" : "[действие]"))
                        .withExecutor((sender, args) -> {
                            if (!QuentaRow.hasQuenta(sender.getUniqueId())) {
                                LangMessages.Message.Action_Error.sendMessage(sender);
                                return false;
                            }
                            if (args.length < 2) return false;
                            int sec = Time.formattedTime(args[0]);
                            String action = Stream.of(args).skip(1).collect(Collectors.joining(" "));
                            showAction(sender, split(action, 50, -1), sec);
                            LangMessages.Message.Action_Done.sendMessage(sender);
                            return true;
                        })
                )
                .addCommand("chat.test", v -> v
                        .withCheck(ServerOperator::isOp)
                        .withExecutor((sender, args) -> {
                            sender.sendMessage(ChatHelper.formatComponent(String.join(" ", args)));
                            return true;
                        })
                )
                .addCommand("js.test", v -> v
                        .withCheck(ServerOperator::isOp)
                        .withExecutor((sender, args) -> {
                            sender.sendMessage(String.valueOf(JavaScript.invoke(String.join(" ", args), Collections.emptyMap()).orElse(null)));
                            return true;
                        })
                );
    }

    private static Collection<String> getSmsPresets(String phone) {
        List<String> presets = new ArrayList<>();
        presets.add("[cообщение]");
        Tables.SMSPRESET_TABLE.forEach(row -> {
            if (!phone.equals(row.phone)) return;
            presets.add(row.text);
        });
        return presets;
    }

    public static void init() {
        lime.repeat(() -> sendList.entrySet().removeIf(kv -> kv.getValue().tryRemove()), 1);
    }

    private static List<String> split(String text, int totalLength, int totalLines) {
        List<String> lines = new ArrayList<>();

        String line = "";

        for (String word : text.split(" ")) {
            String _line = (line.equals("") ? "" : (line + " ")) + word;

            if (_line.length() > totalLength) {
                lines.add(line);
                if (word.length() > totalLength) {
                    String s = word;
                    while (true) {
                        int count = Math.min(s.length(), totalLength);
                        if (totalLength > s.length()) break;
                        lines.add(s.substring(0, count));
                        s = new String(s.toCharArray(), count, s.length() - count);
                    }
                    line = s;
                } else {
                    line = word;
                }
            } else {
                line = _line;
            }
        }
        if (!line.equals("")) lines.add(line);

        int length = lines.size();
        if (totalLines > 0 && length > totalLines) {
            length = totalLength;
            lines.set(totalLines - 1, lines.get(totalLines - 1) + "...");
        }

        List<String> ret = new ArrayList<>();
        length = Math.min(length, lines.size());
        for (int i = 0; i < length; i++) ret.add(lines.get(i));

        return ret;
    }
    private static class SendData {
        public final long time;
        public final long showTime;

        public final List<String> lines;

        public SendData(String text) {
            this.time = System.currentTimeMillis();
            this.lines = split(text, 50, 3);
            long showTime = 0;
            for (String line : lines) showTime += Math.max(line.length() * 2, 20) * 50L;
            this.showTime = showTime;
        }

        public boolean tryRemove() {
            return time + showTime <= System.currentTimeMillis();
        }
    }
    private static final ConcurrentHashMap<UUID, SendData> sendList = new ConcurrentHashMap<>();
    public static Optional<List<Component>> chatText(UUID uuid) {
        return Optional.ofNullable(sendList.get(uuid))
                .map(data -> data.lines.stream()
                        .map(line -> Apply.of().add("text", line))
                        .map(LangMessages.Message.Chat_Show::getSingleMessage)
                        .toList()
                );
    }

    public static void showHeadText(UUID uuid, String message) {
        sendList.put(uuid, new ChatMessages.SendData(message));
    }

    @EventHandler
    @Deprecated
    public static void on(org.bukkit.event.player.PlayerChatEvent e) {
        e.setCancelled(true);

        String message = e.getMessage();
        Player owner = e.getPlayer();
        UUID uuid = owner.getUniqueId();
        MenuCreator.showLang(owner, LangEnum.CHAT, Apply.of().add("text", message));
        if (Voice.isMute(uuid)) return;
        lime.log("[CHAT] " + owner.getName() + ": " + message);
        RadioData.getData(owner.getInventory().getItemInMainHand()).filter(v -> v.enable).filter(v -> v.state.isInput).ifPresent(radio -> {
            MenuCreator.showLang(owner, LangEnum.RADIO, Apply.of()
                    .add("text", message)
                    .add("radio_level", String.valueOf(radio.level))
                    .add("radio_distance", String.valueOf(radio.total_distance))
            );
        });
    }

    public static void smsFast(Player player, UserRow user, int phone, Methods.CallType type, String message) {
        UUID uuid = player.getUniqueId();
        String key = "sms." + phone;
        if (!type.check(player.getWorld())) {
            LangMessages.Message.Sms_Error_World.sendMessage(player, Apply.of()
                    .add("phone", String.valueOf(phone))
            );
            return;
        }
        int cooldown = (int)Cooldown.getCooldown(uuid, key);
        if (cooldown > 0) {
            LangMessages.Message.Sms_Error_Cooldown.sendMessage(player, Apply.of()
                    .add("phone", String.valueOf(phone))
                    .add("sec", String.valueOf(cooldown))
            );
            return;
        }
        Cooldown.setCooldown(uuid, key, 90);
        MenuCreator.showLang(player, LangEnum.SMS_DISPLAY);
        lime.once(() -> {
            LangMessages.Message.Sms_Done.sendMessage(player, Apply.of().add("phone", String.valueOf(phone)));
            Methods.callLog(user.id, type, message, log_id -> MenuCreator.showLang(LangEnum.SMS_CALL, Apply.of().add("log_id", String.valueOf(log_id))));
        }, 1);
    }
    public static boolean sms(CommandSender sender, Command command, String label, String[] args) {
        if (!Methods.isTableEnable("calls")) return false;
        if (!(sender instanceof Player player)) return false;
        if (args.length < 2) return false;
        String message = Arrays.stream(args).skip(1).collect(Collectors.joining(" "));
        if (message.length() == 0) return false;
        if (message.length() > 500) {
            LangMessages.Message.Sms_Error_LongMessage.sendMessage(player);
            return true;
        }
        UUID uuid = player.getUniqueId();
        UserRow.getBy(uuid).ifPresentOrElse(user -> {
            Action1<Integer> sms = (phone) -> {
                if (Death.isDamageLay(uuid) && phone != 103) {
                    LangMessages.Message.Sms_Error_Die.sendMessage(player);
                    return;
                }
                switch (phone) {
                    case 101 -> {
                        smsFast(player, user, phone, Methods.CallType.POLICE, message);
                        return;
                    }
                    case 103 -> {
                        smsFast(player, user, phone, Death.isDamageLay(uuid) ? Methods.CallType.MEDIC_DIE : Methods.CallType.MEDIC, message);
                        return;
                    }
                }
                Tables.USER_TABLE.getBy(v -> phone.equals(v.phone)).ifPresentOrElse(row -> {
                    if (!Methods.CallType.PHONE.check(player.getWorld())) {
                        LangMessages.Message.Sms_Error_World.sendMessage(player, Apply.of().add("phone", String.valueOf(phone)));
                        return;
                    }
                    if (user.id == row.id) {
                        LangMessages.Message.Sms_Error_SendToSelf.sendMessage(player);
                        return;
                    }
                    Methods.isIgnore(row.id, user.id, ignore -> {
                        if (ignore) {
                            LangMessages.Message.Sms_Error_Ignore.sendMessage(player, Apply.of().add(row));
                            return;
                        }
                        MenuCreator.showLang(player, LangEnum.SMS_DISPLAY);
                        lime.once(() -> Methods.smsLog(user.id, row.id, message, message_id -> MenuCreator.showLang(player, LangEnum.SMS, Apply.of().add("message_id", String.valueOf(message_id)))), 1);
                    });
                }, () -> LangMessages.Message.Sms_Error_PhoneNotFounded.sendMessage(player));
            };
            FriendRow.getFriendByUUIDandName(player.getUniqueId(), args[0])
                    .flatMap(FriendRow::getFriendPhone)
                    .ifPresentOrElse(sms, () -> ExtMethods.parseUnsignedInt(args[0]).ifPresentOrElse(sms, () -> LangMessages.Message.Sms_Error_PhoneNotFounded.sendMessage(player)));
        }, () -> LangMessages.Message.Sms_Error_NotHavePhone.sendMessage(player));
        return true;
    }

    public static void showAction(Player player, List<String> messages, int time) {
        ActionInstance.createAction(player, player.getLocation(), messages, time);
    }
}













