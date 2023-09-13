package org.lime.gp.player.module;

import com.google.gson.JsonObject;
import org.lime.*;
import org.bukkit.entity.Player;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.extension.JManager;
import org.lime.plugin.CoreElement;

public class Settings {
    public static CoreElement create() {
        return CoreElement.create(Settings.class)
                .withInit(Settings::Init);
    }

    public static void Init() {
        AnyEvent.addEvent("settings", AnyEvent.type.none, builder -> builder.createParam("showBackpack").createParam(Boolean::parseBoolean, "true", "false"), (player, setting, value) -> {
            Data settings = getSettings(player);
            switch (setting) {
                case "showBackpack": settings.showBackpack = value; break;
            }
            setSettings(player, settings);
        });
    }

    public static Data getSettings(Player player) {
        JsonObject json = JManager.get(JsonObject.class, player.getPersistentDataContainer(), "settings", null);
        return json == null ? Data.getEmpty() : Data.parse(json);
    }
    public static void setSettings(Player player, Data data) {
        JManager.set(player.getPersistentDataContainer(), "settings", data.save());
    }

    public static class Data {
        public static Data getEmpty() {
            return new Data();
        }

        public boolean showBackpack = true;

        private Data() { }
        private Data(JsonObject json) {
            if (json.has("showBackpack")) showBackpack = json.get("showBackpack").getAsBoolean();
        }
        public JsonObject save() {
            JsonObject json = new JsonObject();
            json.addProperty("showBackpack", showBackpack);
            return json;
        }

        public static Data parse(JsonObject json) {
            try {
                return new Data(json);
            } catch (Exception ignore) {
                return getEmpty();
            }
        }
    }
}
