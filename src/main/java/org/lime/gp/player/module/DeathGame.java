package org.lime.gp.player.module;

import java.util.Calendar;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.lime.system;
import org.lime.gp.lime;
import org.lime.gp.admin.Administrator;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.module.DayManager;

public class DeathGame {
    public static org.lime.core.element create() {
        return org.lime.core.element.create(DeathGame.class)
                .withInit(DeathGame::init);
    }

    private static final NamespacedKey DEATH_STATE = new NamespacedKey(lime._plugin, "death.state");

    private static final byte STATE_LIFE = 0;
    private static final byte STATE_DIE = 1;
    
    private static void init() {
        lime.repeat(DeathGame::update, 10);
    }
    private static void update() {
        Calendar now = DayManager.now();
        Bukkit.getOnlinePlayers().forEach(player -> {
            UserRow.getBy(player).ifPresent(user -> {
                user.dieDate.ifPresent(dieDate -> {
                    if (dieDate.getTimeInMillis() > now.getTimeInMillis()) return;
                    PersistentDataContainer data = player.getPersistentDataContainer();
                    if (data.getOrDefault(DEATH_STATE, PersistentDataType.BYTE, STATE_LIFE) == STATE_DIE) return;
                    data.set(DEATH_STATE, PersistentDataType.BYTE, STATE_DIE);
                    executeDeath(player, user, dieDate);
                });
            });
        });
    }
    private static void executeDeath(Player player, UserRow user, Calendar dieDate) {
        Administrator.aban(player.getUniqueId(), "Вы мертвы! Дата сметри: " + system.formatCalendar(dieDate, true), null);
    }
}
