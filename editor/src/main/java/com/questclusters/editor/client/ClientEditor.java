package com.questclusters.editor.client;

import com.questclusters.editor.data.BookExtras;
import com.questclusters.editor.data.ExtrasNbt;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

/** Client copy of the editor data, replaced whole by every sync. */
public final class ClientEditor {
    private static BookExtras book = new BookExtras();
    private static int revision;
    private static final Map<String, Icon> ICONS = new HashMap<>();

    private ClientEditor() {
    }

    public static BookExtras book() {
        return book;
    }

    public static int revision() {
        return revision;
    }

    public static void replace(CompoundTag data) {
        BookExtras old = book;
        book = ExtrasNbt.readAll(data);
        revision++;
        // backgrounds and lines are read live while drawing; only chapter visibility is cached by FTB Quests,
        // so refresh (never rebuild the canvas: that would break drags and open menus) when prerequisites changed
        if (ClientQuestFile.INSTANCE != null && !prerequisites(old).equals(prerequisites(book))) {
            ClientQuestFile.INSTANCE.clearCachedData();
            ClientQuestFile.INSTANCE.getQuestScreen().ifPresent(s -> s.refreshChapterPanel());
        }
    }

    private static Map<Long, java.util.List<Long>> prerequisites(BookExtras b) {
        Map<Long, java.util.List<Long>> m = new HashMap<>();
        b.chapters.forEach((id, c) -> {
            if (!c.prerequisites.isEmpty()) m.put(id, c.prerequisites);
        });
        return m;
    }

    public static void clear() {
        book = new BookExtras();
        revision++;
    }

    /** Cached icon for a stored resource location; empty icon for "". */
    public static Icon icon(String location) {
        if (location == null || location.isEmpty()) return Icon.empty();
        return ICONS.computeIfAbsent(location, Icon::getIcon);
    }
}
