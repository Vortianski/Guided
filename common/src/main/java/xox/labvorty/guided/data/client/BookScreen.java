package xox.labvorty.guided.data.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import xox.labvorty.guided.init.GuidedKeyMappings;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import xox.labvorty.guided.GuidedMod;
import xox.labvorty.guided.client.BookResolution;
import xox.labvorty.guided.client.LocalReadProgress;
import xox.labvorty.guided.data.book.Content;
import xox.labvorty.guided.data.registry.BookRegistry;
import xox.labvorty.guided.data.style.StyleDefinition;
import xox.labvorty.guided.data.style.Styles;
import xox.labvorty.guided.data.page.BookPage;
import xox.labvorty.guided.data.page.PageRenderContext;
import xox.labvorty.guided.data.page.PageTypes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class BookScreen extends Screen {
    private static final Map<ResourceLocation, Position> positions = new HashMap<>();
    private final Map<ResourceLocation, Set<String>> read = LocalReadProgress.load();
    private final Map<String, List<Content.Entry>> categoryEntries = new HashMap<>();
    private final Map<String, BookPage> pageInstances = new HashMap<>();
    private final List<PageSlot> shownPages = new ArrayList<>();
    private List<Content.Category> visibleCategories = List.of();
    private List<Component> pageTooltip;
    private final Set<BookPage> openPages = new HashSet<>();
    private final Set<String> failedPages = new HashSet<>();
    private boolean reloading;
    private static long lastReload;
    private Content.Entry hoveredEntry;
    private final ResourceLocation bookId;
    private Screen previous;
    private final Content.Book book;
    private JsonObject visible;
    private int category;
    private int spread;
    private int indexPage;
    private int tabOffset;
    private int categoryScroll;
    private boolean home;
    private Content.Entry reading;
    private double panX;
    private double panY;
    private double zoom = 0.75;
    private BookPage capturedPage;
    private int capturedButton = -1;
    private ItemStack pageItemTooltip;
    private JsonObject indexedVisibility;
    private String error;
    private boolean requested;

    public BookScreen(ResourceLocation resourceLocation, Screen previous) {
        this(resourceLocation);
        this.previous = previous;
    }

    public BookScreen(ResourceLocation resourceLocation) {
        super(Component.translatable(BookRegistry.get(resourceLocation).name()));
        this.bookId = resourceLocation;
        book = Content.get(bookId);
        home = !navigation() && !singleCategory();
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (GuidedKeyMappings.TOGGLE_READ.matches(key, scan)) {
            if (reading == null && hoveredEntry != null && !isCategoryEntry(hoveredEntry)) {
                changeRead(hoveredEntry, unread(hoveredEntry));
            }

            return true;
        }

        if (GuidedKeyMappings.RELOAD.matches(key, scan)) {
            long now = System.nanoTime();
            if (visible != null && now - lastReload >= 1000000000L) {
                lastReload = now;
                clearPages();
                reading = null;
                spread = 0;
                requested = false;
                reloading = true;
                init();
            }

            return true;
        }

        for (PageSlot slot : shownPages) {
            if (slot.page().keyPressed(key, scan, modifiers)) {
                return true;
            }
        }

        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void onClose() {
        playBookSound(previous != null ? "back" : "close");
        if (previous != null && minecraft != null) {
            minecraft.setScreen(previous);
        } else {
            super.onClose();
        }
    }

    @Override
    public void tick() {
        super.tick();
        for (BookPage page : openPages) {
            page.tick();
        }
    }

    @Override
    protected void init() {
        if (requested) return;
        if (!book.valid()) {
            error = "Invalid book";
            return;
        }
        if (!reloading) {
            playBookSound("open");
        }
        requested = true;
        try {
            applyResolved(new BookResolution(book).resolve(minecraft.player));
            requestStatistics();
        } catch (RuntimeException exception) {
            error = "Invalid book conditions";
        }
    }

    @Override
    public void removed() {
        closePages();
        if (previous == null && visible != null && error == null && !categories().isEmpty()) {
            positions.put(bookId, new Position(categories().get(category).id(), reading == null ? "" : reading.id(), spread, indexPage, home, panX, panY, zoom));
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        hoveredEntry = null;
        shownPages.clear();
        pageTooltip = null;
        pageItemTooltip = null;
        renderBlurredBackground(delta);
        StyleDefinition style = Styles.get(BookRegistry.get(bookId).style());
        if (!book.valid() || error != null) {
            style.renderPanel(g, false, panelLeft(), 30, panelWidth(), height - 60);
            JsonObject text = new JsonObject();
            text.addProperty("text", Component.translatable("guided.error.message").getString() + "\n\n" + Component.translatable("guided.error.id", bookId.toString()).getString());
            PageTypes.get(ResourceLocation.parse("guided:text")).render(g, font, text, pageX(0, panelLeft(), panelWidth()), 44, pageWidth(panelWidth()), height - 74);
            return;
        }
        if (visible == null) {
            g.drawCenteredString(font, error == null ? "Invalid book" : error, width / 2, height / 2, 16777215);
            return;
        }
        if (reading != null) {
            if (navigation() && !categories().isEmpty()) {
                renderBrowser(g, style, categories(), entries(), mx, my, false);
                g.flush();
                renderBlurredBackground(delta);
            }
            int w = Math.min(340, width - 20);
            int left = (width - w) / 2;
            int top = 30;
            int bottom = height - 30;
            renderPanel(g, style, false, left, top, w, bottom - top);
            JsonArray pages = visible.getAsJsonArray(reading.id());
            for (int i = 0; i < 2; i++) {
                int index = spread * 2 + i;
                if (index >= pages.size()) break;
                try {
                    Content.Page p = Content.Page.parse(pages.get(index).getAsJsonObject());
                    BookPage page = pageInstances.computeIfAbsent(reading.id() + "/" + index, (k ) -> PageTypes.create(p.type(), p.definition()));
                    int px = pageX(i, left, w);
                    int py = top + 12;
                    int pw = pageWidth(w);
                    int ph = bottom - top - 24;
                    if (p.definition().has("title") && p.definition().get("title").getAsBoolean()) {
                        String name = Component.translatable(reading.name()).getString();
                        float scale = Math.min(1.0F, pw / (float)Math.max(1, font.width(name)));
                        g.pose().pushPose();
                        g.pose().translate(px + pw / 2.0F, py, 0);
                        g.pose().scale(scale, scale, 1);
                        g.drawString(font, name, -font.width(name) / 2, 0, -13097191, false);
                        g.pose().popPose();
                        g.blitSprite(ResourceLocation.parse(texture("divider_texture", "guided:page/divider")), px, py + 11, pw, 5);
                        py += 18;
                        ph -= 18;
                    }
                    if (ph > 0) {
                        PageRenderContext context = new PageRenderContext(g, font, style, px, py, pw, ph, mx, my, delta, pageActions);
                        if (openPages.add(page)) page.onOpen();
                        style.renderPage(page, context);
                        shownPages.add(new PageSlot(page, context));
                    }
                } catch (RuntimeException exception) {
                    String key = reading.id() + "/" + index;
                    if (failedPages.add(key)) GuidedMod.LOGGER.error("Failed to render Guided page {} in {}", key, bookId, exception);
                    g.drawString(font, "Page error", pageX(i, left, w), top + 12, -5636096, false);
                }
            }
            style.renderButton(g, "left", spread > 0, width / 2 - 65, height - 24, mx, my);
            style.renderButton(g, "back", true, width / 2 - 10, height - 24, mx, my);
            style.renderButton(g, "right", (spread + 1) * 2 < pages.size(), width / 2 + 45, height - 24, mx, my);
            if (pageTooltip != null) g.renderTooltip(font, pageTooltip, Optional.empty(), mx, my); else if (pageItemTooltip != null) g.renderTooltip(font, pageItemTooltip, mx, my);
            return;
        }
        List<Content.Category> cats = categories();
        if (cats.isEmpty()) {
            if (navigation()) renderPanel(g, style, true, browserLeft(), browserTop(), browserWidth(), browserHeight()); else renderPanel(g, style, false, panelLeft(), 30, panelWidth(), height - 60);
            g.drawCenteredString(font, "No available entries", width / 2, height / 2, -7829368);
            return;
        }
        if (!navigation()) renderPanel(g, style, false, panelLeft(), 30, panelWidth(), height - 60);
        if (!navigation() && home) {
            renderLandingPage(g, style);
            for (int i = 0; i < homeRows() && i + categoryScroll < cats.size(); i++) {
                Content.Category cat = cats.get(i + categoryScroll);
                int cx = pageX(1, panelLeft(), panelWidth());
                int cy = 45 + i * 18;
                int cw = pageWidth(panelWidth());
                renderListRow(g, cat.json(), cat.name(), cx, cy, cw);
            }
            style.renderButton(g, "left", categoryScroll > 0, width / 2 - 65, height - 24, mx, my);
            style.renderButton(g, "right", categoryScroll + homeRows() < cats.size(), width / 2 + 45, height - 24, mx, my);
            return;
        } else if (!navigation()) {
            Content.Category cat = cats.get(category);
            renderListRow(g, cat.json(), cat.name(), pageX(0, panelLeft(), panelWidth()), 42, pageWidth(panelWidth()));
            style.renderButton(g, "back", true, width / 2 - 10, height - 24, mx, my);
        }
        List<Content.Entry> entries = entries();
        if (!navigation()) {
            for (int i = 0; i < rows() * 2; i++) {
                int n = indexPage * rows() * 2 + i;
                if (n >= entries.size()) break;
                Content.Entry entry = entries.get(n);
                int ex = pageX(i / rows(), panelLeft(), panelWidth());
                int ey = 64 + (i % rows()) * 18;
                int ew = pageWidth(panelWidth());
                renderListRow(g, entry.json(), entry.name(), ex, ey, ew);
                if (unread(entry)) style.renderUnread(g, ex + ew - 5, ey);
                if (mx >= ex && mx < ex + ew && my >= ey && my < ey + 18) hoveredEntry = entry;
            }
            style.renderButton(g, "left", indexPage > 0, width / 2 - 65, height - 24, mx, my);
            style.renderButton(g, "right", (indexPage + 1) * rows() * 2 < entries.size(), width / 2 + 45, height - 24, mx, my);
            return;
        }
        renderBrowser(g, style, cats, entries, mx, my, true);
        if (previous != null) style.renderButton(g, "back", true, width / 2 - 10, height - 24, mx, my);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (visible == null) return false;
        if (reading != null) {
            for (PageSlot slot : shownPages) if (slot.context().contains(x, y) && slot.page().mouseClicked(x, y, button)) {
                if (shownPages.contains(slot)) {
                    capturedPage = slot.page();
                    capturedButton = button;
                }
                return true;
            }
            if (button != 0) return false;
            if (hit(x, y, width / 2 - 65, height - 24) && spread > 0) {
                closePages();
                spread--;
                playBookSound("page_turn");
                return true;
            }
            if (hit(x, y, width / 2 + 45, height - 24) && (spread + 1) * 2 < visible.getAsJsonArray(reading.id()).size()) {
                closePages();
                spread++;
                playBookSound("page_turn");
                return true;
            }
            if (hit(x, y, width / 2 - 10, height - 24)) {
                if (previous != null || bookType().equals("entry")) onClose(); else {
                    closePages();
                    reading = null;
                    playBookSound("back");
                }
                return true;
            }
            return false;
        }
        if (button != 0) return false;
        List<Content.Category> cats = categories();
        if (cats.isEmpty()) return false;
        if (navigation()) {
            if (previous != null && hit(x, y, width / 2 - 10, height - 24)) {
                onClose();
                return true;
            }
            if (singleCategory()) {
            } else {
                if (hit(x, y, panelLeft(), 8) && tabOffset > 0) {
                    tabOffset--;
                    return true;
                }
                if (hit(x, y, panelLeft() + panelWidth() - 20, 8) && tabOffset + tabCount() < cats.size()) {
                    tabOffset++;
                    return true;
                }
                for (int i = 0; i < tabCount() && tabOffset + i < cats.size(); i++) {
                    int tx = browserLeft() + 22 + i * 24;
                    if (x >= tx && x < tx + 22 && y >= 8 && y < browserTop()) {
                        if (category != tabOffset + i) {
                            selectCategory(tabOffset + i);
                            playBookSound("category");
                        }
                        return true;
                    }
                }
            }
        } else if (home) {
            if (hit(x, y, width / 2 - 65, height - 24) && categoryScroll > 0) {
                categoryScroll--;
                playBookSound("page_turn");
                return true;
            }
            if (hit(x, y, width / 2 + 45, height - 24) && categoryScroll + homeRows() < cats.size()) {
                categoryScroll++;
                playBookSound("page_turn");
                return true;
            }
            int row = (int)Math.floor((y - 45) / 18);
            if (x >= pageX(1, panelLeft(), panelWidth()) && x < pageX(1, panelLeft(), panelWidth()) + pageWidth(panelWidth()) && row >= 0 && row < homeRows() && row + categoryScroll < cats.size()) {
                selectCategory(row + categoryScroll);
                playBookSound("category");
                return true;
            }
            return false;
        } else if (hit(x, y, width / 2 - 10, height - 24)) {
            if (previous != null || singleCategory()) onClose(); else {
                home = true;
                playBookSound("back");
            }
            return true;
        }
        if (!navigation()) {
            if (hit(x, y, width / 2 - 65, height - 24) && indexPage > 0) {
                indexPage--;
                playBookSound("page_turn");
                return true;
            }
            if (hit(x, y, width / 2 + 45, height - 24) && (indexPage + 1) * rows() * 2 < entries().size()) {
                indexPage++;
                playBookSound("page_turn");
                return true;
            }
        }
        if (navigation() ? !insideBrowser(x, y) : (x < panelLeft() + 12 || x >= panelLeft() + panelWidth() - 12 || y < 42 || y >= height - 42)) return false;
        List<Content.Entry> es = entries();
        for (int i = 0; i < es.size(); i++) {
            Content.Entry e = es.get(i);
            boolean hit = navigation() ? Math.abs(x - width / 2.0 - panX - e.x() * 32 * zoom) <= 12 * zoom && Math.abs(y - browserCenterY() - panY - e.y() * 32 * zoom) <= 12 * zoom : i >= indexPage * rows() * 2 && i < (indexPage + 1) * rows() * 2 && x >= pageX((i - indexPage * rows() * 2) / rows(), panelLeft(), panelWidth()) && x < pageX((i - indexPage * rows() * 2) / rows(), panelLeft(), panelWidth()) + pageWidth(panelWidth()) && y >= 64 + ((i - indexPage * rows() * 2) % rows()) * 18 && y < 82 + ((i - indexPage * rows() * 2) % rows()) * 18;
            if (hit) {
                if (isCategoryEntry(e)) {
                    String target = Content.string(e.json(), "target", "");
                    for (int n = 0; n < cats.size(); n++) if (cats.get(n).id().equals(target)) {
                        BookScreen next = new BookScreen(bookId, this);
                        next.visible = visible;
                        next.requested = true;
                        next.selectCategory(n);
                        playBookSound("category");
                        minecraft.setScreen(next);
                        break;
                    }
                    return true;
                }
                openEntry(e);
                playBookSound("entry");
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (capturedPage != null && button == capturedButton) return capturedPage.mouseDragged(x, y, button, dx, dy);
        if (reading == null && navigation() && button == 0 && insideBrowser(x, y)) {
            panX += dx;
            panY += dy;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (capturedPage != null && button == capturedButton) {
            BookPage p = capturedPage;
            capturedPage = null;
            capturedButton = -1;
            return p.mouseReleased(x, y, button);
        }
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (reading != null) for (PageSlot slot : shownPages) if (slot.context().contains(x, y) && slot.page().mouseScrolled(x, y, horizontal, vertical)) return true;
        if (reading == null && !navigation() && home && visible != null) {
            categoryScroll = Math.max(0, Math.min(Math.max(0, categories().size() - homeRows()), categoryScroll - (int)Math.signum(vertical)));
            return true;
        }
        if (reading == null && navigation() && visible != null && insideBrowser(x, y)) {
            double old = zoom;
            zoom = Math.max(0.35, Math.min(1.5, zoom * Math.pow(1.15, vertical)));
            panX = x - width / 2.0 - (x - width / 2.0 - panX) * zoom / old;
            panY = y - browserCenterY() - (y - browserCenterY() - panY) * zoom / old;
            return true;
        }
        return false;
    }

    public void onStatisticsUpdated() {
        if (minecraft == null || minecraft.player == null || visible == null || !book.valid()) {
            return;
        }

        JsonObject resolved;
        try {
            resolved = new BookResolution(book).resolve(minecraft.player);
        } catch (RuntimeException exception) {
            GuidedMod.LOGGER.error("Failed to refresh book conditions for {}", bookId, exception);
            return;
        }

        if (resolved.equals(visible)) {
            return;
        }

        List<Content.Category> oldCategories = categories();
        String categoryId = category >= 0 && category < oldCategories.size()
                ? oldCategories.get(category).id() : null;

        clearPages();
        visible = resolved;
        indexedVisibility = null;
        hoveredEntry = null;
        updateCategoryCache();

        category = 0;
        List<Content.Category> updatedCategories = categories();
        for (int i = 0; i < updatedCategories.size(); i++) {
            if (updatedCategories.get(i).id().equals(categoryId)) {
                category = i;
                break;
            }
        }

        if (reading != null) {
            if (!visible.has(reading.id())) {
                reading = null;
                spread = 0;
            } else {
                int pageCount = visible.getAsJsonArray(reading.id()).size();
                spread = Math.max(0, Math.min(spread, (pageCount - 1) / 2));
            }
        }

        indexPage = Math.max(0, Math.min(indexPage, (entries().size() - 1) / Math.max(1, rows() * 2)));
        tabOffset = Math.max(0, Math.min(tabOffset, updatedCategories.size() - tabCount()));
        if (category < tabOffset) {
            tabOffset = category;
        } else if (category >= tabOffset + tabCount()) {
            tabOffset = category - tabCount() + 1;
        }
        categoryScroll = Math.max(0, Math.min(categoryScroll, updatedCategories.size() - homeRows()));

        if (bookType().equals("entry") && reading == null && !entries().isEmpty()) {
            openEntry(entries().getFirst());
        }
    }

    private void requestStatistics() {
        if (minecraft == null || minecraft.getConnection() == null) {
            return;
        }

        minecraft.getConnection().send(new ServerboundClientCommandPacket(
                ServerboundClientCommandPacket.Action.REQUEST_STATS
        ));
    }

    private void openEntry(Content.Entry e) {
        closePages();
        reading = e;
        spread = 0;
        home = false;
        if (unread(e)) changeRead(e, true);
    }

    private void changeRead(Content.Entry entry, boolean value) {
        if (isCategoryEntry(entry)) {
            return;
        }
        Set<String> entries = read.computeIfAbsent(bookId, (id) -> new HashSet<>());
        boolean changed;
        if (value) {
            changed = entries.add(entry.id());
        } else {
            changed = entries.remove(entry.id());
        }
        if (changed) {
            LocalReadProgress.save(read);
        }
    }

    private void followEntry(String id) {
        for (Content.Entry e : book.entries()) {
            if (e.id().equals(id) && visible.has(id)) {
                BookScreen next = new BookScreen(bookId, this);
                next.visible = visible;
                next.requested = true;
                List<Content.Category> cats = next.categories();
                for (int i = 0; i < cats.size(); i++) if (cats.get(i).id().equals(e.category())) next.selectCategory(i);
                next.openEntry(e);
                playBookSound("entry");
                minecraft.setScreen(next);
                return;
            }
        }
    }

    private void closePages() {
        for (BookPage p : openPages) p.onClose();
        openPages.clear();
        shownPages.clear();
        capturedPage = null;
        capturedButton = -1;
    }

    private void clearPages() {
        closePages();
        pageInstances.clear();
        failedPages.clear();
    }

    private void applyResolved(JsonObject resolved) {
        if (reloading) {
            visible = resolved;
            reading = null;
            spread = 0;
            category = Math.max(0, Math.min(category, categories().size() - 1));
            indexPage = 0;
            reloading = false;
            return;
        }
        visible = resolved;
        category = 0;
        restorePosition();
        if (singleCategory()) {
            home = false;
            category = 0;
            if (bookType().equals("entry") && !entries().isEmpty() && reading == null) {
                openEntry(entries().getFirst());
            }
        }
    }

    private void restorePosition() {
        Position saved = positions.get(bookId);
        if (saved == null) return;
        List<Content.Category> cats = categories();
        for (int i = 0; i < cats.size(); i++) {
            if (!cats.get(i).id().equals(saved.category())) continue;
            selectCategory(i);
            home = !navigation() && saved.home();
            panX = saved.panX();
            panY = saved.panY();
            zoom = saved.zoom();
            indexPage = Math.max(0, Math.min(saved.indexPage(), (entries().size() - 1) / Math.max(1, rows() * 2)));
            for (Content.Entry entry : entries()) {
                if (!entry.id().equals(saved.entry())) continue;
                reading = entry;
                spread = Math.max(0, Math.min(saved.spread(), (visible.getAsJsonArray(entry.id()).size() - 1) / 2));
                home = false;
                break;
            }
            return;
        }
    }

    private void updateCategoryCache() {
        if (indexedVisibility == visible) return;
        indexedVisibility = visible;
        categoryEntries.clear();
        if (visible == null) {
            visibleCategories = List.of();
            return;
        }
        for (Content.Entry entry : book.entries()) {
            if (visible.has(entry.id())) {
                categoryEntries.computeIfAbsent(entry.category(), (key ) -> new ArrayList<>()).add(entry);
            }
        }
        visibleCategories = book.categories().stream().filter((category ) -> categoryEntries.containsKey(category.id())).toList();
    }

    private void selectCategory(int selected) {
        category = selected;
        home = false;
        indexPage = 0;
        panX = panY = 0;
        if (category < tabOffset) {
            tabOffset = category;
        }
        if (category >= tabOffset + tabCount()) {
            tabOffset = category - tabCount() + 1;
        }
    }

    private void playBookSound(String action) {
        if (minecraft == null) return;
        SoundEvent fallback = SoundEvents.BOOK_PAGE_TURN;
        SoundEvent sound = fallback;
        JsonElement sounds = book.json().get("sounds");
        if (sounds != null && sounds.isJsonObject()) {
            JsonElement value = sounds.getAsJsonObject().get(action);
            if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
                if (id != null && minecraft.getSoundManager().getSoundEvent(id) != null) {
                    sound = SoundEvent.createVariableRangeEvent(id);
                }
            }
        }
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F));
    }

    private void renderTabs(GuiGraphics g, StyleDefinition style, List<Content.Category> cats, int mx, int my) {
        if (cats.size() > tabCount()) {
            style.renderButton(g, "left", tabOffset > 0, browserLeft(), 8, mx, my);
            style.renderButton(g, "right", tabOffset + tabCount() < cats.size(), browserLeft() + browserWidth() - 20, 8, mx, my);
        }
        for (int i = 0; i < tabCount() && tabOffset + i < cats.size(); i++) {
            int x = browserLeft() + 22 + i * 24;
            Content.Category cat = cats.get(tabOffset + i);
            style.renderBookmark(g, x, 8, tabOffset + i == category, categoryComplete(cat));
            style.renderEntryIcon(g, cat.json(), x + 3, 12);
        }
    }

    private void renderLandingPage(GuiGraphics graphics, StyleDefinition style) {
        int pageWidth = pageWidth(panelWidth());
        int pageX = pageX(0, panelLeft(), panelWidth());
        int titleY = 55;
        int contentY = titleY + font.lineHeight + 8;
        int contentBottom = height - 45;

        graphics.drawString(font, title,
                panelLeft() + panelWidth() / 4 - font.width(title) / 2,
                titleY, 0xff382719, false);

        if (!bookType().equals("book") || contentBottom <= contentY) {
            return;
        }

        String landingTexture = texture("landing_texture", "");
        String landingText = Content.string(book.json(), "landing_text", "");
        boolean hasText = !landingText.isBlank();
        ResourceLocation image = ResourceLocation.tryParse(landingTexture);

        if (!landingTexture.isBlank() && !landingTexture.equals("none") && image != null) {
            int textureWidth = landingTextureDimension("landing_texture_width");
            int textureHeight = landingTextureDimension("landing_texture_height");
            int availableHeight = contentBottom - contentY;

            if (hasText) {
                availableHeight = Math.max(0, (availableHeight - 6) / 2);
            }

            if (availableHeight > 0) {
                float scale = Math.min((float) pageWidth / textureWidth,
                        (float) availableHeight / textureHeight);
                int imageWidth = Math.max(1, (int) (textureWidth * scale));
                int imageHeight = Math.max(1, (int) (textureHeight * scale));
                int imageX = pageX + (pageWidth - imageWidth) / 2;

                if (image.getPath().endsWith(".png")) {
                    graphics.blit(image, imageX, contentY, imageWidth, imageHeight,
                            0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
                } else {
                    style.renderPageSprite(graphics, image, imageX, contentY, imageWidth, imageHeight);
                }

                contentY += imageHeight + 6;
            }
        }

        if (hasText && contentY < contentBottom) {
            JsonObject definition = new JsonObject();
            definition.addProperty("text", landingText);
            PageTypes.get(ResourceLocation.parse("guided:text")).render(
                    graphics, font, definition, pageX, contentY,
                    pageWidth, contentBottom - contentY);
        }
    }

    private int landingTextureDimension(String key) {
        try {
            return Math.max(1, Math.min(4096, Integer.parseInt(texture(key, "128"))));
        } catch (NumberFormatException ignored) {
            return 128;
        }
    }

    private void renderLabel(GuiGraphics g, String label, int x, int y, int maxWidth) {
        if (maxWidth <= 0) return;
        float scale = Math.min(1.0F, maxWidth / (float)Math.max(1, font.width(label)));
        g.pose().pushPose();
        g.pose().translate(x, y + (font.lineHeight * (1 - scale)) / 2, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, label, 0, 0, -13097191, false);
        g.pose().popPose();
    }

    private void renderListRow(GuiGraphics g, JsonObject json, String name, int x, int y, int w) {
        g.pose().pushPose();
        try {
            g.pose().translate(x, y + 4, 0);
            g.pose().scale(0.5F, 0.5F, 1.0F);
            renderIcon(g, json, 0, 0);
        } finally {
            g.pose().popPose();
        }
        renderLabel(g, translated(name), x + 11, y + 4, w - 11);
    }

    private void renderBrowser(GuiGraphics g, StyleDefinition style, List<Content.Category> cats, List<Content.Entry> entries, int mx, int my, boolean tooltips) {
        if (!singleCategory()) renderTabs(g, style, cats, mx, my);
        int border = browserBorder();
        g.enableScissor(browserLeft() + border, browserTop() + border, browserLeft() + browserWidth() - border, browserTop() + browserHeight() - border);
        renderBrowserBackground(g, style);
        g.pose().pushPose();
        g.pose().translate(width / 2.0 + panX, browserCenterY() + panY, 0);
        g.pose().scale((float)zoom, (float)zoom, 1);
        Map<String, Content.Entry> map = new HashMap<>();
        entries.forEach((e ) -> map.put(e.id(), e));
        for (Content.Entry e : entries) if (e.json().has("parents")) for (JsonElement p : e.json().getAsJsonArray("parents")) {
            Content.Entry parent = map.get(p.isJsonObject() ? Content.string(p.getAsJsonObject(), "entry", "") : p.getAsString());
            if (p.isJsonObject() && p.getAsJsonObject().has("line_enabled") && !p.getAsJsonObject().get("line_enabled").getAsBoolean()) continue;
            if (parent != null && intersectsBrowser(Math.min(parent.x(), e.x()) * 32 - 32, Math.min(parent.y(), e.y()) * 32 - 32, Math.max(parent.x(), e.x()) * 32 + 32, Math.max(parent.y(), e.y()) * 32 + 32, style.getBrowserCullingMargin())) style.renderConnection(g, new StyleDefinition.Point(parent.x() * 32, parent.y() * 32), new StyleDefinition.Point(e.x() * 32, e.y() * 32), p.isJsonObject() && p.getAsJsonObject().has("line_reversed") && p.getAsJsonObject().get("line_reversed").getAsBoolean(), !p.isJsonObject() || !p.getAsJsonObject().has("draw_arrow") || p.getAsJsonObject().get("draw_arrow").getAsBoolean());
        }
        for (Content.Entry e : entries) {
            if (!intersectsBrowser(e.x() * 32 - 16, e.y() * 32 - 16, e.x() * 32 + 16, e.y() * 32 + 16, style.getBrowserCullingMargin())) continue;
            g.pose().pushPose();
            g.pose().translate(e.x() * 32, e.y() * 32, 0);
            style.renderEntryBackground(g, e.json());
            style.renderEntryIcon(g, e.json(), -8, -8);
            if (unread(e)) style.renderUnread(g, 7, -11);
            g.pose().popPose();
        }
        g.pose().popPose();
        g.flush();
        g.disableScissor();
        renderPanel(g, style, true, browserLeft(), browserTop(), browserWidth(), browserHeight());
        if (!tooltips) return;
        if (insideBrowser(mx, my)) {
            for (Content.Entry e : entries) {
                if (Math.abs(mx - width / 2.0 - panX - e.x() * 32 * zoom) <= 12 * zoom && Math.abs(my - browserCenterY() - panY - e.y() * 32 * zoom) <= 12 * zoom) {
                    hoveredEntry = e;
                    g.renderTooltip(font, tooltipName(e.name(), e.id()), mx, my);
                    break;
                }
            }
        }
        for (int i = 0; !singleCategory() && i < tabCount() && tabOffset + i < cats.size(); i++) {
            int x = panelLeft() + 22 + i * 24;
            if (mx >= x && mx < x + 22 && my >= 8 && my < 28) {
                Content.Category cat = cats.get(tabOffset + i);
                g.renderTooltip(font, tooltipName(cat.name(), cat.id()), mx, my);
                break;
            }
        }
    }

    private void renderPanel(GuiGraphics g, StyleDefinition style, boolean browser, int x, int y, int w, int h) {
        String custom = texture(browser ? "browser_frame" : "book_texture", "");
        if (!browser) {
            if (custom.isEmpty()) style.renderPanel(g, false, x, y, w, h); else g.blit(ResourceLocation.parse(custom), x, y, w, h, 0, 0, 272, 180, 272, 180);
            return;
        }
        g.flush();
        g.pose().pushPose();
        g.pose().translate(0, 0, 150);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            if (custom.isEmpty()) style.renderPanel(g, true, x, y, w, h); else g.blitSprite(ResourceLocation.parse(custom), x, y, w, h);
            if (w >= 72 && h >= 70) {
                renderOverlay(g, style, "top", x + (w - 72) / 2, y, 72, 17);
                renderOverlay(g, style, "bottom", x + (w - 72) / 2, y + h - 17, 72, 15);
                renderOverlay(g, style, "left", x, y + (h - 70) / 2, 16, 70);
                renderOverlay(g, style, "right", x + w - 16, y + (h - 70) / 2, 16, 70);
            }
            g.flush();
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            g.pose().popPose();
        }
    }

    private void renderOverlay(GuiGraphics g, StyleDefinition style, String side, int x, int y, int w, int h) {
        String custom = texture(side + "_overlay", "");
        if (custom.equals("none")) return;
        if (custom.isEmpty()) style.renderFrameOverlay(g, side, x, y, w, h); else g.blitSprite(ResourceLocation.parse(custom), x, y, w, h);
    }

    private void renderBrowserBackground(GuiGraphics g, StyleDefinition style) {
        String fallback = texture("browser_background", "guided:textures/gui/sprites/core/browser_background.png");
        ResourceLocation id = ResourceLocation.parse(texture("parallax_image", fallback));
        int tw = textureDimension("parallax_width");
        int th = textureDimension("parallax_height");
        style.renderParallax(g, id, browserLeft(), browserTop(), browserWidth(), browserHeight(), tw, th, panX, panY);
    }

    private void renderIcon(GuiGraphics g, JsonObject json, int x, int y) {
        if (json.has("icon_texture")) {
            g.blit(ResourceLocation.parse(json.get("icon_texture").getAsString()), x, y, 0, 0, 16, 16, 16, 16);
        } else {
            ResourceLocation id = ResourceLocation.tryParse(Content.string(json, "icon", "minecraft:book"));
            if (id != null) g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(id)), x, y);
        }
    }

    private String bookType() {
        return Content.string(book.json(), "type", BookRegistry.get(bookId).navigation() ? "browser" : "book");
    }

    private boolean singleCategory() {
        return Set.of("entry", "pamphlet", "network").contains(bookType());
    }

    private boolean isCategoryEntry(Content.Entry entry) {
        return Content.string(entry.json(), "type", "guided:entry").equals("guided:category");
    }

    private boolean unread(Content.Entry entry) {
        return !isCategoryEntry(entry) && !read.getOrDefault(bookId, Set.of()).contains(entry.id());
    }

    private boolean browserOpen() {
        return navigation() && reading == null;
    }

    private int browserBorder() {
        return Math.max(0, Math.min(16, Math.min(browserWidth(), browserHeight()) / 2 - 1));
    }

    private boolean insideBrowser(double x, double y) {
        int border = browserBorder();
        return x >= browserLeft() + border && x < browserLeft() + browserWidth() - border && y >= browserTop() + border && y < browserTop() + browserHeight() - border;
    }

    private Component tooltipName(String key, String fallback) {
        String name = translated(key);
        return Component.literal(name.isBlank() ? fallback : name).withStyle(ChatFormatting.WHITE);
    }

    private List<Content.Category> categories() {
        updateCategoryCache();
        return visibleCategories;
    }

    private List<Content.Entry> entries() {
        List<Content.Category> categories = categories();
        if (categories.isEmpty() || category >= categories.size()) {
            return List.of();
        }
        return categoryEntries.getOrDefault(categories.get(category).id(), List.of());
    }

    private String translated(String key) {
        return Component.translatable(key).getString();
    }

    private int homeRows() {
        return Math.max(1, (height - 85) / 18);
    }

    private int tabCount() {
        return Math.max(1, (browserWidth() - 44) / 24);
    }

    private boolean categoryComplete(Content.Category cat) {
        updateCategoryCache();
        List<Content.Entry> available = categoryEntries.getOrDefault(cat.id(), List.of());
        return !available.isEmpty() && available.stream().noneMatch(this::unread);
    }

    private boolean navigation() {
        return book.valid() && Set.of("browser", "network").contains(bookType());
    }

    private int browserWidth() {
        return Math.max(1, Math.min(480, width - 12));
    }

    private int browserLeft() {
        return (width - browserWidth()) / 2;
    }

    private int browserTop() {
        return 28;
    }

    private int browserHeight() {
        return Math.max(1, height - browserTop() - 6);
    }

    private double browserCenterY() {
        return browserTop() + browserHeight() / 2.0;
    }

    private int panelWidth() {
        return browserOpen() ? browserWidth() : Math.max(1, Math.min(340, width - 20));
    }

    private int pageX(int page, int left, int w) {
        return left + page * (w / 2) + (page == 0 ? 24 : 8);
    }

    private int pageWidth(int w) {
        return Math.max(1, w / 2 - 32);
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int rows() {
        return Math.max(1, (height - 112) / 18);
    }

    private boolean hit(double x, double y, int bx, int by) {
        return x >= bx && x < bx + 20 && y >= by && y < by + 16;
    }

    private boolean intersectsBrowser(double minX, double minY, double maxX, double maxY, double margin) {
        double originX = width / 2.0 + panX;
        double originY = browserCenterY() + panY;
        int border = browserBorder();
        return originX + maxX * zoom >= browserLeft() + border - margin && originX + minX * zoom <= browserLeft() + browserWidth() - border + margin && originY + maxY * zoom >= browserTop() + border - margin && originY + minY * zoom <= browserTop() + browserHeight() - border + margin;
    }

    private String texture(String key, String fallback) {
        String value = Content.string(book.json(), key, Styles.get(BookRegistry.get(bookId).style()).resolveTexture(key, fallback));
        if (visible != null && !home && !categories().isEmpty()) value = Content.string(categories().get(category).json(), key, value);
        return value;
    }

    private int textureDimension(String key) {
        try {
            return Math.max(1, Math.min(4096, Integer.parseInt(texture(key, "64"))));
        } catch (NumberFormatException ignored) {
            return 64;
        }
    }
    private final PageRenderContext.Actions pageActions = new PageRenderContext.Actions() {

        @Override
        public String texture(String key, String fallback) {
            return BookScreen.this.texture(key, fallback);
        }

        @Override
        public void openEntry(String id) {
            followEntry(id);
        }

        @Override
        public void openCategory(String id) {
            List<Content.Category> cats = categories();
            for (int i = 0; i < cats.size(); i++) if (cats.get(i).id().equals(id)) {
                closePages();
                reading = null;
                selectCategory(i);
                home = false;
                playBookSound("category");
                return;
            }
        }

        @Override
        public void playSound(ResourceLocation id) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(id), 1.0F));
        }

        @Override
        public void openLink(String url) {
            if (url.startsWith("https://") || url.startsWith("http://")) minecraft.setScreen(new ConfirmLinkScreen((ok ) -> {
                if (ok) Util.getPlatform().openUri(url);
                minecraft.setScreen(BookScreen.this);
            }, url, true));
        }

        @Override
        public void tooltip(List<Component> lines) {
            pageTooltip = lines;
        }

        @Override
        public void itemTooltip(ItemStack stack) {
            pageItemTooltip = stack;
        }
    };

    private record PageSlot(BookPage page, PageRenderContext context) {
    }

    private record Position(
            String category,
            String entry,
            int spread,
            int indexPage,
            boolean home,
            double panX,
            double panY,
            double zoom
    ) {
    }
}
