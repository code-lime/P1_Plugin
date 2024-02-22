package org.lime.gp.item.settings.list;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import net.minecraft.world.item.ItemDisplayContext;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.display.models.shadow.IBuilder;
import org.lime.display.transform.LocalLocation;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.extension.ItemNMS;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.gp.lime;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.IterableUtils;
import org.lime.system.utils.MathUtils;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Stream;

@Setting(name = "table_display") @Setting(name = "table_dispaly") public class TableDisplaySetting extends ItemSetting<JsonObject> {
    public enum TableType {
        inventory,
        converter,
        laboratory,
        clicker,
        crops,

        all;

        public static Stream<TableDisplaySetting.TableType> all() { return Stream.of(TableType.inventory, TableType.converter, TableType.laboratory, TableType.clicker, TableType.crops); }
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
            return new Context(data.get("display").getAsString(), MathUtils.transformation(data));
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

    private final HashMap<Toast2<TableDisplaySetting.TableType, String>, TableDisplaySetting.ITableInfo> infos = new HashMap<>();

    public Optional<TableDisplaySetting.ITableInfo> of(TableDisplaySetting.TableType tableType, @Nullable String type) {
        return type == null
                ? Optional.ofNullable(infos.get(Toast.of(tableType, null)))
                : Optional.ofNullable(infos.get(Toast.of(tableType, type))).or(() -> Optional.ofNullable(infos.get(Toast.of(tableType, null))));
    }

    public TableDisplaySetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        json.entrySet().forEach(kv -> {
            String[] args = kv.getKey().split(":", 2);
            TableDisplaySetting.TableType type = TableType.valueOf(args[0]);
            TableDisplaySetting.ITableInfo info = new DynamicTableInfo(kv.getValue().getAsJsonObject()).optimize();
            (type == TableType.all ? TableType.all() : Stream.of(type))
                    .forEach(_type -> infos.put(Toast.of(_type, args.length > 1 ? args[1] : null), info));
        });
    }

    public static net.minecraft.world.item.ItemStack builderItem(ItemStack item, TableDisplaySetting.TableType table, @Nullable String type) {
        return Items.getOptional(TableDisplaySetting.class, item)
                .flatMap(v -> v.of(table, type))
                .map(v -> v.display(item))
                .orElseGet(() -> CraftItemStack.asNMSCopy(item));
    }
    public static IBuilder builderItem(ItemStack item, Transformation base, TableDisplaySetting.TableType table, @Nullable String type) {
        return Items.getOptional(TableDisplaySetting.class, item)
                .flatMap(v -> v.of(table, type))
                .map(v -> Toast.of(v.display(item), v.context()))
                .orElseGet(() -> Toast.of(CraftItemStack.asNMSCopy(item), Optional.empty()))
                .invokeGet((model, context) -> lime.models.builder().item()
                        .item(model)
                        .context(context.map(TableDisplaySetting.Context::display).orElse(ItemDisplayContext.NONE))
                        .transform(MathUtils.transform(base, context.map(TableDisplaySetting.Context::transformation).orElseGet(Transformation::identity)))
                );
    }
    public static IBuilder builderItem(net.minecraft.world.item.ItemStack item, Transformation base, TableDisplaySetting.TableType table, @Nullable String type) {
        return Items.getOptional(TableDisplaySetting.class, item)
                .flatMap(v -> v.of(table, type))
                .map(v -> Toast.of(v.display(item), v.context()))
                .orElseGet(() -> Toast.of(item.copy(), Optional.empty()))
                .invokeGet((model, context) -> lime.models.builder().item()
                        .item(model)
                        .context(context.map(Context::display).orElse(ItemDisplayContext.NONE))
                        .transform(MathUtils.transform(base, context.map(Context::transformation).orElseGet(Transformation::identity)))
                );
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        IIndexGroup table_type = JsonEnumInfo.of("TABLE_TYPE", "table_type", TableType.class);
        IIndexGroup table_info = JsonGroup.of("TABLE_INFO", "table_info", JObject.of(
                JProperty.optional(IName.raw("material"), IJElement.link(docs.vanillaMaterial()), IComment.text("Изменение типа предмета")),
                JProperty.optional(IName.raw("id"), IJElement.raw(10), IComment.empty()
                        .append(IComment.text("Изменение "))
                        .append(IComment.raw("id"))
                        .append(IComment.text(" предмета"))),
                JProperty.optional(IName.raw("context"), IJElement.any(), IComment.warning("Будет удалено в последующем обновлении"))
        ));
        IIndexGroup table_key = JsonEnumInfo.of("TABLE_KEY", "table_key", ImmutableList.of(
                IJElement.link(table_type),
                IJElement.link(table_type).concat(":", IJElement.text("ANY_TYPE"))
        ));

        return JsonGroup.of(index, index, IJElement.anyObject(
                JProperty.require(IName.link(table_key), IJElement.link(table_info))
        ), IComment.text("Устанавливает статус работы урона сплешом у меча"))
                .withChilds(table_type, table_key, table_info);
    }
}

