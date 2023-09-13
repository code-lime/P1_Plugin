package org.lime.gp.craft.book;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.network.protocol.game.PacketPlayInAutoRecipe;
import net.minecraft.network.protocol.game.PacketPlayOutAutoRecipe;
import net.minecraft.network.protocol.game.PacketPlayOutRecipeUpdate;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.stats.RecipeBookServer;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerFurnace;
import net.minecraft.world.inventory.ContainerRecipeBook;
import net.minecraft.world.item.crafting.IRecipe;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftNamespacedKey;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.craft.recipe.AbstractRecipe;
import org.lime.gp.craft.recipe.IDisplayRecipe;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.lime;
import org.lime.gp.player.perm.Perms;
import org.lime.system;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class RecipePackets {
    public static CoreElement create() {
        return CoreElement.create(RecipePackets.class)
                .withInit(RecipePackets::init);
    }

    private static void init() {
        PacketManager.adapter()
                .add(PacketPlayOutRecipeUpdate.class, RecipePackets::onPacket)
                .add(PacketPlayInAutoRecipe.class, RecipePackets::onPacket)
                .listen();
    }


    private static final ImmutableList<net.minecraft.world.item.crafting.Recipes<?>> furnaceRecipeTypes = ImmutableList.of(
            net.minecraft.world.item.crafting.Recipes.SMELTING,
            net.minecraft.world.item.crafting.Recipes.BLASTING,
            net.minecraft.world.item.crafting.Recipes.SMOKING
    );

    public static boolean isSync(Container container) {
        return container instanceof IRecipesBookContainer || container instanceof ContainerFurnace;
    }

    public static Stream<? extends IRecipe<?>> getActiveRecipes(EntityPlayer player) {
        Perms.ICanData data = Perms.getCanData(player.getUUID());

        Container container = player.containerMenu;
        
        if (container instanceof IRecipesBookContainer recipeContainer)
            return recipeContainer.getRecipesCustom()
                    .stream()
                    .map(v -> v instanceof IRecipe<?> r ? r : null)
                    .filter(Objects::nonNull)
                    .filter(v -> data.isCanCraft(v.getId().getPath()));

        boolean isFurnace = container instanceof ContainerFurnace;

        Stream<IRecipe<?>> recipes = Recipes.CRAFTING_MANAGER.byName.values().stream().filter(isFurnace
                ? v -> furnaceRecipeTypes.contains(v.getType())
                : v -> !furnaceRecipeTypes.contains(v.getType())
        );
        return recipes.filter(v -> !v.isSpecial() && !(v instanceof AbstractRecipe) && (isFurnace || data.isCanCraft(v.getId().getPath())));
    }
    public static Stream<IRecipe<?>> getActiveDisplayRecipes(EntityPlayer player) {
        IRegistryCustom custom = player.level.registryAccess();
        return getActiveRecipes(player)
                .flatMap(v -> v instanceof IDisplayRecipe display
                        ? display.getDisplayRecipe(custom)
                        : Stream.of(v)
                );
    }

    private static <T>Iterable<T> iterable(Stream<T> stream) {
        return new Iterable<>() {
            @NotNull
            @Override public Iterator<T> iterator() { return stream.iterator(); }
            @Override public void forEach(Consumer<? super T> action) { stream.forEach(action); }
            @Override public Spliterator<T> spliterator() { return stream.spliterator(); }
        };
    }
    public static void sendUpdate(EntityPlayer player) {
        player.connection.send(new PacketPlayOutRecipeUpdate(Collections.emptyList()));
    }
    public static void syncRecipe(EntityPlayer player) {
        RecipeBookServer recipeBook = player.getRecipeBook();
        recipeBook.copyOverData(new net.minecraft.stats.RecipeBook());
        Set<MinecraftKey> known = recipeBook.known;
        getActiveDisplayRecipes(player).forEach(recipe -> known.add(recipe.getId()));
        sendUpdate(player);
        lime.once(() -> recipeBook.sendInitialRecipeBook(player), 0.2);
        /*
        public static void sendRecipes(EntityPlayer player, Perms.ICanData data) {
            List<IRecipe<?>> recipes = new ArrayList<>();

            IRegistryCustom custom = player.level.registryAccess();
            if (player.containerMenu instanceof IRecipesBookContainer container) {
                container.getRecipesCustom().forEach(recipe -> {
                    if (recipe instanceof IRecipe<?> _recipe && data.isCanCraft(_recipe.getId().getPath()))
                        recipe.getDisplayRecipe(custom).forEach(recipes::add);
                });
            } else {
                Recipes.CRAFTING_MANAGER.byName.forEach((key, recipe) -> {
                    if (recipe.isSpecial()) return;
                    if (!data.isCanCraft(key.getPath())) return;
                    if (recipe instanceof IDisplayRecipe abstractRecipe) {
                        if (recipe instanceof RecipeCrafting) {
                            abstractRecipe.getDisplayRecipe(custom).forEach(_recipe -> recipes.add(_recipe));
                        }
                    } else {
                        recipes.add(recipe);
                    }
                });
            }

            RecipeBookServer recipeBook = player.getRecipeBook();
            recipeBook.copyOverData(new net.minecraft.stats.RecipeBook());
            Set<MinecraftKey> known = recipeBook.known;
            recipes.forEach(recipe -> known.add(recipe.getId()));

            RecipePackets.sendUpdate(player);
            lime.once(() -> recipeBook.sendInitialRecipeBook(player), 0.2);
        }
        */
    }

    private static void onPacket(PacketPlayOutRecipeUpdate packet, PacketEvent event) {
        Optional.of(event.getPlayer())
            .map(v -> v instanceof CraftPlayer cp ? cp : null)
            .map(Entity::getUniqueId)
            .map(Recipes.PLAYER_LIST::getPlayer)
            .ifPresentOrElse(player -> {
                Collection<IRecipe<?>> recipes = getActiveDisplayRecipes(player)
                        .sorted(Comparator.comparing(v -> v.getId().toString()))
                        .toList();
                event.setPacket(new PacketContainer(event.getPacketType(), new PacketPlayOutRecipeUpdate(recipes)));
            }, () -> {
                if (event.isPlayerTemporary()) lime.logOP("[PPORU] PlayerTemporary: " + event.getPlayer());
                event.setPacket(new PacketContainer(event.getPacketType(), new PacketPlayOutRecipeUpdate(Collections.emptyList())));
            });
    }
    private static void onPacket(PacketPlayInAutoRecipe packet, PacketEvent e) {
        MinecraftKey generic_recipe_key = packet.getRecipe();
        String namespace = generic_recipe_key.getNamespace();
        int postfixPos = namespace.indexOf(".g");
        if (postfixPos == -1) return;

        e.setCancelled(true);
        MinecraftKey parent_recipe_key = new MinecraftKey(namespace.substring(0, postfixPos), generic_recipe_key.getPath());
        lime.nextTick(() -> {
            EntityPlayer handler =((CraftPlayer)e.getPlayer()).getHandle();
            PlayerConnection connection = handler.connection;
            handler.resetLastActionTime();
            if (handler.isSpectator()) return;
            if (!(handler.containerMenu instanceof ContainerRecipeBook<?> container)) return;
            if (container.containerId != packet.getContainerId()) return;
            PlayerRecipeBookClickEvent event = new PlayerRecipeBookClickEvent(connection.player.getBukkitEntity(), CraftNamespacedKey.fromMinecraft(parent_recipe_key), packet.isShiftDown());
            if (!event.callEvent()) return;
            IRegistryCustom custom = connection.player.level.registryAccess();
            Recipes.CRAFTING_MANAGER
                    .byKey(CraftNamespacedKey.toMinecraft(event.getRecipe()))
                    .stream()
                    .flatMap(v -> v instanceof IDisplayRecipe ar ? ar.getDisplayRecipe(custom) : Stream.empty())
                    .filter(v -> v.getId().equals(generic_recipe_key))
                    .filter(connection.player.getRecipeBook()::contains)
                    .findFirst()
                    .ifPresent(recipe -> connection.send(new PacketPlayOutAutoRecipe(connection.player.containerMenu.containerId, recipe)));
        });
    }
}
