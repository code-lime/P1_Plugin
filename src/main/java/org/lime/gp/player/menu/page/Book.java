package org.lime.gp.player.menu.page;

import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.Player;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.database.Rows;
import org.lime.gp.player.menu.ActionSlot;
import org.lime.gp.player.ui.EditorUI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Book extends Base {
    public final ActionSlot output;
    public List<List<String>> pages = new ArrayList<>();

    public Book(JsonObject json) {
        super(json);
        output = json.has("output") ? ActionSlot.parse(this, json.getAsJsonObject("output")) : null;
        json.get("pages").getAsJsonArray().forEach(_json -> pages.add(_json.isJsonArray() ? Streams.stream(_json.getAsJsonArray().iterator()).map(JsonElement::getAsString).collect(Collectors.toList()) : Collections.singletonList(_json.getAsString())));
    }

    @Override protected void showGenerate(Rows.UserRow row, Player player, int page, Apply apply) {
        if (player == null) return;
        List<Component> _pages = pages.stream()
            .map(v -> v.stream()
                .map(_v -> ChatHelper.formatText(_v, apply))
                .collect(Collectors.joining("\n"))
            )
            .flatMap(v -> Arrays.stream(v.split("\t")))
            .map(v -> ChatHelper.formatComponent(v))
            .map(v -> v.color(NamedTextColor.BLACK))
            .collect(Collectors.toList());
        if (output == null) EditorUI.openBook(player, _pages);
    }
}










