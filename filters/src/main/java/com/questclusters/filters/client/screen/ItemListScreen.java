package com.questclusters.filters.client.screen;

import com.questclusters.filters.client.ItemSources;
import com.questclusters.filters.client.ItemSources.Choice;
import com.questclusters.filters.filter.FilterIds;
import com.questclusters.filters.filter.ItemListFilter;
import com.questclusters.filters.filter.ListArgs;
import dev.ftb.mods.ftbfiltersystem.api.client.gui.AbstractFilterScreen;
import dev.ftb.mods.ftbfiltersystem.api.filter.SmartFilter;
import dev.ftb.mods.ftbfiltersystem.client.gui.GhostDropReceiver;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Editor for {@code smart_filter_extras:items}: pick any number of items (or item variants) from a grid. */
public class ItemListScreen extends FilterScreenBase<ItemListFilter> implements GhostDropReceiver {
    private static final int COLS = 9, SLOT = 18;
    private final int rows;
    private final Map<String, ItemListFilter.Entry> selected = new LinkedHashMap<>();
    private final Set<String> original;
    private boolean inventory, variants, onlySelected;
    private String query = "";
    private List<Choice> shown = List.of();
    private int scroll, selPage;
    private long lastClick;
    private String lastClickText;

    public ItemListScreen(ItemListFilter filter, AbstractFilterScreen parent) {
        this(filter, parent, rowsFor(106));
    }

    private ItemListScreen(ItemListFilter filter, AbstractFilterScreen parent, int rows) {
        super(filter, parent, COLS * SLOT + 16, rows * SLOT + 66);
        this.rows = rows;
        filter.entries().forEach(e -> selected.put(e.text(), e));
        original = new HashSet<>(selected.keySet());
        variants = filter.entries().stream().anyMatch(ItemListFilter.Entry::variant);
    }

    // ------------------------------------------------------------------ layout

    private int gridX() {
        return leftPos + 8;
    }

    private int gridY() {
        return topPos + 18 + 20;
    }

    private int toolbarY() {
        return gridY() + rows * SLOT + 3;
    }

    private int selY() {
        return toolbarY() + 13;
    }

    @Override
    protected void init() {
        super.init();
        int y = topPos + 19;
        makeSearch(leftPos + 8, y, guiWidth - 16 - 30, query, s -> {
            query = s;
            scroll = 0;
            refresh();
        });
        addRenderableWidget(new IconButton(leftPos + guiWidth - 37, y, 14, 14, "✔", tr("items.only_selected"),
                () -> {
                    onlySelected = !onlySelected;
                    scroll = 0;
                    refresh();
                }, () -> onlySelected));
        addRenderableWidget(new IconButton(leftPos + guiWidth - 22, y, 14, 14, "", tr("items.variants"),
                () -> {
                    variants = !variants;
                    scroll = 0;
                    refresh();
                }, () -> variants).withIcon(new ItemStack(Items.POTION)));
        // item source tabs in the title bar
        addRenderableWidget(new IconButton(leftPos + guiWidth - 56, topPos + 3, 16, 14, "", tr("items.source.all"),
                () -> {
                    inventory = false;
                    refresh();
                }, () -> !inventory).withIcon(new ItemStack(Items.COMPASS)));
        addRenderableWidget(new IconButton(leftPos + guiWidth - 39, topPos + 3, 16, 14, "", tr("items.source.inventory"),
                () -> {
                    inventory = true;
                    refresh();
                }, () -> inventory).withIcon(new ItemStack(Items.CHEST)));
        int ty = toolbarY();
        addRenderableWidget(new IconButton(leftPos + guiWidth - 8 - 11 * 3 - 2, ty, 11, 11, "⧉", tr("items.copy"), this::copy, () -> false));
        addRenderableWidget(new IconButton(leftPos + guiWidth - 8 - 11 * 2 - 1, ty, 11, 11, "⎘", tr("items.paste"), this::paste, () -> false));
        addRenderableWidget(new IconButton(leftPos + guiWidth - 8 - 11, ty, 11, 11, "✖", tr("items.clear"), () -> {
            selected.clear();
            selPage = 0;
            refresh();
        }, () -> false));
        refresh();
    }

