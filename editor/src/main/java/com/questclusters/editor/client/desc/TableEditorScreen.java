package com.questclusters.editor.client.desc;

import com.questclusters.editor.data.DescriptionTable;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.KeyModifiers;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/**
 * Visual editor for description tables. Click a cell to type in it (Enter finishes, Shift+Enter adds a line break,
 * Esc discards); drag to select several cells; right-click for merge/split, rows, columns and settings.
 * Ctrl+Z / Ctrl+Y undo and redo.
 */
public final class TableEditorScreen extends BaseScreen {
    private final DescriptionTable table;
    private final Consumer<DescriptionTable> onDone;
    private final TableRenderer renderer;
    private final Deque<String> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    private int selR0, selC0, selR1, selC1;
    private int[] pendingEdit;
    private boolean dragging;
    private int editR = -1, editC = -1, cursor;
    private StringBuilder buf = new StringBuilder();
    private Grid grid;

    private TableEditorScreen(DescriptionTable table, Consumer<DescriptionTable> onDone) {
        this.table = table;
        this.onDone = onDone;
        this.renderer = new TableRenderer(table);
    }

    /** The editor opened last (for the dev self-test). */
    public static TableEditorScreen last;

    public static void open(DescriptionTable table, Consumer<DescriptionTable> onDone) {
        last = new TableEditorScreen(table, onDone);
        last.openGui();
    }

    static Component tr(String key, Object... args) {
        return Component.translatable("quest_clusters_editor.table." + key, args);
    }

    @Override
    public boolean onInit() {
        setWidth(Math.min(getScreen().getGuiScaledWidth() - 20, 360));
        setHeight(getScreen().getGuiScaledHeight() - 30);
        return true;
    }

    @Override
    public void addWidgets() {
        grid = new Grid();
        add(grid);
        add(button("confirm", Icons.ACCEPT, () -> {
            commitEdit();
            onDone.accept(table);
        }));
        add(button("cancel", Icons.CANCEL, this::cancel));
        add(button("undo", Icons.LEFT, this::undo));
        add(button("redo", Icons.RIGHT, this::redo));
        add(button("table_settings", Icons.SETTINGS, this::tableSettings));
    }

    @Override
    public void alignWidgets() {
        grid.setPosAndSize(8, 20, width - 16, height - 48);
        int x = 8;
        for (Widget w : getWidgets()) {
            if (w instanceof SimpleTextButton b) {
                b.setPos(x, height - 24);
                x += b.width + 4;
            }
        }
    }

    private SimpleTextButton button(String key, Icon icon, Runnable r) {
        return new SimpleTextButton(this, tr(key), icon) {
            @Override
            public void onClicked(MouseButton button) {
                playClickSound();
                r.run();
            }
        };
    }

    @Override
    public void drawBackground(GuiGraphics g, Theme theme, int x, int y, int w, int h) {
        super.drawBackground(g, theme, x, y, w, h);
        theme.drawString(g, tr("hint"), x + 8, y + 7, Color4I.GRAY, 0);
    }

    private void cancel() {
        ClientQuestFile.INSTANCE.getQuestScreen().ifPresentOrElse(s -> s.openGui(), ClientQuestFile::openGui);
    }

    // ------------------------------------------------------------------ editing state

    private void snapshot() {
        undo.push(table.encode());
        if (undo.size() > 100) undo.removeLast();
        redo.clear();
    }

    private void restore(String encoded) {
        DescriptionTable t = DescriptionTable.decode(encoded);
        if (t == null) return;
        table.columns = t.columns;
        table.header = t.header;
        table.alignment = t.alignment;
        table.tableWidth = t.tableWidth;
        table.rowHeight = t.rowHeight;
        table.lineWidth = t.lineWidth;
        table.borderColor = t.borderColor;
        table.headerColor = t.headerColor;
        table.cellColor = t.cellColor;
        table.textColor = t.textColor;
        table.rows.clear();
        table.rows.addAll(t.rows);
        table.merges.clear();
        table.merges.addAll(t.merges);
        table.cellStyles.clear();
        table.cellStyles.putAll(t.cellStyles);
        clampSelection();
        relayout();
    }

    private void undo() {
        commitEdit();
        if (!undo.isEmpty()) {
            redo.push(table.encode());
            restore(undo.pop());
        }
    }

