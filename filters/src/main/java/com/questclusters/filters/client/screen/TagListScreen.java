package com.questclusters.filters.client.screen;

import com.questclusters.filters.filter.ListArgs;
import com.questclusters.filters.filter.TagListFilter;
import dev.ftb.mods.ftbfiltersystem.api.client.gui.AbstractFilterScreen;
import dev.ftb.mods.ftbfiltersystem.client.gui.GhostDropReceiver;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Editor for several item tags or block tags (any one matches). */
public class TagListScreen<F extends TagListFilter> extends FilterScreenBase<F> implements GhostDropReceiver {
    private static final int ROW = 12;

    /** One tag: id, how many entries, up to 27 sample stacks; loaded = exists in this world. */
    record TagInfo(ResourceLocation id, int count, List<ItemStack> samples, boolean loaded) {
    }

    private final boolean blocks;
    private final int rows;
    private final Set<ResourceLocation> checked = new LinkedHashSet<>();
    private final Set<ResourceLocation> original;
    private final List<TagInfo> allTags = new ArrayList<>();
    private List<TagInfo> shown = List.of();
    private Set<ResourceLocation> onlyCheckedSnapshot;
    private boolean onlyChecked;
    private String query = "";
    private int scroll;
    private ItemStack reverse = ItemStack.EMPTY;
    private int reverseIndex = -1;
    private long hoverStart;
    private ResourceLocation hoverId, lastClickId;
    private long lastClick;

    public TagListScreen(F filter, AbstractFilterScreen parent, boolean blocks) {
        this(filter, parent, blocks, rowsFor(114));
    }

    private TagListScreen(F filter, AbstractFilterScreen parent, boolean blocks, int rows) {
        super(filter, parent, 220, rows * ROW + 74);
        this.blocks = blocks;
        this.rows = rows;
        checked.addAll(filter.tags());
        original = new LinkedHashSet<>(checked);
        loadTags();
    }

    private void loadTags() {
        var registry = blocks ? BuiltInRegistries.BLOCK : BuiltInRegistries.ITEM;
        registry.getTags().forEach(pair -> {
            List<ItemStack> samples = new ArrayList<>();
            int count = 0;
            for (Holder<?> h : pair.getSecond()) {
                count++;
                if (samples.size() < 27) {
                    Item item = h.value() instanceof net.minecraft.world.level.block.Block b ? b.asItem() : (Item) h.value();
                    if (item != net.minecraft.world.item.Items.AIR) samples.add(new ItemStack(item));
                }
            }
            allTags.add(new TagInfo(pair.getFirst().location(), count, samples, true));
        });
        allTags.sort(Comparator.comparing(t -> t.id().toString()));
    }

    private int listY() {
        return topPos + 57;
    }

    private int listX() {
        return leftPos + 8;
    }

    private int listW() {
        return guiWidth - 16;
    }

    @Override
    protected void init() {
        super.init();
        int y = topPos + 19;
        makeSearch(leftPos + 8, y, guiWidth - 16 - 16, query, s -> {
            query = s;
            scroll = 0;
            refresh(true);
        });
        addRenderableWidget(new IconButton(leftPos + guiWidth - 23, y, 14, 14, "✔", tr("tags.only_checked"), () -> {
            onlyChecked = !onlyChecked;
            scroll = 0;
            refresh(true);
        }, () -> onlyChecked));
        int ry = topPos + 55;
        addRenderableWidget(new IconButton(leftPos + guiWidth - 8 - 11 * 3 - 2, ry - 15, 11, 11, "⧉", tr("tags.copy"), this::copy, () -> false));
        addRenderableWidget(new IconButton(leftPos + guiWidth - 8 - 11 * 2 - 1, ry - 15, 11, 11, "⎘", tr("tags.paste"), this::paste, () -> false));
        addRenderableWidget(new IconButton(leftPos + guiWidth - 8 - 11, ry - 15, 11, 11, "✖", tr("tags.clear"), () -> {
            checked.clear();
            refresh(false);
        }, () -> false));
        refresh(true);
    }

    @Override
    public void resize(net.minecraft.client.Minecraft mc, int w, int h) {
        int newRows = rowsFor(114);
        if (newRows != rows) {
            TagListScreen<F> s = new TagListScreen<>(filter, parentScreen, blocks, newRows);
            s.checked.clear();
            s.checked.addAll(checked);
            s.original.clear();
            s.original.addAll(original);
            s.query = query;
            s.reverse = reverse;
            s.onlyChecked = onlyChecked;
            mc.setScreen(s);
        } else {
            super.resize(mc, w, h);
        }
    }

