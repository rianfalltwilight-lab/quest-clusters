package com.questclusters.editor.client.gui;

import com.questclusters.editor.client.CanvasEdit;
import com.questclusters.editor.client.ClientEditor;
import com.questclusters.editor.client.EditorActions;
import com.questclusters.editor.client.GifTextures;
import com.questclusters.editor.data.CanvasElement;
import com.questclusters.editor.data.ChapterExtras;
import com.questclusters.editor.data.NodeKeys;
import com.questclusters.mixin.client.QuestScreenAccessor;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.SelectQuestObjectScreen;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestButton;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestLink;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.util.ConfigQuestObject;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Editor entries in FTB Quests' right-click menus (edit mode only). */
public final class EditorMenus {
    private EditorMenus() {
    }

    static MutableComponent tr(String key, Object... args) {
        return Component.translatable("quest_clusters_editor.menu." + key, args);
    }

    private static boolean canEdit() {
        return ClientQuestFile.INSTANCE != null && ClientQuestFile.INSTANCE.canEdit();
    }

    private static List<Movable> selected(QuestScreen s) {
        return List.copyOf(((QuestScreenAccessor) s).quest_clusters$getSelectedObjects());
    }

    private static Chapter chapter(QuestScreen s) {
        return ((QuestScreenAccessor) s).quest_clusters$getSelectedChapter();
    }

    private static ContextMenuItem item(String key, Icon icon, Runnable r) {
        return new ContextMenuItem(tr(key), icon, b -> r.run());
    }

    private static Component state(boolean on) {
        return Component.translatable(on ? "quest_clusters_editor.on" : "quest_clusters_editor.off");
    }

    // ------------------------------------------------------------------ single object (quests, links, chapters)

    public static void objectItems(List<ContextMenuItem> menu, QuestScreen screen, QuestObjectBase object) {
        if (!canEdit()) return;
        List<ContextMenuItem> add = new ArrayList<>();
        if (object instanceof Chapter ch) {
            String bg = ClientEditor.book().chapter(ch.id).background;
            add.add(item("chapter_background", Icons.ART, () -> EditorActions.pickImage(screen, bg,
                    v -> EditorActions.editChapter("chapter_background", ch.id, c -> c.background = v))));
            add.add(ContextMenuItem.subMenu(tr("prerequisites"), Icons.LOCK, prerequisiteItems(screen, ch)));
        } else if (object instanceof Quest q) {
            var e = ClientEditor.book().quest(q.id);
            add.add(item("quest_background", Icons.ART, () -> EditorActions.pickImage(screen, e.boxBackground(),
                    v -> EditorActions.editQuests("quest_background", List.of(q.id), x -> x.withBoxBackground(v)))));
            add.add(item("view_background", Icons.ART, () -> EditorActions.pickImage(screen, e.viewBackground(),
                    v -> EditorActions.editQuests("view_background", List.of(q.id), x -> x.withViewBackground(v)))));
            add.add(new ContextMenuItem(tr("scroll_paging", state(e.disableScrollPaging())), Icons.SETTINGS,
                    b -> EditorActions.editQuests("scroll_paging", List.of(q.id), x -> x.withDisableScrollPaging(!e.disableScrollPaging()))));
        }
        if ((object instanceof Quest || object instanceof QuestLink) && !screen.isViewingQuest()) {
            List<ContextMenuItem> lines = hiddenLineItems(screen, (Movable) object);
            if (!lines.isEmpty()) add.add(ContextMenuItem.subMenu(tr("hidden_lines"), Icons.SETTINGS, lines));
        }
        if (!add.isEmpty()) {
            menu.add(ContextMenuItem.SEPARATOR);
            menu.addAll(add);
        }
    }

    private static List<ContextMenuItem> prerequisiteItems(QuestScreen screen, Chapter ch) {
        List<ContextMenuItem> out = new ArrayList<>();
        List<Long> current = ClientEditor.book().chapter(ch.id).prerequisites;
        out.add(item("prerequisites.add", Icons.ADD, () -> {
            ConfigQuestObject<Quest> cfg = new ConfigQuestObject<>(o -> o instanceof Quest q && q.getChapter() != ch && !current.contains(q.id));
            new SelectQuestObjectScreen<>(cfg, accepted -> {
                EditorActions.reopen(screen);
                if (accepted && cfg.getValue() instanceof Quest q) {
                    EditorActions.editChapter("prerequisites", ch.id, c -> {
                        if (!c.prerequisites.contains(q.id)) c.prerequisites.add(q.id);
                    });
                }
            }).openGui();
        }));
        for (long id : current) {
            Component name = ClientQuestFile.INSTANCE.getBase(id) instanceof Quest q ? q.getTitle() : Component.literal(NodeKeys.hex(id));
            out.add(new ContextMenuItem(tr("prerequisites.remove", name), Icons.REMOVE,
                    b -> EditorActions.editChapter("prerequisites", ch.id, c -> c.prerequisites.remove(id))));
        }
        if (!current.isEmpty()) {
            out.add(item("prerequisites.clear", Icons.CANCEL, () -> EditorActions.editChapter("prerequisites", ch.id, c -> c.prerequisites.clear())));
        }
        return out;
    }

