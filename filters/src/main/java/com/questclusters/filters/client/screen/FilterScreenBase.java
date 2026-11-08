package com.questclusters.filters.client.screen;

import dev.ftb.mods.ftbfiltersystem.api.client.gui.AbstractFilterConfigScreen;
import dev.ftb.mods.ftbfiltersystem.api.client.gui.AbstractFilterScreen;
import dev.ftb.mods.ftbfiltersystem.api.filter.SmartFilter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Shared behaviour of the filter config screens: Enter saves; Esc / Cancel with changes asks first; "Done" is only
 * active when something is selected; floating hints; typing anywhere goes to the search box; window-height rows.
 */
abstract class FilterScreenBase<T extends SmartFilter> extends AbstractFilterConfigScreen<T> {
    protected EditBox search;
    private Button doneButton;
    private boolean saving, confirmed;
    private Component hint;
    private long hintUntil;

    FilterScreenBase(T filter, AbstractFilterScreen parent, int width, int height) {
        super(filter, parent, width, height);
    }

    static net.minecraft.network.chat.MutableComponent tr(String key, Object... args) {
        return Component.translatable("quest_clusters_filters." + key, args);
    }

    /** 4 to 8 rows of 18 px, depending on the window height left after {@code fixed} pixels. */
    static int rowsFor(int fixed) {
        int h = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return Math.max(4, Math.min(8, (h - fixed - 40) / 18));
    }

    /** True if the current state differs from the filter the screen was opened with. */
    protected abstract boolean changed();

    /** False when nothing is selected (then "Done" is inactive). */
    protected abstract boolean canSave();

    @Override
    protected void init() {
        super.init();
        for (var r : renderables) {
            if (r instanceof Button b && b.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                    && t.getKey().equals("gui.done")) {
                doneButton = b;
            }
        }
    }

    /** Called by FTB Filter System's "Done": remember that the screen closes by saving. */
    protected final T saved(T newFilter) {
        if (newFilter != null) saving = true;
        return newFilter;
    }

    @Override
    public void tick() {
        super.tick();
        if (doneButton != null) doneButton.active = canSave();
    }

