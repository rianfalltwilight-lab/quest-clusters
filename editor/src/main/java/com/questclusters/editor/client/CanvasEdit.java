package com.questclusters.editor.client;

import com.questclusters.editor.data.CanvasElement;
import com.questclusters.mixin.client.QuestScreenAccessor;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.misc.SimpleToast;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.net.CopyChapterImageMessage;
import dev.ftb.mods.ftbquests.net.CopyQuestMessage;
import dev.ftb.mods.ftbquests.net.CreateObjectMessage;
import dev.ftb.mods.ftbquests.net.EditObjectMessage;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestLink;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

/** Creating and editing special canvas elements; transforming and multi-copying selections. */
public final class CanvasEdit {
    public static final String CLIPBOARD_MARKER = "<quest-clusters-multi>";

    private record ClipEntry(long id, boolean quest, double dx, double dy) {
    }

    private static List<ClipEntry> clipboard = List.of();

    private CanvasEdit() {
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable("quest_clusters_editor." + key, args);
    }

    // ------------------------------------------------------------------ creation

    private static void create(Chapter chapter, double x, double y, double w, double h, String image, String payload) {
        ChapterImage img = new ChapterImage(0L, chapter);
        CompoundTag t = new CompoundTag();
        t.putDouble("x", x);
        t.putDouble("y", y);
        t.putDouble("width", w);
        t.putDouble("height", h);
        t.putString("image", image);
        t.putString("click_action", "none:" + payload);
        img.readData(t, ClientQuestFile.INSTANCE.holderLookup());
        NetworkManager.sendToServer(CreateObjectMessage.requestCreation(img));
    }

    public static void addGuidePoint(Chapter chapter, double x, double y) {
        create(chapter, x, y, 0.4, 0.4, "", new CanvasElement.Anchor(UUID.randomUUID().toString()).encode());
    }

    public static void addText(QuestScreen screen, Chapter chapter, double x, double y) {
        openTextEditor(screen, new CanvasElement.Text("", CanvasElement.DEFAULT_FONT, 1.0), t -> createText(screen, chapter, x, y, t));
    }

    /** Sized to fit the text at the current zoom, like a typed label. */
    public static void createText(QuestScreen screen, Chapter chapter, double x, double y, CanvasElement.Text t) {
        var font = Minecraft.getInstance().font;
        Component c = Component.literal(t.text()).withStyle(Style.EMPTY.withFont(ResourceLocation.parse(t.font())));
        double bs = Math.max(1, screen.getQuestButtonSize());
        create(chapter, x, y, Math.max(0.25, (font.width(c) + 2) / bs), Math.max(0.25, (font.lineHeight + 2) / bs), "", t.encode());
    }

    public static void editText(QuestScreen screen, ChapterImage img, CanvasElement.Text current) {
        openTextEditor(screen, current, t -> setPayload(img, t.encode()));
    }

    public static void addGif(Chapter chapter, double x, double y, ResourceLocation gif) {
        float aspect = GifTextures.aspect(gif);
        create(chapter, x, y, 4.0, Math.max(0.25, 4.0 / aspect), "", new CanvasElement.Gif(gif.toString()).encode());
    }

    public static void setPayload(ChapterImage img, String payload) {
        CompoundTag t = new CompoundTag();
        img.writeData(t, ClientQuestFile.INSTANCE.holderLookup());
        t.putString("click_action", "none:" + payload);
        img.readData(t, ClientQuestFile.INSTANCE.holderLookup());
        EditObjectMessage.sendToServer(img);
    }

    private static void openTextEditor(QuestScreen screen, CanvasElement.Text start, java.util.function.Consumer<CanvasElement.Text> done) {
        String[] text = {start.text()};
        String[] font = {start.font()};
        double[] scale = {start.scale()};
        ConfigGroup group = new ConfigGroup("quest_clusters_editor.canvas_text", accepted -> {
            EditorActions.reopen(screen);
            if (accepted && !text[0].isBlank()) {
                done.accept(new CanvasElement.Text(text[0], font[0], scale[0]));
            }
        });
        group.addString("text", text[0], v -> text[0] = v, "");
        List<String> fonts = fonts();
        if (!fonts.contains(font[0])) fonts.add(font[0]);
        group.addEnum("font", font[0], v -> font[0] = v, NameMap.of(CanvasElement.DEFAULT_FONT, fonts).create());
        group.addDouble("scale", scale[0], v -> scale[0] = v, 1.0, 0.05, 10.0);
        new EditConfigScreen(group).openGui();
    }

    /** Every font in the loaded resource packs (except font/include/), plus the default. */
    public static List<String> fonts() {
        TreeSet<String> out = new TreeSet<>();
        out.add(CanvasElement.DEFAULT_FONT);
        Minecraft.getInstance().getResourceManager().listResources("font", rl -> rl.getPath().endsWith(".json")).keySet()
                .forEach(rl -> {
                    String p = rl.getPath();
                    if (!p.startsWith("font/include/")) {
                        out.add(rl.getNamespace() + ":" + p.substring(5, p.length() - 5));
                    }
                });
        return new ArrayList<>(out);
    }

