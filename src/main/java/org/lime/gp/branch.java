package org.lime.gp;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.lime.plugin.CoreElement;
import org.lime.system.json;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

import com.google.gson.JsonObject;

public class branch {
    private record BranchData(Map<String, String> Data) {
        public static HashMap<String, BranchData> getBranchList() {
            JsonObject branch = json.parse(lime.readAllConfig("branch")).getAsJsonObject();
            HashMap<String, BranchData> out = new HashMap<String, BranchData>();
            branch.entrySet().forEach(kv -> {
                out.put(kv.getKey(), new BranchData(kv.getValue()
                    .getAsJsonObject()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, _kv -> _kv.getValue().getAsString()))));
            });
            return out;
        }
        public static Collection<String> getBranchKeys() {
            return json.parse(lime.readAllConfig("branch")).getAsJsonObject().keySet();
        }
        public void applyBranch() {
            for (String configFile : new String[] { "autodownload", "share", "database" }) {
                JsonObject config = json.parse(lime.readAllConfig(configFile)).getAsJsonObject();
                if (!config.has("format")) continue;
                JsonObject format = config.getAsJsonObject("format");

                format.entrySet().forEach(kv -> {
                    String value = kv.getValue().getAsString();
                    for (Map.Entry<String, String> _kv : Data.entrySet()) {
                        value = value.replace("{"+_kv.getKey()+"}", _kv.getValue());
                        value = value.replace("{"+_kv.getKey()+".lowercase}", _kv.getValue().toLowerCase());
                    }
                    config.addProperty(kv.getKey(), value);
                });
                lime.writeAllConfig(configFile, json.format(config));
            }
        }
    }

    public static CoreElement create() {
        return CoreElement.create(branch.class)
            .addCommand("branch.swap", v -> v
                .withCheck(CommandSender::isOp)
                .withTab((sender, args) -> args.length == 1 
                    ? BranchData.getBranchKeys()
                    : Collections.emptyList())
                .withExecutor((sender, args) -> {
                    if (args.length != 1) return false;
                    BranchData.getBranchList().get(args[0]).applyBranch();
                    sender.sendMessage("Branch swap to '"+args[0]+"'");
                    return true;
                })
            );
    }
}
