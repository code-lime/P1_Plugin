package org.lime.gp.filter;

import org.lime.gp.filter.data.IFilterData;
import org.lime.gp.filter.data.IFilterInfo;

import java.util.regex.Pattern;

public interface IFilter<T extends IFilterData<T>> {
    boolean isFilter(T data);

    static <T extends IFilterData<T>>IFilter<T> parse(IFilterInfo<T> filterInfo, String value) {
        if (value.endsWith("]")) value = value.substring(0, value.length() - 1);
        String[] args = value.split(Pattern.quote("["), 2);
        String key = args[0];
        boolean inverse = key.startsWith("!");
        if (inverse) key = key.substring(1);
        IFilter<T> filter = switch (key) {
            case "any" -> new AnyFilter<>(filterInfo, args[1]);
            case "all" -> new AllFilter<>(filterInfo, args[1]);
            case "block" -> new BlockFilter<>(filterInfo, args[1]);
            case "debug" -> new DebugFilter<>(filterInfo, args[1]);
            case "random" -> new RandomFilter<>(filterInfo, args[1]);
            case "tag" -> new TagFilter<>(filterInfo, args[1]);
            case "true" -> new StaticFilter<>(true);
            case "false" -> new StaticFilter<>(false);
            default -> throw new IllegalArgumentException("SpawnFilter type '" + key + "' not founded!");
        };
        return inverse ? new InverseFilter<>(filter) : filter;
    }
}
