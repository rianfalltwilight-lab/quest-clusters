package com.questclusters.migration;

import java.util.ArrayList;
import java.util.List;

/** Modules register their migrations here during mod construction. */
public final class MigrationRegistry {
    private static final List<Migration> MIGRATIONS = new ArrayList<>();

    private MigrationRegistry() {
    }

    public static synchronized void register(Migration m) {
        if (MIGRATIONS.stream().anyMatch(x -> x.id().equals(m.id()))) {
            throw new IllegalArgumentException("Duplicate migration id " + m.id());
        }
        MIGRATIONS.add(m);
    }

    public static synchronized List<Migration> all() {
        return List.copyOf(MIGRATIONS);
    }
}
