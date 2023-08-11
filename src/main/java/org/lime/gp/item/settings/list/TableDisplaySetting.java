package org.lime.gp.item.settings.list;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemDisplayContext;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftMagicNumbers;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.display.models.Builder;
import org.lime.gp.extension.ItemNMS;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.gp.lime;
import org.lime.system;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Stream;

@Setting(name = "table_dispaly") @Setting(name = "table_display") public class TableDisplaySetting extends ItemSetting<JsonObject> {
    public enum TableType {
        inventory,
        converter,
        laboratory,
        clicker,
        crops,
        all;

        public static Stream<TableDisplaySetting.TableType> all() {
            return Stream.of(TableType.inventory, TableType.converter, TableType.laboratory, TableType.clicker, TableType.crops);
        }
    }

    public record Context(ItemDisplayContext display, Transformation transformation) {
        public Context(String display, Transformation transformation) {
            this(Arrays.stream(ItemDisplayContext.values())
                    .filter(v -> display.equals(v.getSerializedName()))
                    .findFirst()
                    .orElseThrow(),
                    transformation);
        }
        public static Context parse(JsonElement json) {
            if (json.isJsonPrimitive()) return new Context(json.getAsString(), Transformation.identity());
            JsonObject data = json.getAsJsonObject();
            return new Context(data.get("display").getAsString(), system.transformation(data));
        }
    }

    public static abstract class ITableInfo {
        public abstract net.minecraft.world.item.ItemStack display(ItemStack original);
        public abstract net.minecraft.world.item.ItemStack display(net.minecraft.world.item.ItemStack original);
        public abstract Optional<Context> context();
        public abstract TableDisplaySetting.ITableInfo optimize();
    }
    public static class StaticTableInfo extends TableDisplaySetting.ITableInfo {
        public final Material material;
        public final int id;
        public final @Nullable Context context;
        public final ItemStack bukkit_item;
        private final net.minecraft.world.item.ItemStack nms_item;

        public StaticTableInfo(Material material, int id, @Nullable Context context) {
            this.material = material;
            this.id = id;
            this.context = context;

            bukkit_item = new ItemStack(material);
            ItemMeta meta = bukkit_item.getItemMeta();
            meta.setCustomModelData(id);
            bukkit_item.setItemMeta(meta);
            nms_item = CraftItemStack.asNMSCopy(bukkit_item);
        }
        @Override public net.minecraft.world.item.ItemStack display(ItemStack original) { return nms_item; }
        @Override public net.minecraft.world.item.ItemStack display(net.minecraft.world.item.ItemStack original) { return nms_item; }
        @Override public Optional<Context> context() { return Optional.ofNullable(context); }

        @Override public TableDisplaySetting.ITableInfo optimize() { return this; }
    }
    public static class DynamicTableInfo extends TableDisplaySetting.ITableInfo {
        public final Optional<Material> material;
        public final Optional<Integer> id;
        public final Optional<Context> context;

        public DynamicTableInfo(JsonObject json) {
            material = json.has("material")
                    ? Optional.of(Material.valueOf(json.get("material").getAsString()))
                    : Optional.empty();
            id = json.has("id")
                    ? Optional.of(json.get("id").getAsInt())
                    : Optional.empty();
            context = json.has("context")
                    ? Optional.of(Context.parse(json.get("context")))
                    : Optional.empty();
        }

        @Override public net.minecraft.world.item.ItemStack display(ItemStack original) {
            ItemStack item = new ItemStack(material.orElseGet(original::getType));
            ItemMeta meta = item.getItemMeta();
            meta.setCustomModelData(id.orElseGet(() -> {
                ItemMeta _meta = original.getItemMeta();
                return _meta.hasCustomModelData() ? _meta.getCustomModelData() : null;
            }));
            item.setItemMeta(meta);
            return CraftItemStack.asNMSCopy(item);
        }
        @Override public net.minecraft.world.item.ItemStack display(net.minecraft.world.item.ItemStack original) {
            ItemStack item = new ItemStack(material.orElseGet(() -> CraftMagicNumbers.getMaterial(original.getItem())));
            ItemMeta meta = item.getItemMeta();
            meta.setCustomModelData(id.orElseGet(() -> ItemNMS.getCustomModelData(original).orElse(null)));
            item.setItemMeta(meta);
            return CraftItemStack.asNMSCopy(item);
        }
        @Override public Optional<Context> context() { return context; }

