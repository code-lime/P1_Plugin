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
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.item.BookPaper;
import org.lime.gp.item.data.ItemCreator;
import org.lime.gp.item.settings.*;

import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@Setting(name = "book") public class BookSetting extends ItemSetting<JsonObject> {
    public Optional<String> author_id;
    public Optional<String> title;
    public Optional<String> generation;
    public List<List<String>> pages = new ArrayList<>();

    public BookSetting(ItemCreator creator, JsonObject json) {
        super(creator);
        author_id = json.has("author_id") ? Optional.of(json.get("author_id").getAsString()) : Optional.empty();
        title = json.has("title") ? Optional.of(json.get("title").getAsString()) : Optional.empty();
        generation = json.has("generation") ? Optional.of(json.get("generation").getAsString()) : Optional.empty();
        json.get("pages").getAsJsonArray().forEach(_json -> pages.add(_json.isJsonArray() ? Streams.stream(_json.getAsJsonArray().iterator()).map(JsonElement::getAsString).collect(Collectors.toList()) : Collections.singletonList(_json.getAsString())));
    }

    @Override public void apply(ItemStack item, ItemMeta meta, Apply apply) {
        if (meta instanceof BookMeta book) {
            author_id.map(apply::apply).flatMap(ExtMethods::parseInt).ifPresent(id -> BookPaper.setAuthorID(book, id));
            generation.map(apply::apply).map(Generation::valueOf).ifPresent(book::setGeneration);
            title.map(apply::apply).map(ChatHelper::formatComponent).ifPresent(book::title);
            List<Component> pages = this.pages.stream()
            .map(v -> v.stream()
                .map(_v -> ChatHelper.formatText(_v, apply))
                .collect(Collectors.joining("\n"))
            )
            .flatMap(v -> Arrays.stream(v.split("\t")))
            .map(v -> ChatHelper.formatComponent(v))
            .map(v -> v.color(NamedTextColor.BLACK))
            .collect(Collectors.toList());
            book.pages(pages);
            BookPaper.setPageCount(book, pages.size());
            BookPaper.updateInfo(book);
        }
    }
}
