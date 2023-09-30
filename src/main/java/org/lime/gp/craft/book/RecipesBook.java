package org.lime.gp.craft.book;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutRecipeUpdate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerFurnace;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftInventoryView;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import org.lime.core;
import org.lime.plugin.CoreElement;
import org.lime.gp.craft.recipe.AbstractRecipe;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.lime;
import org.lime.gp.module.EntityPosition;
import org.lime.gp.player.perm.Perms;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class RecipesBook implements Listener {
    public static CoreElement create() {
        return CoreElement.create(RecipesBook.class)
                .withInit(RecipesBook::init)
                .<JsonObject>addConfig("recipe_book", v -> v
                        .withDefault(new JsonObject())
                        .withInvoke(RecipesBook::config)
                )
                .addCommand("recipe.sync", v -> v
                        .withCheck(s -> s instanceof Player)
                        .withExecutor(sender -> {
                            CraftPlayer player = (CraftPlayer)sender;
                            EntityPlayer handle = player.getHandle();
                            lime.logOP("Recipes: " + handle.getRecipeBook().known.size());
                            handle.getRecipeBook()
                                    .known
                                    .forEach(rec -> lime.logOP(" - " + rec));

                            sender.sendMessage("Recipes resynced!");
                            return true;
                        })
                )
                .withInstance();
    }

    private static String generateKey(EntityPlayer player) {
        Container container = player.containerMenu;
        return container instanceof IRecipesBookContainer ? "_recipes.custom:" + container.containerId : "";
    }

    private static final HashMap<UUID, String> playerCans = new HashMap<>();
    public static final HashMap<String, RecipesBookData> recipesBooks = new HashMap<>();

    public static void init() {
        RecipesBook.update();
        lime.repeat(RecipesBook::update, 5*60);
    }
    private static final HashMap<String, RecipesBookData> defaultBooks = new HashMap<>();
    public static void addDefaultBook(String key, Component title) {
        defaultBooks.put(key, new RecipesBookData(key, title));
    }
    public static void config(JsonObject json) {
        HashMap<String, RecipesBookData> recipesBooks = new HashMap<>();
        json.entrySet().forEach(kv -> recipesBooks.put(kv.getKey(), new RecipesBookData(kv.getKey(), kv.getValue().getAsJsonObject())));
        RecipesBook.recipesBooks.clear();
        RecipesBook.recipesBooks.putAll(defaultBooks);
        RecipesBook.recipesBooks.putAll(recipesBooks);
    }
    @SuppressWarnings("all")
    public static void reload() {
        PacketPlayOutRecipeUpdate packetplayoutrecipeupdate = new PacketPlayOutRecipeUpdate(Recipes.CRAFTING_MANAGER.getRecipes());
        for (EntityPlayer entityplayer : MinecraftServer.getServer().getPlayerList().players) {
            entityplayer.connection.send(packetplayoutrecipeupdate);
            entityplayer.getRecipeBook().sendInitialRecipeBook(entityplayer);
        }
        RecipesBook.resend();
    }
    public static void update() {
        EntityPosition.onlinePlayers.keySet().forEach((uuid) -> UserRow.getBy(uuid).ifPresent(RecipesBook::editRow));
    }

    public static void resend() {
        playerCans.clear();
        update();
    }
    public static void editRow(UserRow row) {
        if (row == null) return;
        lime.nextTick(() -> {
            UUID uuid = row.uuid;
            if (!(row.getOnline() instanceof CraftPlayer player)) {
                playerCans.remove(uuid);
                return;
            }
            Perms.ICanData data = Perms.getCanData(uuid);
            EntityPlayer handle = player.getHandle();
            String dataKey = data.unique() + generateKey(handle);
            if (dataKey.equals(playerCans.get(uuid))) return;
            playerCans.put(uuid, dataKey);
            RecipePackets.syncRecipe(handle);
        });
    }
    @EventHandler public static void on(PlayerJoinEvent e) {
        if (e.getPlayer() instanceof CraftPlayer player)
            RecipePackets.syncRecipe(player.getHandle());
    }
    private static boolean skipEvent = false;
    @EventHandler public static void on(PlayerRecipeDiscoverEvent e) {
        if (skipEvent) return;
        e.setCancelled(true);
    }
    @EventHandler public static void on(InventoryOpenEvent e) {
        if (e.getView() instanceof CraftInventoryView view && RecipePackets.isSync(view.getHandle()) && e.getPlayer() instanceof CraftPlayer player)
            lime.nextTick(() -> RecipePackets.syncRecipe(player.getHandle()));
    }
    @EventHandler public static void on(InventoryCloseEvent e) {
        if (e.getView() instanceof CraftInventoryView view && RecipePackets.isSync(view.getHandle()) && e.getPlayer() instanceof CraftPlayer player)
            lime.nextTick(() -> RecipePackets.syncRecipe(player.getHandle()));
    }

    public static <T extends AbstractRecipe>Optional<Component> getCustomWorkbenchName(Recipes<T> recipes, String id) {
        return Optional.ofNullable(recipesBooks.get(recipes.id() + ":" + id)).map(v -> v.adventure$title);
    }
    public static <T extends AbstractRecipe>Optional<Component> getCustomWorkbenchName(Recipes<T> recipes) {
        return Optional.ofNullable(recipesBooks.get(recipes.id())).map(v -> v.adventure$title);
    }
    public static Optional<Component> getCustomWorkbenchName(String id) {
        return Optional.ofNullable(recipesBooks.get(id)).map(v -> v.adventure$title);
    }
}






    /*
    public static int syncAddRecipes(Collection<IRecipe<?>> append, Collection<IRecipe<?>> remove, EntityPlayer player, boolean highlight) {
        RecipeBookServer recipeBook = player.getRecipeBook();
        int i2 = 0;
        for (IRecipe<?> irecipe : remove) {
            MinecraftKey minecraftkey = irecipe.getId();
            if (recipeBook.known.contains(minecraftkey) || irecipe.isSpecial() || !CraftEventFactory.handlePlayerRecipeListUpdateEvent(player, minecraftkey)) continue;
            recipeBook.add(irecipe);
            if (highlight) recipeBook.addHighlight(irecipe);
            CriterionTriggers.RECIPE_UNLOCKED.trigger(player, irecipe);
            ++i2;
        }
        Recipes.send(player, append);
        recipeBook.sendInitialRecipeBook(player);
        return i2;
    }
    */