    @Override
    public void resize(net.minecraft.client.Minecraft mc, int w, int h) {
        int newRows = rowsFor(106);
        if (newRows != rows) { // the panel height is fixed when the screen is created: reopen with the same state
            ItemListScreen s = new ItemListScreen(filter, parentScreen, newRows);
            s.selected.clear();
            s.selected.putAll(selected);
            s.original.clear();
            s.original.addAll(original);
            s.inventory = inventory;
            s.variants = variants;
            s.onlySelected = onlySelected;
            s.query = query;
            mc.setScreen(s);
        } else {
            super.resize(mc, w, h);
        }
    }

    private void refresh() {
        List<Choice> src = inventory ? ItemSources.inventory(variants) : ItemSources.all(variants);
        List<Choice> out = new ArrayList<>();
        for (Choice c : src) {
            if (onlySelected && !selected.containsKey(c.text())) continue;
            if (ItemSources.matches(c, query)) out.add(c);
        }
        if (onlySelected) { // selected entries not in the source list (other variants, inventory mode) still show
            for (ItemListFilter.Entry e : selected.values()) {
                if (e.missing() || out.stream().anyMatch(c -> c.text().equals(e.text()))) continue;
                ItemStack s = ItemListFilter.stackOf(e);
                Choice c = new Choice(s, e.text(), s.getHoverName().getString().toLowerCase(), e.text().toLowerCase());
                if (ItemSources.matches(c, query)) out.add(c);
            }
        }
        shown = out;
        int maxScroll = Math.max(0, (shown.size() + COLS - 1) / COLS - rows);
        scroll = Math.min(scroll, maxScroll);
        int pages = Math.max(1, (selected.size() + COLS - 1) / COLS);
        selPage = Math.min(selPage, pages - 1);
    }

    // ------------------------------------------------------------------ selection

    private int baseLength;

    /** Length of the whole filter tree (everything in this smart filter) if this list had {@code entries}. */
    private int treeLength(List<ItemListFilter.Entry> entries) {
        var reg = net.minecraft.client.Minecraft.getInstance().level.registryAccess();
        if (baseLength == 0) {
            SmartFilter root = filter;
            while (root.getParent() != null) root = root.getParent();
            baseLength = root.asString(reg).length() - filter.asString(reg).length();
        }
        return baseLength + new ItemListFilter(null, entries).asString(reg).length();
    }

    /** Adds an entry; false (with a hint) if the list is full or the filter would get too long. */
    private boolean add(String text, ItemStack stack) {
        if (selected.containsKey(text)) return true;
        if (selected.size() >= ItemListFilter.MAX_ENTRIES) {
            showHint(tr("items.full", ItemListFilter.MAX_ENTRIES));
            return false;
        }
        if (text.length() > ItemListFilter.MAX_ENTRY_LENGTH) {
            showHint(tr("items.entry_too_long"));
            return false;
        }
        var reg = minecraft.level.registryAccess();
        ItemListFilter.Entry e;
        try {
            e = ItemListFilter.parseEntry(text, reg);
        } catch (Exception ex) {
            showHint(Component.literal(ex.getMessage()));
            return false;
        }
        List<ItemListFilter.Entry> next = new ArrayList<>(selected.values());
        next.add(e);
        if (treeLength(next) > 32000) {
            showHint(tr("items.too_long"));
            return false;
        }
        selected.put(e.text(), e);
        return true;
    }

    private void toggle(Choice c) {
        if (selected.remove(c.text()) == null) add(c.text(), c.stack());
        refresh();
    }

    @Override
    protected ItemListFilter makeNewFilter() {
        return selected.isEmpty() ? null : saved(new ItemListFilter(filter.getParent(), List.copyOf(selected.values())));
    }

