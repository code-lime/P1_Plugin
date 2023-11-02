package org.lime.gp.database.mysql;

import org.lime.system.execute.Action0;
import org.lime.system.execute.Action1;

public class ExecuteData {
    private Action1<String> onSQL = null;
    private Action1<String> onLog = null;
    private Action1<Throwable> onError = null;
    private Action0 onFinally = null;

    public ExecuteData withSQL(Action1<String> onSQL) { this.onSQL = onSQL; return this; }
    public ExecuteData withLog(Action1<String> onLog) { this.onLog = onLog; return this; }
    public ExecuteData withError(Action1<Throwable> onError) { this.onError = onError; return this; }
    public ExecuteData withFinally(Action0 onFinally) { this.onFinally = onFinally; return this; }

    public void onSQL(String sql) { if (this.onSQL != null) this.onSQL.invoke(sql); }
    public void onLog(String log) { if (this.onLog != null) this.onLog.invoke(log); }
    public void onError(Throwable error) { if (this.onError != null) this.onError.invoke(error); }
    public void onFinally() { if (this.onFinally != null) this.onFinally.invoke(); }
}