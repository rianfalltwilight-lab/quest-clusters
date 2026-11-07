package com.questclusters.migration;

import net.minecraft.nbt.Tag;

/**
 * Dev self-test only (registered when -Dquest_clusters.devtest is set): exercises the migration pipeline on the
 * fixture's Quest Enhance keys without changing anything, the way the phase E importer will read them.
 */
public final class DevProbeMigration implements Migration {
    public static final String ID = "quest_clusters:devtest_probe";
    public static int linesSeen = -1;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Dev self-test: read Quest Enhance decorative lines from the raw chapter files";
    }

    @Override
    public boolean isNeeded(MigrationContext ctx) {
        return ctx.anyChapterHasKey("quest_enhance_decorative_lines");
    }

    @Override
    public void run(MigrationContext ctx) {
        int n = 0;
        for (var t : ctx.rawChapters().values()) {
            n += t.getList("quest_enhance_decorative_lines", Tag.TAG_COMPOUND).size();
        }
        linesSeen = n;
        ctx.log("found " + n + " Quest Enhance decorative line(s)");
    }
}
