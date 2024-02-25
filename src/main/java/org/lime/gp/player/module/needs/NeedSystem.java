package org.lime.gp.player.module.needs;

import com.google.common.collect.Streams;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.lime.gp.item.settings.list.ArmorNeedSetting;
import org.lime.gp.player.module.DeathGame;
import org.lime.plugin.CoreElement;
import org.lime.gp.lime;
import org.lime.gp.player.module.needs.food.ProxyFoodMetaData;
import org.lime.gp.player.module.needs.thirst.Thirst;
import org.lime.system.execute.Func1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class NeedSystem {
    public static CoreElement create() {
        return CoreElement.create(NeedSystem.class)
                .withInit(NeedSystem::init);
    }

    private static void init() {
        lime.repeatTicks(NeedSystem::tick, 1);
    }
    private static void tick() {
        Bukkit.getOnlinePlayers()
                .forEach(player -> getPlayerNeeds(INeedEffect.Type.EFFECT, player)
                        .forEach(effect -> effect.tick(player)));
    }
    public static void register(Func1<Player, Stream<INeedEffect<?>>> needs) {
        NeedSystem.needsComponents.add(needs);
    }
    private static final List<Func1<Player, Stream<INeedEffect<?>>>> needsComponents = new ArrayList<>();

    private static final HashMap<Player, List<INeedEffect<?>>> cacheEffects = new HashMap<>();
    private static List<INeedEffect<?>> readPlayerNeeds(Player player) {
        return needsComponents.stream().flatMap(v -> v.invoke(player)).toList();
    }
    public static Stream<INeedEffect<?>> getPlayerNeeds(Player player) {
        if (MinecraftServer.currentTick % 40 == 0) cacheEffects.clear();
        return cacheEffects.computeIfAbsent(player, NeedSystem::readPlayerNeeds).stream();
    }
    public static <T extends INeedEffect<T>> Stream<T> getPlayerNeeds(INeedEffect.Type<T> type, Player player) {
        return getPlayerNeeds(player).map(type::nullCast).filter(Objects::nonNull);
    }
    public static double getPlayerMutate(INeedEffect.Type<INeedEffect.Mutate> type, Player player) {
        return getPlayerNeeds(type, player)
                .mapToDouble(INeedEffect.Mutate::value)
                .reduce(1, (a,b) -> a*b);
    }

    public static double getSleepMutate(Player player) { return getPlayerMutate(INeedEffect.Type.SLEEP, player); }
    public static double getThirstMutate(Player player) { return getPlayerMutate(INeedEffect.Type.THIRST, player); }
    public static double getFoodMutate(Player player) { return getPlayerMutate(INeedEffect.Type.FOOD, player); }
}







