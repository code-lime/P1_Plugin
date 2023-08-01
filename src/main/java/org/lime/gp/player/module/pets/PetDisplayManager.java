package org.lime.gp.player.module.pets;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.lime.display.DisplayManager;
import org.lime.gp.database.rows.PetsRow;
import org.lime.gp.database.tables.Tables;
import org.lime.gp.lime;
import org.lime.gp.module.EntityPosition;

import java.util.Map;

public class PetDisplayManager extends DisplayManager<Integer, PetsRow, PetDisplay> {
    @Override public boolean isFast() { return true; }
    @Override public boolean isAsync() { return true; }
    @Override public Map<Integer, PetsRow> getData() {
        return Tables.PETS_TABLE.getMapBy(v -> {
            if (v == null) return false;
            if (v.isHide) return false;
            if (!Pets.pets.containsKey(v.pet)) return false;
            Player player = EntityPosition.onlinePlayers.get(v.uuid);
            if (player == null) return false;
            if (player.getGameMode() == GameMode.SPECTATOR) return false;
            if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) return false;
            return true;
        }, v -> v.id);
    }
    @Override public PetDisplay create(Integer integer, PetsRow row) {
        return new PetDisplay(Pets.pets.get(row.pet), row);
    }
}
