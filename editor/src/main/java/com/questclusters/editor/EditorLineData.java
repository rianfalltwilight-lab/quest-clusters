package com.questclusters.editor;

import com.questclusters.api.LineData;
import com.questclusters.editor.client.ClientEditor;
import com.questclusters.editor.data.BookExtras;
import com.questclusters.editor.data.CanvasElement;
import com.questclusters.editor.data.ChapterExtras;
import com.questclusters.editor.server.ServerEditor;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestLink;

import java.util.List;
import java.util.Optional;

/** Serves the editor's lines to Quest Clusters core (anchors following clusters, merged-line hiding). */
final class EditorLineData implements LineData {
    static BookExtras bookFor(Chapter chapter) {
        return chapter != null && chapter.getQuestFile().isServerSide() ? ServerEditor.book() : ClientEditor.book();
    }

    @Override
    public List<List<String>> decorativeLines(Chapter chapter) {
        return chapter == null ? List.of() : bookFor(chapter).chapter(chapter.id).decorativeLines;
    }

    @Override
    public Optional<String> anchorKey(ChapterImage image) {
        return CanvasElement.parse(image.getClickAction().actionData()) instanceof CanvasElement.Anchor a
                ? Optional.ofNullable(a.nodeKey()) : Optional.empty();
    }

    @Override
    public Optional<Boolean> hiddenLine(Chapter chapter, Movable dependent, Movable dependency) {
        String a = key(dependent), b = key(dependency);
        if (chapter == null || a == null || b == null) return Optional.empty();
        ChapterExtras c = bookFor(chapter).chapter(chapter.id);
        return c.hiddenLines.isEmpty() ? Optional.empty() : c.hiddenLine(a, b).map(ChapterExtras.HiddenLine::revealOnHover);
    }

    static String key(Movable m) {
        if (m instanceof Quest q) return LineData.questNode(q.id);
        if (m instanceof QuestLink l) return LineData.linkNode(l.id);
        return null;
    }
}
