package com.questclusters.editor.client.gui;

import com.questclusters.editor.client.ClientEditor;
import com.questclusters.editor.client.EditorActions;
import com.questclusters.editor.data.BookExtras;
import com.questclusters.editor.data.ChapterExtras;
import com.questclusters.editor.data.EditorPatch;
import com.questclusters.editor.data.LinePreset;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.config.ui.EditStringConfigOverlay;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.ui.misc.AbstractButtonListScreen;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Edit mode: the quest book's decorative line presets (name + image), edited on a local copy and sent as one
 * undoable change on Accept, like FTB Quests' reward table list. Deleting a preset also clears it from chapters and
 * nodes, which then fall back to the chapter default / built-in look.
 */
public final class LinePresetsScreen extends AbstractButtonListScreen {
    private final QuestScreen questScreen;
    private final List<LinePreset> presets;
    private final SimpleTextButton addButton;
    private boolean changed;

    public LinePresetsScreen(QuestScreen questScreen) {
        this.questScreen = questScreen;
        this.presets = new ArrayList<>(ClientEditor.book().linePresets);
        setTitle(Component.translatable("quest_clusters_editor.line_presets"));
        setHasSearchBox(false);
        setBorder(1, 1, 1);
        addButton = new SimpleTextButton(topPanel, Component.translatable("gui.add"), Icons.ADD) {
            @Override
            public void onClicked(MouseButton button) {
                playClickSound();
                if (presets.size() >= LinePreset.MAX) {
                    return;
                }
                askName("", name -> pickImage("", texture -> {
                    presets.add(new LinePreset(LinePreset.newId(), name, texture));
                    changed = true;
                }));
            }
        };
    }

    /** Name first (a small text box over this screen), then the image. */
    private void askName(String current, java.util.function.Consumer<String> then) {
        StringConfig cfg = new StringConfig();
        cfg.setValue(current);
        EditStringConfigOverlay<String> overlay = new EditStringConfigOverlay<>(this, cfg, accepted -> {
            String name = cfg.getValue() == null ? "" : cfg.getValue().strip();
            if (accepted && !name.isEmpty()) {
                then.accept(name.length() > LinePreset.MAX_NAME ? name.substring(0, LinePreset.MAX_NAME) : name);
            }
        }, Component.translatable("quest_clusters_editor.line_presets.name")).atPosition(addButton.getX() - 120, addButton.getY() + 22);
        overlay.setExtraZlevel(300);
        pushModalPanel(overlay);
    }

    /** FTB Library's image picker; comes back to this screen either way. */
    private void pickImage(String current, java.util.function.Consumer<String> then) {
        EditorActions.pickImage(null, current, texture -> {
            if (!texture.isEmpty()) {
                then.accept(texture);
                refreshWidgets();
            }
        }, () -> {
            openGui();
            refreshWidgets();
        });
    }

    @Override
    public void addButtons(Panel panel) {
        for (int i = 0; i < presets.size(); i++) {
            panel.add(new PresetButton(panel, i));
        }
    }

    @Override
    protected int getTopPanelHeight() {
        return 25;
    }

    @Override
    protected Panel createTopPanel() {
        return new TitleTopPanel();
    }

    @Override
    public boolean onInit() {
        int widest = presets.stream().mapToInt(p -> getTheme().getStringWidth(p.name())).max().orElse(0);
        setWidth(Math.max(getTheme().getStringWidth(getTitle()) + 110, widest + 60));
        setHeight(getGui().getScreen().getGuiScaledHeight() * 3 / 4);
        return true;
    }

    @Override
    public boolean onClosedByKey(Key key) {
        if (super.onClosedByKey(key)) {
            doCancel();
            return true;
        }
        return false;
    }

    @Override
    protected void doCancel() {
        if (changed) {
            openYesNo(Component.translatable("ftblibrary.unsaved_changes"), Component.empty(), () -> EditorActions.reopen(questScreen));
        } else {
            EditorActions.reopen(questScreen);
        }
    }

    @Override
    protected void doAccept() {
        if (changed) {
            EditorActions.send("line_presets", patch(ClientEditor.book(), presets));
        }
        EditorActions.reopen(questScreen);
    }

    /** The new list, plus every chapter that still points at a preset that is gone (cleared). */
    public static EditorPatch patch(BookExtras book, List<LinePreset> presets) {
        EditorPatch p = new EditorPatch();
        p.presets = List.copyOf(presets);
        java.util.Set<String> ids = new java.util.HashSet<>();
        presets.forEach(x -> ids.add(x.id()));
        book.chapters.forEach((id, c) -> {
            boolean stale = !c.linePreset.isEmpty() && !ids.contains(c.linePreset)
                    || c.nodeLinePresets.values().stream().anyMatch(v -> !ids.contains(v));
            if (stale) {
                ChapterExtras copy = c.copy();
                if (!ids.contains(copy.linePreset)) copy.linePreset = "";
                copy.nodeLinePresets.values().removeIf(v -> !ids.contains(v));
                p.chapters.put(id, copy);
            }
        });
        return p;
    }

