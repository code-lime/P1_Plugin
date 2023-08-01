package org.lime.gp.craft;

import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayInAutoRecipe;
import net.minecraft.network.protocol.game.PacketPlayOutAutoRecipe;
import net.minecraft.network.protocol.game.PacketPlayOutRecipeUpdate;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.stats.RecipeBookServer;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.TileInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.crafting.IRecipe;
import net.minecraft.world.item.crafting.RecipeCrafting;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftNamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import org.lime.core;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.craft.recipe.AbstractRecipe;
import org.lime.gp.craft.recipe.IDisplayRecipe;
import org.lime.gp.craft.recipe.Recipes;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.extension.PacketManager;
import org.lime.gp.lime;
import org.lime.gp.module.EntityPosition;
import org.lime.gp.player.inventory.InterfaceManager;
import org.lime.gp.player.perm.Perms;

import java.util.*;
import java.util.stream.Stream;

public class RecipesBook implements Listener {
    public static core.element create() {
        return core.element.create(RecipesBook.class)
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
    public interface IRecipesBookContainer {
        Collection<? extends IDisplayRecipe> getRecipesCustom();
        private static String generateKey(EntityPlayer player) {
            Container container = player.containerMenu;
            return container instanceof IRecipesBookContainer
                ? "_recipes.custom:" + container.containerId
                : ""; }
    }

    private static final HashMap<UUID, String> playerCans = new HashMap<>();
    private static final HashMap<String, RecipesBookData> recipesBooks = new HashMap<>();
    private static class RecipesBookData {
        @SuppressWarnings("unused")
        public final String id;
        public final IChatBaseComponent title;
        public final Component adventure$title;

        public RecipesBookData(String id, JsonObject json) {
            this.id = id;
            this.title = ChatHelper.toNMS(adventure$title = ChatHelper.formatComponent(json.get("title").getAsString()));
        }
        public RecipesBookData(String id, Component component) {
            this.id = id;
            this.title = ChatHelper.toNMS(adventure$title = component);
        }
    }

    public static void init() {
        PacketManager.adapter()
                .add(PacketPlayInAutoRecipe.class, (packet, e) -> {
                    MinecraftKey generic_recipe_key = packet.getRecipe();
                    String namespace = generic_recipe_key.getNamespace();
                    int postfixPos = namespace.indexOf(".g");
                    if (postfixPos == -1) return;
                    e.setCancelled(true);
                    MinecraftKey parent_recipe_key = new MinecraftKey(namespace.substring(0, postfixPos), generic_recipe_key.getPath());
                    lime.nextTick(() -> {
                        PlayerRecipeBookClickEvent event;
                        PlayerConnection connection = ((CraftPlayer)e.getPlayer()).getHandle().connection;
                        connection.player.resetLastActionTime();
                        if (!connection.player.isSpectator()
                                && connection.player.containerMenu.containerId == packet.getContainerId()
                                && connection.player.containerMenu instanceof ContainerRecipeBook
                                && (event = new PlayerRecipeBookClickEvent(connection.player.getBukkitEntity(), CraftNamespacedKey.fromMinecraft(parent_recipe_key), packet.isShiftDown())).callEvent()
                        ) {
                            IRegistryCustom custom = connection.player.level.registryAccess();
                            Recipes.CRAFTING_MANAGER
                                    .byKey(CraftNamespacedKey.toMinecraft(event.getRecipe()))
                                    .stream()
                                    .flatMap(v -> v instanceof IDisplayRecipe ar ? ar.getDisplayRecipe(custom) : Stream.empty())
                                    .filter(v -> v.getId().equals(generic_recipe_key))
                                    .filter(connection.player.getRecipeBook()::contains)
                                    .findFirst()
                                    .ifPresent(irecipe -> connection.send(new PacketPlayOutAutoRecipe(connection.player.containerMenu.containerId, irecipe)));
                            //((ContainerRecipeBook) connection.player.containerMenu).handlePlacement(false, irecipe, connection.player);
                        }
                    });
                })
                .listen();
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
            if (!(row.getOnline() instanceof CraftPlayer player)) return;
            UUID uuid = row.uuid;
            Perms.ICanData data = Perms.getCanData(uuid);
            EntityPlayer handle = player.getHandle();
            if ((data.unique() + IRecipesBookContainer.generateKey(handle)).equals(playerCans.get(uuid))) return;
            sendRecipes(handle, data);
        });
    }
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

        Recipes.send(player, recipes);
        lime.once(() -> recipeBook.sendInitialRecipeBook(player), 0.2);
    }
    @EventHandler public static void on(PlayerJoinEvent e) {
        if (e.getPlayer() instanceof CraftPlayer player) sendRecipes(player.getHandle(), Perms.getCanData(player.getUniqueId()));
    }
    private static boolean skipEvent = false;
    @EventHandler public static void on(PlayerRecipeDiscoverEvent e) {
        if (skipEvent) return;
        e.setCancelled(true);
    }
    @EventHandler public static void on(InventoryOpenEvent e) {
        if (e.getView() instanceof CraftInventoryView view && view.getHandle() instanceof IRecipesBookContainer && e.getPlayer() instanceof CraftPlayer player)
            lime.nextTick(() -> sendRecipes(player.getHandle(), Perms.getCanData(player)));

    }
    @EventHandler public static void on(InventoryCloseEvent e) {
        if (e.getView() instanceof CraftInventoryView view && view.getHandle() instanceof IRecipesBookContainer && e.getPlayer() instanceof CraftPlayer player)
            lime.nextTick(() -> sendRecipes(player.getHandle(), Perms.getCanData(player)));
    }

    public static class RecipesBookContainerWorkbench extends ContainerWorkbench implements IRecipesBookContainer {
        private final UUID block_uuid;
        private final Collection<? extends IDisplayRecipe> recipes;
        public RecipesBookContainerWorkbench(int syncId, PlayerInventory playerInventory, Collection<? extends IDisplayRecipe> recipes, UUID block_uuid, ContainerAccess context) {
            super(syncId, playerInventory, context);
            this.block_uuid = block_uuid;
            this.recipes = recipes;
        }
        @Override public Collection<? extends IDisplayRecipe> getRecipesCustom() { return recipes; }
        @Override protected Slot addSlot(Slot slot) { return super.addSlot(InterfaceManager.AbstractSlot.noneInteractSlot(slot)); }
        @Override public boolean stillValid(EntityHuman player) {
            if (!this.checkReachable) return true;
            return stillValid(this.access, player, block_uuid);
        }
        protected static boolean stillValid(ContainerAccess context, EntityHuman player, UUID block_uuid) {
            return context.evaluate((world, blockposition) -> world.getBlockEntity(blockposition) instanceof TileEntityLimeSkull skull
                    && skull.customUUID().filter(block_uuid::equals).isPresent()
                    && player.distanceToSqr(blockposition.getX() + 0.5, blockposition.getY() + 0.5, blockposition.getZ() + 0.5) <= 64.0, true);
        }
    }

    /*public static IChatBaseComponent TEMP_TITLE = ChatHelper.toNMS(Component.empty()
            .append(ImageBuilder.of(0xE600, 170).addOffset(59).withColor(NamedTextColor.WHITE).build())
            .append(Component.text(ChatHelper.getSpaceSize(-1) + "ASD"))
    );*/
    /*public static IChatBaseComponent TEMP_TITLE = ChatHelper.toNMS(Component.empty()
            .append(ImageBuilder.of(0xE600, 170).addOffset(56).withColor(NamedTextColor.WHITE).build())
            .append(Component.text(ChatHelper.getSpaceSize(-1) + "ASD"))
    );*/
    public static <T extends AbstractRecipe>Optional<Component> getCustomWorkbenchName(Recipes<T> recipes, String id) {
        return Optional.ofNullable(recipesBooks.get(recipes.id() + ":" + id)).map(v -> v.adventure$title);
    }
    public static <T extends AbstractRecipe>Optional<Component> getCustomWorkbenchName(Recipes<T> recipes) {
        return Optional.ofNullable(recipesBooks.get(recipes.id())).map(v -> v.adventure$title);
    }
    public static Optional<IChatBaseComponent> getCustomWorkbenchName(String id) {
        return Optional.ofNullable(recipesBooks.get(id)).map(v -> v.title);
    }

    public static <T extends AbstractRecipe>EnumInteractionResult openCustomWorkbench(EntityHuman player, CustomTileMetadata metadata, Recipes<T> recipes, Collection<T> recipeList) {
        return Optional.ofNullable(recipesBooks.get(recipes.id()))
                .map(book -> {
                    TileEntityLimeSkull skull = metadata.skull;
                    UUID uuid = metadata.key.uuid();
                    player.openMenu(new TileInventory((syncId, inventory, _player) -> new RecipesBookContainerWorkbench(syncId, inventory, recipeList, uuid, ContainerAccess.create(skull.getLevel(), skull.getBlockPos())), book.title));
                    return EnumInteractionResult.CONSUME;
                })
                .orElse(EnumInteractionResult.PASS);
    }
    public static <T extends AbstractRecipe>EnumInteractionResult openCustomWorkbench(EntityHuman player, CustomTileMetadata metadata, Recipes<T> recipes, String id, Collection<T> recipeList) {
        return Optional.ofNullable(recipesBooks.get(recipes.id() + ":" + id))
                .map(book -> {
                    TileEntityLimeSkull skull = metadata.skull;
                    UUID uuid = metadata.key.uuid();
                    player.openMenu(new TileInventory((syncId, inventory, _player) -> new RecipesBookContainerWorkbench(syncId, inventory, recipeList, uuid, ContainerAccess.create(skull.getLevel(), skull.getBlockPos())), book.title));
                    return EnumInteractionResult.CONSUME;
                })
                .orElse(EnumInteractionResult.PASS);
    }
}















