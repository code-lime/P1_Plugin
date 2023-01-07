package p1.blocks;

import com.google.gson.JsonObject;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.lime.core;
import p1.ItemManager;
import PopulateLootEvent;
import lime;

import java.util.Optional;

public class StemBlockData implements Listener {
    public static core.element create() {
        /*ItemManager.addHardcodeItem("stem.temp", system.json.object()
                .add("item", Material.MELON_SEEDS.name())
                .add("id", -6)
                .add("name", "Тест семена")
                .addObject("settings", v -> v
                        .addObject("block", _v -> _v
                                .add("material", Material.MELON_STEM)
                                .add("class", StemBlock.class.getName())
                        )
                        .addObject("stem", _v -> _v
                                .add("key", "TEMP_STEM")
                                .add("result", "RESULT_STEM")
                        )
                )
                .build()
        );*/
        return core.element.create(StemBlockData.class)
                .withInstance()
                .disable()
                .withInit(StemBlockData::init);
    }
    public static void init() {
        CustomMeta.loadMeta(StemBlock.class, CustomMeta.LoadedBlock.class, v -> v.withFilter(StemBlock::filter));
    }
    public static class StemBlock extends ItemManager.BlockSetting.IBlock<JsonObject> {
        @Override public void readBlock(JsonObject json) { }
        @Override public JsonObject writeBlock() { return new JsonObject(); }

        public Optional<ItemManager.StemSetting> stem() {
            return getCreator().map(v -> v.getOrNull(ItemManager.StemSetting.class));
        }

        @Override public void load() { }

        @Override public ItemManager.BlockSetting.IBlock<?> owner() { return this; }

        @Override public void populate(PopulateLootEvent e) {
            e.setCancelled(true);
            getCreator().ifPresent(v -> ItemManager.dropBlockItem(getLocation(), v.createItem(1)));
        }

        @Override public void create() {

        }
    }
    @EventHandler public static void on(BlockGrowEvent e) {
        lime.logOP("BGE:" + e.getBlock().getType() + " -> " + e.getNewState().getType());
    }
}

