    private static Component title(LinePreset p) {
        var t = Component.literal(p.name());
        return p.scroll() == 0F ? t : t.append(Component.literal("  " + speed(p.scroll())).withStyle(ChatFormatting.GRAY));
    }

    private static String speed(float v) {
        return v == 0F ? "0" : (v > 0 ? "→ " : "← ") + String.format(java.util.Locale.ROOT, "%.2g", Math.abs(v)) + "/s";
    }

    private class TitleTopPanel extends TopPanel {
        @Override
        public void addWidgets() {
            add(addButton);
        }

        @Override
        public void alignWidgets() {
            addButton.setPosAndSize(width - addButton.width - 2, 1, addButton.width, 20);
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            super.draw(graphics, theme, x, y, w, h);
            theme.drawString(graphics, getGui().getTitle(), x + 6, y + 6, Theme.SHADOW);
        }
    }

    /** One preset: its image as icon; click for rename / change image / delete. */
    private class PresetButton extends SimpleTextButton {
        private final int index;

        PresetButton(Panel panel, int index) {
            super(panel, title(presets.get(index)), Icon.getIcon(presets.get(index).texture()));
            this.index = index;
            setHeight(18);
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            LinePreset p = presets.get(index);
            getGui().openContextMenu(List.of(
                    new ContextMenuItem(Component.translatable("quest_clusters_editor.line_presets.rename"), Icons.SETTINGS,
                            b -> askName(p.name(), name -> {
                                presets.set(index, new LinePreset(p.id(), name, p.texture(), p.scroll()));
                                changed = true;
                                refreshWidgets();
                            })),
                    new ContextMenuItem(Component.translatable("quest_clusters_editor.line_presets.image"), Icons.ART,
                            b -> pickImage(p.texture(), texture -> {
                                presets.set(index, new LinePreset(p.id(), p.name(), texture, p.scroll()));
                                changed = true;
                            })),
                    ContextMenuItem.subMenu(Component.translatable("quest_clusters_editor.line_presets.gif"), Icons.ART, gifItems(p)),
                    new ContextMenuItem(Component.translatable("quest_clusters_editor.line_presets.scroll", speed(p.scroll())), Icons.SETTINGS,
                            b -> askScroll(p)),
                    new ContextMenuItem(Component.translatable("gui.remove").withStyle(ChatFormatting.RED), Icons.BIN, b -> {
                        presets.remove(index);
                        changed = true;
                        refreshWidgets();
                    })));
        }

        private java.util.List<ContextMenuItem> gifItems(LinePreset p) {
            java.util.List<ContextMenuItem> out = new java.util.ArrayList<>();
            var gifs = com.questclusters.editor.client.GifTextures.list();
            if (gifs.isEmpty()) {
                out.add(new ContextMenuItem(Component.translatable("quest_clusters_editor.line_presets.gif.none"), Icons.ART, b -> {
                }).setEnabled(false));
            }
            for (var gif : gifs) {
                out.add(new ContextMenuItem(Component.literal(gif.toString()), Icons.ART, b -> {
                    presets.set(index, new LinePreset(p.id(), p.name(), gif.toString(), p.scroll()));
                    changed = true;
                    refreshWidgets();
                }));
            }
            return out;
        }

        /** Repeats per second along the line, -20..20 (negative flows towards the line's start); 0 = still. */
        private void askScroll(LinePreset p) {
            var cfg = new dev.ftb.mods.ftblibrary.config.DoubleConfig(-LinePreset.MAX_SCROLL, LinePreset.MAX_SCROLL);
            cfg.setValue((double) p.scroll());
            var overlay = new EditStringConfigOverlay<>(LinePresetsScreen.this, cfg, accepted -> {
                if (accepted && cfg.getValue() != null) {
                    float v = (float) Math.max(-LinePreset.MAX_SCROLL, Math.min(LinePreset.MAX_SCROLL, cfg.getValue()));
                    presets.set(index, presets.get(index).withScroll(v));
                    changed = true;
                    refreshWidgets();
                }
            }, Component.translatable("quest_clusters_editor.line_presets.scroll.title")).atPosition(getX() + 20, getY() + height);
            overlay.setExtraZlevel(300);
            pushModalPanel(overlay);
        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            if (isMouseOver) {
                Color4I.WHITE.withAlpha(30).draw(graphics, x, y, w, h);
            }
            Color4I.GRAY.withAlpha(40).draw(graphics, x, y + h, w, 1);
        }
    }
}
