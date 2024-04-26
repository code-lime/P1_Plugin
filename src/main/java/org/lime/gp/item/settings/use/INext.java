package org.lime.gp.item.settings.use;

import org.bukkit.entity.Player;
import org.lime.gp.item.settings.IItemSetting;

public interface INext extends IItemSetting {
    void executeNext(Player player);
}
