package org.lime.gp;

import org.bukkit.permissions.ServerOperator;
import org.lime.plugin.CoreElement;
import org.lime.system.toast.*;

import java.util.Arrays;
import java.util.stream.Collectors;

public class TestData {
    public enum DataType {
        SQL,
        Other,
        Off;

        public boolean isSQL() { return this == SQL; }


        public boolean isOther() { return this == Other; }

        public boolean isOff() { return this == Off; }
    }

    public static final LockToast1<DataType> ENABLE_TYPE = Toast.lock(DataType.Off);

    public static CoreElement create() {
        return CoreElement.create(TestData.class)
                .addCommand("test.data", v -> v
                        .withCheck(ServerOperator::isOp)
                        .withUsage("/test.data ["+Arrays.stream(DataType.values()).map(Enum::name).map(String::toLowerCase).collect(Collectors.joining("|"))+"]")
                        .withTab(Arrays.stream(DataType.values()).map(Enum::name).map(String::toLowerCase).toList())
                        .withExecutor((s, args) -> {
                            if (args.length != 1) return false;
                            ENABLE_TYPE.set0(DataType.valueOf(args[0]));
                            s.sendMessage("Set test data: " + ENABLE_TYPE.get0());
                            return true;
                        })
                );
    }
}