    /** Every prerequisite line touching this node: "← X" (it requires X) and "→ Y" (Y requires it). */
    private static List<ContextMenuItem> hiddenLineItems(QuestScreen screen, Movable node) {
        Chapter ch = chapter(screen);
        String self = key(node);
        if (ch == null || self == null) return List.of();
        Map<String, ContextMenuItem> out = new LinkedHashMap<>();
        for (Widget w : screen.questPanel.getWidgets()) {
            if (!(w instanceof QuestButton qb)) continue;
            String from = key(qb.moveAndDeleteFocus());
            if (from == null) continue;
            for (QuestButton dep : qb.getDependencies().values()) {
                String to = key(dep.moveAndDeleteFocus());
                if (to == null) continue;
                if (from.equals(self)) {
                    out.putIfAbsent(from + ":" + to, hiddenLineMenu(ch, from, to, Component.literal("← ").append(dep.moveAndDeleteFocus().getTitle())));
                } else if (to.equals(self)) {
                    out.putIfAbsent(from + ":" + to, hiddenLineMenu(ch, from, to, Component.literal("→ ").append(qb.moveAndDeleteFocus().getTitle())));
                }
            }
        }
        return new ArrayList<>(out.values());
    }

    private static ContextMenuItem hiddenLineMenu(Chapter ch, String source, String dependency, Component label) {
        Optional<ChapterExtras.HiddenLine> cur = ClientEditor.book().chapter(ch.id).hiddenLine(source, dependency);
        int mode = cur.isEmpty() ? 0 : cur.get().revealOnHover() ? 2 : 1;
        String[] keys = {"hidden_lines.show", "hidden_lines.hide", "hidden_lines.hover"};
        List<ContextMenuItem> choices = new ArrayList<>();
        for (int m = 0; m < 3; m++) {
            int mm = m;
            choices.add(new ContextMenuItem(tr(keys[m]), m == mode ? Icons.ACCEPT : Icon.empty(), b ->
                    EditorActions.editChapter("hidden_line", ch.id, c -> {
                        c.hiddenLines.removeIf(h -> h.source().equals(source) && h.dependency().equals(dependency));
                        if (mm > 0) c.hiddenLines.add(new ChapterExtras.HiddenLine(source, dependency, mm == 2));
                    })));
        }
        MutableComponent text = label.copy().append(Component.literal("  ")).append(tr(keys[mode]).withStyle(net.minecraft.ChatFormatting.GRAY));
        return ContextMenuItem.subMenu(text, mode == 0 ? Icons.ACCEPT_GRAY : Icons.CANCEL, choices);
    }

    // ------------------------------------------------------------------ multi-selection (quest buttons)

    public static void selectionItems(List<ContextMenuItem> menu, QuestScreen screen) {
        if (!canEdit()) return;
        List<Movable> sel = selected(screen);
        List<ContextMenuItem> add = new ArrayList<>();
        if (sel.size() >= 2 && sel.stream().allMatch(m -> m instanceof Quest)) {
            List<Long> ids = sel.stream().map(Movable::getMovableID).toList();
            String first = ClientEditor.book().quest(ids.getFirst()).boxBackground();
            String firstView = ClientEditor.book().quest(ids.getFirst()).viewBackground();
            add.add(item("bulk_quest_background", Icons.ART, () -> EditorActions.pickImage(screen, first,
                    v -> EditorActions.editQuests("quest_background", ids, x -> x.withBoxBackground(v)))));
            add.add(item("bulk_view_background", Icons.ART, () -> EditorActions.pickImage(screen, firstView,
                    v -> EditorActions.editQuests("view_background", ids, x -> x.withViewBackground(v)))));
        }
        add.addAll(lineAndTransformItems(screen, sel));
        if (!add.isEmpty()) {
            menu.add(ContextMenuItem.SEPARATOR);
            menu.addAll(add);
        }
    }

    private static List<ContextMenuItem> lineAndTransformItems(QuestScreen screen, List<Movable> sel) {
        List<ContextMenuItem> add = new ArrayList<>();
        Chapter ch = chapter(screen);
        List<String> nodes = new ArrayList<>();
        for (Movable m : sel) {
            String k = key(m);
            if (k == null) {
                nodes = null;
                break;
            }
            if (!nodes.contains(k)) nodes.add(k);
        }
        if (ch != null && nodes != null && nodes.size() >= 2) {
            List<String> line = nodes;
            List<List<String>> lines = ClientEditor.book().chapter(ch.id).decorativeLines;
            if (lines.stream().noneMatch(l -> l.containsAll(line))) {
                add.add(item("decorative.add", Icons.ADD, () -> EditorActions.editChapter("decorative_line", ch.id, c -> c.decorativeLines.add(line))));
            }
            if (lines.stream().anyMatch(l -> l.containsAll(line))) {
                add.add(item("decorative.remove", Icons.REMOVE, () -> EditorActions.editChapter("decorative_line", ch.id, c -> {
                    for (int i = 0; i < c.decorativeLines.size(); i++) {
                        if (c.decorativeLines.get(i).containsAll(line)) {
                            c.decorativeLines.remove(i);
                            return;
                        }
                    }
                })));
            }
        }
        if (sel.size() >= 2) {
            add.add(ContextMenuItem.subMenu(tr("transform"), Icons.SETTINGS, transformItems(screen)));
        }
        return add;
    }