    @Override
    protected boolean changed() {
        return !selected.keySet().equals(original);
    }

    @Override
    protected boolean canSave() {
        return !selected.isEmpty();
    }

    @Override
    protected void copy() {
        if (setClipboard(String.join(",", selected.keySet()))) showHint(tr("items.copied", selected.size()));
    }

    @Override
    protected void paste() {
        String clip = minecraft.keyboardHandler.getClipboard();
        int added = 0, skipped = 0;
        for (String part : idsIn(clip)) {
            String text = ListArgs.decode(part);
            try {
                if (ItemListFilter.parseEntry(text, minecraft.level.registryAccess()).missing()) {
                    skipped++;
                    continue;
                }
            } catch (Exception e) {
                skipped++;
                continue;
            }
            if (!add(text, ItemStack.EMPTY)) break;
            added++;
        }
        showHint(tr("items.pasted", added, skipped));
        refresh();
    }

    /** Ids in pasted text: a plain list (commas, spaces, new lines) or filter strings like items(...) / item(...). */
    static List<String> idsIn(String clip) {
        List<String> out = new ArrayList<>();
        var m = java.util.regex.Pattern.compile("(?:[a-z0-9_.-]+:)?(?:items|item)\\(([^()]*)\\)").matcher(clip);
        boolean any = false;
        while (m.find()) {
            any = true;
            out.addAll(ListArgs.split(m.group(1)));
        }
        if (!any) {
            for (String p : ListArgs.split(clip.replace('\n', ',').replace('\r', ','))) {
                for (String w : p.contains("[") ? List.of(p) : List.of(p.split("\\s+"))) if (!w.isBlank()) out.add(w.trim());
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ input

    private int gridIndexAt(double mx, double my) {
        int c = (int) ((mx - gridX()) / SLOT), r = (int) ((my - gridY()) / SLOT);
        if (mx < gridX() || my < gridY() || c >= COLS || r >= rows) return -1;
        int i = (scroll + r) * COLS + c;
        return i < shown.size() ? i : -1;
    }

    private int selIndexAt(double mx, double my) {
        if (my < selY() || my >= selY() + SLOT || mx < gridX()) return -1;
        int c = (int) ((mx - gridX()) / SLOT);
        int i = selPage * COLS + c;
        return c < COLS && i < selected.size() ? i : -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        int gi = gridIndexAt(mx, my);
        if (gi >= 0) {
            Choice c = shown.get(gi);
            long now = System.currentTimeMillis();
            boolean dbl = button == 0 && c.text().equals(lastClickText) && now - lastClick < 300;
            lastClick = now;
            lastClickText = c.text();
            if (Screen.hasShiftDown()) {
                if (button == 0) {
                    for (Choice s : shown) if (!add(s.text(), s.stack())) break;
                } else {
                    shown.forEach(s -> selected.remove(s.text()));
                }
                refresh();
            } else if (dbl) {
                add(c.text(), c.stack());
                if (canSave()) applyChanges();
            } else if (button == 1) {
                selected.remove(c.text());
                refresh();
            } else {
                toggle(c);
            }
            return true;
        }
        int si = selIndexAt(mx, my);
        if (si >= 0) {
            String key = new ArrayList<>(selected.keySet()).get(si);
            selected.remove(key);
            lastClickText = null;
            refresh();
            return true;
        }
        // paging arrows and the missing-ids warning in the toolbar row
        if (my >= toolbarY() && my < toolbarY() + 11) {
            int pages = Math.max(1, (selected.size() + COLS - 1) / COLS);
            if (mx >= gridX() && mx < gridX() + 9 && selPage > 0) {
                selPage--;
                return true;
            }
            if (mx >= gridX() + 10 && mx < gridX() + 19 && selPage < pages - 1) {
                selPage++;
                return true;
            }
            int wx = leftPos + guiWidth - 8 - 11 * 4 - 4;
            if (missingCount() > 0 && mx >= wx && mx < wx + 11) {
                selected.values().removeIf(ItemListFilter.Entry::missing);
                refresh();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (my >= selY() && my < selY() + SLOT) {
            int pages = Math.max(1, (selected.size() + COLS - 1) / COLS);
            selPage = Math.max(0, Math.min(pages - 1, selPage - (int) Math.signum(dy)));
            return true;
        }
        int maxScroll = Math.max(0, (shown.size() + COLS - 1) / COLS - rows);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(dy)));
        return true;
    }

    private long missingCount() {
        return selected.values().stream().filter(ItemListFilter.Entry::missing).count();
    }

    // ------------------------------------------------------------------ JEI drop

    @Override
    public Rect2i getGhostDropRegion() {
        return new Rect2i(gridX(), gridY(), COLS * SLOT, selY() + SLOT - gridY());
    }

    @Override
    public void receiveGhostDrop(ItemStack stack) {
        ItemSources.Choice c = com.questclusters.filters.client.ItemSourcesAccess.choice(stack, variants);
        add(c.text(), c.stack());
        refresh();
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        // grid
        Set<String> plainSelected = new HashSet<>();
        Set<String> partial = new HashSet<>();
        for (ItemListFilter.Entry e : selected.values()) {
            if (e.missing()) continue;
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.item()).toString();
            if (e.variant()) partial.add(id);
            else plainSelected.add(id);
        }
        Choice hovered = null;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < COLS; c++) {
                int x = gridX() + c * SLOT, y = gridY() + r * SLOT, i = (scroll + r) * COLS + c;
                slot(g, x, y);
                if (i >= shown.size()) continue;
                Choice ch = shown.get(i);
                String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ch.stack().getItem()).toString();
                boolean sel = selected.containsKey(ch.text());
                boolean viaPlain = !sel && variants && !ch.text().equals(id) && plainSelected.contains(id);
                if (sel) g.fill(x + 1, y + 1, x + 17, y + 17, 0xA04CAF50);
                else if (viaPlain) g.fill(x + 1, y + 1, x + 17, y + 17, 0x5081C784);
                g.renderItem(ch.stack(), x + 1, y + 1);
                if (sel || viaPlain) {
                    g.pose().pushPose();
                    g.pose().translate(0, 0, 200);
                    g.drawString(font, "✔", x + 10, y + 1, sel ? 0xFF1B5E20 : 0x801B5E20, false);
                    g.pose().popPose();
                }
                if (!variants && !sel && partial.contains(id)) g.fill(x + 1, y + 15, x + 17, y + 17, 0xFF2196F3);
                if (mx >= x && mx < x + SLOT && my >= y && my < y + SLOT) {
                    g.fill(x + 1, y + 1, x + 17, y + 17, 0x60FFFFFF);
                    hovered = ch;
                }
            }
        }
        // scrollbar
        int totalRows = (shown.size() + COLS - 1) / COLS;
        if (totalRows > rows) {
            int h = rows * SLOT, bar = Math.max(8, h * rows / totalRows);
            int by = gridY() + (h - bar) * scroll / Math.max(1, totalRows - rows);
            g.fill(gridX() + COLS * SLOT + 1, by, gridX() + COLS * SLOT + 3, by + bar, 0xFF606060);
        }
        // toolbar: paging, count, warning
        int ty = toolbarY(), pages = Math.max(1, (selected.size() + COLS - 1) / COLS);
        if (pages > 1) {
            g.drawString(font, "◀", gridX(), ty + 2, selPage > 0 ? 0xFF404040 : 0xFFA0A0A0, false);
            g.drawString(font, "▶", gridX() + 10, ty + 2, selPage < pages - 1 ? 0xFF404040 : 0xFFA0A0A0, false);
        }
        Component count = tr("items.selected", selected.size());
        int cx = gridX() + (pages > 1 ? 22 : 0);
        int room = leftPos + guiWidth - 8 - 11 * 4 - 6 - cx;
        g.drawString(font, font.width(count) <= room ? count : Component.literal(String.valueOf(selected.size())), cx, ty + 2, 0xFF404040, false);
        long missing = missingCount();
        int wx = leftPos + guiWidth - 8 - 11 * 4 - 4;
        if (missing > 0) g.drawString(font, "⚠", wx + 2, ty + 2, 0xFFE0A000, false);
        // selected row
        List<ItemListFilter.Entry> sel = new ArrayList<>(selected.values());
        ItemListFilter.Entry hoveredSel = null;
        if (sel.isEmpty()) {
            g.fill(gridX(), selY(), gridX() + COLS * SLOT, selY() + SLOT, 0xFF8B8B8B);
            g.drawString(font, tr("items.empty_row"), gridX() + 4, selY() + 5, 0xFF505050, false);
        }
        for (int c = 0; c < COLS; c++) {
            int i = selPage * COLS + c, x = gridX() + c * SLOT, y = selY();
            if (sel.isEmpty()) break;
            slot(g, x, y);
            if (i >= sel.size()) continue;
            ItemListFilter.Entry e = sel.get(i);
            if (e.missing()) g.drawString(font, "?", x + 6, y + 5, 0xFFE0A000, false);
            else g.renderItem(ItemListFilter.stackOf(e), x + 1, y + 1);
            if (mx >= x && mx < x + SLOT && my >= y && my < y + SLOT) {
                g.fill(x + 1, y + 1, x + 17, y + 17, 0x60FF0000);
                g.drawString(font, "×", x + 12, y, 0xFFFF4040, false);
                hoveredSel = e;
            }
        }
        renderHint(g, selY() - 2);
        // tooltips
        if (hovered != null) {
            List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(minecraft, hovered.stack()));
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(hovered.stack().getItem()).toString();
            if (!variants && partial.contains(id) && !selected.containsKey(hovered.text())) lines.add(tr("items.partial").withStyle(ChatFormatting.BLUE));
            if (variants && !selected.containsKey(hovered.text()) && plainSelected.contains(id) && !hovered.text().equals(id)) {
                lines.add(tr("items.matched_by_item").withStyle(ChatFormatting.DARK_GREEN));
            }
            lines.add(tr(selected.containsKey(hovered.text()) ? "items.hint_remove" : "items.hint_add").withStyle(ChatFormatting.DARK_GRAY));
            g.renderTooltip(font, lines, Optional.empty(), hovered.stack(), mx, my);
        } else if (hoveredSel != null) {
            List<Component> lines = new ArrayList<>();
            if (hoveredSel.missing()) lines.add(tr("items.missing_entry", hoveredSel.text()).withStyle(ChatFormatting.GOLD));
            else lines.addAll(Screen.getTooltipFromItem(minecraft, ItemListFilter.stackOf(hoveredSel)));
            lines.add(tr("items.hint_remove").withStyle(ChatFormatting.DARK_GRAY));
            g.renderComponentTooltip(font, lines, mx, my);
        } else if (missing > 0 && my >= ty && my < ty + 11 && mx >= wx && mx < wx + 11) {
            g.renderComponentTooltip(font, List.of(tr("items.missing", missing), tr("items.missing_remove").withStyle(ChatFormatting.DARK_GRAY)), mx, my);
        }
    }

    static void slot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        g.fill(x, y, x + 17, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + 17, 0xFF373737);
        g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
    }

    // for the dev self-test
    public Map<String, ItemListFilter.Entry> selectedForTest() {
        return selected;
    }

    public void setVariantsForTest(boolean v) {
        variants = v;
        refresh();
    }

    public List<Choice> shownForTest() {
        return shown;
    }

    public void searchForTest(String q) {
        search.setValue(q);
    }

    public boolean addForTest(String text) {
        boolean ok = add(text, ItemStack.EMPTY);
        refresh();
        return ok;
    }

    static boolean isItems(SmartFilter f) {
        return f.getId().equals(FilterIds.ITEMS);
    }
}
