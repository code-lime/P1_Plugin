package org.lime.gp.filter.data;

import java.util.List;
import java.util.Map;

public interface AppendFunction {
    void appendNan(String name);

    void appendWithoutValue(String name);

    void appendValue(String name, String value);

    static AppendFunction of(List<String> list) {
        return new AppendFunction() {
            @Override
            public void appendNan(String name) {
                list.add("!" + name);
            }

            @Override
            public void appendWithoutValue(String name) {
                list.add(name);
            }

            @Override
            public void appendValue(String name, String value) {
                list.add(name + "=" + value);
            }
        };
    }

    static AppendFunction of(Map<String, String> map) {
        return new AppendFunction() {
            @Override
            public void appendNan(String name) {
                map.put(name, "false");
            }

            @Override
            public void appendWithoutValue(String name) {
                map.put(name, "true");
            }

            @Override
            public void appendValue(String name, String value) {
                map.put(name, value);
            }
        };
    }
}
