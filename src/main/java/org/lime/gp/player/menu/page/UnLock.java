package org.lime.gp.player.menu.page;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.EnumHand;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.database.rows.UserRow;
import org.lime.gp.extension.Cooldown;
import org.lime.gp.extension.inventory.ReadonlyInventory;
import org.lime.gp.item.Items;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.list.DeKeySetting;
import org.lime.gp.lime;
import org.lime.gp.module.JavaScript;
import org.lime.gp.player.inventory.gui.InterfaceManager;
import org.lime.gp.player.menu.ActionSlot;
import org.lime.gp.player.menu.Logged;
import org.lime.gp.player.perm.Perms;
import org.lime.gp.player.ui.ImageBuilder;
import org.lime.gp.sound.Sounds;
import org.lime.system.map;
import org.lime.system.toast.*;
import org.lime.system.execute.*;
import org.lime.system.utils.RandomUtils;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class UnLock extends Base {
    public String soundCrash;
    public String soundProgress;
    public String soundOpen;

    public List<Toast2<String, UnLockData>> unlock = new ArrayList<>();
    public List<ActionSlot> skip = new ArrayList<>();

    public UnLock(JsonObject json) {
        super(json);
        json.get("unlock")
                .getAsJsonObject()
                .entrySet()
                .forEach(kv -> unlock.add(Toast.of(kv.getKey(), new UnLockData(this, kv.getValue().getAsJsonObject()))));
        if (json.has("skip")) json.get("skip").getAsJsonArray().forEach(kv -> skip.add(ActionSlot.parse(this, kv.getAsJsonObject())));
        if (json.has("sound")) {
            JsonObject sound = json.getAsJsonObject("sound");
            soundCrash = sound.has("crash") ? sound.get("crash").getAsString() : null;
            soundProgress = sound.has("progress") ? sound.get("progress").getAsString() : null;
            soundOpen = sound.has("open") ? sound.get("open").getAsString() : null;
        } else {
            soundCrash = null;
            soundProgress = null;
            soundOpen = null;
        }
    }

    @Override protected void showGenerate(UserRow row, Player player, int page, Apply apply) {
        if (player == null) {
            lime.logOP("Menu '"+getKey()+"' not called! User is NULL");
            return;
        }
        apply.get("block_pos_x").ifPresent(x -> apply.get("block_pos_y").ifPresent(y -> apply.get("block_pos_z").ifPresent(z -> {
            Location blockLocation = new Location(player.getWorld(), Integer.parseInt(x), Integer.parseInt(y), Integer.parseInt(z));
            for (Toast2<String, UnLockData> item : unlock) {
                if (JavaScript.isJsTrue(ChatHelper.formatText(item.val0, apply)).filter(_v -> _v).isEmpty()) continue;
                open(player, apply, item.val1, blockLocation);
                return;
            }
            skip.forEach(i -> i.invoke(player, apply, true));
        })));
    }

    private static boolean useDeKey(EntityHuman entityhuman) {
        ItemStack deKey = entityhuman.getItemInHand(EnumHand.MAIN_HAND);
        if (Items.getItemCreator(deKey)
                .map(v -> v instanceof ItemCreator c ? c : null)
                .filter(v -> v.has(DeKeySetting.class))
                .filter(v -> Perms.getCanData(entityhuman.getUUID()).isCanUse(v.getKey()))
                .isEmpty()
        ) return false;
        deKey.shrink(1);
        entityhuman.setItemInHand(EnumHand.MAIN_HAND, deKey);
        return true;
    }
    private void open(Player player, Apply apply, UnLockData data, Location location) {
        if (!(player instanceof CraftPlayer cplayer)) return;
        EntityHuman human = cplayer.getHandle();
        if (!useDeKey(human)) return;
        ReadonlyInventory view = ReadonlyInventory.ofNMS(NonNullList.withSize((data.isSmall ? 4 : 6) * 9, net.minecraft.world.item.ItemStack.EMPTY));
        human.openMenu(new UnLockInventory(
                location, data.isSmall,
                soundCrash, soundProgress, soundOpen,
                view, () -> data.done.forEach(slot -> slot.invoke(player, apply, true))
        ));
    }
    private record UnLockData(boolean isSmall, List<ActionSlot> done) {
        public UnLockData(Logged.ILoggedDelete base, JsonObject json) {
            this(json.get("small").getAsBoolean(), new ArrayList<>());
            json.getAsJsonArray("done").forEach(item -> done.add(ActionSlot.parse(base, item.getAsJsonObject())));
        }
    }
    private static class UnLockInventory implements ITileInventory {
        public final boolean isSmall;
        public final List<Integer> values;
        public final int[] progress = new int[9];
        public final int[] data;
        private IChatBaseComponent title;
        private final ReadonlyInventory view;

        private final Location location;
        private final String soundCrash;
        private final String soundProgress;
        private final String soundOpen;
        private final Action0 callbackOpen;

        public UnLockInventory(Location location, boolean isSmall, String soundCrash, String soundProgress, String soundOpen, ReadonlyInventory view, Action0 callbackOpen) {
            this.isSmall = isSmall;
            this.view = view;
            this.soundCrash = soundCrash;
            this.soundProgress = soundProgress;
            this.soundOpen = soundOpen;
            this.callbackOpen = callbackOpen;
            this.location = location.toCenterLocation();
            this.values = isSmall ? Arrays.asList(-1, 1) : Arrays.asList(-2,-1,1,2);
            this.title = createTitle(isSmall);
            this.data = IntStream.range(0, 9).map(v -> RandomUtils.rand(values)).toArray();
            this.preview = map.<Integer, ItemStack>of()
                    .add((isSmall
                            ? Stream.of(Toast.of(1, 10015), Toast.of(0, 0), Toast.of(-1, 10016))
                            : Stream.of(Toast.of(2, 10011), Toast.of(1, 10012), Toast.of(0, 0), Toast.of(-1, 10013), Toast.of(-2, 10014))
                    ).toList(), kv -> kv.val0, kv -> {
                        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.BARRIER);
                        ItemMeta meta = item.getItemMeta();
                        meta.setCustomModelData(kv.val1);
                        meta.displayName(Component.text("Использовать").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
                        item.setItemMeta(meta);
                        return CraftItemStack.asNMSCopy(item);
                    })
                    .build();
        }

        public final Map<Integer, ItemStack> preview;
        @Override public Container createMenu(int syncId, PlayerInventory inventory, EntityHuman target) {
            ITileInventory _this = this;
            return new ContainerChest((isSmall ? Containers.GENERIC_9x4 : Containers.GENERIC_9x6), syncId, inventory, view, isSmall ? 4 : 6) {
                public void progress(boolean crash) {
                    if (crash) {
                        Sounds.playSound(soundCrash, location);
                        if (!useDeKey(target)) {
                            target.closeContainer();
                            return;
                        }
                    } else {
                        Sounds.playSound(soundProgress, location);
                    }
                    title = createTitle(isSmall);
                    target.openMenu(_this);
                }
                public void open() {
                    Sounds.playSound(soundOpen, location);
                    callbackOpen.invoke();
                    target.closeContainer();
                }
                @Override protected Slot addSlot(Slot slot) {
                    if (slot.container == view) {
                        return super.addSlot(new InterfaceManager.AbstractBaseSlot(slot) {
                            @Override public boolean isPacketOnly() { return true; }

                            @Override public void onSlotClick(EntityHuman player, InventoryClickType type, ClickType click) {
                                if (Cooldown.hasOrSetCooldown(player.getUUID(), "safe_box.click", 0.1)) return;
                                int x = getRowX();
                                int y = getRowY();
                                if (y == (isSmall ? 3 : 5)) return;
                                if (progress[x] == 0 && (x <= 0 || progress[x - 1] != 0)) {
                                    int delta = (isSmall ? 1 : 2) - y;
                                    if (delta != 0) {
                                        if (data[x] == delta) {
                                            progress[x] = delta;
                                            progress(false);
                                        }
                                        else progress(true);
                                    }
                                    if (x == 8 && progress[x] != 0) open();
                                }
                            }

                            @Override public net.minecraft.world.item.ItemStack getItem() {
                                int x = getRowX();
                                int y = getRowY();
                                if (progress[x] == 0 && (x <= 0 || progress[x - 1] != 0)) {
                                    int delta = (isSmall ? 1 : 2) - y;
                                    if (delta == 0) return ItemStack.EMPTY;
                                    return preview.getOrDefault(delta, ItemStack.EMPTY);
                                }
                                return ItemStack.EMPTY;
                            }
                            @Override public boolean mayPickup(EntityHuman playerEntity) { return false; }
                            @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack) { return false; }
                        });
                    }
                    return InterfaceManager.AbstractSlot.noneInteractSlot(super.addSlot(slot));
                }
                @Override public boolean stillValid(EntityHuman player) {
                    return player.distanceToSqr(location.getX(), location.getY(), location.getZ()) <= 64;
                }
            };
        }

        private IChatBaseComponent createTitle(boolean isSmall) {
            List<ImageBuilder> components = new ArrayList<>();
            components.add(ImageBuilder.of(isSmall ? 0xE740 : 0xE746, 176).addOffset(81));
            int distance = -1;
            for (int i = 0; i < 9; i++) {
                if (progress[i] != 0) distance = i;
                if (isSmall) {
                    components.add(ImageBuilder.of(switch (progress[i]) {
                        case -1 -> 0xE73F;
                        default -> 0xE73E;
                        case 1 -> 0xE73D;
                    }, 14).addOffset(9 + 18 * i));
                } else {
                    components.add(ImageBuilder.of(switch (progress[i]) {
                        case -2 -> 0xE745;
                        case -1 -> 0xE744;
                        default -> 0xE743;
                        case 1 -> 0xE742;
                        case 2 -> 0xE741;
                    }, 14).addOffset(9 + 18 * i));
                }
            }
            distance++;
            for (int i = 0; i < 9; i++) {
                int delta = distance - i;
                if (delta < 0) continue;
                components.add((switch (delta) {
                    case 0 -> ImageBuilder.of(isSmall ? 0xE73C : 0xE74A, 12).addOffset(-3);
                    case 1, 2, 3 -> ImageBuilder.of(isSmall ? 0xE73B : 0xE749, 18);
                    case 4 -> ImageBuilder.of(isSmall ? 0xE73A : 0xE748, 18);
                    default -> ImageBuilder.of(isSmall ? 0xE739 : 0xE747, 18);
                }).addOffset(9 + 18 * i));
            }
            return ChatHelper.toNMS(ImageBuilder.join(components).color(NamedTextColor.WHITE));
        }

        @Override public IChatBaseComponent getDisplayName() {
            return title;
        }
    }
}
