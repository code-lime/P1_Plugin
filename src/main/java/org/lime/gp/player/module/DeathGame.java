package org.lime.gp.player.module;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.lime.gp.module.biome.time.DateTime;
import org.lime.gp.lime;
import org.lime.gp.admin.Administrator;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.module.biome.time.DayManager;

import java.util.Set;

public class DeathGame {
    public static org.lime.core.element create() {
        return org.lime.core.element.create(DeathGame.class)
                .withInit(DeathGame::init);
    }
    private static void init() {
        lime.repeat(DeathGame::update, 10);
    }
    private static void update() {
        DateTime now = DayManager.now();
        Bukkit.getOnlinePlayers().forEach(player -> UserRow.getBy(player).ifPresent(user -> user.dieDate.ifPresent(dieDate -> {
            if (dieDate.getTotalSeconds() > now.getTotalSeconds()) return;
            Set<String> tags = player.getScoreboardTags();
            if (tags.contains("death")) return;
            tags.add("death");
            executeDeath(player, user, dieDate);
        })));
    }
    private static void executeDeath(Player player, UserRow user, DateTime dieDate) {
        Administrator.aban(player.getUniqueId(), "Вы мертвы! Дата сметри: " + dieDate.toString(), null);
    }
}
