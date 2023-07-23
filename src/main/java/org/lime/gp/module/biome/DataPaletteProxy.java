package org.lime.gp.module.biome;

import net.minecraft.util.DataBits;
import net.minecraft.world.level.chunk.DataPalette;
import net.minecraft.world.level.chunk.DataPaletteBlock;
import org.lime.reflection;

import java.util.Map;

public class DataPaletteProxy<T> {
    private final DataPaletteBlock<T> handle;
    private static final reflection.field<Object> data_DataPaletteBlock = reflection.dynamic.ofStatic(DataPaletteBlock.class).getMojangField("data");
    private static final reflection.field<DataBits> storage_data_DataPaletteBlock = reflection.dynamic.ofStatic(data_DataPaletteBlock.field.getType()).getMojangField("storage");
    private static final reflection.field<DataPalette<?>> palette_data_DataPaletteBlock = reflection.dynamic.ofStatic(data_DataPaletteBlock.field.getType()).getMojangField("palette");
    private static final reflection.field<DataPaletteBlock.d> strategy_DataPaletteBlock = reflection.dynamic.ofStatic(DataPaletteBlock.class).getMojangField("strategy");

    private DataBits storage() { return storage_data_DataPaletteBlock.get(handle); }
    private DataPalette<T> palette() { return (DataPalette<T>)palette_data_DataPaletteBlock.get(handle); }
    private DataPaletteBlock.d strategy() { return strategy_DataPaletteBlock.get(handle); }

    private DataPaletteProxy(DataPaletteBlock<T> handle) { this.handle = handle; }
    public static <T>DataPaletteProxy<T> of(DataPaletteBlock<T> handle) { return new DataPaletteProxy<>(handle); }

    public void setPaletteMapper(Map<Integer, Integer> mapper) {

    }
    /*public void set(int x2, int y2, int z2, T value) {
        this.acquire();
        try {
            this.set(this.strategy.getIndex(x2, y2, z2), value);
        }
        finally {
            this.release();
        }
    }


    public void mapAll(Map<Integer, Integer> mapper) {

    }

    public void modifySingle(int x, int y, int z, system.Func1<Integer, Integer> mapper) {
        handle.acquire();
        try {
            int index = strategy().getIndex(x, y, z);
            DataBits storage = storage();
            storage.set(index, mapper.invoke(storage.get(index)));
        }
        finally {
            handle.release();
        }
    }
    public void modifyAll(system.Func1<Integer, Integer> mapper) {
        handle.acquire();
        try {
            //this.data.storage.forEach((location, data) -> consumer.accept(this.data.palette.valueFor(data), location));
            storage().forEach((location, data) -> {
                palette().valueFor()
                mapper.invoke(data);
            });
            storage().forEach();
            this.set(this.strategy.getIndex(x2, y2, z2), value);
        }
        finally {
            handle.release();
        }
    }*/
}