    /** @param full also re-takes the "only checked" snapshot (unchecked rows stay until the list is refreshed) */
    private void refresh(boolean full) {
        if (full || onlyCheckedSnapshot == null) onlyCheckedSnapshot = new LinkedHashSet<>(checked);
        String q = query.trim().toLowerCase(Locale.ROOT);
        String typed = q.isEmpty() ? null : ListArgs.normaliseTag(q);
        List<TagInfo> out = new ArrayList<>();
        // checked tags that this world does not have
        for (ResourceLocation id : checked) {
            if (allTags.stream().noneMatch(t -> t.id().equals(id))) out.add(new TagInfo(id, 0, List.of(), false));
        }
        for (TagInfo t : allTags) out.add(t);
        List<TagInfo> filtered = new ArrayList<>();
        for (TagInfo t : out) {
            if (onlyChecked && !onlyCheckedSnapshot.contains(t.id())) continue;
            if (!q.isEmpty() && !t.id().toString().contains(q.startsWith("#") ? q.substring(1) : q)) continue;
            if (!reverse.isEmpty() && !contains(t, reverse)) continue;
            filtered.add(t);
        }
        if (typed != null && q.contains(":") && reverse.isEmpty() && filtered.stream().noneMatch(t -> t.id().toString().equals(typed))
                && allTags.stream().noneMatch(t -> t.id().toString().equals(typed))) {
            filtered.addFirst(new TagInfo(ResourceLocation.parse(typed), 0, List.of(), false));
        }
        shown = filtered;
        scroll = Math.max(0, Math.min(scroll, shown.size() - rows));
    }