    @Override
    public void onClose() {
        if (!saving && !confirmed && changed()) {
            minecraft.setScreen(new ConfirmScreen(discard -> {
                if (discard) {
                    confirmed = true;
                    onClose();
                } else {
                    minecraft.setScreen(this);
                }
            }, tr("unsaved.title"), tr("unsaved.message"), tr("unsaved.discard"), tr("unsaved.keep")) {
                @Override
                public boolean keyPressed(int key, int scan, int mods) {
                    if (key == GLFW.GLFW_KEY_ESCAPE) {
                        minecraft.setScreen(FilterScreenBase.this);
                        return true;
                    }
                    return super.keyPressed(key, scan, mods);
                }
            });
            return;
        }
        super.onClose();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            if (canSave()) applyChanges();
            return true;
        }
        if (search != null && !search.isFocused() && key == GLFW.GLFW_KEY_BACKSPACE && !search.getValue().isEmpty()) {
            search.setValue(search.getValue().substring(0, search.getValue().length() - 1));
            return true;
        }
        if (search == null || !search.isFocused()) {
            if (Screen.hasControlDown() && key == GLFW.GLFW_KEY_C) {
                copy();
                return true;
            }
            if (Screen.hasControlDown() && key == GLFW.GLFW_KEY_V) {
                paste();
                return true;
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (search != null && !search.isFocused() && c >= ' ' && !Screen.hasControlDown()) {
            setFocused(search);
            search.setFocused(true);
            return search.charTyped(c, mods);
        }
        return super.charTyped(c, mods);
    }

    protected void copy() {
    }

    protected void paste() {
    }

    /** Copies text to the clipboard, reading it back to make sure (another program may hold the clipboard). */
    protected boolean setClipboard(String text) {
        for (int i = 0; i < 3; i++) {
            minecraft.keyboardHandler.setClipboard(text);
            if (text.equals(minecraft.keyboardHandler.getClipboard())) return true;
            try {
                Thread.sleep(30);
            } catch (InterruptedException ignored) {
            }
        }
        showHint(tr("clipboard.busy"));
        return false;
    }

    protected void showHint(Component text) {
        hint = text;
        int lines = font.split(text, guiWidth - 20).size();
        hintUntil = System.currentTimeMillis() + 2500 + 1200L * Math.max(0, lines - 1);
    }

    /** Draws the floating hint with its bottom edge at {@code bottom}. */
    protected void renderHint(GuiGraphics g, int bottom) {
        if (hint == null || System.currentTimeMillis() > hintUntil) return;
        List<FormattedCharSequence> lines = font.split(hint, guiWidth - 24);
        lines = lines.subList(0, Math.min(6, lines.size()));
        long left = hintUntil - System.currentTimeMillis();
        int alpha = (int) Math.min(255, left / 2);
        int h = lines.size() * 10 + 4, top = bottom - h;
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.fill(leftPos + 10, top, leftPos + guiWidth - 10, bottom, (Math.min(0xD0, alpha) << 24) | 0x202020);
        int y = top + 3;
        for (FormattedCharSequence l : lines) {
            g.drawString(font, l, leftPos + 14, y, (alpha << 24) | 0xFFFFFF, false);
            y += 10;
        }
        g.pose().popPose();
    }

    /** Search box with a magnifier hint and an × that clears it. */
    protected EditBox makeSearch(int x, int y, int w, String value, java.util.function.Consumer<String> onChange) {
        EditBox box = new EditBox(font, x, y, w - 12, 14, Component.empty());
        box.setMaxLength(256);
        box.setValue(value);
        box.setHint(Component.literal("🔍 ").append(tr("search.hint")).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        box.setResponder(onChange);
        box.setTooltip(Tooltip.create(tr("search.tooltip")));
        addRenderableWidget(box);
        addRenderableWidget(new IconButton(x + w - 11, y + 1, 12, 12, "×", tr("search.clear"), () -> box.setValue(""), () -> false));
        search = box;
        return box;
    }

    /** A small square button showing a glyph, with a tooltip; {@code pressed} draws it as toggled on. */
    static final class IconButton extends AbstractWidget {
        private final String glyph;
        private final Runnable action;
        private final BooleanSupplier pressed;
        net.minecraft.world.item.ItemStack icon = net.minecraft.world.item.ItemStack.EMPTY;

        IconButton(int x, int y, int w, int h, String glyph, Component tooltip, Runnable action, BooleanSupplier pressed) {
            super(x, y, w, h, tooltip);
            this.glyph = glyph;
            this.action = action;
            this.pressed = pressed;
            setTooltip(Tooltip.create(tooltip));
        }

        IconButton withIcon(net.minecraft.world.item.ItemStack stack) {
            this.icon = stack;
            return this;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean on = pressed.getAsBoolean();
            int bg = on ? 0xFF8BC34A : isHovered() ? 0xFFB0B0B0 : 0xFFC6C6C6;
            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF555555);
            g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, bg);
            if (on) g.fill(getX() + 1, getY() + height - 2, getX() + width - 1, getY() + height - 1, 0xFF2E7D32);
            var font = net.minecraft.client.Minecraft.getInstance().font;
            if (!icon.isEmpty()) {
                g.pose().pushPose();
                float s = (Math.min(width, height) - 2) / 16F;
                g.pose().translate(getX() + (width - 16 * s) / 2F, getY() + (height - 16 * s) / 2F, 0);
                g.pose().scale(s, s, 1);
                g.renderFakeItem(icon, 0, 0);
                g.pose().popPose();
            } else {
                g.drawString(font, glyph, getX() + (width - font.width(glyph)) / 2 + 1, getY() + (height - 8) / 2, 0xFF303030, false);
            }
        }

        @Override
        public void onClick(double mx, double my) {
            action.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            defaultButtonNarrationText(out);
        }
    }
}
