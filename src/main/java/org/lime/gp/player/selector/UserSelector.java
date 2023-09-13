package org.lime.gp.player.selector;

import org.lime.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class UserSelector implements Listener {
    public static CoreElement create() {
        return CoreElement.create(UserSelector.class)
                .withInit(UserSelector::init)
                .withInstance();
    }

    private final static HashMap<UUID, ISelector> users = new HashMap<>();
    public static void init() {
        lime.repeat(UserSelector::update, 0.5);
        AnyEvent.addEvent("user.selector", AnyEvent.type.other, v -> v.createParam("remove"), (player, action) -> {
            switch (action) {
                case "remove":
                    removeSelector(player);
                    break;
            }
        });
    }
    public static void update() {
        users.entrySet().removeIf(kv -> {
            Player player = Bukkit.getPlayer(kv.getKey());
            ISelector selector = kv.getValue();
            if (player == null || selector.isRemoveUpdate()) {
                selector.onRemove();
                return true;
            }
            return false;
        });
    }
    public static Optional<ISelector> getSelector(Player player) {
        return getSelector(player.getUniqueId());
    }
    public static Optional<ISelector> getSelector(UUID uuid) {
        return Optional.ofNullable(users.get(uuid));
    }
    public static void setSelector(Player player, ISelector selector) {
        if (selector == null) {
            removeSelector(player);
            return;
        }
        selector.player = player;
        ISelector old = users.put(player.getUniqueId(), selector);
        if (old != null) old.onRemove();
        selector.init();
    }
    public static void removeSelector(Player player) {
        removeSelector(player.getUniqueId());
    }
    public static void removeSelector(UUID uuid) {
        Optional.ofNullable(users.remove(uuid)).ifPresent(ISelector::onRemove);
    }

    @EventHandler public void onBlockClick(PlayerInteractEvent e) {
        if (e.getAction() == Action.PHYSICAL) return;
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        Optional.ofNullable(users.get(uuid))
                .filter(v -> v.onBlockClick(e))
                .ifPresent(v -> e.setCancelled(true));
    }
}














