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
import org.lime.gp.lime;
import org.lime.system;

import java.util.Optional;
import java.util.function.Predicate;

public class ExtMethods {
    public static Optional<LivingEntity> damagerEntity(EntityDamageEvent e) {
        if (!(e instanceof EntityDamageByEntityEvent _e)) return Optional.empty();
        Entity damager = _e.getDamager();
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
        return player.getResourcePackStatus() != PlayerResourcePackStatusEvent.Status.ACCEPTED;
    }
    public static Optional<Integer> parseInt(String text) {
        try { return Optional.of(Integer.parseInt(text)); }
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

    public static <T> Predicate<T> filterLog(system.Func1<T, String> action) {
        return t -> {
            lime.logOP(action.invoke(t));
            return true;
        };
    }
    public static <T> Predicate<T> filterLog(String text) {
        return filterLog(v -> text.replace("{0}", v + ""));
    }
}




























