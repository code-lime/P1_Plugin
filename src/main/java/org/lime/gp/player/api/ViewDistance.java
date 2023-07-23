package org.lime.gp.player.api;

import org.bukkit.entity.Player;
import org.lime.gp.lime;
import org.lime.reflection;

public class ViewDistance {
    private static final reflection.method getPlayerView_ViewDistance;
    private static final reflection.method clear_ViewMap;
    static {
        reflection.method _getPlayerView_ViewDistance = null;
        reflection.method _clear_ViewMap = null;
        try {
            Class<?> viewDistance = Class.forName("xuan.cat.fartherviewdistance.api.ViewDistance");
            Class<?> viewMap = Class.forName("xuan.cat.fartherviewdistance.api.data.PlayerView");
            _getPlayerView_ViewDistance = reflection.method.of(viewDistance, "getPlayerView", Player.class);
            _clear_ViewMap = reflection.method.of(viewMap, "clear");
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable e) {
            lime.logStackTrace(e);
        }
        getPlayerView_ViewDistance = _getPlayerView_ViewDistance;
        clear_ViewMap = _clear_ViewMap;
    }

    public static boolean clearPlayerView(Player player) {
        if (getPlayerView_ViewDistance == null || clear_ViewMap == null) return false;
        try {
            clear_ViewMap.call(getPlayerView_ViewDistance.call(null, new Object[]{ player }), new Object[0]);
            return true;
        } catch (Throwable e) {
            lime.logStackTrace(e);
            return false;
        }
    }
}















