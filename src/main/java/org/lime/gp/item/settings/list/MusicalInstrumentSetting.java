package org.lime.gp.item.settings.list;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.use.IUse;
import org.lime.gp.item.settings.use.target.ITarget;
import org.lime.gp.item.settings.use.target.SelfTarget;
import org.lime.system.execute.*;
import org.lime.gp.chat.Apply;
import org.lime.gp.extension.Cooldown;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;
import org.lime.gp.player.menu.MenuCreator;
import org.lime.gp.sound.Sounds;

import com.google.gson.JsonObject;

import net.kyori.adventure.sound.Sound;

@Setting(name = "musical_instrument") public class MusicalInstrumentSetting extends ItemSetting<JsonObject> implements IUse<SelfTarget> {
    public final Action2<Location, Float> pitch;
    public final EquipmentSlot arm;
    public final double cooldown;
    public final String menu;
    public final HashMap<String, String> args = new HashMap<>();
    public MusicalInstrumentSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        this.pitch = json.has("note") ? new Action2<>() {
            final org.bukkit.Sound sound = org.bukkit.Sound.valueOf("BLOCK_NOTE_BLOCK_" + json.get("note").getAsString());
            @Override public void invoke(Location location, Float pitch) {
                location.getWorld().playSound(Sound.sound(sound, Sound.Source.PLAYER, 1.0f, pitch), location.getX(), location.getY(), location.getZ());
            }
        } : new Action2<>() {
            final String sound = json.get("sound").getAsString();
            @Override public void invoke(Location location, Float pitch) {
                Sounds.playSound(sound, location);
            }
        };
        this.cooldown = json.has("cooldown") ? json.get("cooldown").getAsDouble() : 0;
        this.menu = json.has("menu") ? json.get("menu").getAsString() : null;
        this.arm = EquipmentSlot.valueOf(json.get("arm").getAsString());
        if (json.has("args")) json.get("args").getAsJsonObject().entrySet().forEach(kv -> args.put(kv.getKey(), kv.getValue().getAsString()));
    }

    @Override public Optional<SelfTarget> tryCast(Player player, ITarget target, EquipmentSlot arm, boolean shift) {
        return Optional.of(SelfTarget.Instance);
    }
    @Override public boolean use(Player player, SelfTarget target, EquipmentSlot arm, boolean shift) {
        Location location = player.getLocation();
        if (cooldown > 0 && Cooldown.hasOrSetCooldown(player.getUniqueId(), creator().getKey(), cooldown)) return false;
        float pitch = (Math.min(30, Math.max(-20, player.getEyeLocation().getPitch())) + 20) / 50;
        this.pitch.invoke(location, pitch);
        if (this.menu != null) MenuCreator.show(player, this.menu, Apply.of().add(args));
        return false;
    }

    @Override public EquipmentSlot arm() { return arm; }


    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        JObject base = JObject.of(
                JProperty.require(IName.raw("arm"), IJElement.link(docs.handType()), IComment.text("Тип руки, в которой происходит взаимодействие")),
                JProperty.optional(IName.raw("cooldown"), IJElement.raw(10), IComment.text("Время задержки повторного использования в секундах")),
                JProperty.optional(IName.raw("menu"), IJElement.raw(10), IComment.text("Открываемая меню при взаимодействии")),
                JProperty.optional(IName.raw("args"),
                        IJElement.anyObject(JProperty.require(IName.raw("KEY"), IJElement.raw("VALUE"))),
                        IComment.empty()
                                .append(IComment.raw("args"))
                                .append(IComment.text(" передаваемые в "))
                                .append(IComment.field("menu")))
        );
        IIndexGroup note_type = JsonEnumInfo.of("NOTE_TYPE", "note_type", Arrays.stream(org.bukkit.Sound.values())
                .map(Enum::name)
                .filter(v -> v.startsWith("BLOCK_NOTE_BLOCK_"))
                .map(v -> IJElement.raw(v.substring(17)))
                .collect(ImmutableList.toImmutableList())
        );

        return JsonGroup.of(index, index, base.add(
                JProperty.require(IName.raw("note"), IJElement.link(note_type), IComment.text("Звук нотного блока"))
        ).or(base.add(
                JProperty.require(IName.raw("sound"), IJElement.link(docs.sound()), IComment.text("Пользовательский звук"))
        )), IComment.text("Предмет при использовании воспроизводит звук")).withChild(note_type);
    }
}