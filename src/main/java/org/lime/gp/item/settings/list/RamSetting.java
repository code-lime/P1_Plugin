package org.lime.gp.item.settings.list;

import com.google.gson.JsonObject;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.gp.item.settings.use.ITimeUse;
import org.lime.gp.item.settings.use.target.BlockTarget;
import org.lime.gp.item.settings.use.target.ITarget;
import org.lime.gp.sound.Sounds;
import org.lime.system.utils.RandomUtils;

import java.util.Optional;

@Setting(name = "ram") public class RamSetting extends ItemSetting<JsonObject> implements ITimeUse<BlockTarget> {
    public final int time;
    public final Boolean shift;
    public final EquipmentSlot arm;
    public final String sound;

    public final String prefix;

    public final double chance;
    private final boolean silent;

    public RamSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        this.arm = EquipmentSlot.valueOf(json.get("arm").getAsString());
        this.time = json.has("time") ? json.get("time").getAsInt() : 0;
        this.shift = json.get("shift").isJsonNull() ? null : json.get("shift").getAsBoolean();
        this.prefix = json.has("prefix") ? json.get("prefix").getAsString() : "";
        sound = json.has("sound") ? json.get("sound").getAsString() : null;
        this.chance = json.get("chance").getAsDouble();
        this.silent = !json.has("silent") || json.get("silent").getAsBoolean();
    }
    @Override public boolean silent() { return silent; }
    @Override public EquipmentSlot arm() { return arm; }

    @Override public int getTime() { return time; }
    @Override public int getCooldown() { return 0; }
    @Override public String prefix(boolean self) { return prefix; }
    @Override public String cooldownPrefix() { return ""; }

    @Override public Optional<BlockTarget> tryCast(Player player, ITarget target, EquipmentSlot arm, boolean shift) {
        return target.castToBlock().filter(v -> {
            BlockState state = v.getState();
            return state instanceof TrapDoor || state instanceof Door || state instanceof Gate;
        });
    }
    @Override public boolean timeUse(Player player, BlockTarget target, ItemStack item) {
        Block block = target.getLocation().getBlock();
        BlockState state = block.getState();
        if (!(state instanceof TrapDoor || state instanceof Door || state instanceof Gate)) return false;
        Sounds.playSound(sound, player.getLocation());
        if (RandomUtils.rand_is(chance))
            block.breakNaturally();
        return true;
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("arm"), IJElement.link(docs.handType()), IComment.text("Тип руки, в которой происходит взаимодействие")),
                JProperty.optional(IName.raw("time"), IJElement.raw(10), IComment.text("Время использования предмета в тиках")),
                JProperty.optional(IName.raw("sound"), IJElement.link(docs.sound()), IComment.text("Звук при взаимодействии")),
                JProperty.optional(IName.raw("silent"), IJElement.bool(), IComment.text("Будет ли проигран звук ломания предмета")),
                JProperty.optional(IName.raw("shift"), IJElement.bool(), IComment.empty()
                        .append(IComment.text("Требуется ли нажимать "))
                        .append(IComment.raw("SHIFT"))
                        .append(IComment.text(" для использования. Если не указано - проверка на нажатие отсуствует"))),
                JProperty.optional(IName.raw("prefix"), IJElement.raw("PREFIX TEXT"), IComment.text("Отображаемый префикс таймера использования")),
                JProperty.require(IName.raw("chance"), IJElement.raw(1.0), IComment.empty()
                        .append(IComment.text("Шанс срабатывания ломания блока (от "))
                        .append(IComment.raw(0.0))
                        .append(IComment.text(" до "))
                        .append(IComment.raw(1.0))
                        .append(IComment.text(")")))
        ), IComment.text("Предмет, который выламывает любую дверь (и железную), любой люк (и железный), а также любую калитку с определенным шансом"));
    }
}