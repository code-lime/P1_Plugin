package org.lime.gp.chat;

import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.JoinConfiguration;
import org.lime.core;
import org.lime.gp.lime;
import org.lime.system;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class LangMessages {
    public static core.element create() {
        return core.element.create(LangMessages.class)
                .<JsonObject>addConfig("lang", v -> v.withInvoke(LangMessages::config).withDefault(() -> {
                    JsonObject def = new JsonObject();
                    for (Message msg : Message.values()) msg.addDefault(def);
                    return def;
                }));
    }

    private static final HashMap<Message, List<String>> msgs = new HashMap<>();
    public enum Message {
        Medic_Teleport_HP("medic", "teleport", "hp"),
        Medic_Teleport_Die("medic", "teleport", "die"),

        Discord_Check("discord", "check"),
        Discord_Cancel("discord", "cancel"),
        Discord_Timeout("discord", "timeout"),
        Discord_Done("discord", "done"),
        Discord_Reset("discord", "reset"),

        Tab_Header("tab", "header"), //{online} {tps}
        Tab_Footer("tab", "footer"), //{online} {tps}

        PayDay_Done("payday", "done"), //{}
        PayDay_Error("payday", "error"),

        Chat_Show("chat", "show"), //{text}

        Chat_MyID("chat", "my_id"), //{id}
        Chat_Join("chat", "join"),

        Chat_NoVoice("chat", "no_voice"),
        Chat_NoVoiceActionBar("chat", "no_voice_actionbar"),

        Menu_Error_Weapon("menu", "error", "weapon"),

        Sms_Error_PhoneNotFounded("sms", "error", "phone_not_founded"),
        Sms_Error_SendToSelf("sms", "error", "send_to_self"),
        Sms_Error_LongMessage("sms", "error", "long_message"),
        Sms_Error_NotHavePhone("sms", "error", "not_have_phone"),
        Sms_Error_Ignore("sms", "error", "ignore"),
        Sms_Error_Cooldown("sms", "error", "cooldown"),
        Sms_Error_World("sms", "error", "world"),
        Sms_Error_Die("sms", "error", "die"),
        Sms_Done("sms", "done"),

        BookEditor_Paper_SignEmpty("book_editor", "paper", "sign_empty"),
        BookEditor_Paper_Sign("book_editor", "paper", "sign"),
        BookEditor_Paper_Name("book_editor", "paper", "name"),
        BookEditor_Book_Title("book_editor", "book", "title"),
        BookEditor_Book_Lore("book_editor", "book", "lore"),

        Phone_Wallet_Title("phone", "wallet", "title"),

        Brush_Bucket_NotFound("brush", "bucket", "not_found"),
        Brush_Bucket_Empty("brush", "bucket", "empty"),

        Entity_BackPack_Lock("entity", "backpack", "lock"),
        Entity_BackPack_Unlock("entity", "backpack", "unlock"),

        Work_Error_Use("work", "error", "use");

        public final List<String> args;
        private final Component def;

        Message(String... args) {
            this(ChatHelper.formatComponent("<RED>Сообщение '"+getPath(Arrays.asList(args))+"'</>"), args);
        }
        Message(Component def, String... args) {
            this.args = Arrays.asList(args);
            this.def = def;
        }

        public List<String> parse(JsonObject parent) {
            try
            {
                JsonElement dat = parent;
                for (String arg : args) dat = dat.getAsJsonObject().get(arg);
                return dat.isJsonNull() ? null : dat.isJsonArray() ? Streams.stream(dat.getAsJsonArray().iterator()).map(JsonElement::getAsString).collect(Collectors.toList()) : List.of(dat.getAsString());
            }
            catch (Exception e) {
                return null;
            }
        }

        public String path() {
            return getPath(args);
        }
        private static String getPath(List<String> args) {
            return String.join(".", args);
        }
        public void addDefault(JsonObject json) {
            JsonObject dat = json;
            int length = args.size();
            for (int i = 0; i < length; i++) {
                String arg = args.get(i);
                if (i == length - 1) {
                    dat.add(arg, JsonNull.INSTANCE);
                    return;
                }
                if (!dat.has(arg) || !dat.get(arg).isJsonObject()) dat.add(arg, new JsonObject());
                dat = dat.get(arg).getAsJsonObject();
            }
        }

        public Component getSingleMessage() {
            return Component.join(JoinConfiguration.separator(Component.newline()), getMessages());
        }
        public Component getSingleMessage(Apply apply) {
            return Component.join(JoinConfiguration.separator(Component.newline()), getMessages(apply));
        }
        public List<Component> getMessages() {
            return getMessages(Apply.of());
        }
        public List<Component> getMessages(Apply apply) {
            return Optional.ofNullable(msgs.get(this)).map(v -> ChatHelper.formatComponent(v, apply)).orElseGet(() -> Collections.singletonList(def));
        }
        public void sendMessage(Player player, Apply apply) {
            this.getMessages(apply).forEach(player::sendMessage);
        }
        public void sendMessage(Player player) {
            this.sendMessage(player, Apply.of());
        }
    }
    public static void config(JsonObject json) {
        msgs.clear();
        for (Message msg : Message.values()) {
            List<String> list = msg.parse(json);
            if (list == null) {
                lime.logOP(ChatColor.GOLD + "Lang not founded: '" + msg.path() + "'");
                msg.addDefault(json);
            }
            else msgs.put(msg, list);
        }
        lime.writeAllConfig("lang", system.toFormat(json));
    }
}

































