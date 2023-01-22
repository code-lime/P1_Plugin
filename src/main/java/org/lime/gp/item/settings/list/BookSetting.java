package org.lime.gp.item.settings.list;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.BookMeta.Generation;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.ChatHelper;
import org.lime.gp.item.Items;
import org.lime.gp.item.settings.*;

import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.kyori.adventure.text.format.NamedTextColor;

@Setting(name = "book") public class BookSetting extends ItemSetting<JsonObject> {
    public Optional<String> author;
    public Optional<String> generation;
    public List<List<String>> pages = new ArrayList<>();

    public BookSetting(Items.ItemCreator creator, JsonObject json) {
        super(creator);
        author = json.has("author") ? Optional.of(json.get("author").getAsString()) : Optional.empty();
        generation = json.has("generation") ? Optional.of(json.get("generation").getAsString()) : Optional.empty();
        json.get("pages").getAsJsonArray().forEach(_json -> pages.add(_json.isJsonArray() ? Streams.stream(_json.getAsJsonArray().iterator()).map(JsonElement::getAsString).collect(Collectors.toList()) : Collections.singletonList(_json.getAsString())));
    }

    @Override public void apply(ItemStack item, ItemMeta meta, Apply apply) {
        if (meta instanceof BookMeta book) {
            author.map(apply::apply).map(ChatHelper::formatComponent).ifPresent(book::author);
            generation.map(apply::apply).map(Generation::valueOf).ifPresent(book::setGeneration);
            book.pages(pages.stream()
                .map(v -> v.stream()
                    .map(_v -> ChatHelper.formatText(_v, apply))
                    .collect(Collectors.joining("\n"))
                )
                .flatMap(v -> Arrays.stream(v.split("\t")))
                .map(v -> ChatHelper.formatComponent(v))
                .map(v -> v.color(NamedTextColor.BLACK))
                .collect(Collectors.toList()));
        }
    }
}