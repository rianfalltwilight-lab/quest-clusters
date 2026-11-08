package com.questclusters.editor.client.desc;

import com.questclusters.editor.data.DescriptionTable;
import dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.translation.TranslationKey;

import java.util.List;

/** Editing raw description lines of the shown quest. */
public final class DescriptionLines {
    private DescriptionLines() {
    }

    /** Replaces (or with null removes) the first raw line holding this table payload. */
    public static void replace(Quest quest, ViewQuestPanel view, String tablePayload, String newLine) {
        quest.modifyTranslatableListValue(TranslationKey.QUEST_DESC, list -> {
            for (int i = 0; i < list.size(); i++) {
                if (tablePayload.equals(DescriptionTable.payloadOf(list.get(i)))) {
                    if (newLine == null) list.remove(i);
                    else list.set(i, newLine);
                    return;
                }
            }
        });
        reopen(view);
    }

    /** Replaces a raw line by index ({@code -1} or out of range: appended). */
    public static void set(Quest quest, ViewQuestPanel view, int line, String newLine) {
        quest.modifyTranslatableListValue(TranslationKey.QUEST_DESC, list -> {
            if (line >= 0 && line < list.size()) list.set(line, newLine);
            else list.add(newLine);
        });
        reopen(view);
    }

    public static void append(Quest quest, ViewQuestPanel view, List<String> lines) {
        quest.modifyTranslatableListValue(TranslationKey.QUEST_DESC, list -> list.addAll(lines));
        reopen(view);
    }

    static void reopen(ViewQuestPanel view) {
        if (view != null) {
            view.getGui().openGui();
            view.refreshWidgets();
        }
    }
}