        public Optional<TableDisplaySetting.StaticTableInfo> tryStatic() {
            return this.material.flatMap(material -> this.id.map(id -> new StaticTableInfo(material, id, context.orElse(null))));
        }
        @Override public TableDisplaySetting.ITableInfo optimize() { return tryStatic().<TableDisplaySetting.ITableInfo>map(v -> v).orElse(this); }
    }

    private final HashMap<system.Toast2<TableDisplaySetting.TableType, String>, TableDisplaySetting.ITableInfo> infos = new HashMap<>();

    public Optional<TableDisplaySetting.ITableInfo> of(TableDisplaySetting.TableType tableType, @Nullable String type) {
        return type == null
                ? Optional.ofNullable(infos.get(system.toast(tableType, null)))
                : Optional.ofNullable(infos.get(system.toast(tableType, type))).or(() -> Optional.ofNullable(infos.get(system.toast(tableType, null))));
    }

    public TableDisplaySetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        json.entrySet().forEach(kv -> {
            String[] args = kv.getKey().split(":", 2);
            TableDisplaySetting.TableType type = TableType.valueOf(args[0]);
            TableDisplaySetting.ITableInfo info = new DynamicTableInfo(kv.getValue().getAsJsonObject()).optimize();
            (type == TableType.all ? TableType.all() : Stream.of(type))
                    .forEach(_type -> infos.put(system.toast(_type, args.length > 1 ? args[1] : null), info));
        });
    }

    public static Builder builderItem(ItemStack item, Transformation base, TableDisplaySetting.TableType table, @Nullable String type) {
        return Items.getOptional(TableDisplaySetting.class, item)
                .flatMap(v -> v.of(table, type))
                .map(v -> system.toast(v.display(item), v.context()))
                .orElseGet(() -> system.toast(CraftItemStack.asNMSCopy(item), Optional.empty()))
                .invokeGet((model, context) -> lime.models.builder(EntityTypes.ITEM_DISPLAY)
                        .nbt(() -> {
                            Display.ItemDisplay display = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, lime.MainWorld.getHandle());
                            display.setItemTransform(context.map(TableDisplaySetting.Context::display).orElse(ItemDisplayContext.NONE));
                            display.setTransformation(base.compose(context.map(TableDisplaySetting.Context::transformation).orElseGet(Transformation::identity)));
                            return display;
                        })
                        .addEquipment(EnumItemSlot.HEAD, model)
                );
    }
    public static Builder builderItem(net.minecraft.world.item.ItemStack item, Transformation base, TableDisplaySetting.TableType table, @Nullable String type) {
        return Items.getOptional(TableDisplaySetting.class, item)
                .flatMap(v -> v.of(table, type))
                .map(v -> system.toast(v.display(item), v.context()))
                .orElseGet(() -> system.toast(item.copy(), Optional.empty()))
                .invokeGet((model, context) -> lime.models.builder(EntityTypes.ITEM_DISPLAY)
                        .nbt(() -> {
                            Display.ItemDisplay display = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, lime.MainWorld.getHandle());
                            display.setItemTransform(context.map(TableDisplaySetting.Context::display).orElse(ItemDisplayContext.NONE));
                            display.setTransformation(base.compose(context.map(TableDisplaySetting.Context::transformation).orElseGet(Transformation::identity)));
                            return display;
                        })
                        .addEquipment(EnumItemSlot.HEAD, model)
                );
    }
}