package org.lime.gp.item.loot;

import com.google.gson.JsonObject;
import org.bukkit.inventory.ItemStack;
import org.lime.docs.IIndexDocs;
import org.lime.docs.IIndexGroup;
import org.lime.docs.json.*;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.filter.IFilter;
import org.lime.gp.filter.data.IFilterInfo;
import org.lime.gp.module.loot.IPopulateLoot;
import org.lime.gp.module.loot.Parameters;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FilterLoot implements ILoot {
    public List<Toast2<IFilter<IPopulateLoot>, ILoot>> loot = new ArrayList<>();

    public FilterLoot(JsonObject json) {
        IFilterInfo<IPopulateLoot> filterInfo = Parameters.filterInfo();
        json.entrySet().forEach(kv -> loot.add(Toast.of(IFilter.parse(filterInfo, kv.getKey()), ILoot.parse(kv.getValue()))));
    }

    @Override public List<ItemStack> generateLoot(IPopulateLoot loot) {
        List<ItemStack> items = new ArrayList<>();
        for (Toast2<IFilter<IPopulateLoot>, ILoot> item : this.loot) {
            if (item.val0.isFilter(loot)) {
                return item.val1.generateLoot(loot);
            }
        }
        return items;
    }

    public static IIndexGroup docs(String index, IJElement loot, IDocsLink docs) {
        IIndexGroup filterVariable = JsonEnumInfo.of("FILTER_VARIABLE")
                .add(IJElement.join(
                        IJElement.field("debug"),
                        IJElement.text("["),
                        IJElement.field("PREFIX_NAME"),
                        IJElement.text("]")
                ), IComment.text("Отображает все парамерты текущего вызова. В заголоске информации содержится ").append(IComment.field("PREFIX_NAME")))

                .add(IJElement.join(
                        IJElement.field("random"),
                        IJElement.text("["),
                        IJElement.link(docs.range()),
                        IJElement.text("]")
                ), IComment.join(
                        IComment.text("Срабатывает когда случайное значение "),
                        IComment.range(0.0, 1.0),
                        IComment.text(" попадает в одно из возможных значений "),
                        IComment.link(docs.range())
                ))

                .add(IJElement.join(
                        IJElement.field("tag"),
                        IJElement.text("["),
                        IJElement.field("TAG"),
                        IJElement.text(";"),
                        IJElement.field("TAG"),
                        IJElement.text(";"),
                        IJElement.any(),
                        IJElement.text(";"),
                        IJElement.field("TAG"),
                        IJElement.text("]")
                ), IComment.text("Срабатывает когда все тэги текущего энтити совпадают"))

                .add(IJElement.join(
                        IJElement.field("any"),
                        IJElement.text("["),
                        IJElement.field("PARAM"),
                        IJElement.text(";"),
                        IJElement.field("PARAM"),
                        IJElement.text(";"),
                        IJElement.any(),
                        IJElement.text(";"),
                        IJElement.field("PARAM"),
                        IJElement.text("]")
                ), IComment.text("Срабатывает когда одно из всех условий ").append(IComment.field("PARAM")).append(IComment.text(" выполнены")))

                .add(IJElement.join(
                        IJElement.field("all"),
                        IJElement.text("["),
                        IJElement.field("PARAM"),
                        IJElement.text(";"),
                        IJElement.field("PARAM"),
                        IJElement.text(";"),
                        IJElement.any(),
                        IJElement.text(";"),
                        IJElement.field("PARAM"),
                        IJElement.text("]")
                ), IComment.text("Срабатывает когда все условия ").append(IComment.field("PARAM")).append(IComment.text(" выполнены")))

                .add(IJElement.join(
                        IJElement.field("block"),
                        IJElement.text("["),
                        IJElement.field("block="),
                        IJElement.link(docs.vanillaMaterial()),
                        IJElement.text(";"),
                        IJElement.field("PARAM"),
                        IJElement.text(";"),
                        IJElement.field("PARAM"),
                        IJElement.text(";"),
                        IJElement.any(),
                        IJElement.text(";"),
                        IJElement.field("PARAM"),
                        IJElement.text("]")
                ), IComment.join(
                        IComment.text("Срабатывает когда "),
                        IComment.field("block"),
                        IComment.text(" совпадает и все условия "),
                        IComment.field("PARAM"),
                        IComment.text(" выполнены. Значение "),
                        IComment.field("block"),
                        IComment.text(" является "),
                        IComment.field("regex"),
                        IComment.text(" выражением")
                ))

                .add(IJElement.raw(true), IComment.text("Всегда срабатывает"))
                .add(IJElement.raw(false), IComment.text("Никогда не срабатывает"))
                .add(IJElement.text("!").join(IJElement.linkCurrent()), IComment.text("Инвертирует проверяемое значение"));

        return JsonGroup.of(index, IJElement.anyObject(
                JProperty.require(IName.link(filterVariable), loot)
        )).withChilds(filterVariable);
    }
}
