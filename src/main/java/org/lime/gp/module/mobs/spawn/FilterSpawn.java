package org.lime.gp.module.mobs.spawn;

import com.google.gson.JsonObject;
import org.bukkit.inventory.ItemStack;
import org.lime.gp.filter.IFilter;
import org.lime.gp.filter.data.IFilterInfo;
import org.lime.gp.module.mobs.IMobCreator;
import org.lime.gp.module.mobs.IPopulateSpawn;
import org.lime.gp.module.mobs.Parameters;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FilterSpawn implements ISpawn {
    public List<Toast2<IFilter<IPopulateSpawn>, ISpawn>> spawn = new ArrayList<>();

    public FilterSpawn(JsonObject json) {
        IFilterInfo<IPopulateSpawn> filterSpawn = Parameters.filterInfo();
        json.entrySet().forEach(kv -> spawn.add(Toast.of(IFilter.parse(filterSpawn, kv.getKey()), ISpawn.parse(kv.getValue()))));
    }

    @Override public Optional<IMobCreator> generateMob(IPopulateSpawn populate) {
        List<ItemStack> items = new ArrayList<>();
        for (Toast2<IFilter<IPopulateSpawn>, ISpawn> item : this.spawn) {
            if (item.val0.isFilter(populate)) {
                return item.val1.generateMob(populate);
            }
        }
        return Optional.empty();
    }
}
