package org.lime.gp.player.selector;

import java.util.Optional;

public enum SelectorType {
    Main("main"),
    ZoneMain("zone_main"),
    ZoneReadonly("zone_readonly");

    private final String name;

    SelectorType(String name) {
        this.name = name;
    }

    public static Optional<SelectorType> of(String name) {
        for (SelectorType type : SelectorType.values())
            if (type.name.equals(name))
                return Optional.of(type);
        return Optional.empty();
    }

    public String getName() {
        return name;
    }
}
