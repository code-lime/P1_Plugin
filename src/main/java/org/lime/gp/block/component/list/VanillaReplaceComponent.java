package org.lime.gp.block.component.list;

import com.google.gson.JsonObject;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_19_R3.block.CraftBlock;
import org.lime.ToDoException;
import org.lime.docs.IIndexGroup;
import org.lime.gp.block.BlockInfo;
import org.lime.gp.block.component.ComponentStatic;
import org.lime.gp.block.component.InfoComponent;
import org.lime.gp.docs.IDocsLink;
import org.lime.gp.lime;
import org.lime.system;

import java.util.*;
import java.util.stream.Collectors;

@InfoComponent.Component(name = "vanilla_replace")
public final class VanillaReplaceComponent extends ComponentStatic<JsonObject> {
    private static boolean is(List<system.Toast2<String, List<String>>> filter, Map<String, String> values) {
        for (system.Toast2<String, List<String>> kv : filter) {
            String str = values.getOrDefault(kv.val0, null);
            if (!kv.val1.contains(str)) return false;
        }
        return true;
    }

    public VanillaReplaceComponent(BlockInfo creator, JsonObject json) {
        super(creator, json);
        json.entrySet().forEach(kv -> {
            String[] _arr = kv.getKey().split("\\?");
            Material material = Material.valueOf(_arr[0]);
            String[] _args = Arrays.stream(_arr).skip(1).collect(Collectors.joining("?")).replace('?', '&').split("&");
            List<system.Toast2<String, List<String>>> filter = new ArrayList<>();
            for (String _arg : _args) {
                String[] _kv = _arg.split("=");
                if (_kv.length == 1) {
                    if (_kv[0].length() > 0)
                        lime.logOP("[Warning] Key '" + _kv[0] + "' of VanillaReplace '" + kv.getKey() + "' in block '" + creator.getKey() + "' is empty. Skipped");
                    continue;
                }
                filter.add(system.toast(_kv[0], Arrays.asList(Arrays.stream(_kv).skip(1).collect(Collectors.joining("=")).split(","))));
            }

            Map<String, JsonObject> variable = kv.getValue().getAsJsonObject().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, _kv -> _kv.getValue().getAsJsonObject()));

            creator.addReplace(material, new BlockInfo.Replacer<Integer>() {
                @Override
                public BlockInfo info() {
                    return creator;
                }

                @Override
                public Optional<Integer> read(Block block) {
                    IBlockData data = ((CraftBlock) block).getNMS();
                    Map<String, String> values = new HashMap<>();
                    data.getProperties().forEach(state -> values.put(state.getName(), BlockInfo.getValue(data, state)));
                    if (!is(filter, values)) return Optional.empty();
                    return Optional.of(0);
                }

                @Override
                public Map<String, JsonObject> variable(Integer value) {
                    return variable;
                }
            });
        });
    }
    @Override public IIndexGroup docs(String index, IDocsLink docs) { throw new ToDoException("BLOCK COMPONENT: " + index); }
}
