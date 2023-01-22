package org.lime.gp.item;

import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.lime.core;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.lime.gp.admin.AnyEvent;
import org.lime.gp.chat.Apply;
import org.lime.gp.chat.LangMessages;
import org.lime.gp.database.Rows;
import org.lime.gp.database.Tables;
import org.lime.gp.database.Rows.UserRow;
import org.lime.gp.extension.ExtMethods;
import org.lime.gp.lime;
import org.lime.gp.extension.JManager;
import org.lime.system;
import org.lime.gp.chat.ChatHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookPaper implements Listener {
    public static core.element create() {
        return core.element.create(BookPaper.class)
                .withInit(BookPaper::init)
                .withInstance();
    }

    private static ItemStack WRITABLE_BOOK;
    private static int getPageCount(ItemStack book) {
        return getPageCount(book.getItemMeta());
    }
    private static int getPageCount(ItemMeta book) {
        return JManager.get(JsonPrimitive.class, book.getPersistentDataContainer(), "page_count", new JsonPrimitive(3)).getAsInt();
    }
    public static int getAuthorID(ItemMeta book) {
        return book.getPersistentDataContainer().getOrDefault(AUTHOR_KEY, PersistentDataType.INTEGER, -1);
    }
    private static void setPageCount(ItemStack book, int count) {
        ItemMeta meta = book.getItemMeta();
        if (!(meta instanceof BookMeta bookMeta)) return;
        setPageCount(bookMeta, count);
        book.setItemMeta(bookMeta);
        updateInfo(book);
    }
    public static void setPageCount(BookMeta meta, int count) {
        JManager.set(meta.getPersistentDataContainer(), "page_count", new JsonPrimitive(count));
        List<Component> old_pages = meta.pages();
        int old_count = old_pages.size();
        List<Component> pages = new ArrayList<>();
        for (int i = 0; i < Math.min(count, old_count); i++) pages.add(old_pages.get(i));
        for (int i = old_count; i < count; i++) pages.add(Component.empty());
        meta.pages(pages);
    }
    private static void updateInfo(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (updateInfo(meta)) item.setItemMeta(meta);
    }
    public static boolean updateInfo(ItemMeta meta) {
        return meta instanceof BookMeta book && updateInfo(book);
    }
    public static boolean updateInfo(BookMeta meta) {
        int count = getPageCount(meta);
        
        PersistentDataContainer data = meta.getPersistentDataContainer();
        String author_name = meta.hasAuthor() ? Optional.ofNullable(data.getOrDefault(AUTHOR_KEY, PersistentDataType.INTEGER, null))
            .flatMap(UserRow::getBy)
            .map(v -> ChatColor.AQUA + v.firstName + " " + v.lastName + " " + ChatColor.GREEN + Integer.toHexString(0xFFFF - v.id).toUpperCase())
            .orElseGet(() -> ChatColor.RED + "Неизвестно") : null;

        List<Component> lore = (author_name == null
            ? LangMessages.Message.BookEditor_Book_Lore
            : LangMessages.Message.BookEditor_Book_SignLore)
            .getMessages(Apply.of().add("pages", String.valueOf(count)));
        
        boolean modify = false;

        if (!lore.equals(meta.lore())) {
            meta.lore(lore);
            modify = true;
        }
        if (author_name != null) {
            meta.setAuthor(author_name);
            modify = true;
        }
        
        return modify;
    }
    private static List<String> getLines(String page) {
        List<String> lines = new ArrayList<>();
        int size = page.length();
        int index = 0;
        StringBuilder curr = new StringBuilder();
        for (int i = 0; i < size; i++) {
            char ch = page.charAt(i);
            index++;
            if (ch == '\n') {
                lines.add(curr.toString());
                curr.setLength(0);
                index = 0;
                continue;
            }
            curr.append(ch);
            if (index > 24) {
                lines.add(curr.toString());
                curr.setLength(0);
                index = 0;
                continue;
            }
            if (index > 18 && ch == ' ') {
                lines.add(curr.toString());
                curr.setLength(0);
                index = 0;
            }
        }
        if (curr.length() != 0) lines.add(curr.toString());
        return lines;
    }
    private static ItemStack createPaper(Component page, Rows.UserRow sign) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        JManager.set(meta.getPersistentDataContainer(), "page", ChatHelper.toJson(page));
        List<Component> lore = new ArrayList<>();
        getLines(ChatHelper.getLegacyText(page)).forEach(v -> lore.add(ChatHelper.fromText(v, NamedTextColor.GRAY)));
        lore.add(Component.empty());
        lore.add(sign == null ? LangMessages.Message.BookEditor_Paper_SignEmpty.getSingleMessage() : LangMessages.Message.BookEditor_Paper_Sign.getSingleMessage(Apply.of().add(sign)));
        meta.lore(lore);
        meta.displayName(LangMessages.Message.BookEditor_Paper_Name.getSingleMessage());
        item.setItemMeta(meta);
        return item;
    }
    public static void init() {
        AnyEvent.addEvent("book.subtract", AnyEvent.type.none, builder -> builder.onError(lime::logStackTrace).createParam(Integer::parseInt, "[page:int]").createParam(Boolean::parseBoolean, "true","false"), BookPaper::eventBookSubtract);
        AnyEvent.addEvent("book.generate", AnyEvent.type.owner, builder -> builder.createParam("[page|page]"), (player, pages) -> {
            ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
            BookMeta meta = (BookMeta)item.getItemMeta();
            List<Component> _pages = new ArrayList<>();
            for (String page : pages.split("\\|")) _pages.add(ChatHelper.formatComponent(page.replace("\\n", "\n")));
            meta.pages(_pages);
            meta.setAuthor("");
            meta.setTitle("");
            meta.setGeneration(BookMeta.Generation.ORIGINAL);
            item.setItemMeta(meta);
            Items.dropGiveItem(player, item, false);
        });

        ItemStack writable_book = new ItemStack(Material.WRITABLE_BOOK);
        setPageCount(writable_book, 3);
        updateInfo(writable_book);
        WRITABLE_BOOK = writable_book;

        for (Recipe recipe : Streams.stream(Bukkit.recipeIterator()).toList()) {
            if (recipe instanceof ShapelessRecipe shapelessRecipe) {
                if (shapelessRecipe.getKey().getKey().equals("writable_book")) {
                    Bukkit.removeRecipe(shapelessRecipe.getKey());
                    ShapelessRecipe newShapelessRecipe = new ShapelessRecipe(NamespacedKey.minecraft("writable_book"), WRITABLE_BOOK);
                    shapelessRecipe.getChoiceList().forEach(newShapelessRecipe::addIngredient);
                    Bukkit.addRecipe(newShapelessRecipe);
                }
            }
        }

        ExecuteItem.execute.add(BookPaper::onExecute);
    }

    private static final NamespacedKey AUTHOR_KEY = new NamespacedKey(lime._plugin, "author");

    private static boolean onExecute(ItemStack item, system.Toast1<ItemMeta> metaBox) {
        return metaBox.val0 instanceof BookMeta meta ? onExecute(meta) : false;
    }
    private static boolean onExecute(BookMeta meta) {
        boolean update = false;
        if (meta.hasAuthor()) {
            PersistentDataContainer data = meta.getPersistentDataContainer();
            Integer author_id = data.getOrDefault(AUTHOR_KEY, PersistentDataType.INTEGER, null);
            if (author_id == null) {
                String author = meta.getAuthor().replaceAll("&.", "");
                author_id = Optional.ofNullable(Bukkit.getOfflinePlayer(author))
                    .map(OfflinePlayer::getUniqueId)
                    .flatMap(Rows.UserRow::getBy)
                    .or(() -> Tables.USER_TABLE.getBy(v -> v.firstName.equals(author)))
                    .map(v -> v.id)
                    .orElse(-1);
                data.set(AUTHOR_KEY, PersistentDataType.INTEGER, author_id);
                update = true;
            }
        }
        return updateInfo(meta) || update;
    }

    @EventHandler public static void onClick(InventoryClickEvent e) {
        switch (e.getClick()) {
            case RIGHT:
            case LEFT:
                ItemStack cursor = e.getCursor();
                ItemStack item = e.getCurrentItem();
                if (cursor == null || item == null) return;
                if (cursor.getType() != Material.PAPER || cursor.getItemMeta().hasCustomModelData() || item.getType() != Material.WRITABLE_BOOK) return;
                int pages = getPageCount(item);
                Component page = ChatHelper.fromJson(JManager.get(JsonElement.class, cursor.getItemMeta().getPersistentDataContainer(), "page", null));
                int addPages = Math.min(100 - pages, cursor.getAmount());
                if (addPages <= 0) return;

                pages += addPages;
                setPageCount(item, pages);
                BookMeta meta = (BookMeta) item.getItemMeta();
                List<Component> _pages = new ArrayList<>(meta.pages());
                for (int i = _pages.size(); i < pages; i++) _pages.add(Component.empty());
                for (int i = pages - addPages; i < pages; i++) _pages.set(i, page);
                meta.pages(_pages);
                item.setItemMeta(meta);

                cursor.subtract(addPages);
                e.setCancelled(true);
                break;
            default:
                break;
        }
    }
    @EventHandler public static void onInteract(PlayerInteractEvent e) {
        switch (e.getAction()) {
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                ItemStack item = e.getItem();
                if (item == null) return;
                if (e.getHand() != EquipmentSlot.HAND) return;
                switch (item.getType()) {
                    case WRITABLE_BOOK: {
                        if (!e.getPlayer().isSneaking()) return;
                        e.setCancelled(true);
                        ExtMethods.openBook(createBookEditor(item), e.getPlayer());
                        return;
                    }
                    case PAPER: {
                        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
                        if (JManager.has(container, "page")) {
                            JsonElement json = JManager.get(JsonElement.class, container, "page", null);
                            Component page = json == null ? null : ChatHelper.fromJson(json);
                            if (page == null) return;
                            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
                            BookMeta meta = (BookMeta)book.getItemMeta();
                            meta.pages(page);
                            meta.setAuthor("");
                            meta.setTitle("");
                            meta.setGeneration(BookMeta.Generation.ORIGINAL);
                            book.setItemMeta(meta);
                            ExtMethods.openBook(book, e.getPlayer());
                        } else {
                            JsonElement json = JManager.get(JsonElement.class, container, "pages", null);
                            if (json == null) return;
                            List<Component> pages = system.list.<Component>of().add(json.getAsJsonArray(), ChatHelper::fromJson).build();
                            if (pages == null) return;
                            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
                            BookMeta meta = (BookMeta)book.getItemMeta();
                            meta.pages(pages);
                            meta.setAuthor("");
                            meta.setTitle("");
                            meta.setGeneration(BookMeta.Generation.ORIGINAL);
                            book.setItemMeta(meta);
                            ExtMethods.openBook(book, e.getPlayer());
                        }
                        return;
                    }
                    default:
                        break;
                }
            default:
                break;
        }
    }
    @EventHandler public static void onBook(PlayerEditBookEvent e) {
        BookMeta _new = e.getNewBookMeta();
        setPageCount(_new, getPageCount(_new));
        if (e.isSigning()) JManager.set(_new.getPersistentDataContainer(), "signing_time", new JsonPrimitive(System.currentTimeMillis()));
        onExecute(_new);
        e.setNewBookMeta(_new);
        Player player = e.getPlayer();
        lime.once(player::updateInventory, 0.1);
    }
    private static void eventBookSubtract(Player player, int page, boolean sign) {
        ItemStack item = player.getInventory().getItem(EquipmentSlot.HAND);
        if (item.getType() != Material.WRITABLE_BOOK) return;
        int pages = getPageCount(item);
        if (page >= pages) return;
        pages--;
        BookMeta meta = (BookMeta)item.getItemMeta();
        List<Component> components = new ArrayList<>(meta.pages());
        Component _page;
        if (components.size() <= page) {
            _page = Component.empty();
        } else {
            _page = components.get(page);
            components.remove(page);
        }
        meta.pages(components);
        item.setItemMeta(meta);
        if (pages <= 0) item.subtract(1);
        else setPageCount(item, pages);
        ItemStack paper = createPaper(_page, sign ? Rows.UserRow.getBy(player.getUniqueId()).orElse(null) : null);
        Items.dropGiveItem(player, paper, true);
    }

    private static ItemStack createBookEditor(ItemStack item) {
        BookMeta original = (BookMeta)item.getItemMeta();
        List<Component> pages = new ArrayList<>(original.pages());
        int count = getPageCount(item);
        ItemStack show = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) show.getItemMeta();
        for (int i = original.getPageCount(); i < count; i++) pages.add(Component.empty());
        for (int i = 0; i < count; i++) pages.set(i, LangMessages.Message.BookEditor_Book_Title.getSingleMessage(Apply.of().add("page", String.valueOf(i))).append(pages.get(i)));
        for (int i = count; i < original.getPageCount(); i++) pages.add(LangMessages.Message.BookEditor_Book_Title.getSingleMessage(Apply.of().add("page", String.valueOf(i))).append(pages.get(i)));
        meta.pages(pages);
        meta.setAuthor("");
        meta.setTitle("");
        meta.setGeneration(BookMeta.Generation.ORIGINAL);
        show.setItemMeta(meta);
        return show;
    }
}




















