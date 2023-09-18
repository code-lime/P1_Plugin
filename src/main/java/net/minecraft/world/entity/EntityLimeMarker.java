package net.minecraft.world.entity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.LimeKey;
import net.minecraft.world.level.World;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import org.bukkit.craftbukkit.v1_20_R1.persistence.CraftPersistentDataContainer;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EntityLimeMarker extends Marker {
    public CraftPersistentDataContainer persistentDataContainer;

    private EntityInLevelCallback proxy(EntityInLevelCallback base) {
        return new EntityInLevelCallback() {
            @Override public void onMove() { base.onMove(); }
            @Override public void onRemove(RemovalReason reason) {
                EntityMarkerEventDestroy.execute(EntityLimeMarker.this, reason);
                base.onRemove(reason);
            }
        };
    }

    public EntityLimeMarker(EntityTypes<?> type, World world) {
        super(type, world);
        persistentDataContainer = this.getBukkitEntity().getPersistentDataContainer();
        setLevelCallback(EntityInLevelCallback.NULL);
    }

    @Nullable private LimeKey key = null;

    public Optional<LimeKey> customKey() { return Optional.ofNullable(key); }
    public Optional<UUID> customUUID() { return customKey().map(LimeKey::uuid); }
    public Optional<String> customType() { return customKey().map(LimeKey::type); }

    @Override public void setLevelCallback(EntityInLevelCallback changeListener) {
        super.setLevelCallback(proxy(changeListener));
        key = LimeKey.getKey(persistentDataContainer, LimeKey.KeyType.CUSTOM_ENTITY).orElse(null);
    }

    @Override public void load(NBTTagCompound nbt) {
        super.load(nbt);
        key = LimeKey.getKey(persistentDataContainer, LimeKey.KeyType.CUSTOM_ENTITY).orElse(null);
    }
    @Override public void restoreFrom(Entity original) {
        super.restoreFrom(original);
        persistentDataContainer = this.getBukkitEntity().getPersistentDataContainer();
    }


    private final Map<MinecraftKey, Object> metadata = new ConcurrentHashMap<>();
    public Optional<Object> getMetadata(MinecraftKey key) {
        return Optional.ofNullable(metadata.get(key));
    }
    public Object setMetadata(MinecraftKey key, Object value) {
        return value == null ? metadata.remove(key) : metadata.put(key, value);
    }
    public Object removeMetadata(MinecraftKey key) {
        return metadata.remove(key);
    }
}












