package org.lime.gp.block.component.display.partial.list;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.lime.display.ItemParser;
import org.lime.display.models.shadow.IBuilder;
import org.lime.docs.IIndexDocs;
import org.lime.docs.json.*;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.block.component.display.partial.PartialEnum;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.extension.ItemNMS;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.kyori.adventure.text.format.TextColor;
import org.lime.gp.lime;
import org.lime.system.toast.*;
import org.lime.system.utils.MathUtils;

public class ViewPartial extends BlockPartial implements IModelPartial {
    private final ItemStack item;
    private final net.minecraft.world.item.ItemStack nms_item;

    private final InfoComponent.Rotation.Value rotation;
    private final double back_angle;
    private final boolean show;
    private final double offset_rotation;
    private final Vector offset_translation;
    private final Vector offset_scale;

    public InfoComponent.Rotation.Value rotation() { return rotation; }
    public double back_angle() { return back_angle; }
    public boolean show() { return show; }
    public double offset_rotation() { return offset_rotation; }
    public Vector offset_translation() { return offset_translation; }
    public Vector offset_scale() { return offset_scale; }

    private final String model;
    private final double modelDistance;
    private IBuilder generic = null;

    public ViewPartial(int distanceChunk, JsonObject json) {
        super(distanceChunk, json);
        this.item = ItemParser.readItem(json.get("view"));
        /*
        JsonElement item = json.get("view");
        if (item.isJsonPrimitive()) {
            String[] args = item.getAsString().split("\\^");
            this.item = new ItemStack(Material.valueOf(args[0]));
            if (args.length >= 2) {
                ItemMeta meta = this.item.getItemMeta();
                meta.setCustomModelData(Integer.parseInt(args[1]));
                this.item.setItemMeta(meta);
            }
        } else {
            JsonObject _item = item.getAsJsonObject();
            this.item = new ItemStack(Material.valueOf(_item.get("material").getAsString()));
            ItemMeta meta = this.item.getItemMeta();
            if (_item.has("id")) meta.setCustomModelData(_item.get("id").getAsInt());
            this.item.setItemMeta(meta);
        }*/
        this.back_angle = json.has("back_angle") ? json.get("back_angle").getAsDouble() : -1;
        if (json.has("offset")) {
            JsonObject offset = json.getAsJsonObject("offset");
            offset_rotation = offset.has("rotation") ? offset.get("rotation").getAsDouble() : 0;
            offset_translation = offset.has("translation") ? MathUtils.getVector(offset.get("translation").getAsString()) : new Vector();
            offset_scale = offset.has("scale") ? MathUtils.getVector(offset.get("scale").getAsString()) : new Vector(1, 1, 1);
        } else {
            offset_rotation = 0;
            offset_translation = new Vector();
            offset_scale = new Vector(1,1,1);
        }
        this.rotation = json.has("rotation") ? InfoComponent.Rotation.Value.ofAngle(json.get("rotation").getAsInt()) : InfoComponent.Rotation.Value.ANGLE_0;

        this.nms_item = CraftItemStack.asNMSCopy(this.item);
        this.show = true;

        this.model = json.has("model") ? parseModel(json.get("model")) : null;
        this.modelDistance = json.has("model_distance") ? json.get("model_distance").getAsDouble() : Double.POSITIVE_INFINITY;
    }

    private String parseModel(JsonElement json) {
        if (json.isJsonPrimitive()) return json.getAsString();
        generic = lime.models.builder().parse(json);
        return "#generic";
    }

    private final ConcurrentHashMap<String, net.minecraft.world.item.ItemStack> variableConvert = new ConcurrentHashMap<>();
    public net.minecraft.world.item.ItemStack nms(Map<String, String> variable) {
        String color = variable.getOrDefault("display.color", "default");
        String key = "display.color=" + color;
        return variableConvert.computeIfAbsent(key, (_key) -> {
            net.minecraft.world.item.ItemStack nms_item = this.nms_item.copy();
            if (color.equals("default")) return nms_item;
            ItemNMS.setColor(nms_item, TextColor.fromHexString("#" + color));
            return nms_item;
        });
    }

    @Override public PartialEnum type() { return PartialEnum.Frame; }
    @Override public String toString() { return super.toString()+ "^" + item + "R" + rotation.angle; }
    @Override public Optional<Toast2<IBuilder, Double>> model() {
        return Optional.ofNullable(generic).or(() -> model == null ? Optional.empty() : lime.models.get(model)).map(v -> Toast.of(v, modelDistance));
    }

    public static JObject docs(IDocsLink docs, IIndexDocs variable) {
        return BlockPartial.docs(docs, variable).addFirst(
                JProperty.require(IName.raw("view"), IJElement.link(docs.parseItem()), IComment.text("Отображаемый модель-предмет")),
                JProperty.optional(IName.raw("back_angle"), IJElement.raw(10), IComment.text("Угол за спиной на который не будет отображаться модель")),
                JProperty.optional(IName.raw("offset"), JObject.of(
                        JProperty.optional(IName.raw("rotation"), IJElement.raw(10), IComment.text("Относительный поворот модели")),
                        JProperty.optional(IName.raw("translation"), IJElement.link(docs.vector()), IComment.text("Относительный сдвиг")),
                        JProperty.optional(IName.raw("scale"), IJElement.link(docs.vector()), IComment.text("Относительный размер модели"))
                ), IComment.text("Относительное преобразование модели")),
                JProperty.optional(IName.raw("rotation"), IJElement.link(docs.rotation()), IComment.text("Поворот модели блока")),
                JProperty.optional(IName.raw("model"), IJElement.link(docs.model()), IComment.text("Отображемая модель")),
                JProperty.optional(IName.raw("model_distance"), IJElement.raw(10.0), IComment.text("Максимальная дальность отображения модели"))
        );
    }
}