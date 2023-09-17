package org.lime.gp.module.npc.eplayer;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.EnumItemSlot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.item.Items;
import org.lime.gp.lime;
import org.lime.gp.module.npc.EPlayerModule;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.player.module.Advancements;
import org.lime.gp.player.module.Skins;
import org.lime.system.utils.MathUtils;

import java.util.*;

public class ConfigEPlayer implements IEPlayer {
    private final String key;

    private final Location location;
    private final String skin;
    private final String menu;
    private final Map<String, String> args = new HashMap<>();
    private final Boolean single;
    private final List<Component> name = new ArrayList<>();
    private final boolean hide;
    private final boolean sit;
    private final String shift_menu;
    private final HashMap<EnumItemSlot, ItemStack> equipment = new HashMap<>();

    private final List<Component> advancementName = new ArrayList<>();
    private final List<String> advancements = new ArrayList<>();

    public ConfigEPlayer(String key, JsonObject json) {
        this.key = key;

        location = MathUtils.getLocation(json.has("world") ? Bukkit.getWorlds().get(json.get("world").getAsInt()) : lime.MainWorld, json.get("location").getAsString());
        skin = json.get("skin").getAsString();
        menu = json.has("menu") ? json.get("menu").getAsString() : null;
        if (json.has("single")) {
            if (json.get("single").isJsonNull()) single = null;
            else single = json.get("single").getAsBoolean();
        } else single = true;
        hide = json.has("hide") && json.get("hide").getAsBoolean();
        sit = json.has("sit") && json.get("sit").getAsBoolean();
        if (json.has("name")) {
            if (json.get("name").isJsonArray())
                json.get("name").getAsJsonArray().forEach(_name -> name.add(ChatHelper.formatComponent(_name.getAsString())));
            else name.add(ChatHelper.formatComponent(json.get("name").getAsString()));
            Collections.reverse(name);
        }
        if (json.has("advancement_name")) {
            if (json.get("advancement_name").isJsonArray())
                json.get("advancement_name").getAsJsonArray().forEach(_name -> advancementName.add(ChatHelper.formatComponent(_name.getAsString())));
            else advancementName.add(ChatHelper.formatComponent(json.get("advancement_name").getAsString()));
            Collections.reverse(advancementName);
        }
        if (json.has("advancements")) {
            if (json.get("advancements").isJsonArray())
                json.get("advancements").getAsJsonArray().forEach(_name -> advancements.add(_name.getAsString()));
            else advancements.add(json.get("advancements").getAsString());
        }
        shift_menu = json.has("shift_menu") ? json.get("shift_menu").getAsString() : null;
        if (json.has("args"))
            json.getAsJsonObject("args").entrySet().forEach(kv -> this.args.put(kv.getKey(), kv.getValue().getAsString()));
        if (json.has("equipment"))
            json.get("equipment").getAsJsonObject().entrySet().forEach(kv -> equipment.put(EnumItemSlot.byName(kv.getKey()), Items.createItem(kv.getValue().getAsString()).orElseThrow()));
    }

    @Override public String key() { return key; }
    @Override public Location location() { return location; }
    @Override public Boolean single() { return single; }
    @Override public Pose pose() { return sit ? Pose.SIT : Pose.NONE; }

    @Override public boolean isShow(UUID uuid) { return !hide || EPlayerModule.shows(uuid).contains(key); }
    @Override public GameProfile setSkin(GameProfile profile) { return Skins.setSkinOrDownload(profile, skin); }

    @Override public List<Component> getDisplayName(Player player) {
        List<Component> nick = new ArrayList<>(name);
        for (String advancement : advancements)
            if (Advancements.advancementState(player, advancement) == Advancements.AdvancementState.Parent) {
                nick.addAll(advancementName);
                break;
            }
        return nick;
    }
    @Override public void click(Player player, boolean isShift) {
        String menu = this.menu;
        if (isShift) menu = this.shift_menu == null ? menu : this.shift_menu;
        MenuCreator.show(player, menu, Apply.of().add(this.args));
    }
    @Override public Map<EnumItemSlot, net.minecraft.world.item.ItemStack> createEquipment() {
        Map<EnumItemSlot, net.minecraft.world.item.ItemStack> equipment = new HashMap<>();
        this.equipment.forEach((k, v) -> equipment.put(k, CraftItemStack.asNMSCopy(v)));
        return equipment;
    }
}
