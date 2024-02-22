package org.lime.gp.docs;

import org.lime.display.models.shadow.serialize.IDocsTypes;
import org.lime.docs.IIndexDocs;

public class TempDocsTypes implements IDocsTypes {
    private final IIndexDocs temp;
    public TempDocsTypes(IIndexDocs temp) { this.temp = temp; }

    @Override public IIndexDocs vector() { return temp; }
    @Override public IIndexDocs transformation() { return temp; }
    @Override public IIndexDocs material() { return temp; }
    @Override public IIndexDocs nbt() { return temp; }
    @Override public IIndexDocs item() { return temp; }
    @Override public IIndexDocs text() { return temp; }
    @Override public IIndexDocs modelKey() { return temp; }

    @Override public IIndexDocs smoothMove() { return temp; }
    @Override public IIndexDocs billboard() { return temp; }
    @Override public IIndexDocs color() { return temp; }
    @Override public IIndexDocs animation() { return temp; }
    @Override public IIndexDocs entity() { return temp; }
    @Override public IIndexDocs itemDisplay() { return temp; }
    @Override public IIndexDocs itemSlot() { return temp; }
    @Override public IIndexDocs align() { return temp; }
}
