package org.lime.gp.item.settings.list;

import com.google.gson.JsonObject;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.chat.Apply;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.use.ITimeUse;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.gp.item.settings.use.target.ITarget;
import org.lime.gp.item.settings.use.target.SelfTarget;
import org.lime.gp.lime;
import org.lime.gp.sound.Sounds;
import org.lime.system.utils.ItemUtils;

import java.util.Optional;

@Setting(name = "zip") public class ZipSetting extends ItemSetting<JsonObject> implements ITimeUse<SelfTarget> {
    private static final NamespacedKey KEY_ZIP_ITEM = new NamespacedKey(lime._plugin, "zip.item");

    public final int time;
    public final Boolean shift;
    public final EquipmentSlot arm;
    public final String sound;

    public final String prefix;
    private final boolean silent;

    public ZipSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        this.arm = EquipmentSlot.valueOf(json.get("arm").getAsString());
        this.time = json.has("time") ? json.get("time").getAsInt() : 0;
        this.shift = json.get("shift").isJsonNull() ? null : json.get("shift").getAsBoolean();
        this.sound = json.has("sound") ? json.get("sound").getAsString() : null;
        this.prefix = json.has("prefix") ? json.get("prefix").getAsString() : "";
        this.silent = !json.has("silent") || json.get("silent").getAsBoolean();
    }
    @Override public boolean silent() { return silent; }

    @Override public EquipmentSlot arm() { return arm; }


    @Override public int getTime() { return time; }
    @Override public int getCooldown() { return 0; }
    @Override public String prefix(boolean self) { return prefix; }
    @Override public String cooldownPrefix() { return ""; }

    @Override public Optional<SelfTarget> tryCast(Player player, ITarget target, EquipmentSlot arm, boolean shift) {
        return this.shift != null && this.shift != shift
                ? Optional.empty()
                : Optional.of(SelfTarget.Instance);
    }
    @Override public boolean timeUse(Player player, SelfTarget target, ItemStack item) {
        Sounds.playSound(sound, player.getLocation());
        unzip(item)
                .ifPresentOrElse(
                        present -> Items.dropGiveItem(player, present, false),
                        () -> lime.logOP("WARNING! In item '"+ItemUtils.saveItem(item)+"' empty ZIP content!"));
        return true;
    }
    @Override public void apply(ItemMeta meta, Apply apply) {
        apply.get("zip.item")
                .ifPresent(item -> meta.getPersistentDataContainer().set(KEY_ZIP_ITEM, PersistentDataType.STRING, item));
    }
    public static Optional<ItemStack> unzip(ItemStack item) {
        String rawItem = item.getItemMeta().getPersistentDataContainer().get(KEY_ZIP_ITEM, PersistentDataType.STRING);
        return rawItem == null
                ? Optional.empty()
                : Optional.ofNullable(ItemUtils.loadItem(rawItem));
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("arm"), IJElement.link(docs.handType()), IComment.text("Тип руки, в которой происходит взаимодействие")),
                JProperty.optional(IName.raw("time"), IJElement.raw(10), IComment.text("Время использования предмета в тиках")),
                JProperty.optional(IName.raw("silent"), IJElement.bool(), IComment.text("Будет ли проигран звук ломания предмета")),
                JProperty.optional(IName.raw("shift"), IJElement.bool(), IComment.empty()
                        .append(IComment.text("Требуется ли нажимать "))
                        .append(IComment.raw("SHIFT"))
                        .append(IComment.text(" для использования. Если не указано - проверка на нажатие отсуствует"))),
                JProperty.optional(IName.raw("prefix"), IJElement.raw("PREFIX TEXT"), IComment.text("Отображаемый префикс таймера использования"))
        ), IComment.text("Предмет, который распаковывает предмет. После использования возможен вызов ").append(IComment.link(docs.settingsLink(NextSetting.class))));
    }
}