    // ------------------------------------------------------------------ transform

    public static final int ROTATE_CW = 0, ROTATE_CCW = 1, MIRROR_H = 2, MIRROR_V = 3;

    /** Rotates or mirrors the selected objects' positions around their bounding box centre. */
    public static boolean transform(QuestScreen screen, int mode) {
        QuestScreenAccessor acc = (QuestScreenAccessor) screen;
        Chapter chapter = acc.quest_clusters$getSelectedChapter();
        List<Movable> sel = new ArrayList<>(new LinkedHashSet<>(acc.quest_clusters$getSelectedObjects()));
        sel.removeIf(m -> m == null || m.getChapter() != chapter);
        if (!ClientQuestFile.INSTANCE.canEdit()) return false;
        if (sel.size() < 2) {
            toast("transform.too_few");
            return true;
        }
        if (sel.stream().anyMatch(m -> !(m instanceof Quest || m instanceof QuestLink || m instanceof ChapterImage))) {
            toast("transform.unsupported");
            return true;
        }
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Movable m : sel) {
            minX = Math.min(minX, m.getX() - m.getWidth() / 2);
            maxX = Math.max(maxX, m.getX() + m.getWidth() / 2);
            minY = Math.min(minY, m.getY() - m.getHeight() / 2);
            maxY = Math.max(maxY, m.getY() + m.getHeight() / 2);
        }
        double cx = (minX + maxX) / 2, cy = (minY + maxY) / 2;
        for (Movable m : sel) {
            double x = m.getX(), y = m.getY();
            double[] n = transformed(mode, cx, cy, x, y);
            m.requestMove(chapter, n[0], n[1]);
        }
        return true;
    }

    static double[] transformed(int mode, double cx, double cy, double x, double y) {
        return switch (mode) {
            case ROTATE_CW -> new double[]{cx - (y - cy), cy + (x - cx)};
            case ROTATE_CCW -> new double[]{cx + (y - cy), cy - (x - cx)};
            case MIRROR_H -> new double[]{2 * cx - x, y};
            default -> new double[]{x, 2 * cy - y};
        };
    }

    // ------------------------------------------------------------------ multi copy / paste

    /** @return true if handled (2+ quests/images selected) */
    public static boolean copy(QuestScreen screen) {
        List<Movable> sel = ((QuestScreenAccessor) screen).quest_clusters$getSelectedObjects();
        if (sel.size() < 2) return false;
        if (sel.stream().anyMatch(m -> !(m instanceof Quest || m instanceof ChapterImage))) {
            toast("copy.unsupported");
            return true;
        }
        double minX = sel.stream().mapToDouble(Movable::getX).min().orElse(0);
        double minY = sel.stream().mapToDouble(Movable::getY).min().orElse(0);
        List<ClipEntry> out = new ArrayList<>();
        for (Movable m : sel) out.add(new ClipEntry(m.getMovableID(), m instanceof Quest, m.getX() - minX, m.getY() - minY));
        clipboard = List.copyOf(out);
        Minecraft.getInstance().keyboardHandler.setClipboard(CLIPBOARD_MARKER);
        SimpleToast.info(tr("copy.done", out.size()), Component.empty());
        return true;
    }

    public static boolean hasMultiClipboard() {
        return clipboard.size() > 1 && CLIPBOARD_MARKER.equals(Minecraft.getInstance().keyboardHandler.getClipboard());
    }

    public static int clipboardSize() {
        return clipboard.size();
    }

    /** @return true if handled */
    public static boolean paste(Chapter target, double x, double y, boolean withDeps) {
        if (!hasMultiClipboard() || target == null) return false;
        for (ClipEntry e : clipboard) {
            if (e.quest()) {
                NetworkManager.sendToServer(new CopyQuestMessage(e.id(), target.id, x + e.dx(), y + e.dy(), withDeps));
            } else {
                NetworkManager.sendToServer(new CopyChapterImageMessage(e.id(), target.id, x + e.dx(), y + e.dy()));
            }
        }
        return true;
    }

    /** Copying a quest link copies the quest it points to. Returns true if handled. */
    public static boolean copyLink(QuestLink link) {
        var quest = link.getQuest();
        if (quest.isEmpty()) {
            toast("copy.link_target_missing");
            return true;
        }
        quest.get().copyToClipboard();
        SimpleToast.info(Component.translatable("ftbquests.quest.copied"), Component.literal(quest.get().getTitle().getString()));
        return true;
    }

    private static void toast(String key) {
        SimpleToast.error(tr(key), Component.empty());
    }
}
