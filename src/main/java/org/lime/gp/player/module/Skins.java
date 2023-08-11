package org.lime.gp.player.module;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.lime.core;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.chat.Apply;
import org.lime.gp.database.Methods;
import org.lime.gp.database.mysql.MySql;
import org.lime.gp.lime;
import org.lime.skin;
import org.lime.system;
import org.lime.gp.player.menu.MenuCreator;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Skins implements Listener {
    public static core.element create() {
        return core.element.create(Skins.class)
                .withInit(Skins::init)
                .withInstance();
    }
    public static void init() {
        AnyEvent.addEvent("skin.set", AnyEvent.type.other, builder -> builder.createParam(Integer::parseUnsignedInt, "[skin_id]", "0"), Skins::setSkin);
        AnyEvent.addEvent("skin.get", AnyEvent.type.other, player -> {
            PropertyMap properties = map(player);
            com.mojang.authlib.properties.Property property = of(properties, "old_textures");
            boolean custom;
            if (property == null) {
                custom = false;
                property = of(properties, "textures");
            } else {
                custom = true;
            }
            if (property == null) return;
            MenuCreator.show(player, "cupboard.menu.get", Apply.of()
                    .add("custom", custom ? "true" : "false")
                    .add("value", property.getValue())
                    .add("signature", property.getSignature())
            );
        });
    }
    @EventHandler public static void on(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        Methods.SQL.Async.rawSqlOnce("SELECT user_flags.skin_id FROM user_flags WHERE user_flags.uuid = '"+player.getUniqueId()+"';", Integer.class, skin_id -> {
            if (skin_id == null) return;
            setSkin(player, skin_id);
        });
    }

    private static PropertyMap map(Player player) { return ((CraftPlayer)player).getHandle().getGameProfile().getProperties(); }
    //private static com.mojang.authlib.properties.Property of(Player player, String key) { return of(map(player), key); }
    private static com.mojang.authlib.properties.Property of(PropertyMap properties, String key) { return properties.containsKey(key) ? properties.get(key).iterator().next() : null; }

    private static void updateSkin(Player player) {
        Bukkit.getOnlinePlayers().forEach(other -> {
            if (other.equals(player)) return;
            if (!other.canSee(player)) return;
            other.hidePlayer(lime._plugin, player);
            other.showPlayer(lime._plugin, player);
        });

        player.leaveVehicle();

        EntityPlayer ep = ((CraftPlayer) player).getHandle();

        ClientboundPlayerInfoRemovePacket removeInfo = new ClientboundPlayerInfoRemovePacket(Collections.singletonList(ep.getUUID()));
        ClientboundPlayerInfoUpdatePacket addInfo = new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.a.ADD_PLAYER, ep);
        
        Location loc = player.getLocation().clone();
        ep.connection.send(removeInfo);
        ep.connection.send(addInfo);
        player.teleport(loc);
        lime.nextTick(() -> {
            player.teleport(loc);
            WorldServer worldserver = ep.getLevel();
            worldserver.getServer().getPlayerList().respawn(ep, worldserver, true, loc, true, PlayerRespawnEvent.RespawnReason.PLUGIN);
            lime.nextTick(() -> player.teleport(loc));
        });
    }
    public static system.Toast2<String, String> getSkinData(Player player) {
        if (player == null) return null;
        EntityPlayer ep = ((CraftPlayer)player).getHandle();
        if (ep == null) return null;
        com.mojang.authlib.properties.Property textures = of(ep.getGameProfile().getProperties(), "textures");
        if (textures == null) return null;
        return system.toast(textures.getValue(), textures.getSignature());
    }
    public static String getSkinURL(Player player) {
        if (player == null) return null;
        EntityPlayer ep = ((CraftPlayer)player).getHandle();
        if (ep == null) return null;
        com.mojang.authlib.properties.Property textures = of(ep.getGameProfile().getProperties(), "textures");
        if (textures == null) return null;
        try {
            return system.json.getter(system.json.parse(new String(Base64.getDecoder().decode(textures.getValue()))))
                    .of("textures")
                    .of("SKIN")
                    .of("url")
                    .other(JsonElement::getAsString, null);
        } catch (Exception ignored) {
            return null;
        }
    }
    public static void setProfile(GameProfile profile, String value, String signature, boolean save) {
        PropertyMap properties = profile.getProperties();
        com.mojang.authlib.properties.Property old_textures = of(properties, "old_textures");
        if (old_textures == null) old_textures = of(properties, "textures");
        properties.clear();
        properties.put("textures", new com.mojang.authlib.properties.Property("textures", value, signature));
        if (old_textures == null || !save) return;
        properties.put("old_textures", new com.mojang.authlib.properties.Property("old_textures", old_textures.getValue(), old_textures.getSignature()));
    }
    private static void resetProfile(GameProfile profile) {
        PropertyMap properties = profile.getProperties();
        com.mojang.authlib.properties.Property property = of(properties, "old_textures");
        if (property == null) return;
        setProfile(profile, property.getValue(), property.getSignature(), false);
    }
    public static void setSkin(Player player, int skin_id) {
        EntityPlayer ep = ((CraftPlayer)player).getHandle();
        UUID uuid = player.getUniqueId();
        if (skin_id == 0) {
            resetProfile(ep.getGameProfile());
            updateSkin(player);
            return;
        }
        Methods.SQL.Async.rawSqlOnce("SELECT skins.value, skins.signature FROM skins WHERE skins.`uuid` = '"+uuid+"' AND skins.id = "+skin_id, set -> system.toast(MySql.readObject(set, "value", String.class), MySql.readObject(set, "signature", String.class)), obj -> {
            if (obj == null) return;
            String value = obj.val0;
            String signature = obj.val1;
            setProfile(ep.getGameProfile(), value, signature, true);
            updateSkin(player);
        });
    }

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
    private static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }
    private static String getMD5(String text) {
        MessageDigest md;
        try { md = MessageDigest.getInstance("MD5"); } catch (NoSuchAlgorithmException e) { throw new IllegalArgumentException(e); }
        md.update(text.getBytes());
        return bytesToHex(md.digest());
    }

    public static GameProfile setSkinOrDownload(GameProfile profile, String url) {
        if (!skins.containsKey(getMD5(url))) addSkins(Collections.singletonList(url));
        return setSkin(profile, url);
    }
    public static GameProfile setSkin(GameProfile profile, String url) {
        Property skin = skins.getOrDefault(getMD5(url), null);
        if (skin == null) return profile;
        PropertyMap properties = profile.getProperties();
        properties.clear();
        properties.put("textures", new com.mojang.authlib.properties.Property("textures", skin.value, skin.signature));
        return profile;
    }
    private final static HashMap<String, Property> skins = new HashMap<>();
    public static void addSkins(List<String> urls) {
        addSkins(urls, null);
    }
    public static void addSkins(List<String> urls, system.Action0 callback) {
        File dir = lime.getConfigFile("skins/");
        if (!dir.exists()) dir.mkdir();
        urls.forEach(url -> {
            String md5 = getMD5(url);
            String path = "skins/" + md5;
            if (lime.existConfig(path)) skins.put(md5, new Property(system.json.parse(lime.readAllConfig(path)).getAsJsonObject()));
            else
            {
                Property prop = genSkin(url);
                lime.writeAllConfig(path, prop.ToJson().toString());
                skins.put(md5, prop);
            }
        });
        if (callback != null) callback.invoke();
    }
    public static void setSkin(PlayerProfile profile, String url) {
        Property skin = skins.getOrDefault(getMD5(url), null);
        if (skin == null) return;
        Set<ProfileProperty> properties = profile.getProperties();
        properties.clear();
        profile.setProperty(new ProfileProperty("textures", skin.value, skin.signature));
    }
    private static class Property {
        private String value;
        private String signature;

        public Property(JsonObject json) { this(json.get("value").getAsString(), json.get("signature").getAsString()); }
        public Property(skin.uploaded uploaded) {
            this(uploaded.value, uploaded.signature);
        }
        public Property(String value, String signature) {
            this.value = value;
            this.signature = signature;
        }
        public JsonObject ToJson() {
            JsonObject json = new JsonObject();
            json.addProperty("value", value);
            json.addProperty("signature", signature);
            return json;
        }
    }
    private static Property genSkin(String url) {
        return new Property(org.lime.skin.upload(url));
    }
}




