package org.lime.gp.extension;

import net.minecraft.nbt.*;
import org.lime.reflection;
import org.lime.system.toast.*;
import org.lime.system.execute.*;

public class NbtTagModify implements TagVisitor {
    private static final reflection.field<String> NBT_STRING_FIELD = reflection.field.<String>ofMojang(NBTTagString.class, "data").nonFinal();
    private final Func1<String, String> modify;
    public NbtTagModify(Func1<String, String> modify) {
        this.modify = modify;
    }

    public NBTBase visit(NBTBase element) {
        element = element.copy();
        element.accept(this);
        return element;
    }

    @Override public void visitString(NBTTagString element) {
        NBT_STRING_FIELD.set(element, modify.invoke(NBT_STRING_FIELD.get(element)));
    }

    @Override public void visitByte(NBTTagByte element) { }
    @Override public void visitShort(NBTTagShort element) { }
    @Override public void visitInt(NBTTagInt element) { }
    @Override public void visitLong(NBTTagLong element) {}
    @Override public void visitFloat(NBTTagFloat element) {}
    @Override public void visitDouble(NBTTagDouble element) {}

    @Override public void visitByteArray(NBTTagByteArray element) {}
    @Override public void visitIntArray(NBTTagIntArray element) {}
    @Override public void visitLongArray(NBTTagLongArray element) {}
    @Override public void visitList(NBTTagList element) {
        int size = element.size();
        for (int i = 0; i < size; i++)
            element.setTag(i, this.visit(element.get(i)));
    }
    @Override public void visitCompound(NBTTagCompound compound) {
        for (String key : compound.tags.keySet().toArray(String[]::new))
            compound.put(key, this.visit(compound.get(key)));
    }
    @Override public void visitEnd(NBTTagEnd element) { }
}


