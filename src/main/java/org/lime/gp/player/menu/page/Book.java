package org.lime.gp.player.menu.page;

import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.minecraft.network.protocol.game.PacketPlayOutOpenBook;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.database.Rows;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.player.menu.Slot;
import org.lime.gp.player.ui.EditorUI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Book extends Base {
    public final Slot output;
    public List<List<String>> pages = new ArrayList<>();

    public Book(JsonObject json) {
        super(json);
        output = json.has("output") ? Slot.parse(this, json.getAsJsonObject("output")) : null;
        json.get("pages").getAsJsonArray().forEach(_json -> pages.add(_json.isJsonArray() ? Streams.stream(_json.getAsJsonArray().iterator()).map(JsonElement::getAsString).collect(Collectors.toList()) : Collections.singletonList(_json.getAsString())));
    }

    private static List<Component> createBookEditorOfLines(List<List<Component>> pages) {
        return pages.stream().map(v -> Component.join(JoinConfiguration.separator(Component.newline()), v)).collect(Collectors.toList());
    }

    @Override protected void showGenerate(Rows.UserRow row, Player player, int page, Apply apply) {
        if (player == null) return;
        List<List<Component>> components = new ArrayList<>();
        for (List<String> _page : pages) {
            List<Component> __page = new ArrayList<>();
            for (String line : _page) {
                String _line = ChatHelper.formatText(line, apply);
                String[] _parts = _line.split("\t");
                __page.add(ChatHelper.formatComponent(_parts[0]));

                int length = _parts.length;
                for (int i = 1; i < length; i++) {
                    String _part = _parts[i];
                    components.add(__page);
                    __page = new ArrayList<>();
                    __page.add(ChatHelper.formatComponent(_part));
                }
            }
            components.add(__page);
        }
        List<Component> pages = createBookEditorOfLines(components);
        if (output == null) EditorUI.openBook(player, pages);
        else EditorUI.openBook(player, pages, _pages -> output.invoke(player, apply
                .add("pages", _pages.stream().map(v -> v.replace("\t", "")).collect(Collectors.joining("\t"))), true)
        );
    }
}










