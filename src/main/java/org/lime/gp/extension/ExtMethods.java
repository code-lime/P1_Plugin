package org.lime.gp.extension;

import net.minecraft.network.protocol.game.PacketPlayOutOpenBook;
import net.minecraft.world.EnumHand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.lime.display.Displays;
import org.lime.gp.lime;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class ExtMethods {
    public static Optional<LivingEntity> damagerEntity(EntityDamageEvent e) {
        return e instanceof EntityDamageByEntityEvent _e
                ? damagerEntity(_e.getDamager())
                : Optional.empty();
    }
    public static Optional<LivingEntity> damagerEntity(Entity damager) {
        if (damager == null) return Optional.empty();

        Set<String> tags = damager.getScoreboardTags();
        for (String tag : tags)
            if (tag.startsWith("owner:"))
                if (Bukkit.getEntity(UUID.fromString(tag.substring(6))) instanceof LivingEntity livingEntity)
                    return Optional.of(livingEntity);

        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (!(source instanceof Entity)) return Optional.empty();
            damager = (Entity)source;
        }
        return damager instanceof LivingEntity livingEntity ? Optional.of(livingEntity) : Optional.empty();
    }
    public static Optional<Player> damagerPlayer(EntityDamageEvent e) {
        return damagerEntity(e).map(v -> v instanceof Player player ? player : null);
    }
    public static Optional<Player> damagerPlayer(Entity damager) {
        return damagerEntity(damager).map(v -> v instanceof Player player ? player : null);
    }
    public static void executeCommand(String cmd) {
        Bukkit.getCommandMap().dispatch(Bukkit.getConsoleSender(), cmd);
    }
    public static void openBook(final ItemStack book, final Player player) {
        final int slot = player.getInventory().getHeldItemSlot();
        final ItemStack old = player.getInventory().getItem(slot);
        player.getInventory().setItem(slot, book);
        PacketManager.sendPacket(player, new PacketPlayOutOpenBook(EnumHand.MAIN_HAND));
        player.getInventory().setItem(slot, old);
    }
    public static boolean isPlayerLoaded(Player player) {
        return Displays.isPlayerLoaded(player);
    }
    public static Optional<Integer> parseInt(String text) {
        try { return Optional.of(Integer.parseInt(text)); }
        catch (Exception e) { return Optional.empty(); }
    }
    public static Optional<Long> parseLong(String text) {
        try { return Optional.of(Long.parseLong(text)); }
        catch (Exception e) { return Optional.empty(); }
    }
    public static Optional<UUID> parseUUID(String text) {
        try { return Optional.of(UUID.fromString(text)); }
        catch (Exception e) { return Optional.empty(); }
    }
    public static Optional<Double> parseDouble(String text) {
        try { return Optional.of(Double.parseDouble(text)); }
        catch (Exception e) { return Optional.empty(); }
    }
    public static Optional<Integer> parseUnsignedInt(String text) {
        try { return Optional.of(Integer.parseUnsignedInt(text)); }
        catch (Exception e) { return Optional.empty(); }
    }
    public static Optional<Integer> parseUnsignedInt(String text, int radix) {
        try { return Optional.of(Integer.parseUnsignedInt(text, radix)); }
        catch (Exception e) { return Optional.empty(); }
    }

    public static <T> Predicate<T> filterLogExecute(Action1<String> execute, Func1<T, String> action) {
        return t -> {
            execute.invoke(action.invoke(t));
            return true;
        };
    }
    public static <T> Predicate<T> filterLogExecute(Action1<String> execute, String text) {
        return filterLogExecute(execute, v -> text.replace("{0}", String.valueOf(v)));
    }

    public static <T> Predicate<T> filterLog(Func1<T, String> action) { return filterLogExecute(lime::logOP, action); }
    public static <T> Predicate<T> filterLog(String text) { return filterLogExecute(lime::logOP, text); }
}




























