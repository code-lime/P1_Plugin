package org.lime.gp.item.settings.list;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.chat.Apply;
import org.lime.gp.item.ExecuteItem;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.IUpdate;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.item.data.UpdateType;
import org.lime.gp.item.settings.ItemSetting;
import org.lime.gp.item.settings.Setting;
import org.lime.gp.lime;
import org.lime.plugin.CoreElement;
import org.lime.system;

import java.util.Set;
import java.util.stream.Collectors;

@Setting(name = "resync") public class ReSyncSetting extends ItemSetting<JsonObject> {
    public final String version;
    public final Set<UpdateType> list;

    public ReSyncSetting(ItemCreator creator, JsonObject json) {
        super(creator, json);
        this.version = json.get("version").getAsString();
        this.list = json.getAsJsonArray("list")
                .asList()
                .stream()
                .map(JsonElement::getAsString)
                .map(String::toUpperCase)
                .map(UpdateType::valueOf)
                .collect(Collectors.toSet());
    }

    public boolean tryUpdate(ItemMeta item) {
        PersistentDataContainer container = item.getPersistentDataContainer();
        if (version.equals(container.get(RESYNC_VERSION, PersistentDataType.STRING))) return false;
        container.set(RESYNC_VERSION, PersistentDataType.STRING, version);
        this.creator().update(item, Apply.of(), IUpdate.of(list));
        return true;
    }

    private static final NamespacedKey RESYNC_VERSION = new NamespacedKey(lime._plugin, "resync.version");

    public static CoreElement create() {
        return CoreElement.create(ReSyncSetting.class)
                .withInit(ReSyncSetting::init);
    }
    private static void init() { ExecuteItem.execute.add(ReSyncSetting::onExecute); }
    private static boolean onExecute(ItemStack item, system.Toast1<ItemMeta> metaBox) {
        return Items.getOptional(ReSyncSetting.class, item)
                .map(resync -> resync.tryUpdate(metaBox.val0))
                .orElse(false);
    }

    @Override public IIndexGroup docs(String index, IDocsLink docs) {
        IIndexGroup resync_type = JsonEnumInfo.of("RESYNC_TYPE", "resync_type", UpdateType.class);
        return JsonGroup.of(index, index, JObject.of(
                JProperty.require(IName.raw("version"), IJElement.raw("v1.0"), IComment.text("Версия данных")),
                JProperty.require(IName.raw("list"), IJElement.anyList(IJElement.link(resync_type)), IComment.text("Список обновляемых данных"))
        ), "Обновляет ванильные данные предмета при изменении версии")
                .withChild(resync_type);
    }
}






