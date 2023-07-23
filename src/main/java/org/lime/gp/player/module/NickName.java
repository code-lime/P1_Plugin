package org.lime.gp.player.module;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lime.core;
import org.lime.gp.chat.ChatMessages;
import org.lime.gp.extension.Cooldown;
import org.lime.gp.module.DrawText;
import org.lime.gp.module.EntityPosition;
import org.lime.gp.player.voice.Voice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class NickName {
    public static core.element create() {
        return core.element.create(NickName.class)
                .withInit(NickName::init);
    }
    public static void init() {
        DrawText.load(NickName::shows);
    }

    public static boolean isLookingAt(Player target, Player owner) {
        if (owner.getEntityId() == target.getEntityId()) return false;
        if (!owner.canSee(target)) return false;
        if ((target.getGameMode() == GameMode.SPECTATOR || target.isInvisible()) && owner.getGameMode() != GameMode.SPECTATOR) return false;
        Location eye = owner.getEyeLocation();
        Vector toEntity = target.getEyeLocation().toVector().clone().midpoint(target.getLocation().toVector()).add(new Vector(0, 0.5, 0)).subtract(eye.toVector());
        double dot = toEntity.normalize().dot(eye.getDirection());
        return dot > 0.97D;
    }

    public static class ShowNickName implements DrawText.IShow {
        @Override public String getID() { return target.getUniqueId() + ".NickName"; }
        @Override public boolean filter(Player owner) {
            UUID uuid = owner.getUniqueId();
            boolean isLook = isLookingAt(target, owner);
            if (isLook) Cooldown.setCooldown(new UUID[] { target.getUniqueId(), uuid }, "show.nick.time", 1.0);
            else if (!Cooldown.hasCooldown(new UUID[] { target.getUniqueId(), uuid }, "show.nick.time")) return false;
            return true;
        }
        @Override public Component text(Player player) {
            List<Component> lines = TabManager.BufferData.of(player.getUniqueId(), target.getUniqueId()).nick;
            return Component.join(JoinConfiguration.newlines(), lines);
        }
        @Override public Optional<Integer> parent() { return Optional.of(target.getEntityId()); }
        @Override public Location location() { return location; }
        @Override public double distance() { return 5; }
        @Override public boolean tryRemove() { return false; }

        public final Player target;

        public final Location location;

        public ShowNickName(Player player) {
            this.target = player;
            this.location = player.getLocation();
        }
    }
    public static class ShowChat implements DrawText.IShow {
        @Override public String getID() { return target.getUniqueId() + ".Chat"; }
        @Override public boolean filter(Player owner) {
            if (target.getGameMode() == GameMode.SPECTATOR && owner.getGameMode() != GameMode.SPECTATOR) return false;
            return owner.getEntityId() != target.getEntityId();
        }
        @Override public Component text(Player player) { return chat; }
        @Override public Optional<Integer> parent() { return Optional.of(target.getEntityId()); }
        @Override public Location location() { return location; }
        @Override public double distance() { return 25; }
        @Override public boolean tryRemove() { return false; }

        public final Player target;
        public final Location location;

        public final Component chat;

        public ShowChat(Player player, List<Component> chatLines) {
            this.chat = Component.join(JoinConfiguration.newlines(), chatLines);
            this.target = player;
            this.location = player.getLocation();
        }
    }
    public static class ShowVoice implements DrawText.IShow {
        @Override public String getID() { return target.getUniqueId() + ".Voice"; }
        @Override public boolean filter(Player owner) {
            if (!owner.canSee(target) || (target.getGameMode() == GameMode.SPECTATOR && owner.getGameMode() != GameMode.SPECTATOR)) return false;
            return owner.getEntityId() != target.getEntityId();
        }
        @Override public Component text(Player player) { return text; }
        @Override public Optional<Integer> parent() { return Optional.of(target.getEntityId()); }
        @Override public Location location() { return location; }
        @Override public double distance() { return enable ? 10 : 4; }
        @Override public boolean tryRemove() { return false; }

        public final Player target;
        public final Location location;
        public final boolean enable;
        public final Component text;

        public ShowVoice(Player player, boolean enable) {
            this.target = player;
            this.location = player.getLocation();
            this.enable = enable;
            this.text = enable
                    ? Component.text("\uE738")
                    : Component.text("✖").color(TextColor.color(0xFF0000));
        }
    }

    public static Stream<DrawText.IShow> of(Player player) {
        return Stream.concat(switch (Voice.voiceStatus(player.getUniqueId())) {
                    case ACTIVE -> Stream.of(new ShowVoice(player, true));
                    case OFFLINE -> Stream.of(new ShowVoice(player, false));
                    default -> Stream.<ShowVoice>empty();
                },
                Stream.of(ChatMessages.chatText(player.getUniqueId())
                        .<DrawText.IShow>map(lines -> new ShowChat(player, lines))
                        .orElseGet(() -> new ShowNickName(player)))
        );
    }

    public static Stream<DrawText.IShow> shows() {
        return EntityPosition.onlinePlayers
                .values()
                .stream()
                .flatMap(NickName::of);
    }
}
