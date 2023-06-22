package org.lime.gp.item.settings;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Setting.Any.class)
public @interface Setting {
    @Retention(RetentionPolicy.RUNTIME) @interface Any {
        Setting[] value();
    }

    String name();
}