package org.lime.gp.filter;

import org.lime.gp.filter.data.IFilterData;
import org.lime.gp.filter.data.IFilterInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class TagFilter<T extends IFilterData<T>> implements IFilter<T> {
    public List<String> tags = new ArrayList<>();
    public TagFilter(IFilterInfo<T> filterInfo, String argLine) { tags.addAll(Arrays.asList(argLine.split(Pattern.quote(";")))); }
    @Override public boolean isFilter(T loot) { return loot.tags().map(v -> v.containsAll(tags)).orElse(false); }
}
