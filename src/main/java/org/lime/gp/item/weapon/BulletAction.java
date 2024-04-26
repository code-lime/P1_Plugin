package org.lime.gp.item.weapon;

import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.lime.docs.json.IComment;
import org.lime.docs.json.IEnumDocs;
import org.lime.gp.item.elemental.DataContext;
import org.lime.gp.item.elemental.Elemental;
import org.lime.gp.item.settings.use.target.EntityTarget;
import org.lime.gp.item.settings.use.target.ITarget;
import org.lime.gp.item.settings.use.target.PlayerTarget;
import org.lime.gp.player.module.Knock;
import org.lime.json.JsonElementOptional;
import org.lime.system.execute.Action1;
import org.lime.system.execute.Action2;
import org.lime.system.utils.RandomUtils;

public enum BulletAction implements IEnumDocs {
    NONE(IComment.text("Без дополнительного взаимодействия")),
    TASER((value, target) -> target.castToEntity().map(EntityTarget::getEntity).ifPresent(entity -> {
        entity.getPersistentDataContainer()
                .set(Bullets.TASER_TICKS, PersistentDataType.INTEGER, RandomUtils.rand(5 * 20, 10 * 20));
        if (entity instanceof Player player && RandomUtils.rand_is(value.getAsDouble().orElse(0.4)))
            Knock.knock(player);
    }), IComment.join(
            IComment.text("Вызывает эффект мерцания экрана на время в диапазоне "),
            IComment.field("[5;10]"),
            IComment.text(" секунд и с шансом в "),
            IComment.field("bullet_data"),
            IComment.text(" (Если не указано: 40%)"),
            IComment.text(" вызывает посадку игрока")
    )),
    TRAUMATIC((value, target) -> target.castToEntity().map(EntityTarget::getEntity).ifPresent(entity -> {
        if (entity instanceof LivingEntity living)
            living.addPotionEffect(Bullets.TRAUMATIC_FREEZE);
        if (entity instanceof Player player && RandomUtils.rand_is(value.getAsDouble().orElse(0.12)))
            Knock.knock(player);
    }), IComment.join(
            IComment.text("С шансом в "),
            IComment.field("bullet_data"),
            IComment.text(" (Если не указано: 12%)"),
            IComment.text(" вызывает посадку игрока")
    )),
    FLAME((value, target) -> target.castToEntity().map(EntityTarget::getEntity).ifPresent(entity -> {
        if (entity instanceof CraftEntity e)
            e.getHandle().setSecondsOnFire(value.getAsInt().orElse(2));
    }), IComment.join(
            IComment.text("На "),
            IComment.field("bullet_data"),
            IComment.text(" (Если не указано: 2)"),
            IComment.text(" секунды вызывает горение")
    )),
    ELEMENTAL((value, target) -> target.castToPlayer().map(PlayerTarget::getPlayer).ifPresent(player -> value.getAsString().ifPresent(v -> Elemental.execute(player, new DataContext(), v))), IComment.join(
            IComment.text("Вызывает элементаль "),
            IComment.field("bullet_data")
    ));

    private final IComment comment;
    private final Action2<JsonElementOptional, ITarget> action;

    BulletAction(IComment comment) {
        this((a, b) -> {}, comment);
    }
    BulletAction(Action1<ITarget> action, IComment comment) {
        this((a, b) -> action.invoke(b), comment);
    }
    BulletAction(Action2<JsonElementOptional, ITarget> action, IComment comment) {
        this.comment = comment;
        this.action = action;
    }

    public void execute(JsonElementOptional data, ITarget target) { action.invoke(data, target); }

    @Override public IComment docsComment() { return comment; }
}
