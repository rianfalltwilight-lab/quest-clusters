package com.questclusters.filters.client.screen;

import com.questclusters.filters.client.ItemSourcesAccess;
import com.questclusters.filters.filter.FuelFilter;
import dev.ftb.mods.ftbfiltersystem.api.NumericComparison;
import dev.ftb.mods.ftbfiltersystem.api.NumericComparison.ComparisonOp;
import dev.ftb.mods.ftbfiltersystem.api.client.gui.AbstractFilterScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Editor for {@code fuel(...)}: furnace burn time in ticks (200 ticks smelt one item). */
public class FuelScreen extends FilterScreenBase<FuelFilter> {
    private static final ComparisonOp[] ORDER = {ComparisonOp.GE, ComparisonOp.GT, ComparisonOp.EQ, ComparisonOp.NE, ComparisonOp.LE, ComparisonOp.LT};
    private static final int COLS = 10;
    private ComparisonOp op;
    private int value;
    private final String original;
    private EditBox number;
    private Button opButton;
    private List<ItemStack> fuels;
    private List<ItemStack> matching = List.of();
    private int scroll;

    public FuelScreen(FuelFilter filter, AbstractFilterScreen parent) {
        super(filter, parent, COLS * 18 + 16, 18 + 18 + 12 + 22 + 3 * 18 + 6);
        op = filter.comparison().op();
        value = filter.comparison().value();
        original = filter.comparison().toString();
    }

    private NumericComparison comparison() {
        return new NumericComparison(op, value, false);
    }

    @Override
    protected void init() {
        super.init();
        int y = topPos + 19;
        opButton = addRenderableWidget(Button.builder(Component.literal(op.toString()), b -> {
            int i = java.util.Arrays.asList(ORDER).indexOf(op);
            op = ORDER[(i + 1) % ORDER.length];
            b.setMessage(Component.literal(opString(op)));
            update();
        }).bounds(leftPos + 8, y, 24, 16).tooltip(Tooltip.create(tr("fuel.op"))).build());
        opButton.setMessage(Component.literal(opString(op)));
        number = new EditBox(font, leftPos + 36, y + 1, 60, 14, Component.empty());
        number.setValue(String.valueOf(value));
        number.setFilter(s -> s.isEmpty() || s.matches("\\d{0,9}"));
        number.setResponder(s -> {
            value = s.isEmpty() ? 0 : Integer.parseInt(s);
            update();
        });
        number.setTooltip(Tooltip.create(tr("fuel.number")));
        addRenderableWidget(number);
        setInitialFocus(number);
        // presets: burn times read from the game (data packs may change them)
        int px = leftPos + 8, py = y + 30;
        preset(px, py, "any", ComparisonOp.GT, 0, ItemStack.EMPTY);
        preset(px + 30, py, "coal", ComparisonOp.GE, FuelFilter.burnTime(new ItemStack(Items.COAL)), new ItemStack(Items.COAL));
        preset(px + 50, py, "blaze_rod", ComparisonOp.GE, FuelFilter.burnTime(new ItemStack(Items.BLAZE_ROD)), new ItemStack(Items.BLAZE_ROD));
        preset(px + 70, py, "coal_block", ComparisonOp.GE, FuelFilter.burnTime(new ItemStack(Items.COAL_BLOCK)), new ItemStack(Items.COAL_BLOCK));
        preset(px + 90, py, "lava_bucket", ComparisonOp.GE, FuelFilter.burnTime(new ItemStack(Items.LAVA_BUCKET)), new ItemStack(Items.LAVA_BUCKET));
        preset(px + 110, py, "none", ComparisonOp.EQ, 0, ItemStack.EMPTY);
        update();
    }

    static String opString(ComparisonOp op) {
        return new NumericComparison(op, 0, false).toString().replace("0", "");
    }

