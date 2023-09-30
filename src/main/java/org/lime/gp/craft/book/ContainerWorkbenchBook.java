package org.lime.gp.craft.book;

import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.TileInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerWorkbench;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.TileEntityLimeSkull;
import org.lime.gp.block.CustomTileMetadata;
import org.lime.gp.craft.recipe.AbstractRecipe;
import org.lime.gp.craft.recipe.IDisplayRecipe;
import org.lime.gp.player.inventory.InterfaceManager;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class ContainerWorkbenchBook extends ContainerWorkbench implements IRecipesBookContainer {
    private final @Nullable UUID block_uuid;
    private final Collection<? extends IDisplayRecipe> recipes;

    public ContainerWorkbenchBook(int syncId, PlayerInventory playerInventory, Collection<? extends IDisplayRecipe> recipes, @Nullable UUID block_uuid, ContainerAccess context) {
        super(syncId, playerInventory, context);
        this.block_uuid = block_uuid;
        this.recipes = recipes;
    }
    @Override public Stream<? extends IDisplayRecipe> getRecipesCustom() { return recipes.stream(); }
    public Slot changeSlot(Slot slot) { return InterfaceManager.AbstractSlot.noneInteractSlot(slot); }
    @Override protected final Slot addSlot(Slot slot) {
        slot.index = this.slots.size();
        return super.addSlot(changeSlot(slot));
    }
    @Override public boolean stillValid(EntityHuman player) {
        if (!this.checkReachable) return true;
        return stillValid(this.access, player, block_uuid);
    }
    protected static boolean stillValid(ContainerAccess context, EntityHuman player, UUID block_uuid) {
        return context.evaluate((world, blockposition) -> world.getBlockEntity(blockposition) instanceof TileEntityLimeSkull skull
                && skull.customUUID().filter(v -> v.equals(block_uuid)).isPresent()
                && player.distanceToSqr(blockposition.getX() + 0.5, blockposition.getY() + 0.5, blockposition.getZ() + 0.5) <= 64.0, true);
    }

    public static <T extends AbstractRecipe>EnumInteractionResult open(EntityHuman player, CustomTileMetadata metadata, Recipes<T> recipes, Collection<T> recipeList) {
        return Optional.ofNullable(RecipesBook.recipesBooks.get(recipes.id()))
                .map(book -> {
                    TileEntityLimeSkull skull = metadata.skull;
                    UUID uuid = metadata.key.uuid();
                    player.openMenu(new TileInventory((syncId, inventory, _player) -> new ContainerWorkbenchBook(syncId, inventory, recipeList, uuid, ContainerAccess.create(skull.getLevel(), skull.getBlockPos())), book.title));
                    return EnumInteractionResult.CONSUME;
                })
                .orElse(EnumInteractionResult.PASS);
    }
    public static <T extends AbstractRecipe>EnumInteractionResult open(EntityHuman player, CustomTileMetadata metadata, Recipes<T> recipes, String id, Collection<T> recipeList) {
        return Optional.ofNullable(RecipesBook.recipesBooks.get(recipes.id() + ":" + id))
                .map(book -> {
                    TileEntityLimeSkull skull = metadata.skull;
                    UUID uuid = metadata.key.uuid();
                    player.openMenu(new TileInventory((syncId, inventory, _player) -> new ContainerWorkbenchBook(syncId, inventory, recipeList, uuid, ContainerAccess.create(skull.getLevel(), skull.getBlockPos())), book.title));
                    return EnumInteractionResult.CONSUME;
                })
                .orElse(EnumInteractionResult.PASS);
    }
}
