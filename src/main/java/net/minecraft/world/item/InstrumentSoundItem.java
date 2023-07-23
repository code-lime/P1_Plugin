package net.minecraft.world.item;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.TagKey;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.Iterator;
import java.util.Optional;

public class InstrumentSoundItem extends InstrumentItem {
    private interface IInstrument {
        int useDuration();
        void play(World world, EntityHuman player);

        static IInstrument of(Instrument instrument) {
            return new IInstrument() {
                @Override public int useDuration() { return instrument.useDuration(); }
                @Override public void play(World world, EntityHuman player) {
                    SoundEffect soundEvent = instrument.soundEvent().value();
                    float volume = instrument.range() / 16.0f;
                    world.playSound(player, player, soundEvent, SoundCategory.RECORDS, volume, 1.0f);
                    world.gameEvent(GameEvent.INSTRUMENT_PLAY, player.position(), GameEvent.a.of(player));
                }
            };
        }
        static IInstrument of(String sound, float range, int cooldown) {
            return new IInstrument() {
                @Override public int useDuration() { return cooldown; }
                @Override public void play(World world, EntityHuman player) {
                    world.getWorld()
                            .playSound(player.getBukkitEntity(), sound, org.bukkit.SoundCategory.RECORDS, range / 16.0f, 1.0f);
                    world.gameEvent(GameEvent.INSTRUMENT_PLAY, player.position(), GameEvent.a.of(player));
                }
            };
        }
    }

    private static final String TAG_INSTRUMENT = "instrument";
    private static final String TAG_CUSTOM = "custom";

    private static final String TAG_CUSTOM_SOUND = "sound";
    private static final String TAG_CUSTOM_RANGE = "range";
    private static final String TAG_CUSTOM_COOLDOWN = "cooldown";

    private final TagKey<Instrument> instruments;
    public InstrumentSoundItem(Info settings, TagKey<Instrument> instrumentTag) {
        super(settings, instrumentTag);
        this.instruments = instrumentTag;
    }

    @Override public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        Optional<IInstrument> optional = this.getInstrument(itemStack);
        if (optional.isEmpty()) return InteractionResultWrapper.fail(itemStack);
        IInstrument instrument = optional.get();
        user.startUsingItem(hand);
        instrument.play(world, user);
        user.getCooldowns().addCooldown(this, instrument.useDuration());
        user.awardStat(StatisticList.ITEM_USED.get(this));
        return InteractionResultWrapper.consume(itemStack);
    }
    @Override public int getUseDuration(ItemStack stack) {
        return this.getInstrument(stack).map(IInstrument::useDuration).orElse(0);
    }
    public static void setInstrument(ItemStack item, String sound, float range, int cooldown) {
        setInstrument(item.getOrCreateTag(), sound, range, cooldown);
    }
    public static void setInstrument(NBTTagCompound itemTag, String sound, float range, int cooldown) {
        NBTTagCompound custom = itemTag.contains(TAG_CUSTOM, NBTBase.TAG_COMPOUND) ? itemTag.getCompound(TAG_CUSTOM) : new NBTTagCompound();
        custom.putString(TAG_CUSTOM_SOUND, sound);
        custom.putFloat(TAG_CUSTOM_RANGE, range);
        custom.putInt(TAG_CUSTOM_COOLDOWN, cooldown);
        itemTag.put(TAG_CUSTOM, custom);
        itemTag.putString(TAG_INSTRUMENT, "none");
    }
    private Optional<IInstrument> getInstrument(ItemStack stack) {
        NBTTagCompound compoundTag = stack.getTag();
        if (compoundTag != null) {
            if (compoundTag.contains(TAG_CUSTOM, NBTBase.TAG_COMPOUND)) {
                NBTTagCompound custom = compoundTag.getCompound(TAG_CUSTOM);
                if (custom.contains(TAG_CUSTOM_SOUND, NBTBase.TAG_STRING)
                        && custom.contains(TAG_CUSTOM_RANGE, NBTBase.TAG_FLOAT)
                        && custom.contains(TAG_CUSTOM_COOLDOWN, NBTBase.TAG_INT)) {
                    return Optional.of(IInstrument.of(custom.getString(TAG_CUSTOM_SOUND), custom.getFloat(TAG_CUSTOM_RANGE), custom.getInt(TAG_CUSTOM_COOLDOWN)));
                }
                return Optional.empty();
            } else if (compoundTag.contains(TAG_INSTRUMENT, NBTBase.TAG_STRING)) {
                try {
                    MinecraftKey location = MinecraftKey.tryParse(compoundTag.getString(TAG_INSTRUMENT));
                    return BuiltInRegistries.INSTRUMENT.getOptional(ResourceKey.create(Registries.INSTRUMENT, location)).map(IInstrument::of);
                } catch (Exception ignored) { }
                return Optional.empty();
            }
        }
        Iterator<Holder<Instrument>> iterator = BuiltInRegistries.INSTRUMENT.getTagOrEmpty(this.instruments).iterator();
        return iterator.hasNext() ? Optional.of(iterator.next()).map(Holder::value).map(IInstrument::of) : Optional.empty();
    }
}
