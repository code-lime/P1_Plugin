function create(size, func, separator) {
    var list = [];
    for (var i = 0; i < size; i++) list.push(func(i));
    return list.join(separator)
}
function createEvent(index) {
    if (index === 0) return "";
    var type = create(index, function(i) { return "T" + i; }, ", ");
    var targs = create(index, function(i) { return "T"+i+" val"+i; }, ", ");
    var args = create(index, function(i) { return "val" + i; }, ", ");

    var tt = "T0";
    for (var i = 1; i < index; i++) tt = "system.Toast2<"+tt+","+"T"+i+">";
    var line = "    public static <"+type+">void addEvent(String event, type other, system.Func1<Builder<Player>, Builder<";
    line += tt + ">> build, system.Action"+index+"<"+type+"> execute) {\n";
    line += "        addEvent(event, other, build, (system.Action1<"+tt+">)execute);\n"
    line += "    }";
    return line;
}
var res = [];
for (var i = 2; i < 10; i++) res.push(createEvent(i));
res.join("\n");