    private void preset(int x, int y, String key, ComparisonOp pop, int pv, ItemStack icon) {
        Component tip = tr("fuel.preset." + key).append(Component.literal(" (" + opString(pop) + pv + ")").withStyle(ChatFormatting.GRAY));
        IconButton b = new IconButton(x, y, icon.isEmpty() ? 28 : 18, 18, key.equals("any") ? ">0" : "=0", tip, () -> {
            op = pop;
            value = pv;
            number.setValue(String.valueOf(pv));
            opButton.setMessage(Component.literal(opString(op)));
            update();
        }, () -> op == pop && value == pv);
        if (!icon.isEmpty()) b.withIcon(icon);
        addRenderableWidget(b);
    }

    private List<ItemStack> fuels() {
        if (fuels == null) {
            Map<net.minecraft.world.item.Item, ItemStack> out = new LinkedHashMap<>();
            var mc = minecraft;
            CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(), false, mc.level.registryAccess());
            for (ItemStack s : CreativeModeTabs.searchTab().getDisplayItems()) {
                if (FuelFilter.burnTime(s) > 0) out.putIfAbsent(s.getItem(), new ItemStack(s.getItem()));
            }
            fuels = new ArrayList<>(out.values());
            fuels.sort(Comparator.comparingInt(FuelFilter::burnTime).reversed());
        }
        return fuels;
    }

    private void update() {
        NumericComparison c = comparison();
        matching = fuels().stream().filter(s -> c.test(FuelFilter.burnTime(s))).toList();
        scroll = 0;
    }

    @Override
    protected FuelFilter makeNewFilter() {
        return saved(new FuelFilter(filter.getParent(), comparison()));
    }

    @Override
    protected boolean changed() {
        return !comparison().toString().equals(original);
    }

    @Override
    protected boolean canSave() {
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (number.isMouseOver(mx, my)) {
            int step = Screen.hasShiftDown() ? 1 : 100;
            value = Math.max(0, value + (int) Math.signum(dy) * step);
            number.setValue(String.valueOf(value));
            update();
            return true;
        }
        int rowsTotal = (matching.size() + COLS - 1) / COLS;
        scroll = Math.max(0, Math.min(Math.max(0, rowsTotal - 3), scroll - (int) Math.signum(dy)));
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int y = topPos + 19;
        g.drawString(font, tr("fuel.ticks", String.format("%.2f", value / 200.0).replaceAll("\\.?0+$", "")), leftPos + 100, y + 4, 0xFF404040, false);
        g.drawString(font, tr("fuel.presets"), leftPos + 8, y + 20, 0xFF404040, false);
        int gy = y + 52;
        boolean zero = comparison().test(0);
        Component head = zero ? tr("fuel.matching_zero", matching.size()) : tr("fuel.matching", matching.size());
        g.drawString(font, head, leftPos + 8, gy, 0xFF404040, false);
        ItemStack hovered = null;
        for (int i = 0; i < 3 * COLS; i++) {
            int idx = scroll * COLS + i, x = leftPos + 8 + (i % COLS) * 18, sy = gy + 10 + (i / COLS) * 18;
            ItemListScreen.slot(g, x, sy);
            if (idx >= matching.size()) continue;
            g.renderItem(matching.get(idx), x + 1, sy + 1);
            if (mx >= x && mx < x + 18 && my >= sy && my < sy + 18) hovered = matching.get(idx);
        }
        renderHint(g, gy - 2);
        if (hovered != null) {
            int t = FuelFilter.burnTime(hovered);
            g.renderComponentTooltip(font, List.of(hovered.getHoverName(),
                    tr("fuel.item_ticks", t, String.format("%.2f", t / 200.0).replaceAll("\\.?0+$", "")).withStyle(ChatFormatting.GRAY)), mx, my);
        }
    }

    // for the dev self-test
    public void setForTest(ComparisonOp o, int v) {
        op = o;
        value = v;
        opButton.setMessage(Component.literal(opString(op)));
        number.setValue(String.valueOf(v));
        update();
    }

    public List<ItemStack> matchingForTest() {
        return matching;
    }

}