    private void redo() {
        commitEdit();
        if (!redo.isEmpty()) {
            undo.push(table.encode());
            restore(redo.pop());
        }
    }

    private void relayout() {
        renderer.layout(grid.width);
    }

    private void clampSelection() {
        int maxR = table.rows.size() - 1, maxC = table.columns - 1;
        selR0 = Math.min(selR0, maxR);
        selR1 = Math.min(selR1, maxR);
        selC0 = Math.min(selC0, maxC);
        selC1 = Math.min(selC1, maxC);
    }

    private void startEdit(int r, int c) {
        editR = r;
        editC = c;
        buf = new StringBuilder(table.cell(r, c));
        cursor = buf.length();
    }

    private void commitEdit() {
        if (editR >= 0) {
            if (!buf.toString().equals(table.cell(editR, editC))) {
                snapshot();
                table.setCell(editR, editC, buf.toString());
                relayout();
            }
            editR = editC = -1;
        }
    }

    private void edit(Runnable change) {
        commitEdit();
        snapshot();
        change.run();
        clampSelection();
        relayout();
    }

    // ------------------------------------------------------------------ keys

    @Override
    public boolean keyPressed(Key key) {
        if (editR >= 0) {
            switch (key.keyCode) {
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    if (Screen.hasShiftDown()) insert("\n");
                    else commitEdit();
                }
                case GLFW.GLFW_KEY_ESCAPE -> editR = editC = -1;
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    if (cursor > 0) buf.deleteCharAt(--cursor);
                }
                case GLFW.GLFW_KEY_DELETE -> {
                    if (cursor < buf.length()) buf.deleteCharAt(cursor);
                }
                case GLFW.GLFW_KEY_LEFT -> cursor = Math.max(0, cursor - 1);
                case GLFW.GLFW_KEY_RIGHT -> cursor = Math.min(buf.length(), cursor + 1);
                case GLFW.GLFW_KEY_HOME -> cursor = 0;
                case GLFW.GLFW_KEY_END -> cursor = buf.length();
                case GLFW.GLFW_KEY_V -> {
                    if (Screen.hasControlDown()) insert(Minecraft.getInstance().keyboardHandler.getClipboard());
                }
                default -> {
                }
            }
            return true;
        }
        if (Screen.hasControlDown() && key.is(GLFW.GLFW_KEY_Z)) {
            undo();
            return true;
        }
        if (Screen.hasControlDown() && key.is(GLFW.GLFW_KEY_Y)) {
            redo();
            return true;
        }
        if (key.is(GLFW.GLFW_KEY_ENTER) || key.is(GLFW.GLFW_KEY_F2)) {
            startEdit(Math.min(selR0, selR1), Math.min(selC0, selC1));
            return true;
        }
        if (key.esc()) {
            cancel();
            return true;
        }
        return super.keyPressed(key);
    }

    @Override
    public boolean charTyped(char c, KeyModifiers modifiers) {
        if (editR >= 0 && c >= ' ') {
            insert(String.valueOf(c));
            return true;
        }
        return super.charTyped(c, modifiers);
    }

    private void insert(String s) {
        int room = DescriptionTable.MAX_CELL - buf.length();
        if (room <= 0) return;
        s = s.length() > room ? s.substring(0, room) : s;
        buf.insert(cursor, s);
        cursor += s.length();
    }

    // ------------------------------------------------------------------ menus

    private void openMenu() {
        int r0 = Math.min(selR0, selR1), r1 = Math.max(selR0, selR1), c0 = Math.min(selC0, selC1), c1 = Math.max(selC0, selC1);
        List<ContextMenuItem> m = new ArrayList<>();
        m.add(item("merge", Icons.ADD, () -> edit(() -> table.merge(r0, c0, r1, c1))));
        m.add(item("split", Icons.REMOVE, () -> edit(() -> table.split(r0, c0, r1, c1))));
        m.add(ContextMenuItem.SEPARATOR);
        m.add(item("row_above", Icons.UP, () -> edit(() -> table.insertRow(r0))));
        m.add(item("row_below", Icons.DOWN, () -> edit(() -> table.insertRow(r1 + 1))));
        m.add(item("row_remove", Icons.CANCEL, () -> edit(() -> {
            for (int i = r1; i >= r0; i--) table.removeRow(i);
        })));
        m.add(item("column_left", Icons.LEFT, () -> edit(() -> table.insertColumn(c0))));
        m.add(item("column_right", Icons.RIGHT, () -> edit(() -> table.insertColumn(c1 + 1))));
        m.add(item("column_remove", Icons.CANCEL, () -> edit(() -> {
            for (int i = c1; i >= c0; i--) table.removeColumn(i);
        })));
        m.add(ContextMenuItem.SEPARATOR);
        m.add(item("cell_settings", Icons.SETTINGS, () -> cellSettings(r0, c0, r1, c1)));
        m.add(item("table_settings", Icons.SETTINGS, this::tableSettings));
        openContextMenu(m);
    }

    private static ContextMenuItem item(String key, Icon icon, Runnable r) {
        return new ContextMenuItem(tr(key), icon, b -> r.run());
    }

    enum Align {
        LEFT, CENTER, RIGHT;
        static final NameMap<Align> NAMES = NameMap.of(CENTER, values())
                .name(a -> tr("align." + a.name().toLowerCase(java.util.Locale.ROOT))).create();
    }

    private void cellSettings(int r0, int c0, int r1, int c1) {
        commitEdit();
        DescriptionTable.Style s = table.style(r0, c0);
        Object[] v = {s.header(), Align.values()[s.alignment()], s.cellWidth(), s.rowHeight(), s.lineWidth(),
                Color4I.rgba(s.borderColor()), Color4I.rgba(s.headerColor()), Color4I.rgba(s.cellColor()), Color4I.rgba(s.textColor())};
        ConfigGroup g = new ConfigGroup("quest_clusters_editor.table", accepted -> {
            openGui();
            if (!accepted) return;
            edit(() -> {
                for (int r = r0; r <= r1; r++) {
                    for (int c = c0; c <= c1; c++) {
                        table.setStyle(r, c, new DescriptionTable.Style((Boolean) v[0], ((Align) v[1]).ordinal(), (Integer) v[2],
                                (Integer) v[3], (Integer) v[4], ((Color4I) v[5]).rgba(), ((Color4I) v[6]).rgba(),
                                ((Color4I) v[7]).rgba(), ((Color4I) v[8]).rgba()));
                    }
                }
            });
        });
        g.addBool("header", (Boolean) v[0], x -> v[0] = x, false);
        g.addEnum("alignment", (Align) v[1], x -> v[1] = x, Align.NAMES);
        g.addInt("cell_width", (Integer) v[2], x -> v[2] = x, 0, 0, 1000);
        g.addInt("row_height", (Integer) v[3], x -> v[3] = x, 18, 12, 30);
        g.addInt("line_width", (Integer) v[4], x -> v[4] = x, 1, 1, 4);
        g.addColor("border_color", (Color4I) v[5], x -> v[5] = x, Color4I.rgba(0xFF999999));
        g.addColor("header_color", (Color4I) v[6], x -> v[6] = x, Color4I.rgba(0xFF374151));
        g.addColor("cell_color", (Color4I) v[7], x -> v[7] = x, Color4I.rgba(0xFF1F2937));
        g.addColor("text_color", (Color4I) v[8], x -> v[8] = x, Color4I.WHITE);
        new EditConfigScreen(g).openGui();
    }

    private void tableSettings() {
        commitEdit();
        DescriptionTable t = table;
        Object[] v = {t.tableWidth, t.header, Align.values()[t.alignment], t.rowHeight, t.lineWidth,
                Color4I.rgba(t.borderColor), Color4I.rgba(t.headerColor), Color4I.rgba(t.cellColor), Color4I.rgba(t.textColor)};
        ConfigGroup g = new ConfigGroup("quest_clusters_editor.table", accepted -> {
            openGui();
            if (!accepted) return;
            edit(() -> {
                t.tableWidth = (Integer) v[0];
                t.header = (Boolean) v[1];
                t.alignment = ((Align) v[2]).ordinal();
                t.rowHeight = (Integer) v[3];
                t.lineWidth = (Integer) v[4];
                t.borderColor = ((Color4I) v[5]).rgba();
                t.headerColor = ((Color4I) v[6]).rgba();
                t.cellColor = ((Color4I) v[7]).rgba();
                t.textColor = ((Color4I) v[8]).rgba();
            });
        });
        g.addInt("table_width", (Integer) v[0], x -> v[0] = x, 0, 0, 1000);
        g.addBool("header_row", (Boolean) v[1], x -> v[1] = x, true);
        g.addEnum("alignment", (Align) v[2], x -> v[2] = x, Align.NAMES);
        g.addInt("row_height", (Integer) v[3], x -> v[3] = x, 18, 12, 30);
        g.addInt("line_width", (Integer) v[4], x -> v[4] = x, 1, 1, 4);
        g.addColor("border_color", (Color4I) v[5], x -> v[5] = x, Color4I.rgba(0xFF999999));
        g.addColor("header_color", (Color4I) v[6], x -> v[6] = x, Color4I.rgba(0xFF374151));
        g.addColor("cell_color", (Color4I) v[7], x -> v[7] = x, Color4I.rgba(0xFF1F2937));
        g.addColor("text_color", (Color4I) v[8], x -> v[8] = x, Color4I.WHITE);
        new EditConfigScreen(g).openGui();
    }

    /** For the dev self-test. */
    public DescriptionTable table() {
        return table;
    }

    // ------------------------------------------------------------------ the grid

    private final class Grid extends Widget {
        Grid() {
            super(TableEditorScreen.this);
        }

        private int[] cell() {
            return renderer.cellAt(getMouseX() - getX(), getMouseY() - getY());
        }

        @Override
        public boolean mousePressed(MouseButton button) {
            if (!isMouseOver()) return false;
            int[] c = cell();
            if (button.isRight()) {
                if (c != null && !inSelection(c[0], c[1])) select(c, c);
                commitEdit();
                openMenu();
                return true;
            }
            if (button.isLeft() && c != null) {
                commitEdit();
                select(c, c);
                pendingEdit = c;
                dragging = true;
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseDragged(int button, double dx, double dy) {
            if (dragging) {
                int[] c = cell();
                if (c != null && (c[0] != selR1 || c[1] != selC1)) {
                    selR1 = c[0];
                    selC1 = c[1];
                    pendingEdit = null;
                }
            }
            return dragging;
        }

        @Override
        public void mouseReleased(MouseButton button) {
            if (dragging && pendingEdit != null) startEdit(pendingEdit[0], pendingEdit[1]);
            dragging = false;
            pendingEdit = null;
        }

        private void select(int[] a, int[] b) {
            selR0 = a[0];
            selC0 = a[1];
            selR1 = b[0];
            selC1 = b[1];
        }

        private boolean inSelection(int r, int c) {
            return r >= Math.min(selR0, selR1) && r <= Math.max(selR0, selR1) && c >= Math.min(selC0, selC1) && c <= Math.max(selC0, selC1);
        }

        @Override
        public void draw(GuiGraphics g, Theme theme, int x, int y, int w, int h) {
            boolean blink = System.currentTimeMillis() / 500 % 2 == 0;
            renderer.draw(g, x, y, w, (r, c) -> r == editR && c == editC
                    ? buf.substring(0, cursor) + (blink ? "|" : " ") + buf.substring(cursor) : null);
            int r0 = Math.min(selR0, selR1), r1 = Math.max(selR0, selR1), c0 = Math.min(selC0, selC1), c1 = Math.max(selC0, selC1);
            int[] a = renderer.rect(r0, c0), b = renderer.rect(r1, c1);
            int x0 = x + Math.min(a[0], b[0]), y0 = y + Math.min(a[1], b[1]), x1 = x + Math.max(a[2], b[2]), y1 = y + Math.max(a[3], b[3]);
            g.fill(x0, y0, x1, y1, editR >= 0 ? 0x18FFFFFF : 0x40FFFFFF);
            g.renderOutline(x0, y0, x1 - x0, y1 - y0, 0xFFFFD700);
        }
    }

    // test hooks
    public void testSelect(int r0, int c0, int r1, int c1) {
        selR0 = r0;
        selC0 = c0;
        selR1 = r1;
        selC1 = c1;
    }

    public void testType(int r, int c, String text) {
        startEdit(r, c);
        buf = new StringBuilder(text);
        cursor = text.length();
        commitEdit();
    }

    public void testMerge() {
        int r0 = Math.min(selR0, selR1), r1 = Math.max(selR0, selR1), c0 = Math.min(selC0, selC1), c1 = Math.max(selC0, selC1);
        edit(() -> table.merge(r0, c0, r1, c1));
    }

    public void testUndo() {
        undo();
    }
}
