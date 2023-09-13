package org.lime.gp.filter;

import org.lime.gp.filter.data.AppendFunction;
import org.lime.gp.filter.data.IFilterData;
import org.lime.gp.filter.data.IFilterInfo;
import org.lime.gp.lime;

import java.util.ArrayList;
import java.util.List;

public record DebugFilter<T extends IFilterData<T>>(IFilterInfo<T> filterInfo, String prefix) implements IFilter<T> {
    public boolean isFilter(T data) {
        List<String> params = new ArrayList<>();
        params.add("List '" + prefix + "' of filter:");
        AppendFunction function = AppendFunction.of(params);
        filterInfo.getAllParams().forEach(info -> info.appendTo(data, function));
        lime.logOP(String.join("\n - ", params));
        return true;
    }
}