package org.lime.gp.block.component.display;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.*;
import org.lime.system;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Deprecated
public class ChunkSectionData {
    public int bottomBlockY;
    public short nonEmptyBlockCount;
    public DataPaletteBlock<IBlockData> states;
    public AnyData biomes;
    public ChunkSectionData(int sectionID, PacketDataSerializer buf) {
        this.bottomBlockY = ChunkSection.getBottomBlockY(sectionID);
        this.nonEmptyBlockCount = buf.readShort();
        this.states = new DataPaletteBlock<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), DataPaletteBlock.e.SECTION_STATES, null);
        this.biomes = new AnyData(buf);
    }

    /*public static class BlockData {
        public final Registry<IBlockData> REGISTRY = Block.BLOCK_STATE_REGISTRY;
        public IBlockData[] blocks = new IBlockData[16*16*16];

        public record Data(@M(o="a") DataPalette.a factory, @M(o="b") int bits) {
            @M(o="a")
            public DataPaletteBlock.c<T> createData(Registry<T> idList, DataPaletteExpandable<T> listener, int size) {
                DataBits bitStorage = this.bits == 0 ? new ZeroBitStorage(size) : new SimpleBitStorage(this.bits, size);
                DataPalette<T> palette = this.factory.create(this.bits, idList, listener, List.of());
                return new DataPaletteBlock.c<T>(this, bitStorage, palette);
            }
        }
        public static Data getConfiguration(Registry<A> idList, int bits) {
            return switch (bits) {
                case 0 -> new Data(DataPaletteBlock.e.SINGLE_VALUE_PALETTE_FACTORY, bits);
                case 1, 2, 3, 4 -> new Data(DataPaletteBlock.e.LINEAR_PALETTE_FACTORY, 4);
                case 5, 6, 7, 8 -> new Data(DataPaletteBlock.e.HASHMAP_PALETTE_FACTORY, bits);
                default -> new Data(DataPaletteBlock.e.GLOBAL_PALETTE_FACTORY, MathHelper.ceillog2(idList.size()));
            };
        }

        public BlockData(PacketDataSerializer buf, system.Func4<Integer, Integer, Integer, IBlockData, IBlockData> modify) {
            int bits = buf.readByte();

            var f = DataPaletteBlock.e.SECTION_STATES.getConfiguration(REGISTRY, bits);

            IBlockData[] pallete;
            if (bits == 0) {
                pallete = new IBlockData[] { REGISTRY.byIdOrThrow(buf.readVarInt()) };

                for (int y = 0; y < 16; y++) {
                    int index_y = y * 256;
                    for (int z = 0; z < 16; z++) {
                        int index_yz = index_y + z * 16;
                        for (int x = 0; x < 16; x++) {
                            int index_xyz = x + index_yz;
                            blocks[index_xyz] = modify.invoke(x, y, z, pallete[0]);
                        }
                    }
                }
            }
            else if (bits <= 8) {
                if (bits <= 4) bits = 4;
                int length = buf.readVarInt();
                pallete = new IBlockData[length];
                for (int i = 0; i < length; i++) pallete[i] = REGISTRY.byIdOrThrow(buf.readVarInt());

                SimpleBitStorage storage = new SimpleBitStorage(bits, 16 * 16 * 16, buf.readLongArray());
                for (int y = 0; y < 16; y++) {
                    int index_y = y * 256;
                    for (int z = 0; z < 16; z++) {
                        int index_yz = index_y + z * 16;
                        for (int x = 0; x < 16; x++) {
                            int index_xyz = x + index_yz;
                            blocks[index_xyz] = modify.invoke(x, y, z, pallete[storage.get(index_xyz)]);
                        }
                    }
                }
            } else {
                SimpleBitStorage storage = new SimpleBitStorage(bits, 16 * 16 * 16, buf.readLongArray());
                for (int y = 0; y < 16; y++) {
                    int index_y = y * 256;
                    for (int z = 0; z < 16; z++) {
                        int index_yz = index_y + z * 16;
                        for (int x = 0; x < 16; x++) {
                            int index_xyz = x + index_yz;
                            blocks[index_xyz] = modify.invoke(x, y, z, REGISTRY.byIdOrThrow(storage.get(index_xyz)));
                        }
                    }
                }
            }
        }
        private static long getIndex(int x, int y, int z) {
            return ((long)y * 16 * 16) + (long)z * 16 + (long)x;
        }

        public system.Toast2<Integer, system.Action1<PacketDataSerializer>> serialize() {
            if (this.blocks == null) {
                return system.toast(1 + PacketDataSerializer.getVarIntSize(0) + PacketDataSerializer.getVarIntSize(0), buf -> {
                    buf.writeByte(0);
                    buf.writeVarIntArray(new int[0]);
                    buf.writeLongArray(new long[0]);
                });
            }

            List<IBlockData> pallete_blocks = new ArrayList<>();
            int blocks_length = this.blocks.length;
            int[] blocks = new int[blocks_length];
            for (IBlockData block : this.blocks) {
                int index = pallete_blocks.indexOf(block);
                if (index == -1) {
                    index = pallete_blocks.size();
                    pallete_blocks.add(block);
                }
                blocks[index] = index;
            }
            int pallete_length = pallete_blocks.size();
            int[] pallete = new int[pallete_length];
            int size = 1 + PacketDataSerializer.getVarIntSize(pallete_length);
            for (int i = 0; i < pallete_length; i++) {
                int index = REGISTRY.getId(pallete_blocks.get(i));
                size += PacketDataSerializer.getVarIntSize(index);
                pallete[i] = index;
            }
            int bits = lengthBits(pallete_blocks.size());
            SimpleBitStorage storage = new SimpleBitStorage(bits, 16 * 16 * 16);
            for (int i = 0; i < blocks_length; i++) storage.set(i, blocks[i]);
            size += PacketDataSerializer.getVarIntSize(blocks_length) + blocks_length * 8;
            return system.toast(size, buf -> {
                buf.writeByte(bits);
                buf.writeVarIntArray(pallete);
                buf.writeLongArray(storage.getRaw());
            });
        }
    }*/
    public static class AnyData {
        public int bits;
        public int[] pallete;
        public long[] raw;

        public AnyData(PacketDataSerializer buf) {
            bits = buf.readByte();
            pallete = buf.readVarIntArray();
            raw = buf.readLongArray();
        }

        public system.Toast2<Integer, system.Action1<PacketDataSerializer>> serialize() {
            int pallete_length = pallete.length;
            int size = 1 + PacketDataSerializer.getVarIntSize(pallete_length);
            for (int j : pallete) size += PacketDataSerializer.getVarIntSize(j);
            size += PacketDataSerializer.getVarIntSize(raw.length) + raw.length * 8;
            return system.toast(size, buf -> {
                buf.writeByte(bits);
                buf.writeVarIntArray(pallete);
                buf.writeLongArray(raw);
            });
        }
    }

    public system.Toast2<Integer, system.Action1<PacketDataSerializer>> serialize() {
        system.Toast2<Integer, system.Action1<PacketDataSerializer>> biomes = this.biomes.serialize();
        return system.toast(2 + this.states.getSerializedSize() + biomes.val0, buf -> {
            buf.writeShort(nonEmptyBlockCount);
            this.states.write(buf);
            biomes.val1.invoke(buf);
        });
    }
    public static system.Toast2<Integer, system.Action1<PacketDataSerializer>> combine(Collection<ChunkSectionData> collection) {
        int sum = 0;
        List<system.Action1<PacketDataSerializer>> items = new ArrayList<>();
        for (ChunkSectionData item : collection) {
            system.Toast2<Integer, system.Action1<PacketDataSerializer>> dat = item.serialize();
            sum += dat.val0;
            items.add(dat.val1);
        }
        return system.toast(sum, buf -> items.forEach(item -> item.invoke(buf)));
    }
    /* 
    private static int lengthBits(int val) {
        return Math.max(Integer.SIZE - Integer.numberOfLeadingZeros(val), 1);
    }*/
}
