    private static List<ContextMenuItem> transformItems(QuestScreen screen) {
        String[] keys = {"transform.rotate_cw", "transform.rotate_ccw", "transform.mirror_h", "transform.mirror_v"};
        List<ContextMenuItem> out = new ArrayList<>();
        for (int i = 0; i < keys.length; i++) {
            int mode = i;
            out.add(item(keys[i], Icons.SETTINGS, () -> CanvasEdit.transform(screen, mode)));
        }
        return out;
    }

    // ------------------------------------------------------------------ chapter images

    public static void imageItems(List<ContextMenuItem> menu, QuestScreen screen, ChapterImage img) {
        if (!canEdit()) return;
        List<ContextMenuItem> add = new ArrayList<>();
        CanvasElement el = CanvasElement.parse(img.getClickAction().actionData());
        if (el instanceof CanvasElement.Text t) {
            add.add(item("canvas_text.edit", Icons.NOTES, () -> CanvasEdit.editText(screen, img, t)));
        } else if (el instanceof CanvasElement.Gif) {
            List<ContextMenuItem> gifs = gifItems(rl -> CanvasEdit.setPayload(img, new CanvasElement.Gif(rl.toString()).encode()));
            if (!gifs.isEmpty()) add.add(ContextMenuItem.subMenu(tr("gif.change"), Icons.ART, gifs));
        }
        boolean ex = ClientEditor.book().excludedImages.contains(img.id);
        add.add(new ContextMenuItem(tr("box_select", state(ex)), Icons.SETTINGS, b -> EditorActions.setExcluded(List.of(img.id), !ex)));
        List<Movable> sel = selected(screen);
        if (sel.size() >= 2 && sel.contains(img)) add.addAll(lineAndTransformItems(screen, sel));
        // the menu starts with a title and a separator: put ours after FTB Quests' own entries
        menu.add(ContextMenuItem.SEPARATOR);
        menu.addAll(add);
    }

    // ------------------------------------------------------------------ empty canvas

    public static void canvasItems(List<ContextMenuItem> menu, QuestScreen screen, double qx, double qy) {
        if (!canEdit()) return;
        Chapter ch = chapter(screen);
        if (ch == null) return;
        List<ContextMenuItem> add = new ArrayList<>();
        add.add(item("canvas_text.add", Icons.NOTES, () -> CanvasEdit.addText(screen, ch, qx, qy)));
        add.add(item("guide_point.add", Icons.ADD, () -> CanvasEdit.addGuidePoint(ch, qx, qy)));
        List<ContextMenuItem> gifs = gifItems(rl -> CanvasEdit.addGif(ch, qx, qy, rl));
        add.add(gifs.isEmpty() ? new ContextMenuItem(tr("gif.none"), Icons.ART, b -> {
        }).setEnabled(false) : ContextMenuItem.subMenu(tr("gif.add"), Icons.ART, gifs));
        if (CanvasEdit.hasMultiClipboard()) {
            add.add(new ContextMenuItem(tr("paste_many", CanvasEdit.clipboardSize()), Icons.ADD,
                    b -> CanvasEdit.paste(ch, qx, qy, true)));
        }
        if (selected(screen).size() >= 2) {
            add.add(ContextMenuItem.subMenu(tr("transform"), Icons.SETTINGS, transformItems(screen)));
        }
        menu.add(ContextMenuItem.SEPARATOR);
        menu.addAll(add);
    }

    private static List<ContextMenuItem> gifItems(java.util.function.Consumer<ResourceLocation> pick) {
        List<ContextMenuItem> out = new ArrayList<>();
        for (ResourceLocation rl : GifTextures.list()) {
            out.add(new ContextMenuItem(Component.literal(rl.toString()), Icons.ART, b -> pick.accept(rl)));
            if (out.size() >= 64) break;
        }
        return out;
    }

    // ------------------------------------------------------------------ quest book title button (sidebar)

    public static void sidebarItems(List<ContextMenuItem> menu, QuestScreen screen) {
        if (!canEdit()) return;
        menu.add(ContextMenuItem.SEPARATOR);
        menu.add(item("sidebar_background", Icons.ART, () -> EditorActions.pickImage(screen, ClientEditor.book().sidebarBackground,
                EditorActions::setSidebar)));
    }

    static String key(Movable m) {
        if (m instanceof Quest q) return NodeKeys.quest(q.id);
        if (m instanceof QuestLink l) return NodeKeys.link(l.id);
        if (m instanceof ChapterImage img && CanvasElement.parse(img.getClickAction().actionData()) instanceof CanvasElement.Anchor a) {
            return a.nodeKey();
        }
        return null;
    }
}