    private boolean contains(TagInfo t, ItemStack stack) {
        if (!t.loaded()) return false;
        if (blocks) {
            return stack.getItem() instanceof BlockItem bi && bi.getBlock().builtInRegistryHolder()
                    .is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, t.id()));
        }
        return stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, t.id()));
    }

    private void toggle(ResourceLocation id) {
        if (!checked.remove(id)) {
            if (checked.size() >= TagListFilter.MAX_TAGS) {
                showHint(tr("tags.full", TagListFilter.MAX_TAGS));
                return;
            }
            checked.add(id);
        }
        refresh(false);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected F makeNewFilter() {
        if (checked.isEmpty()) return null;
        List<ResourceLocation> tags = List.copyOf(checked);
        return saved((F) (blocks ? new TagListFilter.Blocks(filter.getParent(), tags) : new TagListFilter.Items(filter.getParent(), tags)));
    }

    @Override
    protected boolean changed() {
        return !checked.equals(original);
    }

    @Override
    protected boolean canSave() {
        return !checked.isEmpty();
    }

    @Override
    protected void copy() {
        if (setClipboard(String.join(",", checked.stream().map(ResourceLocation::toString).toList()))) {
            showHint(tr("tags.copied", checked.size()));
        }
    }

    @Override
    protected void paste() {
        String clip = minecraft.keyboardHandler.getClipboard();
        List<String> parts = new ArrayList<>();
        var m = java.util.regex.Pattern.compile("(?:[a-z0-9_.-]+:)?(?:item_tag|block_tag)\\(([^()]*)\\)").matcher(clip);
        boolean any = false;
        while (m.find()) {
            any = true;
            parts.addAll(ListArgs.split(m.group(1)));
        }
        if (!any) {
            for (String p : clip.split("[,\\s]+")) {
                // need '#' or a namespace, so ordinary words are not taken for tags
                if (p.startsWith("#") || p.contains(":")) parts.add(p);
            }
        }
        int added = 0, unloaded = 0;
        for (String p : parts) {
            String t = ListArgs.normaliseTag(p);
            if (t == null) continue;
            ResourceLocation id = ResourceLocation.parse(t);
            if (checked.contains(id)) continue;
            if (checked.size() >= TagListFilter.MAX_TAGS) break;
            checked.add(id);
            added++;
            if (allTags.stream().noneMatch(x -> x.id().equals(id))) unloaded++;
        }
        showHint(unloaded > 0 ? tr("tags.pasted_unloaded", added, unloaded) : tr("tags.pasted", added));
        refresh(true);
    }

    // ------------------------------------------------------------------ reverse lookup

    private int slotX() {
        return leftPos + 8;
    }

    private int slotY() {
        return topPos + 38;
    }

    private List<ItemStack> candidates() {
        List<ItemStack> out = new ArrayList<>();
        var inv = minecraft.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && (!blocks || s.getItem() instanceof BlockItem) && out.stream().noneMatch(o -> o.is(s.getItem()))) out.add(s.copyWithCount(1));
        }
        return out;
    }

    private void cycle(int dir) {
        List<ItemStack> c = candidates();
        if (c.isEmpty()) {
            showHint(tr(blocks ? "tags.reverse.none_blocks" : "tags.reverse.none_items"));
            return;
        }
        reverseIndex = Math.floorMod(reverseIndex + dir, c.size());
        setReverse(c.get(reverseIndex));
    }

    private void setReverse(ItemStack s) {
        reverse = s;
        scroll = 0;
        refresh(true);
    }

    @Override
    public Rect2i getGhostDropRegion() {
        return new Rect2i(slotX(), slotY(), listW(), listY() + rows * ROW - slotY());
    }

    @Override
    public void receiveGhostDrop(ItemStack stack) {
        if (blocks && !(stack.getItem() instanceof BlockItem)) {
            showHint(tr("tags.reverse.not_block"));
            return;
        }
        setReverse(stack.copyWithCount(1));
    }

    // ------------------------------------------------------------------ input

    private int rowAt(double mx, double my) {
        if (mx < listX() || mx >= listX() + listW() || my < listY() || my >= listY() + rows * ROW) return -1;
        int i = scroll + (int) ((my - listY()) / ROW);
        return i < shown.size() ? i : -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (mx >= slotX() && mx < slotX() + 16 && my >= slotY() && my < slotY() + 16) {
            if (button == 1) setReverse(ItemStack.EMPTY);
            else cycle(1);
            return true;
        }
        if (!reverse.isEmpty() && mx >= slotX() + 18 && mx < slotX() + 26 && my >= slotY() + 4 && my < slotY() + 12) {
            setReverse(ItemStack.EMPTY);
            return true;
        }
        int r = rowAt(mx, my);
        if (r >= 0 && button == 0) {
            ResourceLocation id = shown.get(r).id();
            long now = System.currentTimeMillis();
            if (id.equals(lastClickId) && now - lastClick < 300) {
                checked.add(id);
                if (canSave()) applyChanges();
                return true;
            }
            lastClick = now;
            lastClickId = id;
            toggle(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (mx >= slotX() && mx < slotX() + 16 && my >= slotY() && my < slotY() + 16) {
            cycle(dy > 0 ? -1 : 1);
            return true;
        }
        scroll = Math.max(0, Math.min(Math.max(0, shown.size() - rows), scroll - (int) Math.signum(dy)));
        return true;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        // reverse lookup slot
        ItemListScreen.slot(g, slotX() - 1, slotY() - 1);
        if (!reverse.isEmpty()) {
            g.renderItem(reverse, slotX(), slotY());
            g.drawString(font, "×", slotX() + 20, slotY() + 4, 0xFFC03030, false);
        }
        g.drawString(font, tr(blocks ? "tags.reverse.blocks" : "tags.reverse.items"), slotX() + 30, slotY() + 4, 0xFF404040, false);
        // list
        g.fill(listX(), listY(), listX() + listW(), listY() + rows * ROW, 0xFFE0E0E0);
        TagInfo hovered = null;
        for (int i = 0; i < rows; i++) {
            int idx = scroll + i, y = listY() + i * ROW;
            if (idx >= shown.size()) break;
            TagInfo t = shown.get(idx);
            boolean on = checked.contains(t.id()), hover = mx >= listX() && mx < listX() + listW() && my >= y && my < y + ROW;
            if (hover) {
                g.fill(listX(), y, listX() + listW(), y + ROW, 0xFFC8D8F0);
                hovered = t;
            }
            g.fill(listX() + 2, y + 2, listX() + 10, y + 10, 0xFF606060);
            g.fill(listX() + 3, y + 3, listX() + 9, y + 9, on ? 0xFF4CAF50 : 0xFFFFFFFF);
            Component name = Component.literal(t.id().getNamespace() + ":").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(t.id().getPath()).withStyle(ChatFormatting.BLACK));
            if (!t.loaded()) name = name.copy().append(Component.literal(" ").append(tr("tags.unloaded")).withStyle(ChatFormatting.GOLD));
            g.drawString(font, font.plainSubstrByWidth(name.getString(), listW() - 50).length() < name.getString().length()
                    ? Component.literal(font.plainSubstrByWidth(name.getString(), listW() - 56) + "…") : name, listX() + 13, y + 2, 0xFF000000, false);
            if (t.loaded()) {
                String c = String.valueOf(t.count());
                g.drawString(font, c, listX() + listW() - 4 - font.width(c), y + 2, 0xFF606060, false);
            }
        }
        if (shown.isEmpty()) {
            g.drawCenteredString(font, tr(onlyChecked ? "tags.none_checked" : "tags.no_match"), listX() + listW() / 2, listY() + rows * ROW / 2 - 4, 0xFF606060);
        }
        if (shown.size() > rows) {
            int h = rows * ROW, bar = Math.max(6, h * rows / shown.size());
            int by = listY() + (h - bar) * scroll / Math.max(1, shown.size() - rows);
            g.fill(listX() + listW() - 2, by, listX() + listW(), by + bar, 0xFF808080);
        }
        // summary: checked tags and what they hold together
        int sy = listY() + rows * ROW + 4;
        Set<Item> union = new LinkedHashSet<>();
        for (ResourceLocation id : checked) {
            allTags.stream().filter(t -> t.id().equals(id)).findFirst().ifPresent(t -> t.samples().forEach(s -> union.add(s.getItem())));
        }
        Component summary = tr(blocks ? "tags.summary_blocks" : "tags.summary_items", checked.size(), union.size());
        g.drawString(font, summary, listX(), sy + 3, 0xFF404040, false);
        int ix = listX() + font.width(summary) + 4;
        for (Item item : union) {
            if (ix > listX() + listW() - 12) break;
            g.pose().pushPose();
            g.pose().translate(ix, sy, 0);
            g.pose().scale(0.75F, 0.75F, 1);
            g.renderFakeItem(new ItemStack(item), 0, 0);
            g.pose().popPose();
            ix += 13;
        }
        renderHint(g, sy - 2);
        // tooltips
        if (hovered != null) {
            if (!hovered.id().equals(hoverId)) {
                hoverId = hovered.id();
                hoverStart = System.currentTimeMillis();
            }
            if (System.currentTimeMillis() - hoverStart > 400) preview(g, hovered, mx, my);
        } else {
            hoverId = null;
            if (my >= sy && my < sy + 12 && mx >= listX() && mx < listX() + listW() && !checked.isEmpty()) {
                List<Component> lines = new ArrayList<>();
                checked.forEach(id -> lines.add(Component.literal("#" + id)));
                g.renderComponentTooltip(font, lines, mx, my);
            } else if (mx >= slotX() && mx < slotX() + 16 && my >= slotY() && my < slotY() + 16) {
                List<Component> lines = new ArrayList<>();
                if (!reverse.isEmpty()) lines.add(reverse.getHoverName());
                lines.add(tr("tags.reverse.hint").withStyle(ChatFormatting.DARK_GRAY));
                g.renderComponentTooltip(font, lines, mx, my);
            }
        }
    }

    /** Tooltip with up to 27 icons of what the tag holds. */
    private void preview(GuiGraphics g, TagInfo t, int mx, int my) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("#" + t.id()));
        lines.add(t.loaded() ? tr(blocks ? "tags.preview_blocks" : "tags.preview_items", t.count()).withStyle(ChatFormatting.GRAY)
                : tr("tags.unloaded_hint").withStyle(ChatFormatting.GOLD));
        lines.add(tr(checked.contains(t.id()) ? "tags.hint_uncheck" : "tags.hint_check").withStyle(ChatFormatting.DARK_GRAY));
        int iconsH = t.samples().isEmpty() ? 0 : ((t.samples().size() + 8) / 9) * 18 + 2;
        int w = Math.max(9 * 18, lines.stream().mapToInt(font::width).max().orElse(0)) + 8;
        int h = lines.size() * 10 + iconsH + 6;
        int x = Math.min(mx + 12, width - w - 4), y = Math.max(4, Math.min(my - 12, height - h - 4));
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(x, y, x + w, y + h, 0xF0100010);
        g.renderOutline(x, y, w, h, 0xFF5000FF);
        int ty = y + 4;
        for (Component l : lines) {
            g.drawString(font, l, x + 4, ty, 0xFFFFFFFF, false);
            ty += 10;
        }
        for (int i = 0; i < t.samples().size(); i++) {
            g.renderFakeItem(t.samples().get(i), x + 4 + (i % 9) * 18, ty + 2 + (i / 9) * 18);
        }
        g.pose().popPose();
    }

    // for the dev self-test
    public Set<ResourceLocation> checkedForTest() {
        return checked;
    }

    public List<ResourceLocation> shownForTest() {
        return shown.stream().map(TagInfo::id).toList();
    }

    public void reverseForTest(ItemStack s) {
        if (s.isEmpty()) setReverse(ItemStack.EMPTY);
        else receiveGhostDrop(s);
    }

    public void searchForTest(String q) {
        search.setValue(q);
    }

    public void toggleForTest(ResourceLocation id) {
        toggle(id);
    }
}
