package com.questclusters.editor.migration;

import com.questclusters.editor.data.BookExtras;
import com.questclusters.editor.data.QuestEnhanceImport;
import com.questclusters.editor.server.EditorStorage;
import com.questclusters.migration.Migration;
import com.questclusters.migration.MigrationContext;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;

/**
 * Imports Quest Enhance's data (keys inside FTB Quests' chapter files and data.snbt) into the editor's own files,
 * and recovers canvas elements that 1.20-era books stored in the legacy {@code click} key (FTB Quests 2101 reads that
 * key but drops anything it does not know, so such text and guide points would otherwise turn into plain images).
 * The original keys are left untouched: FTB Quests drops them by itself the next time it saves a chapter.
 */
public final class QuestEnhanceMigration implements Migration {
    public static final String ID = "quest_clusters_editor:import_quest_enhance_v1";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Import Quest Enhance quest book data (backgrounds, decorative and hidden lines, chapter prerequisites, ...)";
    }

    @Override
    public boolean isNeeded(MigrationContext ctx) {
        return QuestEnhanceImport.hasData(ctx.rawFileData(), ctx.rawChapters());
    }

    @Override
    public void run(MigrationContext ctx) throws java.io.IOException {
        QuestEnhanceImport.Result r = QuestEnhanceImport.read(ctx.rawFileData(), ctx.rawChapters());
        BookExtras existing = EditorStorage.load(ctx.questsFolder());
        QuestEnhanceImport.mergeInto(r.book(), existing);

        // on disk first: the book may be reloaded from disk before FTB Quests saves the objects fixed below
        java.util.Set<String> files = new java.util.HashSet<>();
        //? if neoforge {
        r.legacyClicks().forEach(c -> files.add(c.chapterFile()));
        //?}
        // (FTB Quests for 1.20.1 still reads 'click' itself: nothing to move there)
        for (String name : files) {
            java.nio.file.Path path = ctx.questsFolder().resolve("chapters").resolve(name + ".snbt");
            dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag raw = dev.ftb.mods.ftblibrary.snbt.SNBT.read(path);
            if (raw != null && QuestEnhanceImport.fixLegacyClicks(raw) > 0 && !dev.ftb.mods.ftblibrary.snbt.SNBT.write(path, raw)) {
                throw new java.io.IOException("could not rewrite " + path);
            }
        }
        int fixed = 0;
        if (ctx.questFile() instanceof BaseQuestFile file) {
            for (QuestEnhanceImport.LegacyClick c : r.legacyClicks()) {
                ChapterImage img = com.questclusters.platform.Ftbq.image(file, c.imageId());
                if (img != null && com.questclusters.editor.compat.Images.noClick(img)) {
                    com.questclusters.editor.compat.Images.setElement(img, c.data());
                    fixed++;
                }
            }
            if (fixed > 0 && file instanceof ServerQuestFile sf) {
                sf.markDirty();
            }
        }
        // the importer runs before the module loads its data, so write the files now; ids of objects that are gone are
        // dropped by the normal prune on load
        BaseQuestFile file = ctx.questFile() instanceof BaseQuestFile f ? f : null;
        EditorStorage.save(ctx.questsFolder(), existing, id -> {
            if (file == null) return 1L;
            if (file.getBase(id) instanceof dev.ftb.mods.ftbquests.quest.Quest q) return q.getChapter().id;
            ChapterImage img = com.questclusters.platform.Ftbq.image(file, id);
            return img != null ? img.getChapter().id : 0L;
        });
        ctx.log("imported " + r.stats() + (fixed > 0 ? "; restored " + fixed + " legacy canvas element(s)" : ""));
    }
}
