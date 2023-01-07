package org.lime.gp.player.inventory;

import org.lime.core;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.database.Methods;
import org.lime.gp.database.Rows;
import org.lime.gp.database.Tables;
import org.lime.gp.lime;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.chat.LangMessages;
import org.lime.system;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.lime.gp.player.ui.SignUI;
import org.lime.gp.player.module.Death;

import java.util.*;

public final class PhoneInventory implements Listener {
    private static final PhoneInventory Instance = new PhoneInventory();
    private PhoneInventory(){}

    public static core.element create() {
        return core.element.create(PhoneInventory.class)
                .withInit(PhoneInventory::Init)
                .withInstance(Instance);
    }

    private static final PotionEffect WANTED_EFFECT = new PotionEffect(PotionEffectType.GLOWING, 40, 0, false, false, false);
    public static void InputName(Player player, String text, system.Action1<String> name) {
        SignUI.Create(player, "", "------", text, "------").SetCallback(lines -> {
            String _name = lines[0];
            if (_name.length() < 3 || _name.length() > 15) {
                LangMessages.Message.Phone_InputName_Error_LengthNotOf3To15.sendMessage(player);
                return;
            }
            name.invoke(_name);
        }).Show();
    }
    public static void InputName(Player player, system.Action1<String> name) {
        SignUI.Create(player, "", "------", "Введите название", "------").SetCallback(lines -> {
            String _name = lines[0];
            if (_name.length() < 3 || _name.length() > 15) {
                LangMessages.Message.Phone_InputName_Error_LengthNotOf3To15.sendMessage(player);
                return;
            }
            name.invoke(_name);
        }).Show();
    }
    public static void InputValue(Player player, system.Action1<Integer> cost) {
        SignUI.Create(player, "", "------", "Введите цену", "------").SetOutput(0, Integer::parseInt, val -> {
            if (val == null) {
                LangMessages.Message.Phone_InputValue_Error_NotNumber.sendMessage(player);
                return;
            }
            if (val < 0) {
                LangMessages.Message.Phone_InputValue_Error_Negative.sendMessage(player);
                return;
            }
            cost.invoke(val);
        }).Show();
    }

    public static void Init() {
        AnyEvent.addEvent("phone.wanted", AnyEvent.type.other, builder -> builder.createParam("add", "del").createParam("id", "nick").createParam("true", "false").createParam(v -> v, "[search]", "input"), (player, state, type, isReason, value) -> {
            int val;
            switch (state) {
                case "add": val = 1; break;
                case "del": val = -1; break;
                default: return;
            }
            system.Action1<String> next = (_value) -> {
                Rows.UserRow row;
                switch (type) {
                    case "id":
                        int id;
                        try {
                            id = Integer.parseUnsignedInt(_value);
                        } catch (Exception e) {
                            LangMessages.Message.Phone_Wanted_Error_NotNumber.sendMessage(player);
                            return;
                        }

                        row = Rows.UserRow.getByTimedID(id);
                        break;
                    case "nick":
                        row = Tables.USER_TABLE.getBy(v -> v.UserName.equals(_value));
                        break;
                    default: return;
                }
                if (row == null) {
                    LangMessages.Message.Phone_Wanted_Error_PlayerNotFounded.sendMessage(player);
                    return;
                }
                lime.once(() -> {
                    if (isReason.equals("true")) {
                        InputName(player, "Введите причину", reason -> {
                            Methods.WantedModify(row.ID, val, reason, () -> LangMessages.Message.Phone_Wanted_Done.sendMessage(player));
                        });
                    } else {
                        Methods.WantedModify(row.ID, val, () -> LangMessages.Message.Phone_Wanted_Done.sendMessage(player));
                    }
                }, 0.25);
            };
            if (!value.equals("input")) {
                next.invoke(value);
                return;
            }
            String text;
            switch (type) {
                case "id": text = "Введите ID"; break;
                case "nick": text = "Введите ник"; break;
                default: return;
            }
            SignUI.Create(player, "", "------", text, "------").SetCallback(lines -> next.invoke(lines[0])).Show();
        });
    }


}


















