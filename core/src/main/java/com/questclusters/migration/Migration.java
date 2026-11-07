package com.questclusters.migration;

/**
 * A one-time data migration, e.g. importing another addon's quest book data after it was replaced.
 * Runs on the server right after FTB Quests loaded the quest book, before Quest Clusters loads its own data.
 * A successful run is recorded and never repeated; a failed or unneeded one is checked again next start.
 */
public interface Migration {
    /** Stable id, e.g. "quest_clusters_editor:import_quest_enhance_v1". */
    String id();

    /** One line for logs and /questclusters migrate status. */
    String description();

    /** Cheap check whether there is anything to migrate (e.g. old keys present in the raw files). */
    boolean isNeeded(MigrationContext ctx) throws Exception;

    /** Do the migration. Throwing marks it failed: it is not recorded and is retried next start. */
    void run(MigrationContext ctx) throws Exception;
}
